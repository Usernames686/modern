import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawn } from "node:child_process";

const adminBase = process.env.CRMEB_ADMIN_WEB || "http://127.0.0.1:19527";
const appBase = process.env.CRMEB_APP_WEB || "http://127.0.0.1:19528";
const chromePath = process.env.CHROME_PATH || "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
const remotePort = Number(process.env.CRMEB_CHROME_DEBUG_PORT || 9223);
const account = process.env.CRMEB_ADMIN_ACCOUNT || "admin";
const pwd = process.env.CRMEB_ADMIN_PWD || "123456";

if (!fs.existsSync(chromePath)) {
  throw new Error(`Chrome not found: ${chromePath}`);
}

async function requestJson(url, options = {}) {
  const response = await fetch(url, options);
  const text = await response.text();
  try {
    return JSON.parse(text);
  } catch {
    throw new Error(`Expected JSON from ${url}, got: ${text.slice(0, 200)}`);
  }
}

function assertApiOk(name, body) {
  if (body?.code !== 200) {
    throw new Error(`${name} failed: ${JSON.stringify({
      code: body?.code,
      message: body?.message,
      raw: body?.raw
    })}`);
  }
  return body.data;
}

function firstListRow(name, data) {
  const row = Array.isArray(data?.list) ? data.list[0] : null;
  if (!row?.id) {
    throw new Error(`${name} returned no row with id`);
  }
  return row;
}

async function waitForDebugger() {
  const endpoint = `http://127.0.0.1:${remotePort}/json/version`;
  const deadline = Date.now() + 10000;
  let lastError;
  while (Date.now() < deadline) {
    try {
      const data = await requestJson(endpoint);
      if (data.webSocketDebuggerUrl) {
        return data.webSocketDebuggerUrl;
      }
    } catch (error) {
      lastError = error;
      await new Promise((resolve) => setTimeout(resolve, 200));
    }
  }
  throw lastError || new Error("Chrome debugger endpoint not ready");
}

async function createPageDebugger() {
  const response = await fetch(`http://127.0.0.1:${remotePort}/json/new?about:blank`, { method: "PUT" });
  if (response.ok) {
    const target = await response.json();
    if (target.webSocketDebuggerUrl) {
      return target.webSocketDebuggerUrl;
    }
  }
  const targets = await requestJson(`http://127.0.0.1:${remotePort}/json/list`);
  const page = targets.find((item) => item.type === "page" && item.webSocketDebuggerUrl);
  if (!page) {
    throw new Error("No Chrome page target found");
  }
  return page.webSocketDebuggerUrl;
}

class CdpClient {
  constructor(url) {
    this.url = url;
    this.nextId = 1;
    this.pending = new Map();
    this.events = [];
    this.networkFailures = [];
  }

  async connect() {
    this.socket = new WebSocket(this.url);
    this.socket.addEventListener("message", (event) => this.handleMessage(event));
    await new Promise((resolve, reject) => {
      this.socket.addEventListener("open", resolve, { once: true });
      this.socket.addEventListener("error", reject, { once: true });
    });
  }

  handleMessage(event) {
    const message = JSON.parse(event.data);
    if (message.id && this.pending.has(message.id)) {
      const { resolve, reject } = this.pending.get(message.id);
      this.pending.delete(message.id);
      if (message.error) {
        reject(new Error(message.error.message || JSON.stringify(message.error)));
      } else {
        resolve(message.result || {});
      }
      return;
    }
    if (message.method) {
      this.events.push(message);
      if (message.method === "Network.responseReceived" && Number(message.params?.response?.status || 0) >= 400) {
        this.networkFailures.push({
          status: message.params.response.status,
          url: message.params.response.url,
          type: message.params.type
        });
      }
    }
  }

  send(method, params = {}) {
    const id = this.nextId++;
    this.socket.send(JSON.stringify({ id, method, params }));
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject });
      setTimeout(() => {
        if (this.pending.has(id)) {
          this.pending.delete(id);
          reject(new Error(`${method} timed out`));
        }
      }, 15000);
    });
  }

  async evaluate(expression) {
    const result = await this.send("Runtime.evaluate", {
      expression,
      awaitPromise: true,
      returnByValue: true
    });
    if (result.exceptionDetails) {
      throw new Error(result.exceptionDetails.text || "Runtime evaluation failed");
    }
    return result.result?.value;
  }

  close() {
    this.socket?.close();
  }
}

async function waitFor(client, expression, label, timeout = 15000) {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    const ok = await client.evaluate(`Boolean(${expression})`).catch(() => false);
    if (ok) {
      return;
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error(`Timed out waiting for ${label}`);
}

async function navigate(client, url) {
  await client.send("Page.navigate", { url });
  await waitFor(client, "document.readyState === 'complete' || document.querySelector('#app')?.children.length", `page load ${url}`);
  await new Promise((resolve) => setTimeout(resolve, 800));
}

function consoleFailures(events) {
  return events
    .filter((event) => event.method === "Runtime.exceptionThrown" || event.method === "Log.entryAdded")
    .map((event) => ({
      method: event.method,
      text: event.params?.entry?.text || event.params?.exceptionDetails?.text || event.params?.exceptionDetails?.exception?.description || "",
      url: event.params?.entry?.url || ""
    }))
    .filter((event) => (
      !event.url.includes("/favicon.ico")
      && !/\.(png|jpe?g|gif|webp|svg)(\?|$)/i.test(event.url)
    ))
    .filter((event) => {
      const text = event.text || "";
      return event.method === "Runtime.exceptionThrown"
        || /(^|\s)(error|failed|uncaught|exception)(\s|:)/i.test(text);
    });
}

function flattenMenus(items, parents = [], out = []) {
  for (const item of items || []) {
    const chain = [...parents, item.name].filter(Boolean);
    if (item.childList?.length) {
      flattenMenus(item.childList, chain, out);
    } else if (item.component) {
      out.push({
        name: item.name,
        component: item.component,
        chain: chain.join(" / ")
      });
    }
  }
  return out;
}

function waitForProcessExit(child, timeout = 3000) {
  if (child.exitCode !== null || child.signalCode !== null) {
    return Promise.resolve();
  }
  return new Promise((resolve) => {
    const timer = setTimeout(resolve, timeout);
    child.once("exit", () => {
      clearTimeout(timer);
      resolve();
    });
  });
}

async function removeTmpDir(dir) {
  for (let attempt = 0; attempt < 5; attempt += 1) {
    try {
      fs.rmSync(dir, { recursive: true, force: true, maxRetries: 3, retryDelay: 100 });
      return;
    } catch (error) {
      if (attempt === 4) {
        console.warn(`warning: failed to remove temp Chrome profile ${dir}: ${error.message}`);
        return;
      }
      await new Promise((resolve) => setTimeout(resolve, 250));
    }
  }
}

async function assertNoBrokenPage(client, label) {
  const state = await client.evaluate(`(() => ({
    appChildren: document.querySelector('#app')?.children.length || 0,
    text: document.body.innerText.slice(0, 600),
    hasPlaceholder: Boolean(document.querySelector('.module-placeholder')),
    hasLoadingOnly: /加载中\\.\\.\\.$/.test(document.body.innerText.trim())
  }))()`);
  if (!state.appChildren) {
    throw new Error(`${label}: Vue app did not mount`);
  }
  if (state.hasPlaceholder) {
    throw new Error(`${label}: hit module-placeholder`);
  }
  if (state.hasLoadingOnly) {
    throw new Error(`${label}: page still only shows loading`);
  }
  return state;
}

const login = await requestJson(`${adminBase}/api/admin/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account, pwd })
});
if (login.code !== 200 || !login.data?.token) {
  throw new Error(`admin login failed: ${JSON.stringify(login)}`);
}

const menuResult = await requestJson(`${adminBase}/api/admin/getMenus`, {
  headers: { "Authori-zation": login.data.token }
});
if (menuResult.code !== 200) {
  throw new Error(`admin menus failed: ${JSON.stringify(menuResult)}`);
}
const adminMenuLeaves = flattenMenus(menuResult.data || []);
if (adminMenuLeaves.length < 50) {
  throw new Error(`admin menu leaf count too low: ${adminMenuLeaves.length}`);
}

const frontLogin = await requestJson(`${appBase}/api/front/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account: "18888888888", password: "a123456" })
}).catch(() => null);
const frontToken = frontLogin?.code === 200 ? frontLogin.data?.token || "" : "";

const adminAuthHeaders = { "Authori-zation": login.data.token };
const adminProduct = firstListRow(
  "admin product list",
  assertApiOk("admin product list", await requestJson(`${adminBase}/api/admin/store/product/list?page=1&limit=1&type=1`, {
    headers: adminAuthHeaders
  }))
);
const seckillProduct = firstListRow(
  "admin seckill product list",
  assertApiOk("admin seckill product list", await requestJson(`${adminBase}/api/admin/store/seckill/list?page=1&limit=1`, {
    headers: adminAuthHeaders
  }))
);
const bargainProduct = firstListRow(
  "admin bargain product list",
  assertApiOk("admin bargain product list", await requestJson(`${adminBase}/api/admin/store/bargain/list?page=1&limit=1`, {
    headers: adminAuthHeaders
  }))
);
const combinationProduct = firstListRow(
  "admin combination product list",
  assertApiOk("admin combination product list", await requestJson(`${adminBase}/api/admin/store/combination/list?page=1&limit=1`, {
    headers: adminAuthHeaders
  }))
);
const frontProduct = firstListRow(
  "front product list",
  assertApiOk("front product list", await requestJson(`${appBase}/api/front/products?page=1&limit=1`))
);

const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "crmeb-chrome-"));
const chrome = spawn(chromePath, [
  "--headless=new",
  "--disable-gpu",
  "--no-first-run",
  "--no-default-browser-check",
  "--disable-dev-shm-usage",
  `--remote-debugging-port=${remotePort}`,
  `--user-data-dir=${tmpDir}`,
  "about:blank"
], { stdio: "ignore" });

let client;
try {
  await waitForDebugger();
  client = new CdpClient(await createPageDebugger());
  await client.connect();
  await client.send("Page.enable");
  await client.send("Runtime.enable");
  await client.send("Log.enable");
  await client.send("Network.enable");
  await client.send("Emulation.setDeviceMetricsOverride", {
    width: 1366,
    height: 900,
    deviceScaleFactor: 1,
    mobile: false
  });

  const adminPages = [
    ["/", "控制台"],
    ["/order/index", "订单"],
    ["/store/index", "商品"],
    ["/store/list/creatProduct", "商品名称"],
    [`/store/list/creatProduct/${adminProduct.id}`, "商品名称"],
    [`/store/list/creatProduct/${adminProduct.id}/1`, "规格库存"],
    [`/marketing/seckill/creatSeckill/updeta/${seckillProduct.productId || 0}/${seckillProduct.id}`, "秒杀"],
    [`/marketing/groupBuy/creatGroup/${combinationProduct.id}`, "拼团"],
    [`/marketing/bargain/creatBargain/${bargainProduct.id}`, "砍价"],
    ["/design/devise", "装修"],
    ["/user/index", "用户"]
  ];

  const appPages = [
    { pathName: `/?token=${frontToken}`, expected: "推荐商品" },
    {
      pathName: `/pages/goods/goods_details/index?id=${frontProduct.id}${frontToken ? `&token=${frontToken}` : ""}`,
      expected: "加入购物车",
      selector: ".detail-panel"
    },
    { pathName: `/pages/goods_cate/goods_cate${frontToken ? `?token=${frontToken}` : ""}`, expected: "分类" },
    { pathName: `/pages/goods/goods_search/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "搜索" },
    { pathName: `/pages/goods/goods_list/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "推荐商品" },
    { pathName: `/pages/activity/goods_seckill/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "限时秒杀", selector: ".seckill-view" },
    { pathName: `/pages/activity/goods_combination/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "拼团活动", selector: ".combination-view" },
    { pathName: `/pages/activity/goods_bargain/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "砍价活动", selector: ".bargain-view" },
    { pathName: `/pages/activity/couponList/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "领取优惠券", selector: ".coupon-center-view" },
    { pathName: `/pages/news/news_list/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "消息资讯", selector: ".article-list-view" },
    { pathName: `/pages/users/kefu/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "联系客服", selector: ".customer-service-view" },
    { pathName: `/pages/order_addcart/order_addcart${frontToken ? `?token=${frontToken}` : ""}`, expected: "购物车", selector: ".cart-view" },
    { pathName: `/pages/users/order_list/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "我的订单", selector: ".order-list-view" },
    { pathName: `/pages/users/user_return_list/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "退货列表", selector: ".refund-list-view" },
    { pathName: `/pages/users/user_goods_collection/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "收藏商品", selector: ".collection-view" },
    { pathName: `/pages/users/user_coupon/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "我的优惠券", selector: ".user-coupon-view" },
    { pathName: `/pages/users/user_money/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "我的余额", selector: ".balance-view" },
    { pathName: `/pages/users/user_bill/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "账单记录", selector: ".bill-view" },
    { pathName: `/pages/users/user_integral/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "积分中心", selector: ".integral-view" },
    { pathName: `/pages/users/user_sgin/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "签到", selector: ".sign-view" },
    { pathName: `/pages/users/user_sgin_list/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "签到明细", selector: ".sign-record-view" },
    { pathName: `/pages/infos/user_vip/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "会员", selector: ".member-level-view" },
    { pathName: `/pages/promoter/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "当前佣金", selector: ".spread-view" },
    { pathName: `/pages/user/index${frontToken ? `?token=${frontToken}` : ""}`, expected: "我的服务", selector: ".profile-view" }
  ];

  const checked = [];
  const checkedAdminPaths = new Set();
  await navigate(client, adminBase);
  await client.evaluate(`localStorage.setItem('Authori-zation', ${JSON.stringify(login.data.token)})`);
  await navigate(client, `${adminBase}/`);
  for (const item of adminMenuLeaves) {
    await navigate(client, `${adminBase}${item.component}`);
    await waitFor(client, "document.querySelector('.legacy-main') || document.querySelector('.page-account')", `admin menu shell ${item.component}`);
    await assertNoBrokenPage(client, `admin menu ${item.chain || item.name} ${item.component}`);
    checkedAdminPaths.add(item.component);
    checked.push(`admin-menu:${item.component}`);
  }

  for (const [pathName, expected] of adminPages) {
    if (checkedAdminPaths.has(pathName === "/" ? "/dashboard" : pathName)) {
      continue;
    }
    await navigate(client, `${adminBase}${pathName}`);
    await waitFor(client, "document.querySelector('.legacy-main') || document.querySelector('.page-account')", `admin shell ${pathName}`);
    const state = await assertNoBrokenPage(client, `admin ${pathName}`);
    if (!state.text.includes(expected) && !(pathName === "/" && state.text.includes("后台服务"))) {
      throw new Error(`admin ${pathName}: expected text ${expected}; got ${JSON.stringify(state.text.slice(0, 220))}`);
    }
    checked.push(`admin:${pathName}`);
  }

  for (const { pathName, expected, selector } of appPages) {
    await navigate(client, `${appBase}${pathName}`);
    await waitFor(client, "document.querySelector('.app-shell')", `app shell ${pathName}`);
    if (selector) {
      await waitFor(client, `document.querySelector(${JSON.stringify(selector)})`, `app selector ${selector} ${pathName}`);
    }
    const state = await assertNoBrokenPage(client, `app ${pathName}`);
    const fullText = selector
      ? await client.evaluate(`document.querySelector(${JSON.stringify(selector)})?.innerText || ""`)
      : state.text;
    if (!fullText.includes(expected)) {
      throw new Error(`app ${pathName}: expected text ${expected}; got ${JSON.stringify(state.text.slice(0, 220))}`);
    }
    checked.push(`app:${pathName}`);
  }

  const networkFailures = client.networkFailures.filter((item) => (
    !item.url.includes("/favicon.ico") && item.type !== "Image"
  ));
  if (networkFailures.length) {
    throw new Error(`browser network failures: ${JSON.stringify(networkFailures.slice(0, 20), null, 2)}`);
  }

  const failures = consoleFailures(client.events);
  if (failures.length) {
    throw new Error(`browser console failures: ${JSON.stringify(failures.slice(0, 10), null, 2)}`);
  }

  console.log(JSON.stringify({
    browserSmokeOk: true,
    adminBase,
    appBase,
    adminMenuLeaves: adminMenuLeaves.length,
    frontLoggedIn: Boolean(frontToken),
    samples: {
      adminProductId: adminProduct.id,
      frontProductId: frontProduct.id,
      seckillId: seckillProduct.id,
      bargainId: bargainProduct.id,
      combinationId: combinationProduct.id
    },
    checked
  }, null, 2));
} finally {
  if (client) {
    await client.send("Browser.close").catch(() => {});
    client.close();
  }
  chrome.kill("SIGTERM");
  await waitForProcessExit(chrome);
  await removeTmpDir(tmpDir);
}
