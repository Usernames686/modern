import fs from "node:fs";
import net from "node:net";
import os from "node:os";
import path from "node:path";
import { randomUUID } from "node:crypto";
import { spawn } from "node:child_process";

const adminBase = process.env.CRMEB_ADMIN_WEB || "http://127.0.0.1:19527";
const appBase = process.env.CRMEB_APP_WEB || "http://127.0.0.1:19528";
const chromePath = process.env.CHROME_PATH || "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
const remotePort = Number(process.env.CRMEB_CHROME_DEBUG_PORT || 9224);
const account = process.env.CRMEB_ADMIN_ACCOUNT || "admin";
const pwd = process.env.CRMEB_ADMIN_PWD || "123456";
const frontAccount = process.env.CRMEB_FRONT_ACCOUNT || "18888888888";
const frontPwd = process.env.CRMEB_FRONT_PWD || "a123456";
const redisHost = process.env.CRMEB_REDIS_HOST || "127.0.0.1";
const redisPort = Number(process.env.CRMEB_REDIS_PORT || 6379);
const redisPassword = process.env.CRMEB_REDIS_PASSWORD || "";
const redisDatabase = Number(process.env.CRMEB_REDIS_DATABASE || 6);
const frontTokenPrefix = "crmeb-modern:front:token:";

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

function encodeRedisCommand(args) {
  return `*${args.length}\r\n${args.map((arg) => {
    const value = String(arg);
    return `$${Buffer.byteLength(value)}\r\n${value}\r\n`;
  }).join("")}`;
}

function parseRedisResponse(buffer) {
  const type = buffer[0];
  const text = buffer.toString("utf8");
  if (type === 43) {
    return text.slice(1, text.indexOf("\r\n"));
  }
  if (type === 58) {
    return Number(text.slice(1, text.indexOf("\r\n")));
  }
  if (type === 36) {
    const firstLineEnd = text.indexOf("\r\n");
    const length = Number(text.slice(1, firstLineEnd));
    if (length < 0) return null;
    return text.slice(firstLineEnd + 2, firstLineEnd + 2 + length);
  }
  if (type === 45) {
    throw new Error(`Redis error: ${text.slice(1, text.indexOf("\r\n"))}`);
  }
  return text.trim();
}

async function redisCommand(args) {
  const socket = net.createConnection({ host: redisHost, port: redisPort });
  await new Promise((resolve, reject) => {
    socket.once("connect", resolve);
    socket.once("error", reject);
  });
  const commands = [];
  if (redisPassword) {
    commands.push(["AUTH", redisPassword]);
  }
  commands.push(["SELECT", redisDatabase], args);
  let result;
  try {
    for (const command of commands) {
      result = await new Promise((resolve, reject) => {
        const chunks = [];
        const timer = setTimeout(() => reject(new Error(`Redis command timed out: ${command[0]}`)), 5000);
        const onData = (chunk) => {
          chunks.push(chunk);
          const buffer = Buffer.concat(chunks);
          if (buffer.includes("\r\n")) {
            clearTimeout(timer);
            socket.off("data", onData);
            try {
              resolve(parseRedisResponse(buffer));
            } catch (error) {
              reject(error);
            }
          }
        };
        socket.on("data", onData);
        socket.write(encodeRedisCommand(command));
      });
    }
    return result;
  } finally {
    socket.end();
  }
}

async function setTemporaryFrontToken(uid) {
  const token = `browser-smoke-${randomUUID().replaceAll("-", "")}`;
  await redisCommand(["SETEX", `${frontTokenPrefix}${token}`, "600", uid]);
  return token;
}

async function deleteTemporaryFrontToken(token) {
  if (token) {
    await redisCommand(["DEL", `${frontTokenPrefix}${token}`]).catch((error) => {
      console.warn(`warning: failed to delete temporary front token: ${error.message}`);
    });
  }
}

async function createTemporaryTokenFromAdminOrder(status, token) {
  const orders = await requestJson(`${adminBase}/api/admin/store/order/list?page=1&limit=1&status=${encodeURIComponent(status)}&type=2`, {
    headers: { "Authori-zation": token }
  });
  const sample = orders.data?.list?.[0];
  if (!sample?.uid || !sample?.orderId) {
    return null;
  }
  const temporaryToken = await setTemporaryFrontToken(sample.uid);
  return {
    orderNo: sample.orderId,
    token: temporaryToken,
    tokenSource: `temporary-redis-token-for-uid-${sample.uid}`,
    temporaryToken
  };
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
      throw new Error(result.exceptionDetails.text || result.exceptionDetails.exception?.description || "Runtime evaluation failed");
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

async function clickText(client, rootSelector, text, label) {
  const clicked = await client.evaluate(`(() => {
    const root = document.querySelector(${JSON.stringify(rootSelector)}) || document;
    const nodes = Array.from(root.querySelectorAll('button, a, .el-button, .el-radio-button, .el-tabs__item, .el-dropdown-link, .el-dropdown-menu__item, article, [role="button"]'));
    const target = nodes.find((node) => (node.innerText || node.textContent || '').includes(${JSON.stringify(text)}));
    if (!target) return false;
    target.scrollIntoView({ block: 'center', inline: 'center' });
    target.click();
    return true;
  })()`);
  if (!clicked) {
    const visibleText = await client.evaluate(`(document.querySelector(${JSON.stringify(rootSelector)}) || document).innerText.slice(0, 800)`);
    throw new Error(`${label}: button/text ${text} not found in ${rootSelector}; got ${JSON.stringify(visibleText)}`);
  }
  await new Promise((resolve) => setTimeout(resolve, 500));
}

async function clickFirst(client, selector, label) {
  const clicked = await client.evaluate(`(() => {
    const node = document.querySelector(${JSON.stringify(selector)});
    if (!node) return false;
    node.scrollIntoView({ block: 'center', inline: 'center' });
    node.click();
    return true;
  })()`);
  if (!clicked) {
    throw new Error(`${label}: selector ${selector} not found`);
  }
  await new Promise((resolve) => setTimeout(resolve, 500));
}

async function clickVisibleText(client, text, label) {
  const clicked = await client.evaluate(`(() => {
    const nodes = Array.from(document.querySelectorAll('button, a, .el-button, .el-radio-button, .el-tabs__item, .el-dropdown-link, .el-dropdown-menu__item, [role="button"]'));
    const target = nodes.find((node) => {
      const style = window.getComputedStyle(node);
      const rect = node.getBoundingClientRect();
      return style.display !== 'none'
        && style.visibility !== 'hidden'
        && rect.width > 0
        && rect.height > 0
        && (node.innerText || node.textContent || '').includes(${JSON.stringify(text)});
    });
    if (!target) return false;
    target.scrollIntoView({ block: 'center', inline: 'center' });
    target.click();
    return true;
  })()`);
  if (!clicked) {
    const visibleText = await client.evaluate(`document.body.innerText.slice(0, 1200)`);
    throw new Error(`${label}: visible text ${text} not found; got ${JSON.stringify(visibleText)}`);
  }
  await new Promise((resolve) => setTimeout(resolve, 500));
}

async function clickTextInRow(client, rowText, actionText, label) {
  const clicked = await client.evaluate(`(() => {
    const rows = Array.from(document.querySelectorAll('.el-table__body tr'));
    const row = rows.find((item) => (item.innerText || item.textContent || '').includes(${JSON.stringify(rowText)}));
    if (!row) return false;
    const target = Array.from(row.querySelectorAll('button, a, .el-button, .el-dropdown-link, [role="button"]'))
      .find((node) => (node.innerText || node.textContent || '').includes(${JSON.stringify(actionText)}));
    if (!target) return false;
    target.scrollIntoView({ block: 'center', inline: 'center' });
    target.click();
    return true;
  })()`);
  if (!clicked) {
    const tableText = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.slice(0, 1200) || ''`);
    throw new Error(`${label}: row ${rowText} action ${actionText} not found; got ${JSON.stringify(tableText)}`);
  }
  await new Promise((resolve) => setTimeout(resolve, 500));
}

async function fillInputByPlaceholder(client, placeholder, value, label) {
  const filled = await client.evaluate(`(() => {
    const node = Array.from(document.querySelectorAll('input, textarea')).find((item) => item.getAttribute('placeholder') === ${JSON.stringify(placeholder)});
    if (!node) return false;
    node.focus();
    const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value')?.set
      || Object.getOwnPropertyDescriptor(Object.getPrototypeOf(node), 'value')?.set;
    if (setter) {
      setter.call(node, ${JSON.stringify(value)});
    } else {
      node.value = ${JSON.stringify(value)};
    }
    node.dispatchEvent(new Event('input', { bubbles: true }));
    node.dispatchEvent(new Event('change', { bubbles: true }));
    return true;
  })()`);
  if (!filled) {
    const visibleText = await client.evaluate(`document.body.innerText.slice(0, 1200)`);
    throw new Error(`${label}: input placeholder ${placeholder} not found; got ${JSON.stringify(visibleText)}`);
  }
  await new Promise((resolve) => setTimeout(resolve, 250));
}

async function typeInputByPlaceholder(client, placeholder, value, label) {
  const focused = await client.evaluate(`(() => {
    const node = Array.from(document.querySelectorAll('input, textarea')).find((item) => item.getAttribute('placeholder') === ${JSON.stringify(placeholder)});
    if (!node) return false;
    node.focus();
    node.select?.();
    return true;
  })()`);
  if (!focused) {
    const visibleText = await client.evaluate(`document.body.innerText.slice(0, 1200)`);
    throw new Error(`${label}: input placeholder ${placeholder} not found; got ${JSON.stringify(visibleText)}`);
  }
  await client.send("Input.dispatchKeyEvent", { type: "keyDown", key: "Backspace", code: "Backspace", windowsVirtualKeyCode: 8 });
  await client.send("Input.dispatchKeyEvent", { type: "keyUp", key: "Backspace", code: "Backspace", windowsVirtualKeyCode: 8 });
  await client.send("Input.insertText", { text: value });
  await new Promise((resolve) => setTimeout(resolve, 250));
}

async function pressEscape(client) {
  await client.send("Input.dispatchKeyEvent", { type: "keyDown", key: "Escape", code: "Escape", windowsVirtualKeyCode: 27 });
  await client.send("Input.dispatchKeyEvent", { type: "keyUp", key: "Escape", code: "Escape", windowsVirtualKeyCode: 27 });
  await new Promise((resolve) => setTimeout(resolve, 500));
}

async function closeVisibleDialogContaining(client, text, label) {
  const closed = await client.evaluate(`(() => {
    const dialog = Array.from(document.querySelectorAll('.el-dialog')).find((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0 && rect.height > 0 && node.innerText.includes(${JSON.stringify(text)});
    });
    const button = dialog && (
      Array.from(dialog.querySelectorAll('.el-dialog__footer button, .el-dialog__footer .el-button')).find((node) => (node.innerText || node.textContent || '').includes('取消'))
      || Array.from(dialog.querySelectorAll('button, .el-button')).find((node) => (node.innerText || node.textContent || '').includes('取消'))
    );
    if (!button) return false;
    button.click();
    return true;
  })()`);
  if (!closed) {
    throw new Error(`${label}: cancel button not found`);
  }
  await waitFor(client, `!Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0 && node.innerText.includes(${JSON.stringify(text)});
  })`, `${label} closed`);
}

async function closeVisibleDialogHeaderContaining(client, text, label) {
  const closed = await client.evaluate(`(() => {
    const dialog = Array.from(document.querySelectorAll('.el-dialog')).find((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0 && rect.height > 0 && node.innerText.includes(${JSON.stringify(text)});
    });
    const button = dialog?.querySelector('.el-dialog__headerbtn');
    if (!button) return false;
    button.click();
    return true;
  })()`);
  if (!closed) {
    throw new Error(`${label}: close button not found`);
  }
  await waitFor(client, `!Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0 && node.innerText.includes(${JSON.stringify(text)});
  })`, `${label} closed`);
}

async function assertText(client, selector, expected, label) {
  const text = await client.evaluate(`document.querySelector(${JSON.stringify(selector)})?.innerText || ""`);
  if (!text.includes(expected)) {
    throw new Error(`${label}: expected ${expected}; got ${JSON.stringify(text.slice(0, 500))}`);
  }
  return text;
}

async function assertInputPlaceholder(client, selector, expected, label) {
  const found = await client.evaluate(`(() => {
    const root = document.querySelector(${JSON.stringify(selector)}) || document;
    return Array.from(root.querySelectorAll('input, textarea')).some((node) => node.getAttribute('placeholder') === ${JSON.stringify(expected)});
  })()`);
  if (!found) {
    const text = await client.evaluate(`document.querySelector(${JSON.stringify(selector)})?.innerText || ""`);
    const placeholders = await client.evaluate(`(() => {
      const root = document.querySelector(${JSON.stringify(selector)}) || document;
      return Array.from(root.querySelectorAll('input, textarea')).map((node) => node.getAttribute('placeholder')).filter(Boolean);
    })()`);
    throw new Error(`${label}: expected input placeholder ${expected}; got text ${JSON.stringify(text.slice(0, 500))}, placeholders ${JSON.stringify(placeholders)}`);
  }
}

function consoleFailures(events) {
  return events
    .filter((event) => event.method === "Runtime.exceptionThrown" || event.method === "Log.entryAdded")
    .map((event) => ({
      method: event.method,
      text: event.params?.entry?.text || event.params?.exceptionDetails?.exception?.description || event.params?.exceptionDetails?.text || "",
      url: event.params?.entry?.url || event.params?.exceptionDetails?.url || "",
      lineNumber: event.params?.exceptionDetails?.lineNumber,
      columnNumber: event.params?.exceptionDetails?.columnNumber,
      stack: event.params?.exceptionDetails?.stackTrace?.callFrames?.slice(0, 5).map((frame) => ({
        functionName: frame.functionName,
        url: frame.url,
        lineNumber: frame.lineNumber,
        columnNumber: frame.columnNumber
      }))
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

const adminLogin = await requestJson(`${adminBase}/api/admin/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account, pwd })
});
if (adminLogin.code !== 200 || !adminLogin.data?.token) {
  throw new Error(`admin login failed: ${JSON.stringify(adminLogin)}`);
}
const adminToken = adminLogin.data.token;

const frontLogin = await requestJson(`${appBase}/api/front/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account: frontAccount, password: frontPwd })
});
if (frontLogin.code !== 200 || !frontLogin.data?.token) {
  throw new Error(`front login failed: ${JSON.stringify(frontLogin)}`);
}
const frontToken = frontLogin.data.token;

const adminOrders = await requestJson(`${adminBase}/api/admin/store/order/list?page=1&limit=1&status=all&type=2`, {
  headers: { "Authori-zation": adminToken }
});
const adminOrderNo = adminOrders.data?.list?.[0]?.orderId;
if (!adminOrderNo) {
  throw new Error(`admin order sample not found: ${JSON.stringify(adminOrders)}`);
}

const adminRefundingOrders = await requestJson(`${adminBase}/api/admin/store/order/list?page=1&limit=1&status=refunding&type=2`, {
  headers: { "Authori-zation": adminToken }
});
const adminRefundingOrderNo = adminRefundingOrders.data?.list?.[0]?.orderId || "";

const adminNotShippedOrders = await requestJson(`${adminBase}/api/admin/store/order/list?page=1&limit=1&status=notShipped&type=2`, {
  headers: { "Authori-zation": adminToken }
});
const adminNotShippedOrderNo = adminNotShippedOrders.data?.list?.[0]?.orderId || "";

const adminCompletedOrders = await requestJson(`${adminBase}/api/admin/store/order/list?page=1&limit=50&status=complete&type=2`, {
  headers: { "Authori-zation": adminToken }
});
const adminExpressOrder = (adminCompletedOrders.data?.list || []).find((row) => row.deliveryType === "express");
const adminExpressOrderNo = adminExpressOrder?.orderId || "";

const frontProducts = await requestJson(`${appBase}/api/front/products?page=1&limit=1`);
const productId = frontProducts.data?.list?.[0]?.id;
const productKeyword = String(frontProducts.data?.list?.[0]?.storeName || frontProducts.data?.list?.[0]?.keyword || "").trim().slice(0, 8);
if (!productId) {
  throw new Error(`front product sample not found: ${JSON.stringify(frontProducts)}`);
}

const frontCart = await requestJson(`${appBase}/api/front/cart/list?isValid=true&page=1&limit=50`, {
  headers: { "Authori-zation": frontToken }
});
const frontCartCount = frontCart.data?.list?.length || 0;

const frontOrders = await requestJson(`${appBase}/api/front/order/list?page=1&limit=1`, {
  headers: { "Authori-zation": frontToken }
});
let frontOrderToken = frontToken;
let frontOrderTokenSource = "default-front-login";
let temporaryOrderToken = "";
let frontOrderNo = frontOrders.data?.list?.[0]?.orderId;

if (!frontOrderNo) {
  const orderSample = await createTemporaryTokenFromAdminOrder("all", adminToken);
  if (orderSample) {
    frontOrderToken = orderSample.token;
    frontOrderTokenSource = orderSample.tokenSource;
    temporaryOrderToken = orderSample.temporaryToken;
    const sampleFrontOrders = await requestJson(`${appBase}/api/front/order/list?page=1&limit=1`, {
      headers: { "Authori-zation": frontOrderToken }
    });
    frontOrderNo = sampleFrontOrders.data?.list?.[0]?.orderId || orderSample.orderNo;
  }
}

const refundOrders = await requestJson(`${appBase}/api/front/order/refund/list?page=1&limit=1`, {
  headers: { "Authori-zation": frontToken }
});
let refundToken = frontToken;
let refundTokenSource = "default-front-login";
let temporaryRefundToken = "";
let refundOrderNo = refundOrders.data?.list?.[0]?.orderId;

if (!refundOrderNo) {
  const refundSample = await createTemporaryTokenFromAdminOrder("refunded", adminToken);
  if (refundSample) {
    temporaryRefundToken = refundSample.temporaryToken;
    refundToken = refundSample.token;
    refundTokenSource = refundSample.tokenSource;
    const sampleRefundOrders = await requestJson(`${appBase}/api/front/order/refund/list?refundStatus=2&page=1&limit=1`, {
      headers: { "Authori-zation": refundToken }
    });
    refundOrderNo = sampleRefundOrders.data?.list?.[0]?.orderId || refundSample.orderNo;
  }
}

const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "crmeb-interaction-chrome-"));
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

  const checked = [];

  await navigate(client, adminBase);
  await client.evaluate(`localStorage.setItem('Authori-zation', ${JSON.stringify(adminToken)})`);
  await navigate(client, `${adminBase}/order/index`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin order table");
  await fillInputByPlaceholder(client, "请输入订单号", adminOrderNo, "admin order detail search order number");
  await clickText(client, ".legacy-main", "搜索", "admin order detail search");
  await waitFor(client, `document.querySelector('.el-table__body')?.innerText.includes(${JSON.stringify(adminOrderNo)})`, "admin order searched row", 30000);
  await clickTextInRow(client, adminOrderNo, "详情", "admin order detail");
  await waitFor(client, "document.querySelector('.el-drawer__body .order-detail')", "admin order detail drawer", 30000);
  await assertText(client, ".el-drawer__body", "订单信息", "admin order detail tab");
  await clickText(client, ".el-drawer__body", "商品信息", "admin order goods tab");
  await waitFor(client, "document.querySelector('.detail-product-table')", "admin order goods table");
  checked.push({ area: "admin-order", action: "detail drawer and goods tab", orderNo: adminOrderNo });
  await clickFirst(client, ".el-drawer__close-btn", "admin close order detail");
  await waitFor(client, "!document.querySelector('.el-drawer__body .order-detail')", "admin order detail drawer closed");
  await clickText(client, ".el-table__body", "更多", "admin order more dropdown");
  await clickVisibleText(client, "订单记录", "admin order log dropdown item");
  await waitFor(client, "document.querySelector('.el-dialog__body .el-table__body')", "admin order log dialog table");
  await assertText(client, ".el-dialog", "操作记录", "admin order log dialog message column");
  await assertText(client, ".el-dialog", "操作时间", "admin order log dialog time column");
  checked.push({ area: "admin-order", action: "open order log dialog without writing", orderNo: adminOrderNo });
  await clickFirst(client, ".el-dialog__headerbtn", "admin close order log dialog");

  if (adminRefundingOrderNo) {
    await navigate(client, `${adminBase}/order/index`);
    await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin order table for refunding");
    await clickVisibleText(client, "退款中", "admin order refunding tab");
    await waitFor(client, "document.querySelector('.el-table__body')?.innerText.includes('申请退款') || document.querySelector('.el-table__body')?.innerText.includes('退款中')", "admin refunding order row");
    await clickText(client, ".el-table__body", "更多", "admin refunding order more dropdown");
    await clickVisibleText(client, "拒绝退款", "admin refund refuse dropdown item");
    await waitFor(client, "document.querySelector('.el-message-box')", "admin refund refuse prompt");
    await assertText(client, ".el-message-box", "拒绝退款", "admin refund refuse prompt title");
    await assertText(client, ".el-message-box", "请输入拒绝退款原因", "admin refund refuse prompt input label");
    checked.push({ area: "admin-order", action: "open refund refuse prompt without submitting", orderNo: adminRefundingOrderNo });
    await clickVisibleText(client, "取消", "admin cancel refund refuse prompt");
  } else {
    checked.push({ area: "admin-order", action: "refund refuse prompt", skipped: "no refunding order sample in current database" });
  }

  if (adminNotShippedOrderNo) {
    await navigate(client, `${adminBase}/order/index`);
    await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin order table for not shipped");
    await clickVisibleText(client, "未发货", "admin order not shipped tab");
    await waitFor(client, "document.querySelector('.el-table__body')?.innerText.includes('发送货')", "admin not shipped order send action");
    await clickText(client, ".el-table__body", "发送货", "admin send order dialog");
    await waitFor(client, "document.querySelector('.el-dialog')?.innerText.includes('发送货')", "admin send order dialog body");
    await assertText(client, ".el-dialog", "快递公司", "admin send order express company field");
    await assertText(client, ".el-dialog", "快递单号", "admin send order express number field");
    await assertText(client, ".el-dialog", "提交", "admin send order submit button");
    checked.push({ area: "admin-order", action: "open send dialog without submitting", orderNo: adminNotShippedOrderNo });
    await clickText(client, ".el-dialog__footer", "取消", "admin cancel send order dialog");
  } else {
    checked.push({ area: "admin-order", action: "send dialog", skipped: "no not-shipped order sample in current database" });
  }

  if (adminExpressOrderNo) {
    await navigate(client, `${adminBase}/user/index`);
    await waitFor(client, "document.querySelector('.legacy-main')", "admin user page before logistics reset");
    await navigate(client, `${adminBase}/order/index`);
    await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin order table for logistics");
    const expressOrderVisible = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.includes(${JSON.stringify(adminExpressOrderNo)}) || false`);
    if (expressOrderVisible) {
      await clickTextInRow(client, adminExpressOrderNo, "详情", "admin express order detail");
      await waitFor(client, "document.querySelector('.el-drawer__body .order-detail')", "admin express order detail drawer");
      await clickText(client, ".el-drawer__body", "发货记录", "admin express order delivery tab");
      await waitFor(client, "document.querySelector('.el-drawer__body')?.innerText.includes('查看物流')", "admin express order logistics action");
      await clickText(client, ".el-drawer__body", "查看物流", "admin express order logistics dialog");
      await waitFor(client, "document.querySelector('.el-dialog .logistics-card')", "admin logistics dialog card");
      await assertText(client, ".el-dialog", "物流公司", "admin logistics company field");
      await assertText(client, ".el-dialog", "物流单号", "admin logistics number field");
      await assertText(client, ".el-dialog", "第三方快递轨迹需配置物流服务后启用", "admin logistics safe mode tip");
      checked.push({ area: "admin-order", action: "open logistics dialog without external tracking", orderNo: adminExpressOrderNo });
      await clickText(client, ".el-dialog__footer", "关闭", "admin close logistics dialog");
      await clickFirst(client, ".el-drawer__close-btn", "admin close express order detail");
    } else {
      checked.push({ area: "admin-order", action: "logistics dialog", skipped: "express order sample not visible in current browser page" });
    }
  } else {
    checked.push({ area: "admin-order", action: "logistics dialog", skipped: "no completed express order sample in current database" });
  }

  await navigate(client, `${adminBase}/javaMobile/orderStatistics`);
  await waitFor(client, "document.querySelector('.mobile-stat-page')?.innerText.includes('数据统计')", "admin mobile order statistics page");
  await assertText(client, ".mobile-stat-page", "待付款", "admin mobile order statistics unpaid entry");
  await assertText(client, ".mobile-stat-page", "待发货", "admin mobile order statistics not shipped entry");
  await assertText(client, ".mobile-stat-page", "待核销", "admin mobile order statistics writeoff entry");
  await assertText(client, ".mobile-stat-page", "营业趋势", "admin mobile order statistics trend");
  await clickText(client, ".mobile-stat-page", "今日成交额", "admin mobile order statistics price detail");
  await waitFor(client, "document.querySelector('.mobile-stat-detail-page')?.innerText.includes('详细数据')", "admin mobile order statistics detail page");
  await assertText(client, ".mobile-stat-detail-page", "营业额", "admin mobile order statistics detail summary");
  await assertText(client, ".mobile-stat-detail-page", "最近7天", "admin mobile order statistics detail time tab");
  await clickText(client, ".mobile-stat-detail-page", "最近7天", "admin mobile order statistics detail week tab");
  await waitFor(client, "document.querySelector('.mobile-stat-detail-tabs .on')?.innerText.includes('最近7天')", "admin mobile order statistics detail week active");
  checked.push({ area: "admin-mobile-order-statistics", action: "open statistics and detail tabs readonly without exporting or writing order data" });

  await navigate(client, `${adminBase}/javaMobile/orderList/notShipped`);
  await waitFor(client, "document.querySelector('.mobile-order-list-page')?.innerText.includes('待发货')", "admin mobile order list not shipped page");
  await assertText(client, ".mobile-order-list-page", "待付款", "admin mobile order list unpaid tab");
  await assertText(client, ".mobile-order-list-page", "待核销", "admin mobile order list writeoff tab");
  await assertText(client, ".mobile-order-list-page", "退款", "admin mobile order list refund tab");
  await clickText(client, ".mobile-order-list-page", "退款", "admin mobile order list refund tab click");
  await waitFor(client, "document.querySelector('.mobile-order-tabs .on')?.innerText.includes('退款')", "admin mobile order list refund tab active");
  checked.push({ area: "admin-mobile-order-list", action: "open order list and switch status tab readonly without refunding marking or changing price" });

  await navigate(client, `${adminBase}/javaMobile/orderDetail/${encodeURIComponent(adminOrderNo)}`);
  await waitFor(client, "document.querySelector('.mobile-order-detail-page')?.innerText.includes('订单编号')", "admin mobile order detail page");
  await assertText(client, ".mobile-order-detail-page", "订单编号", "admin mobile order detail order number");
  await assertText(client, ".mobile-order-detail-page", "支付状态", "admin mobile order detail pay status");
  await assertText(client, ".mobile-order-detail-page", "支付方式", "admin mobile order detail pay type");
  await assertText(client, ".mobile-order-detail-page", "买家留言", "admin mobile order detail buyer mark");
  await assertText(client, ".mobile-order-detail-page", "实付款", "admin mobile order detail actual pay");
  checked.push({ area: "admin-mobile-order-detail", action: "open real order detail readonly without marking refunding delivery or writeoff", orderNo: adminOrderNo });

  if (adminNotShippedOrderNo) {
    await navigate(client, `${adminBase}/javaMobile/orderDelivery/${encodeURIComponent(adminNotShippedOrderNo)}`);
    await waitFor(client, "document.querySelector('.mobile-delivery-page')?.innerText.includes('确认提交')", "admin mobile delivery page");
    await assertText(client, ".mobile-delivery-page", "订单号", "admin mobile delivery order number");
    await assertText(client, ".mobile-delivery-page", "发货方式", "admin mobile delivery type field");
    await assertText(client, ".mobile-delivery-page", "快递单号", "admin mobile delivery express number field");
    await clickText(client, ".mobile-delivery-page", "送货", "admin mobile delivery send type");
    await waitFor(client, "document.querySelector('.mobile-delivery-page')?.innerText.includes('送货人')", "admin mobile delivery send fields");
    await clickText(client, ".mobile-delivery-page", "无需发货", "admin mobile delivery no need type");
    await waitFor(client, "document.querySelector('.mobile-delivery-page .goods.on')?.innerText.includes('无需发货')", "admin mobile delivery no need active");
    checked.push({ area: "admin-mobile-order-delivery", action: "open delivery form and switch delivery type without submitting shipment", orderNo: adminNotShippedOrderNo });
  } else {
    checked.push({ area: "admin-mobile-order-delivery", action: "delivery form", skipped: "no not-shipped order sample in current database" });
  }

  await navigate(client, `${adminBase}/javaMobile/orderCancellation`);
  await waitFor(client, "document.querySelector('.mobile-cancel-page')?.innerText.includes('立即核销')", "admin mobile order cancellation page");
  await waitFor(client, "document.querySelector('.mobile-cancel-page input[placeholder=\"请输入核销码\"]')", "admin mobile order cancellation code input");
  await assertText(client, ".mobile-cancel-page", "立即核销", "admin mobile order cancellation action");
  checked.push({ area: "admin-mobile-order-cancellation", action: "open writeoff page readonly without querying or confirming writeoff" });

  await navigate(client, `${adminBase}/user/index`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin user table");
  await clickFirst(client, ".el-table__body-wrapper .el-checkbox input", "admin select first user for coupon dialog");
  await clickText(client, ".legacy-main", "发送优惠券", "admin user coupon dialog");
  await waitFor(client, "document.querySelector('.list-Dialog')?.innerText.includes('优惠劵')", "admin user coupon dialog body");
  await assertText(client, ".list-Dialog", "优惠券名称", "admin user coupon dialog coupon name");
  await assertText(client, ".list-Dialog", "优惠券面值", "admin user coupon dialog coupon money");
  await assertText(client, ".list-Dialog", "发送", "admin user coupon dialog send action");
  checked.push({ area: "admin-user", action: "open coupon send dialog without sending" });
  await clickFirst(client, ".list-Dialog .el-dialog__headerbtn", "admin close user coupon dialog");
  await waitFor(client, `!Array.from(document.querySelectorAll('.list-Dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0 && node.innerText.includes('优惠劵');
  })`, "admin user coupon dialog closed");
  await clickText(client, ".el-table__body", "详情", "admin user detail drawer");
  await waitFor(client, "document.querySelector('.el-drawer__body .user-detail')", "admin user detail drawer body");
  await assertText(client, ".el-drawer__body", "用户等级", "admin user detail level field");
  await assertText(client, ".el-drawer__body", "余额", "admin user detail balance field");
  await assertText(client, ".el-drawer__body", "积分", "admin user detail integral field");
  checked.push({ area: "admin-user", action: "open user detail drawer without writing" });
  await clickFirst(client, ".el-drawer__close-btn", "admin close user detail drawer");
  await waitFor(client, "!document.querySelector('.el-drawer__body .user-detail')", "admin user detail drawer closed");

  await clickText(client, ".el-table__body", "编辑", "admin user edit dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('用户编号')
      && node.innerText.includes('用户地址')
      && node.innerText.includes('用户备注')
      && node.innerText.includes('提交');
  })`, "admin user edit dialog body");
  checked.push({ area: "admin-user", action: "open user edit dialog without submitting" });
  await closeVisibleDialogContaining(client, "用户编号", "admin user edit dialog");

  await clickText(client, ".el-table__body", "更多", "admin user more dropdown for funds");
  await clickVisibleText(client, "积分余额", "admin user funds dropdown item");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('修改余额')
      && node.innerText.includes('修改积分')
      && node.innerText.includes('确定');
  })`, "admin user funds dialog body");
  await closeVisibleDialogContaining(client, "修改余额", "admin user funds dialog");

  await clickText(client, ".el-table__body", "更多", "admin user more dropdown for group");
  await clickVisibleText(client, "设置分组", "admin user group dropdown item");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('用户分组')
      && node.innerText.includes('确定');
  })`, "admin user group dialog body");
  await closeVisibleDialogContaining(client, "用户分组", "admin user group dialog");

  await clickText(client, ".el-table__body", "更多", "admin user more dropdown for tag");
  await clickVisibleText(client, "设置标签", "admin user tag dropdown item");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('用户标签')
      && node.innerText.includes('确定');
  })`, "admin user tag dialog body");
  await closeVisibleDialogContaining(client, "用户标签", "admin user tag dialog");

  await clickText(client, ".el-table__body", "更多", "admin user more dropdown for level");
  await clickVisibleText(client, "修改用户等级", "admin user level dropdown item");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('用户等级')
      && node.innerText.includes('扣除经验')
      && node.innerText.includes('确定');
  })`, "admin user level dialog body");
  await closeVisibleDialogContaining(client, "扣除经验", "admin user level dialog");

  await clickText(client, ".el-table__body", "更多", "admin user more dropdown for phone");
  await clickVisibleText(client, "修改手机号", "admin user phone dropdown item");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-message-box')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('修改手机号')
      && node.innerText.includes('确定')
      && node.innerText.includes('取消');
  })`, "admin user phone prompt body");
  const phonePromptClosed = await client.evaluate(`(() => {
    const box = Array.from(document.querySelectorAll('.el-message-box')).find((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0 && rect.height > 0 && node.innerText.includes('修改手机号');
    });
    const button = box && Array.from(box.querySelectorAll('button, .el-button')).find((node) => (node.innerText || node.textContent || '').includes('取消'));
    if (!button) return false;
    button.click();
    return true;
  })()`);
  if (!phonePromptClosed) {
    throw new Error("admin user phone prompt: cancel button not found");
  }
  await waitFor(client, `!Array.from(document.querySelectorAll('.el-message-box')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0 && node.innerText.includes('修改手机号');
  })`, "admin user phone prompt closed");

  await clickText(client, ".el-table__body", "更多", "admin user more dropdown for spread");
  await clickVisibleText(client, "修改上级推广人", "admin user spread dropdown item");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('当前用户')
      && node.innerText.includes('用户头像')
      && node.innerText.includes('推广人编号')
      && node.innerText.includes('确定');
  })`, "admin user spread dialog body");
  await closeVisibleDialogContaining(client, "推广人编号", "admin user spread dialog");
  checked.push({ area: "admin-user", action: "open more-menu funds group tag level phone and spread dialogs without submitting" });

  await navigate(client, `${adminBase}/financial/record/charge`);
  await waitFor(client, "document.querySelector('.financial-recharge-list')?.innerText.includes('充值总金额') && document.querySelector('.el-table__body')", "admin financial recharge list");
  await assertText(client, ".financial-recharge-list", "时间选择", "admin financial recharge time filter");
  await assertText(client, ".financial-recharge-list", "用户ID", "admin financial recharge uid filter");
  await assertText(client, ".financial-recharge-list", "订单号", "admin financial recharge order filter");
  await assertText(client, ".financial-recharge-list", "充值总金额", "admin financial recharge total card");
  await assertText(client, ".financial-recharge-list", "小程序充值金额", "admin financial recharge routine card");
  await assertText(client, ".financial-recharge-list", "公众号充值金额", "admin financial recharge wechat card");
  await assertText(client, ".financial-recharge-list", "UID", "admin financial recharge uid column");
  await assertText(client, ".financial-recharge-list", "用户昵称", "admin financial recharge nickname column");
  await assertText(client, ".financial-recharge-list", "支付金额", "admin financial recharge price column");
  await assertText(client, ".financial-recharge-list", "赠送金额", "admin financial recharge give price column");
  await assertText(client, ".financial-recharge-list", "充值类型", "admin financial recharge type column");
  await assertText(client, ".financial-recharge-list", "支付时间", "admin financial recharge pay time column");
  checked.push({ area: "admin-financial-recharge", action: "open recharge statistics and list readonly without searching exporting refunding or changing funds" });

  await navigate(client, `${adminBase}/financial/record/monitor`);
  await waitFor(client, "document.querySelector('.financial-monitor-list')?.innerText.includes('明细类型') && document.querySelector('.el-table__body')", "admin financial monitor list");
  await assertText(client, ".financial-monitor-list", "时间选择", "admin financial monitor time filter");
  await assertText(client, ".financial-monitor-list", "关键字", "admin financial monitor keyword filter");
  await assertText(client, ".financial-monitor-list", "明细类型", "admin financial monitor type filter");
  await assertText(client, ".financial-monitor-list", "会员ID", "admin financial monitor uid column");
  await assertText(client, ".financial-monitor-list", "昵称", "admin financial monitor nickname column");
  await assertText(client, ".financial-monitor-list", "金额", "admin financial monitor amount column");
  await assertText(client, ".financial-monitor-list", "备注", "admin financial monitor mark column");
  await assertText(client, ".financial-monitor-list", "创建时间", "admin financial monitor create time column");
  checked.push({ area: "admin-financial-monitor", action: "open funds monitor list readonly without filtering exporting refunding or changing funds" });

  await navigate(client, `${adminBase}/financial/brokerage`);
  await waitFor(client, "document.querySelector('.financial-brokerage-list')?.innerText.includes('佣金变动') && document.querySelector('.el-table__body')", "admin financial brokerage list");
  await assertText(client, ".financial-brokerage-list", "变动类型", "admin financial brokerage type filter");
  await assertText(client, ".financial-brokerage-list", "ID", "admin financial brokerage id column");
  await assertText(client, ".financial-brokerage-list", "佣金变动", "admin financial brokerage amount column");
  await assertText(client, ".financial-brokerage-list", "变动信息", "admin financial brokerage mark column");
  await assertText(client, ".financial-brokerage-list", "用户信息", "admin financial brokerage user column");
  await assertText(client, ".financial-brokerage-list", "时间", "admin financial brokerage time column");
  checked.push({ area: "admin-financial-brokerage", action: "open brokerage record list readonly without filtering extracting transferring or changing commission funds" });

  await navigate(client, `${adminBase}/financial/commission/template`);
  await waitFor(client, "document.querySelector('.financial-extract-list')?.innerText.includes('待提现金额') && document.querySelector('.el-table__body')", "admin financial extract list");
  await assertText(client, ".financial-extract-list", "时间选择", "admin financial extract time filter");
  await assertText(client, ".financial-extract-list", "提现状态", "admin financial extract status filter");
  await assertText(client, ".financial-extract-list", "提现方式", "admin financial extract type filter");
  await assertText(client, ".financial-extract-list", "关键字", "admin financial extract keyword filter");
  await assertText(client, ".financial-extract-list", "待提现金额", "admin financial extract pending card");
  await assertText(client, ".financial-extract-list", "佣金总金额", "admin financial extract total card");
  await assertText(client, ".financial-extract-list", "已提现金额", "admin financial extract withdrawn card");
  await assertText(client, ".financial-extract-list", "未提现金额", "admin financial extract undrawn card");
  await assertText(client, ".financial-extract-list", "用户信息", "admin financial extract user column");
  await assertText(client, ".financial-extract-list", "提现金额", "admin financial extract amount column");
  await assertText(client, ".financial-extract-list", "账号", "admin financial extract account column");
  await assertText(client, ".financial-extract-list", "审核状态", "admin financial extract status column");
  await assertText(client, ".financial-extract-list", "备注", "admin financial extract mark column");
  await assertText(client, ".financial-extract-list", "创建时间", "admin financial extract create time column");
  checked.push({ area: "admin-financial-extract", action: "open extract statistics and list readonly without approving rejecting editing or changing commission funds" });

  await navigate(client, `${adminBase}/distribution/index`);
  await waitFor(client, "document.querySelector('.distribution-promoter-list')?.innerText.includes('推广订单数量') && document.querySelector('.el-table__body')", "admin distribution promoter list");
  await assertText(client, ".distribution-promoter-list", "时间选择", "admin distribution promoter time filter");
  await assertText(client, ".distribution-promoter-list", "关键字", "admin distribution promoter keyword filter");
  await assertText(client, ".distribution-promoter-list", "用户信息", "admin distribution promoter user column");
  await assertText(client, ".distribution-promoter-list", "推广用户(一级)数量", "admin distribution promoter spread count column");
  await assertText(client, ".distribution-promoter-list", "推广订单数量", "admin distribution promoter order count column");
  await assertText(client, ".distribution-promoter-list", "推广订单金额", "admin distribution promoter order amount column");
  await assertText(client, ".distribution-promoter-list", "佣金总金额", "admin distribution promoter brokerage total column");
  await assertText(client, ".distribution-promoter-list", "已提现金额", "admin distribution promoter extracted amount column");
  await assertText(client, ".distribution-promoter-list", "未提现金额", "admin distribution promoter brokerage amount column");
  await assertText(client, ".distribution-promoter-list", "冻结中佣金", "admin distribution promoter freeze brokerage column");
  await assertText(client, ".distribution-promoter-list", "成为推广员时间", "admin distribution promoter time column");
  await assertText(client, ".distribution-promoter-list", "上级推广人", "admin distribution promoter parent column");
  const promoterListHasRows = await client.evaluate(`Array.from(document.querySelectorAll('.distribution-promoter-list .el-table__body tr')).some((row) => (row.innerText || row.textContent || '').trim())`);
  if (promoterListHasRows) {
    await clickText(client, ".distribution-promoter-list .el-table__body", "推广人", "admin distribution spread user dialog");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('推广人列表')
        && node.innerText.includes('用户类型')
        && node.innerText.includes('是否推广员')
        && node.innerText.includes('推广人数')
        && node.innerText.includes('订单数');
    })`, "admin distribution spread user dialog body");
    await closeVisibleDialogHeaderContaining(client, "推广人列表", "admin distribution spread user dialog");

    const moreDropdownPoint = await client.evaluate(`(() => {
      const row = Array.from(document.querySelectorAll('.distribution-promoter-list .el-table__body tr'))
        .find((item) => (item.innerText || item.textContent || '').trim());
      const dropdown = row && Array.from(row.querySelectorAll('.el-dropdown-link, button, .el-button, [role="button"]'))
        .find((node) => (node.innerText || node.textContent || '').includes('更多'));
      if (!dropdown) return null;
      dropdown.scrollIntoView({ block: 'center', inline: 'center' });
      const rect = dropdown.getBoundingClientRect();
      return { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 };
    })()`);
    if (moreDropdownPoint) {
      await client.send("Input.dispatchMouseEvent", { type: "mouseMoved", x: moreDropdownPoint.x, y: moreDropdownPoint.y });
      await waitFor(client, `Array.from(document.querySelectorAll('.el-dropdown-menu__item')).some((node) => {
        const rect = node.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0 && (node.innerText || node.textContent || '').includes('推广订单');
      })`, "admin distribution spread order dropdown item");
      await clickVisibleText(client, "推广订单", "admin distribution spread order dropdown item");
      await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
        const rect = node.getBoundingClientRect();
        return rect.width > 0
          && rect.height > 0
          && node.innerText.includes('推广订单列表')
          && node.innerText.includes('时间选择')
          && node.innerText.includes('用户类型')
          && node.innerText.includes('订单ID')
          && node.innerText.includes('返佣金额');
      })`, "admin distribution spread order dialog body");
      await closeVisibleDialogHeaderContaining(client, "推广订单列表", "admin distribution spread order dialog");
      checked.push({ area: "admin-distribution-promoter", action: "open promoter list spread-user and spread-order dialogs readonly without clearing spread relation or changing commission" });
    } else {
      checked.push({ area: "admin-distribution-promoter", action: "open promoter list and spread-user dialog readonly without clearing spread relation or changing commission", skipped: "more dropdown not visible in current table row" });
    }
  } else {
    checked.push({ area: "admin-distribution-promoter", action: "open promoter list readonly", skipped: "no promoter row visible in current database" });
  }

  await navigate(client, `${adminBase}/distribution/distributionconfig`);
  await waitFor(client, "document.querySelector('.distribution-config')?.innerText.includes('分销启用')", "admin distribution config form");
  await assertText(client, ".distribution-config", "分销启用", "admin distribution config enable field");
  await assertText(client, ".distribution-config", "满额分销最低金额", "admin distribution config quota field");
  await assertText(client, ".distribution-config", "分销关系绑定", "admin distribution config bind field");
  await assertText(client, ".distribution-config", "一级返佣比例", "admin distribution config first ratio field");
  await assertText(client, ".distribution-config", "二级返佣比例", "admin distribution config second ratio field");
  await assertText(client, ".distribution-config", "提现最低金额", "admin distribution config extract min field");
  await assertText(client, ".distribution-config", "提现银行卡", "admin distribution config bank field");
  await assertText(client, ".distribution-config", "冻结时间", "admin distribution config freeze field");
  checked.push({ area: "admin-distribution-config", action: "open distribution config readonly without toggling switches or submitting settings" });

  await navigate(client, `${adminBase}/marketing/integral/integralconfig`);
  await waitFor(client, "document.querySelector('.integral-manager')?.innerText.includes('积分抵用比例')", "admin integral config form");
  await assertText(client, ".integral-manager", "积分抵用比例", "admin integral config deduction field");
  await assertText(client, ".integral-manager", "下单赠送积分比例", "admin integral config order give field");
  await assertText(client, ".integral-manager", "积分冻结时间", "admin integral config freeze field");
  await assertText(client, ".integral-manager", "提交", "admin integral config submit action visible");
  checked.push({ area: "admin-integral-config", action: "open integral config readonly without submitting or changing system config" });

  await navigate(client, `${adminBase}/marketing/integral/integrallog`);
  await waitFor(client, "document.querySelector('.integral-manager')?.innerText.includes('积分余量') && document.querySelector('.el-table__body')", "admin integral log list");
  await assertText(client, ".integral-manager", "时间选择", "admin integral log time filter");
  await assertText(client, ".integral-manager", "微信昵称", "admin integral log nickname filter");
  await assertText(client, ".integral-manager", "标题", "admin integral log title column");
  await assertText(client, ".integral-manager", "积分余量", "admin integral log balance column");
  await assertText(client, ".integral-manager", "明细数字", "admin integral log integral column");
  await assertText(client, ".integral-manager", "备注", "admin integral log mark column");
  await assertText(client, ".integral-manager", "用户昵称", "admin integral log user column");
  await assertText(client, ".integral-manager", "添加时间", "admin integral log time column");
  checked.push({ area: "admin-integral-log", action: "open integral log list readonly without filtering exporting or changing points" });

  await navigate(client, `${adminBase}/user/group`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin user group table");
  await assertText(client, ".legacy-main", "添加用户分组", "admin user group create action");
  await clickText(client, ".legacy-main", "添加用户分组", "admin user group create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('添加分组名称')
      && node.innerText.includes('分组名称')
      && node.innerText.includes('确认');
  })`, "admin user group create dialog body");
  await closeVisibleDialogContaining(client, "添加分组名称", "admin user group create dialog");
  const userGroupEditable = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.includes('编辑') || false`);
  if (userGroupEditable) {
    await clickText(client, ".el-table__body", "编辑", "admin user group edit dialog");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('编辑分组名称')
        && node.innerText.includes('分组名称')
        && node.innerText.includes('确认');
    })`, "admin user group edit dialog body");
    await closeVisibleDialogContaining(client, "编辑分组名称", "admin user group edit dialog");
    checked.push({ area: "admin-user-group", action: "open create and edit dialogs without saving" });
  } else {
    checked.push({ area: "admin-user-group", action: "open create dialog without saving", skipped: "no user group row visible in current database" });
  }

  await navigate(client, `${adminBase}/user/label`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin user tag table");
  await assertText(client, ".legacy-main", "添加用户标签", "admin user tag create action");
  await clickText(client, ".legacy-main", "添加用户标签", "admin user tag create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('添加标签名称')
      && node.innerText.includes('标签名称')
      && node.innerText.includes('确认');
  })`, "admin user tag create dialog body");
  await closeVisibleDialogContaining(client, "添加标签名称", "admin user tag create dialog");
  const userTagEditable = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.includes('编辑') || false`);
  if (userTagEditable) {
    await clickText(client, ".el-table__body", "编辑", "admin user tag edit dialog");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('编辑标签名称')
        && node.innerText.includes('标签名称')
        && node.innerText.includes('确认');
    })`, "admin user tag edit dialog body");
    await closeVisibleDialogContaining(client, "编辑标签名称", "admin user tag edit dialog");
    checked.push({ area: "admin-user-tag", action: "open create and edit dialogs without saving" });
  } else {
    checked.push({ area: "admin-user-tag", action: "open create dialog without saving", skipped: "no user tag row visible in current database" });
  }

  await navigate(client, `${adminBase}/user/grade`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin user grade table");
  await assertText(client, ".legacy-main", "添加用户等级", "admin user grade create action");
  await clickText(client, ".legacy-main", "添加用户等级", "admin user grade create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('添加用户等级')
      && node.innerText.includes('等级名称')
      && node.innerText.includes('享受折扣')
      && node.innerText.includes('经验')
      && node.innerText.includes('图标');
  })`, "admin user grade create dialog body");
  await clickText(client, ".el-dialog", "选择素材", "admin user grade attachment picker");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('选择图片')
      && (node.innerText.includes('全部图片') || node.innerText.includes('暂无图片'));
  })`, "admin user grade attachment picker body");
  await closeVisibleDialogHeaderContaining(client, "选择图片", "admin user grade attachment picker");
  await closeVisibleDialogContaining(client, "添加用户等级", "admin user grade create dialog");
  const userGradeEditable = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.includes('编辑') || false`);
  if (userGradeEditable) {
    await clickText(client, ".el-table__body", "编辑", "admin user grade edit dialog");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('编辑用户等级')
        && node.innerText.includes('等级名称')
        && node.innerText.includes('享受折扣')
        && node.innerText.includes('经验')
        && node.innerText.includes('图标');
    })`, "admin user grade edit dialog body");
    await closeVisibleDialogContaining(client, "编辑用户等级", "admin user grade edit dialog");
    checked.push({ area: "admin-user-grade", action: "open create attachment picker and edit dialogs without saving" });
  } else {
    checked.push({ area: "admin-user-grade", action: "open create attachment picker without saving", skipped: "no user grade row visible in current database" });
  }

  await navigate(client, `${adminBase}/store/index`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin product table");
  await assertText(client, ".legacy-main", "添加商品", "admin product list actions");
  await clickText(client, ".legacy-main", "添加商品", "admin product create form");
  await waitFor(client, "document.querySelector('.product-basic-editor')", "admin product create form body");
  await assertText(client, ".product-basic-editor", "商品名称", "admin product create name field");
  await assertText(client, ".product-basic-editor", "商品分类", "admin product create category field");
  await assertText(client, ".product-basic-editor", "当前库存", "admin product create stock field");
  await assertText(client, ".product-basic-editor", "保存", "admin product create save button");
  checked.push({ area: "admin-product", action: "open create form without saving" });
  await clickText(client, ".direct-form-header", "返回列表", "admin product create back to list");
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin product table after create form");
  await clickText(client, ".el-table__body", "编辑", "admin product edit form");
  await waitFor(client, "document.querySelector('.product-basic-editor') && document.querySelector('.direct-form-header')?.innerText.includes('编辑商品')", "admin product edit form body");
  await assertText(client, ".product-basic-editor", "商品名称", "admin product edit name field");
  await assertText(client, ".product-basic-editor", "商品分类", "admin product edit category field");
  await assertText(client, ".product-basic-editor", "商品封面图", "admin product edit cover field");
  await clickText(client, ".product-basic-editor", "其他设置", "admin product edit other settings tab");
  await assertText(client, ".product-basic-editor", "商品推荐", "admin product edit recommend field");
  await assertText(client, ".product-basic-editor", "优惠券", "admin product edit coupon field");
  await clickText(client, ".product-basic-editor", "商品详情", "admin product edit content tab");
  await assertText(client, ".product-basic-editor", "商品详情", "admin product edit content field");
  checked.push({ area: "admin-product", action: "open real edit form and switch tabs without saving" });
  await clickText(client, ".direct-form-header", "返回列表", "admin product edit back to list");
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin product table after edit form");
  await clickText(client, ".el-table__body", "详情", "admin product detail page");
  await waitFor(client, "document.querySelector('.product-detail') && document.querySelector('.direct-form-header')?.innerText.includes('商品详情')", "admin product detail page body");
  await assertText(client, ".product-detail", "商品名称", "admin product detail name field");
  await assertText(client, ".product-detail", "商品分类", "admin product detail category field");
  await assertText(client, ".product-detail", "商品封面图", "admin product detail cover field");
  await clickText(client, ".product-detail", "规格库存", "admin product detail stock tab");
  await assertText(client, ".product-detail", "商品规格", "admin product detail spec field");
  await assertText(client, ".product-detail", "售价", "admin product detail price column");
  await assertText(client, ".product-detail", "库存", "admin product detail stock column");
  await clickText(client, ".product-detail", "商品详情", "admin product detail content tab");
  await assertText(client, ".product-detail", "商品详情", "admin product detail content field");
  await clickText(client, ".product-detail", "其他设置", "admin product detail other tab");
  await assertText(client, ".product-detail", "商品推荐", "admin product detail recommend field");
  await assertText(client, ".product-detail", "优惠券", "admin product detail coupon field");
  checked.push({ area: "admin-product", action: "open detail page and switch tabs without writing" });
  await clickText(client, ".direct-form-header", "返回列表", "admin product detail back to list");
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin product table after detail page");

  await clickText(client, ".legacy-main", "商品采集", "admin product copy dialog");
  await waitFor(client, "document.querySelector('.copy-product-box')", "admin product copy dialog body");
  await assertText(client, ".copy-product-box", "商品链接", "admin product copy dialog link input");
  await assertText(client, ".copy-product-box", "商品采集设置", "admin product copy dialog config tip");
  checked.push({ area: "admin-product", action: "open copy dialog without submitting" });
  await clickText(client, ".el-dialog__footer", "取消", "admin close product copy dialog");
  await waitFor(client, "!document.querySelector('.copy-product-box')", "admin product copy dialog closed");

  await clickText(client, ".el-table__body", "编辑库存", "admin product stock drawer");
  await waitFor(client, "document.querySelector('.el-drawer__body .storeEdit')", "admin product stock drawer body");
  await assertText(client, ".el-drawer__body", "售价", "admin product stock drawer price column");
  await assertText(client, ".el-drawer__body", "库存", "admin product stock drawer stock column");
  await assertText(client, ".el-drawer__body", "增加库存", "admin product stock drawer add stock column");
  await assertText(client, ".el-drawer__body", "确认", "admin product stock drawer submit button");
  checked.push({ area: "admin-product", action: "open stock drawer without saving" });
  await clickFirst(client, ".el-drawer__close-btn", "admin close product stock drawer");

  await navigate(client, `${adminBase}/store/sort`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin product category table");
  await assertText(client, ".legacy-main", "添加商品分类", "admin product category create action");
  await clickText(client, ".legacy-main", "添加商品分类", "admin product category create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('添加商品分类')
      && node.innerText.includes('分类名称')
      && node.innerText.includes('父级')
      && node.innerText.includes('分类图标')
      && node.innerText.includes('排序');
  })`, "admin product category create dialog body");
  await closeVisibleDialogContaining(client, "添加商品分类", "admin product category create dialog");

  await clickText(client, ".el-table__body", "编辑", "admin product category edit dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('编辑商品分类')
      && node.innerText.includes('分类名称')
      && node.innerText.includes('父级')
      && node.innerText.includes('分类图标')
      && node.innerText.includes('状态');
  })`, "admin product category edit dialog body");
  checked.push({ area: "admin-product-category", action: "open create and edit dialogs without saving" });
  await closeVisibleDialogContaining(client, "编辑商品分类", "admin product category edit dialog");

  await navigate(client, `${adminBase}/store/attr`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin product rule table");
  await assertText(client, ".legacy-main", "添加商品规格", "admin product rule create action");
  await clickText(client, ".legacy-main", "添加商品规格", "admin product rule create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('添加商品规格')
      && node.innerText.includes('规格名称')
      && node.innerText.includes('添加新规格')
      && node.innerText.includes('确定');
  })`, "admin product rule create dialog body");
  await clickText(client, ".el-dialog", "添加新规格", "admin product rule expand new spec");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('规格：')
      && node.innerText.includes('规格值：');
  })`, "admin product rule new spec fields");
  await closeVisibleDialogContaining(client, "添加商品规格", "admin product rule create dialog");

  await clickText(client, ".el-table__body", "编辑", "admin product rule edit dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('编辑商品规格')
      && node.innerText.includes('规格名称')
      && node.innerText.includes('添加新规格')
      && node.innerText.includes('确定');
  })`, "admin product rule edit dialog body");
  checked.push({ area: "admin-product-rule", action: "open create and edit dialogs without saving" });
  await closeVisibleDialogContaining(client, "编辑商品规格", "admin product rule edit dialog");

  await navigate(client, `${adminBase}/store/comment`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin product comment table");
  await assertText(client, ".legacy-main", "添加虚拟评论", "admin product comment create action");
  await clickText(client, ".legacy-main", "添加虚拟评论", "admin product comment create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('添加评论')
      && node.innerText.includes('商品')
      && node.innerText.includes('用户名称')
      && node.innerText.includes('评价文字')
      && node.innerText.includes('用户头像')
      && node.innerText.includes('评价图片');
  })`, "admin product comment create dialog body");
  await clickFirst(client, ".el-dialog .upLoadPicBox", "admin product comment product picker");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('商品列表')
      && node.innerText.includes('商品搜索')
      && node.innerText.includes('商品名称')
      && node.innerText.includes('库存');
  })`, "admin product comment product picker body");
  await closeVisibleDialogHeaderContaining(client, "商品列表", "admin product comment product picker");
  await clickText(client, ".el-dialog", "选择素材", "admin product comment avatar attachment picker");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('选择头像')
      && node.innerText.includes('选择一张图片作为用户头像');
  })`, "admin product comment avatar picker body");
  await closeVisibleDialogHeaderContaining(client, "选择头像", "admin product comment avatar picker");
  checked.push({ area: "admin-product-comment", action: "open create dialog product picker and attachment picker without submitting" });
  await closeVisibleDialogHeaderContaining(client, "添加评论", "admin product comment create dialog");

  await navigate(client, `${adminBase}/maintain/picture`);
  await waitFor(client, "document.querySelector('.attachment-page')", "admin attachment manager page");
  await assertText(client, ".attachment-page", "图片", "admin attachment image tab");
  await assertText(client, ".attachment-page", "上传图片", "admin attachment upload image action");
  await waitFor(client, "document.querySelector('.attachment-grid') || document.querySelector('.attachment-empty')", "admin attachment image grid");
  await clickText(client, ".attachment-page", "视频", "admin attachment video tab");
  await waitFor(client, "document.querySelector('.attachment-page')?.innerText.includes('上传视频')", "admin attachment video upload action");
  await assertText(client, ".attachment-page", "全部视频", "admin attachment video root category");
  await clickText(client, ".attachment-page", "图片", "admin attachment back image tab");
  await waitFor(client, "document.querySelector('.attachment-page')?.innerText.includes('上传图片')", "admin attachment image tab restored");
  checked.push({ area: "admin-attachment", action: "open image video tabs without upload delete or move" });

  await navigate(client, `${adminBase}/operation/roleManager/adminList`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin system admin table");
  await assertText(client, ".legacy-main", "添加管理员", "admin system admin create action");
  await clickText(client, ".legacy-main", "添加管理员", "admin system admin create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('创建身份')
      && node.innerText.includes('管理员账号')
      && node.innerText.includes('管理员密码')
      && node.innerText.includes('管理员姓名')
      && node.innerText.includes('管理员身份')
      && node.innerText.includes('手机号');
  })`, "admin system admin create dialog body");
  await closeVisibleDialogContaining(client, "创建身份", "admin system admin create dialog");
  const systemAdminEditable = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.includes('编辑') || false`);
  if (systemAdminEditable) {
    await clickText(client, ".el-table__body", "编辑", "admin system admin edit dialog");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('编辑身份')
        && node.innerText.includes('管理员账号')
        && node.innerText.includes('管理员姓名')
        && node.innerText.includes('管理员身份')
        && node.innerText.includes('手机号');
    })`, "admin system admin edit dialog body");
    await closeVisibleDialogContaining(client, "编辑身份", "admin system admin edit dialog");
    checked.push({ area: "admin-system-admin", action: "open create and edit dialogs without saving" });
  } else {
    checked.push({ area: "admin-system-admin", action: "open create dialog without saving", skipped: "no admin row visible in current database" });
  }

  await navigate(client, `${adminBase}/operation/roleManager/identityManager`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin system role table");
  await assertText(client, ".legacy-main", "添加角色", "admin system role create action");
  await clickText(client, ".legacy-main", "添加角色", "admin system role create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('创建身份')
      && node.innerText.includes('角色名称')
      && node.innerText.includes('状态')
      && node.innerText.includes('菜单权限')
      && node.innerText.includes('展开/折叠');
  })`, "admin system role create dialog body");
  await closeVisibleDialogContaining(client, "创建身份", "admin system role create dialog");
  const systemRoleEditable = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.includes('编辑') || false`);
  if (systemRoleEditable) {
    await clickText(client, ".el-table__body", "编辑", "admin system role edit dialog");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('编辑身份')
        && node.innerText.includes('角色名称')
        && node.innerText.includes('状态')
        && node.innerText.includes('菜单权限')
        && node.innerText.includes('展开/折叠');
    })`, "admin system role edit dialog body");
    await closeVisibleDialogContaining(client, "编辑身份", "admin system role edit dialog");
    checked.push({ area: "admin-system-role", action: "open create and edit permission-tree dialogs without saving" });
  } else {
    checked.push({ area: "admin-system-role", action: "open create permission-tree dialog without saving", skipped: "no role row visible in current database" });
  }

  await navigate(client, `${adminBase}/operation/roleManager/promiseRules`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin system menu rules table");
  await assertText(client, ".legacy-main", "菜单名称", "admin system menu rules name filter");
  await assertText(client, ".legacy-main", "新增", "admin system menu rules create action");
  await assertText(client, ".legacy-main", "展开/折叠", "admin system menu rules expand action");
  await clickText(client, ".legacy-main", "新增", "admin system menu rules create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('添加菜单')
      && node.innerText.includes('上级菜单')
      && node.innerText.includes('菜单类型')
      && node.innerText.includes('菜单名称')
      && node.innerText.includes('显示排序')
      && node.innerText.includes('显示状态');
  })`, "admin system menu rules create dialog body");
  await clickText(client, ".el-dialog", "选择", "admin system menu rules icon picker");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('选择图标')
      && node.innerText.includes('Menu')
      && node.innerText.includes('Setting');
  })`, "admin system menu rules icon picker body");
  await closeVisibleDialogHeaderContaining(client, "选择图标", "admin system menu rules icon picker");
  await closeVisibleDialogContaining(client, "添加菜单", "admin system menu rules create dialog");
  const systemMenuEditable = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.includes('编辑') || false`);
  if (systemMenuEditable) {
    await clickText(client, ".el-table__body", "编辑", "admin system menu rules edit dialog");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('修改菜单')
        && node.innerText.includes('上级菜单')
        && node.innerText.includes('菜单类型')
        && node.innerText.includes('菜单名称')
        && node.innerText.includes('显示排序')
        && node.innerText.includes('显示状态');
    })`, "admin system menu rules edit dialog body");
    await closeVisibleDialogContaining(client, "修改菜单", "admin system menu rules edit dialog");
    checked.push({ area: "admin-system-menu-rules", action: "open create icon picker and edit dialogs without saving" });
  } else {
    checked.push({ area: "admin-system-menu-rules", action: "open create icon picker without saving", skipped: "no menu rule row visible in current database" });
  }

  await navigate(client, `${adminBase}/maintain/schedule/list`);
  await waitFor(client, "document.querySelector('.schedule-page') && document.querySelector('.el-table__body')", "admin schedule jobs table");
  await assertText(client, ".schedule-page", "添加定时任务", "admin schedule create action");
  await assertText(client, ".schedule-page", "任务id", "admin schedule job id column");
  await assertText(client, ".schedule-page", "cron表达式", "admin schedule cron column");
  await clickText(client, ".schedule-page", "添加定时任务", "admin schedule create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('定时任务')
      && node.innerText.includes('定时任务类名')
      && node.innerText.includes('cron表达式')
      && node.innerText.includes('方法名')
      && node.innerText.includes('参数')
      && node.innerText.includes('备注');
  })`, "admin schedule create dialog body");
  await closeVisibleDialogContaining(client, "定时任务类名", "admin schedule create dialog");
  const scheduleEditable = await client.evaluate(`(() => {
    const rows = Array.from(document.querySelectorAll('.el-table__body tr'));
    return rows.some((row) => {
      const text = row.innerText || row.textContent || '';
      const editButton = Array.from(row.querySelectorAll('button, .el-button')).find((node) => (node.innerText || node.textContent || '').includes('编辑'));
      return text.includes('编辑') && editButton && !editButton.disabled && !editButton.classList.contains('is-disabled');
    });
  })()`);
  if (scheduleEditable) {
    const clickedScheduleEdit = await client.evaluate(`(() => {
      const rows = Array.from(document.querySelectorAll('.el-table__body tr'));
      for (const row of rows) {
        const editButton = Array.from(row.querySelectorAll('button, .el-button')).find((node) => (node.innerText || node.textContent || '').includes('编辑'));
        if (editButton && !editButton.disabled && !editButton.classList.contains('is-disabled')) {
          editButton.scrollIntoView({ block: 'center', inline: 'center' });
          editButton.click();
          return true;
        }
      }
      return false;
    })()`);
    if (!clickedScheduleEdit) {
      throw new Error("admin schedule edit button not clickable");
    }
    await new Promise((resolve) => setTimeout(resolve, 500));
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('定时任务')
        && node.innerText.includes('定时任务类名')
        && node.innerText.includes('cron表达式')
        && node.innerText.includes('方法名')
        && node.innerText.includes('备注');
    })`, "admin schedule edit dialog body");
    await closeVisibleDialogContaining(client, "定时任务类名", "admin schedule edit dialog");
    checked.push({ area: "admin-schedule", action: "open create and editable job dialogs without saving" });
  } else {
    checked.push({ area: "admin-schedule", action: "open create dialog without saving", skipped: "no editable paused job row visible in current database" });
  }
  await clickText(client, ".schedule-page", "任务日志", "admin schedule logs tab");
  await waitFor(client, "document.querySelector('.schedule-page')?.innerText.includes('任务日志id')", "admin schedule logs table");
  await assertText(client, ".schedule-page", "任务类名", "admin schedule logs bean filter");
  await assertText(client, ".schedule-page", "耗时(单位：毫秒)", "admin schedule logs times column");
  checked.push({ area: "admin-schedule-log", action: "open logs tab readonly" });

  await navigate(client, `${adminBase}/operation/notification`);
  await waitFor(client, "document.querySelector('.divBox') && document.querySelector('.el-table__body')", "admin notification table");
  await assertText(client, ".divBox", "通知会员", "admin notification member tab");
  await assertText(client, ".divBox", "同步小程序订阅消息", "admin notification routine sync action");
  await assertText(client, ".divBox", "同步微信模版消息", "admin notification wechat sync action");
  await assertText(client, ".divBox", "通知类型", "admin notification type column");
  await assertText(client, ".divBox", "发送短信", "admin notification sms column");
  await assertText(client, ".divBox", "设置", "admin notification setting column");
  const notificationDetailAvailable = await client.evaluate(`(() => {
    const rows = Array.from(document.querySelectorAll('.el-table__body tr'));
    return rows.some((row) => Array.from(row.querySelectorAll('button, .el-button, a')).some((node) => (node.innerText || node.textContent || '').includes('详情')));
  })()`);
  if (notificationDetailAvailable) {
    const clickedNotificationDetail = await client.evaluate(`(() => {
      const rows = Array.from(document.querySelectorAll('.el-table__body tr'));
      for (const row of rows) {
        const detailButton = Array.from(row.querySelectorAll('button, .el-button, a')).find((node) => (node.innerText || node.textContent || '').includes('详情'));
        if (detailButton) {
          detailButton.scrollIntoView({ block: 'center', inline: 'center' });
          detailButton.click();
          return true;
        }
      }
      return false;
    })()`);
    if (!clickedNotificationDetail) {
      throw new Error("admin notification detail button not clickable");
    }
    await new Promise((resolve) => setTimeout(resolve, 700));
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('通知详情')
        && node.innerText.includes('状态')
        && (node.innerText.includes('短信') || node.innerText.includes('模板ID') || node.innerText.includes('模板内容'));
    })`, "admin notification detail dialog body");
    await closeVisibleDialogContaining(client, "通知详情", "admin notification detail dialog");
    checked.push({ area: "admin-notification", action: "open detail dialog without saving or syncing" });
  } else {
    checked.push({ area: "admin-notification", action: "detail dialog", skipped: "no notification detail row visible in current database" });
  }
  await clickText(client, ".divBox", "通知平台", "admin notification platform tab");
  await waitFor(client, "document.querySelector('.divBox')?.innerText.includes('通知平台') && document.querySelector('.el-table__body')", "admin notification platform table");
  await assertText(client, ".divBox", "通知类型", "admin notification platform type column");
  await assertText(client, ".divBox", "发送短信", "admin notification platform sms column");
  await assertText(client, ".divBox", "设置", "admin notification platform setting column");
  checked.push({ area: "admin-notification", action: "open platform tab readonly without syncing switches or saving" });

  await navigate(client, `${adminBase}/operation/setting`);
  await waitFor(client, "document.querySelector('.divBox') && document.querySelector('.el-tabs')", "admin system setting tabs");
  await waitFor(client, `document.querySelector('.divBox')?.innerText.includes('提交')
    || document.querySelector('.divBox')?.innerText.includes('未关联表单配置')`, "admin system setting form or empty state");
  const systemSettingFormVisible = await client.evaluate(`document.querySelector('.divBox')?.innerText.includes('提交')`);
  if (systemSettingFormVisible) {
    await waitFor(client, "document.querySelectorAll('.setting-form .el-form-item').length > 0", "admin system setting dynamic form items");
    await assertText(client, ".divBox", "提交", "admin system setting submit action");
  }
  const switchedSystemSettingTab = await client.evaluate(`(() => {
    const tabs = Array.from(document.querySelectorAll('.divBox .el-tabs__item')).filter((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0 && rect.height > 0 && !node.classList.contains('is-active');
    });
    const target = tabs[0];
    if (!target) return false;
    target.scrollIntoView({ block: 'center', inline: 'center' });
    target.click();
    return true;
  })()`);
  if (switchedSystemSettingTab) {
    await waitFor(client, `document.querySelector('.divBox')?.innerText.includes('提交')
      || document.querySelector('.divBox')?.innerText.includes('未关联表单配置')`, "admin system setting switched tab content");
  }
  const systemSettingAttachmentButtonVisible = await client.evaluate(`Array.from(document.querySelectorAll('.divBox button, .divBox .el-button')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0 && (node.innerText || node.textContent || '').includes('选择图片');
  })`);
  if (systemSettingAttachmentButtonVisible) {
    await clickText(client, ".divBox", "选择图片", "admin system setting attachment picker");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('素材管理')
        && (node.innerText.includes('全部图片') || node.innerText.includes('素材为空') || node.innerText.includes('选择已有图片素材'));
    })`, "admin system setting attachment picker body");
    await closeVisibleDialogHeaderContaining(client, "素材管理", "admin system setting attachment picker");
    checked.push({ area: "admin-system-setting", action: "open dynamic form tab and attachment picker without uploading or saving" });
  } else {
    checked.push({ area: "admin-system-setting", action: "open dynamic form tab without saving", skipped: "no image attachment field visible in current active config tabs" });
  }

  await navigate(client, `${adminBase}/appSetting/publicAccount/wxTemplate`);
  await waitFor(client, "document.querySelector('.wechat-template') && document.querySelector('.el-table__body')", "admin wechat template table");
  await assertText(client, ".wechat-template", "公众号模板消息", "admin wechat template wechat tab");
  await assertText(client, ".wechat-template", "小程序公共模板", "admin wechat template public tab");
  await assertText(client, ".wechat-template", "我的订阅模板", "admin wechat template mine tab");
  await waitFor(client, `Array.from(document.querySelectorAll('.wechat-template input')).some((node) => node.getAttribute('placeholder') === '请输入模板名称 / 编号 / ID')`, "admin wechat template search input");
  await assertText(client, ".wechat-template", "同步微信模版消息", "admin wechat template sync wechat action");
  await assertText(client, ".wechat-template", "同步小程序订阅消息", "admin wechat template sync routine action");
  await clickText(client, ".wechat-template", "添加模板", "admin wechat template create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('添加模板')
      && node.innerText.includes('模板类型')
      && node.innerText.includes('模板名')
      && node.innerText.includes('模板编号')
      && node.innerText.includes('模板内容')
      && node.innerText.includes('状态');
  })`, "admin wechat template create dialog body");
  await closeVisibleDialogContaining(client, "添加模板", "admin wechat template create dialog");
  await clickText(client, ".wechat-template", "小程序公共模板", "admin wechat template public tab");
  await waitFor(client, `document.querySelector('.wechat-template')?.innerText.includes('模板标题')
    && document.querySelector('.wechat-template')?.innerText.includes('订阅类型')`, "admin wechat template public table");
  await clickText(client, ".wechat-template", "我的订阅模板", "admin wechat template mine tab");
  await waitFor(client, `document.querySelector('.wechat-template')?.innerText.includes('添加我的模板')
    && document.querySelector('.wechat-template')?.innerText.includes('一键同步我的模板到小程序')`, "admin wechat template mine actions");
  await clickText(client, ".wechat-template", "添加我的模板", "admin wechat template mine create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('添加我的模板')
      && node.innerText.includes('TID')
      && node.innerText.includes('模板标题')
      && node.innerText.includes('关键词ID')
      && node.innerText.includes('应用场景')
      && node.innerText.includes('状态');
  })`, "admin wechat template mine create dialog body");
  await closeVisibleDialogContaining(client, "添加我的模板", "admin wechat template mine create dialog");
  checked.push({ area: "admin-wechat-template", action: "open wechat public mine tabs and create dialogs without syncing switching deleting or saving" });

  await navigate(client, `${adminBase}/appSetting/publicAccount/wxMenus`);
  await waitFor(client, "document.querySelector('.wechat-menu-page') && document.querySelector('.phone-menu-preview')", "admin wechat menu page");
  await assertText(client, ".wechat-menu-page", "保存并发布", "admin wechat menu save action");
  await assertText(client, ".wechat-menu-page", "删除公众号菜单", "admin wechat menu delete remote action");
  await assertText(client, ".wechat-menu-page", "当前未配置公众号发布服务", "admin wechat menu local publish warning");
  const wechatMenuSelected = await client.evaluate(`(() => {
    const buttons = Array.from(document.querySelectorAll('.wechat-menu-page .main-menu-button, .wechat-menu-page .sub-menu-item'))
      .filter((node) => {
        const rect = node.getBoundingClientRect();
        const text = (node.innerText || node.textContent || '').trim();
        return rect.width > 0 && rect.height > 0 && text && text !== '+';
      });
    const target = buttons[0];
    if (!target) return false;
    target.scrollIntoView({ block: 'center', inline: 'center' });
    target.click();
    return true;
  })()`);
  if (wechatMenuSelected) {
    await waitFor(client, `document.querySelector('.menu-form-panel')?.innerText.includes('菜单信息')
      && document.querySelector('.menu-form-panel')?.innerText.includes('菜单名称')`, "admin wechat menu selected form");
    const openedKeywordSelector = await client.evaluate(`(() => {
      const panel = document.querySelector('.menu-form-panel');
      if (!panel || !panel.innerText.includes('关键字')) return false;
      const buttons = Array.from(panel.querySelectorAll('button, .el-button'));
      const target = buttons.find((node) => (node.innerText || node.textContent || '').includes('选择'));
      if (!target) return false;
      target.scrollIntoView({ block: 'center', inline: 'center' });
      target.click();
      return true;
    })()`);
    if (openedKeywordSelector) {
      await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
        const rect = node.getBoundingClientRect();
        return rect.width > 0
          && rect.height > 0
          && node.innerText.includes('选择关键字回复')
          && node.innerText.includes('回复类型')
          && node.innerText.includes('关键字');
      })`, "admin wechat menu keyword picker body");
      await closeVisibleDialogHeaderContaining(client, "选择关键字回复", "admin wechat menu keyword picker");
      checked.push({ area: "admin-wechat-menu", action: "open existing menu form and keyword picker without saving publishing or deleting" });
    } else {
      const openedLinkSelector = await client.evaluate(`(() => {
        const panel = document.querySelector('.menu-form-panel');
        if (!panel || !(panel.innerText.includes('跳转地址') || panel.innerText.includes('备用网页'))) return false;
        const buttons = Array.from(panel.querySelectorAll('button, .el-button'));
        const target = buttons.find((node) => (node.innerText || node.textContent || '').includes('选择'));
        if (!target) return false;
        target.scrollIntoView({ block: 'center', inline: 'center' });
        target.click();
        return true;
      })()`);
      if (openedLinkSelector) {
        await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
          const rect = node.getBoundingClientRect();
          return rect.width > 0
            && rect.height > 0
            && node.innerText.includes('选择链接')
            && node.innerText.includes('商城页面')
            && node.innerText.includes('个人中心');
        })`, "admin wechat menu link picker body");
        await closeVisibleDialogContaining(client, "选择链接", "admin wechat menu link picker");
        checked.push({ area: "admin-wechat-menu", action: "open existing menu form and link picker without saving publishing or deleting" });
      } else {
        checked.push({ area: "admin-wechat-menu", action: "open existing menu form without saving publishing or deleting", skipped: "selected menu has children or no picker action visible" });
      }
    }
  } else {
    checked.push({ area: "admin-wechat-menu", action: "open menu page", skipped: "no existing menu button visible in current saved draft" });
  }

  await navigate(client, `${adminBase}/appSetting/publicAccount/wxReply/keyword`);
  await waitFor(client, "document.querySelector('.divBox') && document.querySelector('.el-table__body')", "admin wechat keyword reply table");
  await assertText(client, ".divBox", "回复类型", "admin wechat keyword reply type filter");
  await assertText(client, ".divBox", "关键字", "admin wechat keyword reply keyword filter");
  await assertText(client, ".divBox", "添加关键字", "admin wechat keyword reply create action");
  await assertText(client, ".divBox", "回复内容", "admin wechat keyword reply content column");
  await assertText(client, ".divBox", "是否显示", "admin wechat keyword reply status column");
  await clickText(client, ".divBox", "添加关键字", "admin wechat keyword reply create form");
  await waitFor(client, "location.pathname.includes('/appSetting/publicAccount/wxReply/keyword/save') && document.querySelector('.wechat-reply-page')", "admin wechat keyword reply create page");
  await waitFor(client, `document.querySelector('.wechat-reply-page')?.innerText.includes('关键字')
    && document.querySelector('.wechat-reply-page')?.innerText.includes('规则状态')
    && document.querySelector('.wechat-reply-page')?.innerText.includes('消息类型')
    && document.querySelector('.wechat-reply-page')?.innerText.includes('保存并发布')`, "admin wechat keyword reply create form body");
  await clickText(client, ".wechat-reply-page", "返回", "admin wechat keyword reply back to list");
  await waitFor(client, "location.pathname.includes('/appSetting/publicAccount/wxReply/keyword') && document.querySelector('.el-table__body')", "admin wechat keyword reply list restored");
  checked.push({ area: "admin-wechat-reply", action: "open keyword list and create form without saving deleting switching status or uploading" });

  await navigate(client, `${adminBase}/appSetting/publicAccount/wxReply/follow`);
  await waitFor(client, "document.querySelector('.wechat-reply-page')", "admin wechat follow reply page");
  await waitFor(client, `document.querySelector('.wechat-reply-page')?.innerText.includes('规则状态')
    && document.querySelector('.wechat-reply-page')?.innerText.includes('消息类型')
    && document.querySelector('.wechat-reply-page')?.innerText.includes('保存并发布')`, "admin wechat follow reply form body");
  const openedWechatReplyAttachment = await client.evaluate(`(() => {
    const page = document.querySelector('.wechat-reply-page');
    if (!page || !page.innerText.includes('选择素材')) return false;
    const buttons = Array.from(page.querySelectorAll('button, .el-button'));
    const target = buttons.find((node) => (node.innerText || node.textContent || '').includes('选择素材'));
    if (!target) return false;
    target.scrollIntoView({ block: 'center', inline: 'center' });
    target.click();
    return true;
  })()`);
  if (openedWechatReplyAttachment) {
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && (node.innerText.includes('选择图片素材') || node.innerText.includes('选择语音素材'))
        && (node.innerText.includes('全部图片') || node.innerText.includes('全部语音') || node.innerText.includes('素材为空'));
    })`, "admin wechat follow reply attachment picker body");
    await closeVisibleDialogHeaderContaining(client, "选择", "admin wechat follow reply attachment picker");
    checked.push({ area: "admin-wechat-reply", action: "open follow reply form and local attachment picker without saving uploading or publishing" });
  } else {
    checked.push({ area: "admin-wechat-reply", action: "open follow reply form without saving uploading or publishing", skipped: "current reply type has no attachment picker visible" });
  }

  await navigate(client, `${adminBase}/operation/systemSms/config`);
  await waitFor(client, "document.querySelector('.divBox')?.innerText.includes('短信账户')", "admin sms account page");
  await assertText(client, ".divBox", "当前未配置短信服务商", "admin sms account safe warning");
  await assertText(client, ".divBox", "短信账户名称", "admin sms account name field");
  await assertText(client, ".divBox", "当前剩余条数", "admin sms account balance field");
  await assertText(client, ".divBox", "短信模板", "admin sms account template action");
  await assertText(client, ".divBox", "短信开关", "admin sms account switch action");
  await assertText(client, ".divBox", "发送记录", "admin sms account record action");

  await navigate(client, `${adminBase}/operation/systemSms/template`);
  await waitFor(client, "document.querySelector('.divBox')?.innerText.includes('添加短信模板') && document.querySelector('.el-table__body')", "admin sms template table");
  await assertText(client, ".divBox", "模板ID", "admin sms template id column");
  await assertText(client, ".divBox", "模板名称", "admin sms template title column");
  await assertText(client, ".divBox", "模板内容", "admin sms template content column");
  await assertText(client, ".divBox", "模板状态", "admin sms template status column");
  await clickText(client, ".divBox", "添加短信模板", "admin sms template create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('添加模板')
      && node.innerText.includes('模板名称')
      && node.innerText.includes('模板内容')
      && node.innerText.includes('模板类型');
  })`, "admin sms template create dialog body");
  await closeVisibleDialogContaining(client, "添加模板", "admin sms template create dialog");

  await navigate(client, `${adminBase}/operation/systemSms/message`);
  await waitFor(client, "document.querySelector('.divBox')?.innerText.includes('短信开关') && document.querySelector('.sms-config-form')", "admin sms switch form");
  await assertText(client, ".divBox", "支付成功提醒", "admin sms switch order paid field");
  await assertText(client, ".divBox", "发货提醒", "admin sms switch delivery field");
  await assertText(client, ".divBox", "验证码有效时间", "admin sms switch code expire field");
  await assertText(client, ".divBox", "提交", "admin sms switch submit action");

  await navigate(client, `${adminBase}/operation/systemSms/pay`);
  await waitFor(client, "document.querySelector('.divBox')?.innerText.includes('短信购买')", "admin sms pay page");
  await assertText(client, ".divBox", "商品采集", "admin sms pay copy tab");
  await assertText(client, ".divBox", "物流查询", "admin sms pay express query tab");
  await assertText(client, ".divBox", "电子面单打印", "admin sms pay express dump tab");
  await assertText(client, ".divBox", "暂无可购买套餐", "admin sms pay empty package");

  await navigate(client, `${adminBase}/operation/systemSms/record`);
  await waitFor(client, "document.querySelector('.divBox')?.innerText.includes('短信发送记录') && document.querySelector('.sms-record-table')", "admin sms record table");
  await assertText(client, ".divBox", "手机号", "admin sms record phone filter");
  await assertText(client, ".divBox", "短信内容", "admin sms record content column");
  await assertText(client, ".divBox", "发送状态", "admin sms record status column");
  await assertText(client, ".divBox", "平台返回", "admin sms record platform column");
  checked.push({ area: "admin-sms", action: "open account template dialog switch pay and record pages without saving buying sending or external platform calls" });

  await navigate(client, `${adminBase}/maintain/devconfiguration/configCategory`);
  await waitFor(client, "document.querySelector('.divBox')?.innerText.includes('添加分类') && document.querySelector('.el-table__body')", "admin config category table");
  await assertText(client, ".divBox", "分类昵称", "admin config category name column");
  await assertText(client, ".divBox", "英文名称", "admin config category url column");
  await assertText(client, ".divBox", "已关联的表单", "admin config category form column");
  await assertText(client, ".divBox", "启用状态", "admin config category status column");
  await clickText(client, ".divBox", "添加分类", "admin config category create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('添加分类')
      && node.innerText.includes('父级')
      && node.innerText.includes('分类名称')
      && node.innerText.includes('英文名称')
      && node.innerText.includes('排序')
      && node.innerText.includes('状态');
  })`, "admin config category create dialog body");
  await closeVisibleDialogContaining(client, "添加分类", "admin config category create dialog");
  const configCategoryEditable = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.includes('编辑') || false`);
  if (configCategoryEditable) {
    await clickText(client, ".el-table__body", "编辑", "admin config category edit dialog");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('编辑分类')
        && node.innerText.includes('父级')
        && node.innerText.includes('分类名称')
        && node.innerText.includes('英文名称')
        && node.innerText.includes('排序')
        && node.innerText.includes('状态');
    })`, "admin config category edit dialog body");
    await closeVisibleDialogContaining(client, "编辑分类", "admin config category edit dialog");
    await clickText(client, ".el-table__body", "配置列表", "admin config category form selector");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('选择已配置的表单')
        && node.innerText.includes('关键字')
        && node.innerText.includes('ID')
        && node.innerText.includes('名称')
        && node.innerText.includes('描述')
        && node.innerText.includes('更新时间');
    })`, "admin config category form selector body");
    await closeVisibleDialogHeaderContaining(client, "选择已配置的表单", "admin config category form selector");
    checked.push({ area: "admin-config-category", action: "open create edit and form selector dialogs without saving linking or deleting" });
  } else {
    checked.push({ area: "admin-config-category", action: "open create dialog without saving", skipped: "no config category row visible in current database" });
  }

  await navigate(client, `${adminBase}/maintain/devconfiguration/combineddata`);
  await waitFor(client, "document.querySelector('.divBox')?.innerText.includes('添加数据组') && document.querySelector('.el-table__body')", "admin combined data table");
  await assertText(client, ".divBox", "数据搜索", "admin combined data search");
  await assertText(client, ".divBox", "数据组名称", "admin combined data name column");
  await assertText(client, ".divBox", "简介", "admin combined data intro column");
  await assertText(client, ".divBox", "表单数据ID", "admin combined data form id column");
  await clickText(client, ".divBox", "添加数据组", "admin combined data create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('创建数据组')
      && node.innerText.includes('数据组名称')
      && node.innerText.includes('数据简介')
      && node.innerText.includes('表单数据ID')
      && node.innerText.includes('选择模板数据');
  })`, "admin combined data create dialog body");
  await clickText(client, ".el-dialog", "选择模板数据", "admin combined data form selector");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('选择表单模板')
      && node.innerText.includes('关键字')
      && node.innerText.includes('ID')
      && node.innerText.includes('名称')
      && node.innerText.includes('描述')
      && node.innerText.includes('更新时间');
  })`, "admin combined data form selector body");
  await closeVisibleDialogHeaderContaining(client, "选择表单模板", "admin combined data form selector");
  await closeVisibleDialogContaining(client, "创建数据组", "admin combined data create dialog");
  const combinedDataEditable = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.includes('编辑') || false`);
  if (combinedDataEditable) {
    await clickText(client, ".el-table__body", "编辑", "admin combined data edit dialog");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('编辑数据组')
        && node.innerText.includes('数据组名称')
        && node.innerText.includes('数据简介')
        && node.innerText.includes('表单数据ID');
    })`, "admin combined data edit dialog body");
    await closeVisibleDialogContaining(client, "编辑数据组", "admin combined data edit dialog");
    const hasDataList = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.includes('数据列表') || false`);
    if (hasDataList) {
      await clickText(client, ".el-table__body", "数据列表", "admin combined data list dialog");
      const dataListOpened = await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
        const rect = node.getBoundingClientRect();
        return rect.width > 0
          && rect.height > 0
          && node.innerText.includes('组合数据列表')
          && node.innerText.includes('状态')
          && node.innerText.includes('添加数据')
          && node.innerText.includes('编号');
      })`, "admin combined data list dialog body", 8000).then(() => true).catch(() => false);
      if (dataListOpened) {
        await closeVisibleDialogHeaderContaining(client, "组合数据列表", "admin combined data list dialog");
        checked.push({ area: "admin-combined-data", action: "open create template selector edit and data-list dialogs without saving deleting or linking" });
      } else {
        checked.push({ area: "admin-combined-data", action: "open create template selector and edit dialogs without saving", skipped: "first combined data row has no linked form or data list did not open" });
      }
    } else {
      checked.push({ area: "admin-combined-data", action: "open create template selector and edit dialogs without saving", skipped: "no data-list action visible in current database" });
    }
  } else {
    checked.push({ area: "admin-combined-data", action: "open create template selector without saving", skipped: "no combined data row visible in current database" });
  }

  await navigate(client, `${adminBase}/maintain/devconfiguration/formConfig`);
  await waitFor(client, "document.querySelector('.divBox')?.innerText.includes('创建表单') && document.querySelector('.el-table__body')", "admin form config table");
  await assertText(client, ".divBox", "关键字", "admin form config search");
  await assertText(client, ".divBox", "名称", "admin form config name column");
  await assertText(client, ".divBox", "描述", "admin form config description column");
  await assertText(client, ".divBox", "字段数", "admin form config field count column");
  await assertText(client, ".divBox", "更新时间", "admin form config update time column");
  await clickText(client, ".divBox", "创建表单", "admin form config create editor");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('创建表单')
      && node.innerText.includes('表单名称')
      && node.innerText.includes('表单描述')
      && node.innerText.includes('字段配置')
      && node.innerText.includes('JSON 配置')
      && node.innerText.includes('表单预览');
  })`, "admin form config create editor body");
  await clickText(client, ".el-dialog", "JSON 配置", "admin form config create json tab");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('创建表单')
      && Boolean(node.querySelector('.json-editor textarea'));
  })`, "admin form config create json textarea");
  await clickText(client, ".el-dialog", "表单预览", "admin form config create preview tab");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('创建表单')
      && node.innerText.includes('表单预览');
  })`, "admin form config create preview body");
  await closeVisibleDialogContaining(client, "创建表单", "admin form config create editor");
  const formConfigEditable = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.includes('编辑') || false`);
  if (formConfigEditable) {
    await clickText(client, ".el-table__body", "编辑", "admin form config edit editor");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('编辑表单')
        && node.innerText.includes('表单名称')
        && node.innerText.includes('表单描述')
        && node.innerText.includes('字段配置')
        && node.innerText.includes('JSON 配置')
        && node.innerText.includes('表单预览');
    })`, "admin form config edit editor body");
    await clickText(client, ".el-dialog", "JSON 配置", "admin form config edit json tab");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('编辑表单')
        && Boolean(node.querySelector('.json-editor textarea'));
    })`, "admin form config edit json textarea");
    await clickText(client, ".el-dialog", "表单预览", "admin form config edit preview tab");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('编辑表单')
        && node.innerText.includes('表单预览');
    })`, "admin form config edit preview body");
    await closeVisibleDialogContaining(client, "编辑表单", "admin form config edit editor");
    checked.push({ area: "admin-form-config", action: "open create and edit editors switch json preview tabs without adding fields saving or deleting" });
  } else {
    checked.push({ area: "admin-form-config", action: "open create editor switch json preview tabs without saving", skipped: "no form config row visible in current database" });
  }

  await navigate(client, `${adminBase}/maintain/clearCache`);
  await waitFor(client, "document.querySelector('.clear-cache-manager')?.innerText.includes('清除缓存')", "admin clear cache page");
  const clearCacheButtonReady = await client.evaluate(`(() => {
    const button = Array.from(document.querySelectorAll('.clear-cache-manager button, .clear-cache-manager .el-button'))
      .find((node) => (node.innerText || node.textContent || '').includes('清除缓存'));
    if (!button) return false;
    const rect = button.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0 && !button.disabled;
  })()`);
  if (!clearCacheButtonReady) {
    throw new Error("admin clear cache action button not visible or disabled");
  }
  checked.push({ area: "admin-clear-cache", action: "open clear cache page and verify action button without clearing redis or runtime cache" });

  await navigate(client, `${adminBase}/operation/deliverGoods/takeGoods/deliveryAddress`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin pickup point table");
  await assertText(client, ".legacy-main", "添加提货点", "admin pickup point create action");
  await clickText(client, ".legacy-main", "添加提货点", "admin pickup point create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('添加提货点')
      && node.innerText.includes('提货点名称')
      && node.innerText.includes('提货点手机号')
      && node.innerText.includes('提货点地址')
      && node.innerText.includes('提货点logo')
      && node.innerText.includes('经纬度');
  })`, "admin pickup point create dialog body");
  await clickText(client, ".el-dialog", "选择素材", "admin pickup point attachment picker");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('选择图片')
      && (node.innerText.includes('全部图片') || node.innerText.includes('暂无图片'));
  })`, "admin pickup point attachment picker body");
  await closeVisibleDialogHeaderContaining(client, "选择图片", "admin pickup point attachment picker");
  await closeVisibleDialogHeaderContaining(client, "添加提货点", "admin pickup point create dialog");
  const pickupPointEditable = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.includes('编辑') || false`);
  if (pickupPointEditable) {
    await clickText(client, ".el-table__body", "编辑", "admin pickup point edit dialog");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('修改提货点')
        && node.innerText.includes('提货点名称')
        && node.innerText.includes('提货点手机号')
        && node.innerText.includes('提货点地址')
        && node.innerText.includes('经纬度');
    })`, "admin pickup point edit dialog body");
    await closeVisibleDialogHeaderContaining(client, "修改提货点", "admin pickup point edit dialog");
    checked.push({ area: "admin-pickup-point", action: "open create attachment picker and edit dialogs without saving" });
  } else {
    checked.push({ area: "admin-pickup-point", action: "open create attachment picker without saving", skipped: "no pickup point row visible in current database" });
  }

  await navigate(client, `${adminBase}/operation/deliverGoods/takeGoods/collateUser`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin store staff table");
  await assertText(client, ".legacy-main", "添加核销员", "admin store staff create action");
  await clickText(client, ".legacy-main", "添加核销员", "admin store staff create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('添加核销员')
      && node.innerText.includes('管理员')
      && node.innerText.includes('所属提货点')
      && node.innerText.includes('核销员名称')
      && node.innerText.includes('手机号码');
  })`, "admin store staff create dialog body");
  await clickText(client, ".el-dialog", "选择管理员", "admin store staff admin picker");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('请选择管理员')
      && node.innerText.includes('姓名')
      && node.innerText.includes('账号')
      && node.innerText.includes('身份');
  })`, "admin store staff admin picker body");
  await closeVisibleDialogHeaderContaining(client, "请选择管理员", "admin store staff admin picker");
  await closeVisibleDialogHeaderContaining(client, "添加核销员", "admin store staff create dialog");
  const storeStaffEditable = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.includes('编辑') || false`);
  if (storeStaffEditable) {
    await clickText(client, ".el-table__body", "编辑", "admin store staff edit dialog");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('修改核销员')
        && node.innerText.includes('管理员')
        && node.innerText.includes('所属提货点')
        && node.innerText.includes('核销员名称')
        && node.innerText.includes('手机号码');
    })`, "admin store staff edit dialog body");
    await closeVisibleDialogHeaderContaining(client, "修改核销员", "admin store staff edit dialog");
    checked.push({ area: "admin-store-staff", action: "open create admin picker and edit dialogs without saving" });
  } else {
    checked.push({ area: "admin-store-staff", action: "open create admin picker without saving", skipped: "no store staff row visible in current database" });
  }

  await navigate(client, `${adminBase}/operation/deliverGoods/takeGoods/collateOrder`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin writeoff order table");
  await assertText(client, ".legacy-main", "时间选择", "admin writeoff order time filter");
  await assertText(client, ".legacy-main", "选择门店", "admin writeoff order store filter");
  await assertText(client, ".legacy-main", "订单数量", "admin writeoff order count card");
  await assertText(client, ".legacy-main", "退款总金额", "admin writeoff order refund amount card");
  await assertText(client, ".legacy-main", "订单号", "admin writeoff order number column");
  checked.push({ area: "admin-writeoff-order", action: "open readonly list filters stats and table" });

  await navigate(client, `${adminBase}/maintain/logistics/companyList`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin express company table");
  await assertText(client, ".legacy-main", "同步物流公司", "admin express company sync action");
  await assertText(client, ".legacy-main", "物流公司名称", "admin express company name column");
  const expressCompanyEditable = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.includes('编辑') || false`);
  if (expressCompanyEditable) {
    await clickText(client, ".el-table__body", "编辑", "admin express company edit dialog");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('编辑物流公司')
        && node.innerText.includes('排序')
        && node.innerText.includes('是否启用')
        && node.innerText.includes('确定');
    })`, "admin express company edit dialog body");
    await closeVisibleDialogContaining(client, "编辑物流公司", "admin express company edit dialog");
    checked.push({ area: "admin-express-company", action: "open edit dialog without saving" });
  } else {
    checked.push({ area: "admin-express-company", action: "open edit dialog", skipped: "no express company row visible in current database" });
  }

  await navigate(client, `${adminBase}/maintain/logistics/cityList`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin city data table");
  await assertText(client, ".legacy-main", "编号", "admin city data id column");
  await assertText(client, ".legacy-main", "上级名称", "admin city data parent column");
  await assertText(client, ".legacy-main", "地区名称", "admin city data name column");
  const cityEditable = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.includes('编辑') || false`);
  if (cityEditable) {
    await clickText(client, ".el-table__body", "编辑", "admin city data edit confirm");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-message-box')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('修改此配置项')
        && node.innerText.includes('确定')
        && node.innerText.includes('取消');
    })`, "admin city data edit confirm body");
    const cityConfirmClosed = await client.evaluate(`(() => {
      const box = Array.from(document.querySelectorAll('.el-message-box')).find((node) => {
        const rect = node.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0 && node.innerText.includes('修改此配置项');
      });
      const button = box && Array.from(box.querySelectorAll('button, .el-button')).find((node) => (node.innerText || node.textContent || '').includes('取消'));
      if (!button) return false;
      button.click();
      return true;
    })()`);
    if (!cityConfirmClosed) {
      throw new Error("admin city data edit confirm: cancel button not found");
    }
    await waitFor(client, `!Array.from(document.querySelectorAll('.el-message-box')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0 && rect.height > 0 && node.innerText.includes('修改此配置项');
    })`, "admin city data edit confirm closed");
    checked.push({ area: "admin-city-data", action: "open edit confirm without saving" });
  } else {
    checked.push({ area: "admin-city-data", action: "open edit confirm", skipped: "no city row visible in current database" });
  }

  await navigate(client, `${adminBase}/operation/deliverGoods/freightSet`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin shipping template table");
  await assertText(client, ".legacy-main", "添加运费模板", "admin shipping template create action");
  await clickText(client, ".legacy-main", "添加运费模板", "admin shipping template create drawer");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-drawer')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('运费模板')
      && node.innerText.includes('模板名称')
      && node.innerText.includes('包邮方式')
      && node.innerText.includes('排序')
      && node.innerText.includes('确定');
  })`, "admin shipping template create drawer body");
  await clickFirst(client, ".el-drawer__close-btn", "admin close shipping template create drawer");
  const shippingTemplateEditable = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.includes('编辑') || false`);
  if (shippingTemplateEditable) {
    await clickText(client, ".el-table__body", "编辑", "admin shipping template edit drawer");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-drawer')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('运费模板')
        && node.innerText.includes('模板名称')
        && node.innerText.includes('包邮方式')
        && node.innerText.includes('排序')
        && node.innerText.includes('确定');
    })`, "admin shipping template edit drawer body");
    checked.push({ area: "admin-shipping-template", action: "open create and edit drawers without saving" });
    await clickFirst(client, ".el-drawer__close-btn", "admin close shipping template edit drawer");
  } else {
    checked.push({ area: "admin-shipping-template", action: "open create drawer without saving", skipped: "no shipping template row visible in current database" });
  }

  await navigate(client, `${adminBase}/content/classifManager`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin article category table");
  await assertText(client, ".legacy-main", "添加文章分类", "admin article category create action");
  await clickText(client, ".legacy-main", "添加文章分类", "admin article category create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('添加文章分类')
      && node.innerText.includes('分类名称')
      && node.innerText.includes('分类图标')
      && node.innerText.includes('排序')
      && node.innerText.includes('状态')
      && node.innerText.includes('确定');
  })`, "admin article category create dialog body");
  await clickText(client, ".el-dialog", "选择素材", "admin article category attachment picker");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('选择图片')
      && (node.innerText.includes('全部图片') || node.innerText.includes('暂无图片'));
  })`, "admin article category attachment picker body");
  await closeVisibleDialogHeaderContaining(client, "选择图片", "admin article category attachment picker");
  await closeVisibleDialogContaining(client, "添加文章分类", "admin article category create dialog");
  const articleCategoryEditable = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.includes('编辑') || false`);
  if (articleCategoryEditable) {
    await clickText(client, ".el-table__body", "编辑", "admin article category edit dialog");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('编辑文章分类')
        && node.innerText.includes('分类名称')
        && node.innerText.includes('分类图标')
        && node.innerText.includes('排序')
        && node.innerText.includes('状态');
    })`, "admin article category edit dialog body");
    await closeVisibleDialogContaining(client, "编辑文章分类", "admin article category edit dialog");
    checked.push({ area: "admin-article-category", action: "open create attachment picker and edit dialogs without saving" });
  } else {
    checked.push({ area: "admin-article-category", action: "open create attachment picker without saving", skipped: "no article category row visible in current database" });
  }

  await navigate(client, `${adminBase}/content/articleManager`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin article table");
  await assertText(client, ".legacy-main", "添加文章", "admin article create action");
  await clickText(client, ".legacy-main", "添加文章", "admin article create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('添加文章')
      && node.innerText.includes('标题')
      && node.innerText.includes('作者')
      && node.innerText.includes('文章分类')
      && node.innerText.includes('图文封面')
      && node.innerText.includes('文章内容');
  })`, "admin article create dialog body");
  await clickText(client, ".el-dialog", "选择素材", "admin article attachment picker");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('选择图片')
      && (node.innerText.includes('全部图片') || node.innerText.includes('暂无图片'));
  })`, "admin article attachment picker body");
  await closeVisibleDialogHeaderContaining(client, "选择图片", "admin article attachment picker");
  await closeVisibleDialogContaining(client, "添加文章", "admin article create dialog");
  const articleEditable = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.includes('编辑') || false`);
  if (articleEditable) {
    await clickText(client, ".el-table__body", "编辑", "admin article edit dialog");
    await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0
        && rect.height > 0
        && node.innerText.includes('编辑文章')
        && node.innerText.includes('标题')
        && node.innerText.includes('作者')
        && node.innerText.includes('文章分类')
        && node.innerText.includes('图文封面')
        && node.innerText.includes('文章内容');
    })`, "admin article edit dialog body");
    await closeVisibleDialogContaining(client, "编辑文章", "admin article edit dialog");
    checked.push({ area: "admin-article", action: "open create attachment picker and edit dialogs without saving" });
  } else {
    checked.push({ area: "admin-article", action: "open create attachment picker without saving", skipped: "no article row visible in current database" });
  }

  await navigate(client, `${adminBase}/design/devise`);
  await waitFor(client, "document.querySelector('.page-diy-manager')", "admin page diy manager");
  await assertText(client, ".page-diy-manager", "首页装修", "admin page diy home action");
  await clickText(client, ".page-diy-manager", "首页装修", "admin page diy open designer");
  await waitFor(client, "document.querySelector('.el-drawer__body')?.innerText.includes('模板名称')", "admin page diy home base drawer");
  await assertText(client, ".el-drawer__body", "页面标题", "admin page diy home base drawer fields");
  await clickFirst(client, ".el-drawer__close-btn", "admin close page diy home base drawer");
  await clickText(client, ".page-diy-manager", "自定义页面", "admin page diy custom tab");
  await waitFor(client, "document.querySelector('.page-panel .el-table__body')", "admin page diy custom table");
  await clickText(client, ".page-panel .el-table__body", "设计", "admin page diy open custom designer");
  await waitFor(client, "document.querySelector('.page-diy-designer-drawer .designer-layout')", "admin page diy designer layout");
  await assertText(client, ".page-diy-designer-drawer", "当前页面", "admin page diy designer header");
  await assertText(client, ".page-diy-designer-drawer .designer-palette", "基础组件", "admin page diy designer palette");
  await assertText(client, ".page-diy-designer-drawer .designer-config", "组件标题", "admin page diy designer config");
  checked.push({ area: "admin-page-diy", action: "open home base drawer and custom designer without saving" });
  await pressEscape(client);

  await navigate(client, `${adminBase}/design/viewDesign`);
  await waitFor(client, "document.querySelector('.layout-page')?.innerText.includes('个人中心服务')", "admin page layout manager");
  await assertText(client, ".layout-page", "用户信息", "admin page layout phone preview user card");
  await assertText(client, ".layout-page", "我的服务", "admin page layout phone preview service grid");
  await assertText(client, ".layout-page", "保存", "admin page layout save action visible");
  await assertText(client, ".layout-page", "选择链接", "admin page layout link selector action");
  await clickText(client, ".layout-tabs", "首页商品 Tab", "admin page layout index product tab");
  await waitFor(client, "document.querySelector('.layout-page')?.innerText.includes('首页商品 Tab') && document.querySelector('.layout-page')?.innerText.includes('超值爆款')", "admin page layout index product tab body");
  await assertText(client, ".layout-page", "标题", "admin page layout index product title field");
  await assertText(client, ".layout-page", "说明", "admin page layout index product info field");
  await clickText(client, ".layout-tabs", "分类页配置", "admin page layout category tab");
  await waitFor(client, "document.querySelector('.category-config-panel')?.innerText.includes('分类页样式')", "admin page layout category config");
  await assertText(client, ".category-config-panel", "双栏分类", "admin page layout category style");
  await assertText(client, ".category-config-panel", "显示分类导航", "admin page layout category nav switch");
  await clickText(client, ".layout-tabs", "首页导航", "admin page layout home menu tab");
  await waitFor(client, "document.querySelector('.layout-page')?.innerText.includes('首页导航') && document.querySelector('.layout-page')?.innerText.includes('选择素材')", "admin page layout home menu body");
  await clickText(client, ".layout-page", "选择链接", "admin page layout link picker");
  await waitFor(client, "document.querySelector('.el-dialog')?.innerText.includes('选择链接')", "admin page layout link picker dialog");
  await assertText(client, ".el-dialog", "商城页面", "admin page layout link mall tab");
  await assertText(client, ".el-dialog", "商品分类", "admin page layout dynamic category link");
  await assertText(client, ".el-dialog", "自定义链接", "admin page layout custom link tab");
  await closeVisibleDialogContaining(client, "选择链接", "admin page layout link picker");
  await clickText(client, ".layout-page", "选择素材", "admin page layout attachment picker");
  await waitFor(client, "document.querySelector('.el-dialog')?.innerText.includes('选择素材')", "admin page layout attachment dialog");
  await assertText(client, ".el-dialog", "选择已有素材", "admin page layout attachment content");
  await closeVisibleDialogHeaderContaining(client, "选择素材", "admin page layout attachment picker");
  checked.push({ area: "admin-page-layout", action: "open viewDesign tabs link picker and attachment picker without saving sorting deleting or switching status" });

  await navigate(client, `${adminBase}/design/theme`);
  await waitFor(client, "document.querySelector('.theme-page')?.innerText.includes('热情红')", "admin theme color page");
  await assertText(client, ".theme-page", "热情红", "admin theme red option");
  await assertText(client, ".theme-page", "家居橙", "admin theme orange option");
  await assertText(client, ".theme-page", "生鲜绿", "admin theme green option");
  await assertText(client, ".theme-page", "海鲜蓝", "admin theme blue option");
  await assertText(client, ".theme-page", "女神粉", "admin theme pink option");
  await assertText(client, ".theme-page", "保存", "admin theme save action visible");
  checked.push({ area: "admin-theme-color", action: "open theme color page readonly without saving system color config" });

  await navigate(client, `${adminBase}/maintain/authCRMEB`);
  await waitFor(client, "document.querySelector('.auth-crmeb-info')?.innerText.includes('申请授权')", "admin auth crmeb info page");
  await assertText(client, ".auth-crmeb-info", "商城名称", "admin auth site name field");
  await assertText(client, ".auth-crmeb-info", "版本号", "admin auth version field");
  await assertText(client, ".auth-crmeb-info", "授权状态", "admin auth status field");
  await assertText(client, ".auth-crmeb-info", "版权公司", "admin auth company field");
  await assertText(client, ".auth-crmeb-info", "备案号", "admin auth icp field");
  await assertText(client, ".auth-crmeb-info", "服务说明", "admin auth service note");
  checked.push({ area: "admin-auth-crmeb", action: "open authorization info readonly without external license check or writing config" });

  await navigate(client, `${adminBase}/sites/index`);
  await waitFor(client, "document.querySelector('.site-list')?.innerText.includes('站点信息') && document.querySelector('.el-table__body')", "admin site list page");
  await assertText(client, ".site-list", "站点信息", "admin site list site column");
  await assertText(client, ".site-list", "网站地址", "admin site list url column");
  await assertText(client, ".site-list", "支付回调 API", "admin site list api column");
  await assertText(client, ".site-list", "移动商城 API", "admin site list front api column");
  await assertText(client, ".site-list", "图片域名", "admin site list upload domain column");
  await assertText(client, ".site-list", "版本号", "admin site list version column");
  await assertText(client, ".site-list", "站点配置说明", "admin site list config note");
  checked.push({ area: "admin-site-list", action: "open single-site config list readonly without changing domain upload or callback settings" });

  await navigate(client, `${adminBase}/statistic/product`);
  await waitFor(client, "document.querySelector('.statistic-page')?.innerText.includes('商品趋势')", "admin product statistic page");
  await assertText(client, ".statistic-page", "商品总数", "admin product statistic total card");
  await assertText(client, ".statistic-page", "新增商品", "admin product statistic new card");
  await assertText(client, ".statistic-page", "支付件数", "admin product statistic pay count card");
  await assertText(client, ".statistic-page", "商品排行", "admin product statistic ranking");
  await assertText(client, ".statistic-page", "销售额", "admin product statistic sales field");
  await clickText(client, ".statistic-page", "本年", "admin product statistic year filter");
  await waitFor(client, "document.querySelector('.statistic-page .el-radio-button.is-active')?.innerText.includes('本年')", "admin product statistic year active");
  checked.push({ area: "admin-statistic-product", action: "open product statistics and switch date filter readonly without exporting or writing data" });

  await navigate(client, `${adminBase}/statistic/statuser`);
  await waitFor(client, "document.querySelector('.statistic-page')?.innerText.includes('用户趋势')", "admin user statistic page");
  await assertText(client, ".statistic-page", "累计用户", "admin user statistic total card");
  await assertText(client, ".statistic-page", "正常用户", "admin user statistic enabled card");
  await assertText(client, ".statistic-page", "用户分布", "admin user statistic distribution");
  await clickText(client, ".statistic-page", "性别", "admin user statistic sex tab");
  await waitFor(client, "document.querySelector('.statistic-page .el-tabs__item.is-active')?.innerText.includes('性别')", "admin user statistic sex tab active");
  await clickText(client, ".statistic-page", "地区", "admin user statistic area tab");
  await waitFor(client, "document.querySelector('.statistic-page .el-tabs__item.is-active')?.innerText.includes('地区')", "admin user statistic area tab active");
  checked.push({ area: "admin-statistic-user", action: "open user statistics and switch distribution tabs readonly without exporting or writing data" });

  await navigate(client, `${adminBase}/statistic/transaction`);
  await waitFor(client, "document.querySelector('.statistic-page')?.innerText.includes('交易趋势')", "admin trade statistic page");
  await assertText(client, ".statistic-page", "累计订单数", "admin trade statistic order total card");
  await assertText(client, ".statistic-page", "累计支付金额", "admin trade statistic amount total card");
  await assertText(client, ".statistic-page", "退款中订单", "admin trade statistic refunding card");
  await assertText(client, ".statistic-page", "支付订单数", "admin trade statistic pay order card");
  await clickText(client, ".statistic-page", "最近7天", "admin trade statistic lately7 filter");
  await waitFor(client, "document.querySelector('.statistic-page .el-radio-button.is-active')?.innerText.includes('最近7天')", "admin trade statistic lately7 active");
  checked.push({ area: "admin-statistic-trade", action: "open trade statistics and switch date filter readonly without exporting or writing data" });

  await navigate(client, `${adminBase}/marketing/border/list`);
  await waitFor(client, "document.querySelector('.activity-style')?.innerText.includes('添加活动边框') && document.querySelector('.el-table__body')", "admin activity border list");
  await assertText(client, ".activity-style", "创建时间", "admin activity border create time filter");
  await assertText(client, ".activity-style", "活动状态", "admin activity border status filter");
  await assertText(client, ".activity-style", "活动名称", "admin activity border name filter");
  await assertText(client, ".activity-style", "活动边框", "admin activity border image column");
  await assertText(client, ".activity-style", "使用范围", "admin activity border scope column");
  await assertText(client, ".activity-style", "活动日期", "admin activity border date column");
  await assertText(client, ".activity-style", "是否开启", "admin activity border switch column");
  await clickText(client, ".activity-style", "添加活动边框", "admin activity border create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('添加活动边框')
      && node.innerText.includes('基础设置')
      && node.innerText.includes('活动名称')
      && node.innerText.includes('活动时间')
      && node.innerText.includes('活动边框')
      && node.innerText.includes('下一步');
  })`, "admin activity border create dialog body");
  await clickText(client, ".el-dialog", "选择素材", "admin activity border attachment picker");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('选择素材')
      && (node.innerText.includes('全部图片') || node.innerText.includes('素材为空'));
  })`, "admin activity border attachment picker body");
  const borderAttachmentClosed = await client.evaluate(`(() => {
    const dialog = Array.from(document.querySelectorAll('.el-dialog')).find((node) => {
      const rect = node.getBoundingClientRect();
      const title = node.querySelector('.el-dialog__title')?.innerText || '';
      return rect.width > 0 && rect.height > 0 && title.includes('选择素材');
    });
    const button = dialog?.querySelector('.el-dialog__headerbtn');
    if (!button) return false;
    button.click();
    return true;
  })()`);
  if (!borderAttachmentClosed) {
    throw new Error("admin activity border attachment picker: close button not found");
  }
  await waitFor(client, `!Array.from(document.querySelectorAll('.el-overlay .el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    const title = node.querySelector('.el-dialog__title')?.innerText || '';
    return rect.width > 0 && rect.height > 0 && title.includes('选择素材');
  })`, "admin activity border attachment picker closed");
  await clickText(client, ".el-dialog", "使用范围", "admin activity border scope tab");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('全部商品参与')
      && node.innerText.includes('指定商品参与')
      && node.innerText.includes('指定分类参与');
  })`, "admin activity border scope tab body");
  checked.push({ area: "admin-activity-border", action: "open border list create dialog attachment picker and scope tab without saving uploading switching status or deleting" });
  await closeVisibleDialogHeaderContaining(client, "添加活动边框", "admin activity border create dialog");

  await navigate(client, `${adminBase}/marketing/atmosphere/list`);
  await waitFor(client, "document.querySelector('.activity-style')?.innerText.includes('添加氛围图') && document.querySelector('.el-table__body')", "admin activity atmosphere list");
  await assertText(client, ".activity-style", "创建时间", "admin activity atmosphere create time filter");
  await assertText(client, ".activity-style", "活动状态", "admin activity atmosphere status filter");
  await assertText(client, ".activity-style", "活动名称", "admin activity atmosphere name filter");
  await assertText(client, ".activity-style", "氛围图", "admin activity atmosphere image column");
  await assertText(client, ".activity-style", "使用范围", "admin activity atmosphere scope column");
  await assertText(client, ".activity-style", "活动日期", "admin activity atmosphere date column");
  await assertText(client, ".activity-style", "是否开启", "admin activity atmosphere switch column");
  await clickText(client, ".activity-style", "添加氛围图", "admin activity atmosphere create dialog");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('添加氛围图')
      && node.innerText.includes('基础设置')
      && node.innerText.includes('活动名称')
      && node.innerText.includes('活动时间')
      && node.innerText.includes('活动氛围图')
      && node.innerText.includes('下一步');
  })`, "admin activity atmosphere create dialog body");
  await clickText(client, ".el-dialog", "使用范围", "admin activity atmosphere scope tab");
  await waitFor(client, `Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
    const rect = node.getBoundingClientRect();
    return rect.width > 0
      && rect.height > 0
      && node.innerText.includes('全部商品参与')
      && node.innerText.includes('指定商品参与')
      && node.innerText.includes('指定分类参与');
  })`, "admin activity atmosphere scope tab body");
  checked.push({ area: "admin-activity-atmosphere", action: "open atmosphere list and create scope tab without saving uploading switching status or deleting" });
  await closeVisibleDialogHeaderContaining(client, "添加氛围图", "admin activity atmosphere create dialog");

  await navigate(client, `${adminBase}/marketing/seckill/config`);
  await waitFor(client, "document.querySelector('.seckill-config-manager')?.innerText.includes('添加秒杀配置') && document.querySelector('.el-table__body')", "admin seckill config list");
  await assertText(client, ".seckill-config-manager", "秒杀状态", "admin seckill config status filter");
  await assertText(client, ".seckill-config-manager", "秒杀名称", "admin seckill config name filter");
  await assertText(client, ".seckill-config-manager", "秒杀时段", "admin seckill config time column");
  await assertText(client, ".seckill-config-manager", "轮播图", "admin seckill config slider column");
  await clickText(client, ".seckill-config-manager", "添加秒杀配置", "admin seckill config create dialog");
  await waitFor(client, "document.querySelector('.el-dialog')?.innerText.includes('秒杀名称') && document.querySelector('.el-dialog')?.innerText.includes('秒杀时段')", "admin seckill config create dialog body");
  await assertText(client, ".el-dialog", "轮播图", "admin seckill config create slider field");
  await assertText(client, ".el-dialog", "排序", "admin seckill config create sort field");
  await assertText(client, ".el-dialog", "状态", "admin seckill config create status field");
  await assertText(client, ".el-dialog", "选择素材", "admin seckill config create attachment action visible");
  await closeVisibleDialogContaining(client, "添加数据", "admin seckill config create dialog");
  const seckillConfigEditable = await client.evaluate(`document.querySelector('.seckill-config-manager .el-table__body')?.innerText.includes('编辑') || false`);
  if (seckillConfigEditable) {
    await clickText(client, ".seckill-config-manager .el-table__body", "编辑", "admin seckill config edit dialog");
    await waitFor(client, "document.querySelector('.el-dialog')?.innerText.includes('编辑数据') && document.querySelector('.el-dialog')?.innerText.includes('秒杀时段')", "admin seckill config edit dialog body");
    await closeVisibleDialogContaining(client, "编辑数据", "admin seckill config edit dialog");
    checked.push({ area: "admin-seckill-config", action: "open create attachment picker and edit dialogs without saving deleting switching status or adding product" });
  } else {
    checked.push({ area: "admin-seckill-config", action: "open create dialog without saving", skipped: "no seckill config row visible in current database" });
  }

  await navigate(client, `${adminBase}/marketing/seckill/list`);
  await waitFor(client, "document.querySelector('.seckill-product-list')?.innerText.includes('添加秒杀商品') && document.querySelector('.el-table__body')", "admin seckill product list");
  await assertText(client, ".seckill-product-list", "是否显示", "admin seckill product status filter");
  await assertText(client, ".seckill-product-list", "配置名称", "admin seckill product manager filter");
  await assertText(client, ".seckill-product-list", "商品搜索", "admin seckill product search filter");
  await assertText(client, ".seckill-product-list", "秒杀价", "admin seckill product price column");
  await assertText(client, ".seckill-product-list", "限量剩余", "admin seckill product quota column");
  await clickText(client, ".seckill-product-list", "添加秒杀商品", "admin seckill product create form");
  await waitFor(client, "document.querySelector('.seckill-product-form')?.innerText.includes('添加秒杀商品')", "admin seckill product create form body");
  await assertText(client, ".seckill-product-form", "主商品ID", "admin seckill product create product field");
  await assertText(client, ".seckill-product-form", "商品规格", "admin seckill product create spec field");
  await assertText(client, ".seckill-product-form", "活动规格", "admin seckill product create sku table");
  await clickText(client, ".seckill-product-form", "商品详情", "admin seckill product create detail tab");
  await assertText(client, ".seckill-product-form", "商品详情", "admin seckill product create detail field");
  checked.push({ area: "admin-seckill-product", action: "open list and create form tabs without selecting product saving exporting deleting or switching status" });
  await clickText(client, ".page-header", "返回列表", "admin seckill product create back");
  await waitFor(client, "document.querySelector('.seckill-product-list')?.innerText.includes('添加秒杀商品')", "admin seckill product list after create");
  const seckillProductHasDetail = await client.evaluate(`document.querySelector('.seckill-product-list .el-table__body')?.innerText.includes('详情') || false`);
  if (seckillProductHasDetail) {
    await clickText(client, ".seckill-product-list .el-table__body", "详情", "admin seckill product detail drawer");
    await waitFor(client, "document.querySelector('.el-drawer__body')?.innerText.includes('商品标题') && document.querySelector('.el-drawer__body')?.innerText.includes('价格')", "admin seckill product detail drawer body");
    await assertText(client, ".el-drawer__body", "库存", "admin seckill product detail stock field");
    await clickFirst(client, ".el-drawer__close-btn", "admin close seckill product detail drawer");
    checked.push({ area: "admin-seckill-product", action: "open detail drawer without writing" });
  } else {
    checked.push({ area: "admin-seckill-product", action: "detail drawer", skipped: "no seckill product row visible in current database" });
  }

  await navigate(client, `${adminBase}/marketing/bargain/bargainGoods`);
  await waitFor(client, "document.querySelector('.bargain-manager')?.innerText.includes('添加砍价商品') && document.querySelector('.el-table__body')", "admin bargain goods list");
  await assertText(client, ".bargain-manager", "砍价状态", "admin bargain goods status filter");
  await assertText(client, ".bargain-manager", "商品搜索", "admin bargain goods search filter");
  await assertText(client, ".bargain-manager", "砍价价格", "admin bargain goods price column");
  await assertText(client, ".bargain-manager", "最低价", "admin bargain goods min price column");
  await clickText(client, ".bargain-manager", "添加砍价商品", "admin bargain create form");
  await waitFor(client, "document.querySelector('.bargain-product-form')?.innerText.includes('添加砍价商品')", "admin bargain create form body");
  await assertText(client, ".bargain-product-form", "主商品ID", "admin bargain create product field");
  await assertText(client, ".bargain-product-form", "商品属性", "admin bargain create sku field");
  await assertText(client, ".bargain-product-form", "砍价人数", "admin bargain create people field");
  await clickText(client, ".bargain-product-form", "砍价规则", "admin bargain create rule tab");
  await assertText(client, ".bargain-product-form", "砍价规则", "admin bargain create rule field");
  await clickText(client, ".bargain-product-form", "商品详情", "admin bargain create detail tab");
  await assertText(client, ".bargain-product-form", "商品详情", "admin bargain create detail field");
  checked.push({ area: "admin-bargain-product", action: "open list and create form tabs without selecting product saving exporting deleting or switching status" });
  await clickText(client, ".page-header", "返回列表", "admin bargain create back");
  await waitFor(client, "document.querySelector('.bargain-manager')?.innerText.includes('添加砍价商品')", "admin bargain goods list after create");
  const bargainHasDetail = await client.evaluate(`document.querySelector('.bargain-manager .el-table__body')?.innerText.includes('详情') || false`);
  if (bargainHasDetail) {
    await clickText(client, ".bargain-manager .el-table__body", "详情", "admin bargain detail drawer");
    await waitFor(client, "document.querySelector('.el-drawer__body')?.innerText.includes('砍价名称') && document.querySelector('.el-drawer__body')?.innerText.includes('价格')", "admin bargain detail drawer body");
    await assertText(client, ".el-drawer__body", "砍价设置", "admin bargain detail setting field");
    await clickFirst(client, ".el-drawer__close-btn", "admin close bargain detail drawer");
    checked.push({ area: "admin-bargain-product", action: "open detail drawer without writing" });
  } else {
    checked.push({ area: "admin-bargain-product", action: "detail drawer", skipped: "no bargain product row visible in current database" });
  }

  await navigate(client, `${adminBase}/marketing/bargain/bargainList`);
  await waitFor(client, "document.querySelector('.bargain-manager')?.innerText.includes('砍价商品') && document.querySelector('.el-table__body')", "admin bargain record list");
  await assertText(client, ".bargain-manager", "时间选择", "admin bargain record time filter");
  await assertText(client, ".bargain-manager", "砍价状态", "admin bargain record status filter");
  await assertText(client, ".bargain-manager", "发起用户", "admin bargain record user column");
  await assertText(client, ".bargain-manager", "剩余砍价次数", "admin bargain record count column");
  const bargainRecordHasDetail = await client.evaluate(`document.querySelector('.bargain-manager .el-table__body')?.innerText.includes('查看详情') || false`);
  if (bargainRecordHasDetail) {
    await clickText(client, ".bargain-manager .el-table__body", "查看详情", "admin bargain help dialog");
    await waitFor(client, "document.querySelector('.el-dialog')?.innerText.includes('查看详情')", "admin bargain help dialog body");
    await assertText(client, ".el-dialog", "用户名称", "admin bargain help user column");
    await assertText(client, ".el-dialog", "砍价金额", "admin bargain help amount column");
    await closeVisibleDialogHeaderContaining(client, "查看详情", "admin bargain help dialog");
    checked.push({ area: "admin-bargain-record", action: "open record help dialog without writing" });
  } else {
    checked.push({ area: "admin-bargain-record", action: "record help dialog", skipped: "no bargain record row visible in current database" });
  }

  await navigate(client, `${adminBase}/marketing/groupBuy/groupGoods`);
  await waitFor(client, "document.querySelector('.combination-manager')?.innerText.includes('添加拼团商品') && document.querySelector('.el-table__body')", "admin combination goods list");
  await assertText(client, ".combination-manager", "拼团状态", "admin combination goods status filter");
  await assertText(client, ".combination-manager", "商品搜索", "admin combination goods search filter");
  await assertText(client, ".combination-manager", "拼团价", "admin combination goods price column");
  await assertText(client, ".combination-manager", "成团数量", "admin combination goods team column");
  await clickText(client, ".combination-manager", "添加拼团商品", "admin combination create form");
  await waitFor(client, "document.querySelector('.combination-product-form')?.innerText.includes('添加拼团商品')", "admin combination create form body");
  await assertText(client, ".combination-product-form", "选择商品", "admin combination create select tab");
  await clickText(client, ".combination-product-form", "基础信息", "admin combination create base tab");
  await assertText(client, ".combination-product-form", "拼团人数", "admin combination create people field");
  await assertText(client, ".combination-product-form", "活动规格", "admin combination create sku field");
  await clickText(client, ".combination-product-form", "商品详情", "admin combination create detail tab");
  await assertText(client, ".combination-product-form", "商品详情", "admin combination create detail field");
  checked.push({ area: "admin-combination-product", action: "open list and create form tabs without selecting product saving exporting deleting or switching status" });
  await clickText(client, ".page-header", "返回列表", "admin combination create back");
  await waitFor(client, "document.querySelector('.combination-manager')?.innerText.includes('添加拼团商品')", "admin combination goods list after create");
  const combinationHasDetail = await client.evaluate(`document.querySelector('.combination-manager .el-table__body')?.innerText.includes('详情') || false`);
  if (combinationHasDetail) {
    await clickText(client, ".combination-manager .el-table__body", "详情", "admin combination detail drawer");
    await waitFor(client, "document.querySelector('.el-drawer__body')?.innerText.includes('拼团名称') && document.querySelector('.el-drawer__body')?.innerText.includes('商品属性')", "admin combination detail drawer body");
    await assertText(client, ".el-drawer__body", "拼团设置", "admin combination detail setting field");
    await clickFirst(client, ".el-drawer__close-btn", "admin close combination detail drawer");
    checked.push({ area: "admin-combination-product", action: "open detail drawer and sku table without writing" });
  } else {
    checked.push({ area: "admin-combination-product", action: "detail drawer", skipped: "no visible enabled combination product row in current database" });
  }

  await navigate(client, `${adminBase}/marketing/groupBuy/groupList`);
  await waitFor(client, "document.querySelector('.combination-manager')?.innerText.includes('参与人数') && document.querySelector('.el-table__body')", "admin combination record list");
  await assertText(client, ".combination-manager", "时间选择", "admin combination record time filter");
  await assertText(client, ".combination-manager", "拼团状态", "admin combination record status filter");
  await assertText(client, ".combination-manager", "开团团长", "admin combination record leader column");
  await assertText(client, ".combination-manager", "几人参加", "admin combination record joined column");
  const combinationRecordHasDetail = await client.evaluate(`document.querySelector('.combination-manager .el-table__body')?.innerText.includes('查看详情') || false`);
  if (combinationRecordHasDetail) {
    await clickText(client, ".combination-manager .el-table__body", "查看详情", "admin combination order dialog");
    await waitFor(client, "document.querySelector('.el-dialog')?.innerText.includes('查看详情')", "admin combination order dialog body");
    await assertText(client, ".el-dialog", "用户名称", "admin combination order user column");
    await assertText(client, ".el-dialog", "订单编号", "admin combination order no column");
    await assertText(client, ".el-dialog", "订单状态", "admin combination order status column");
    await closeVisibleDialogHeaderContaining(client, "查看详情", "admin combination order dialog");
    checked.push({ area: "admin-combination-record", action: "open record order dialog without writing" });
  } else {
    checked.push({ area: "admin-combination-record", action: "record order dialog", skipped: "no combination record row visible in current database" });
  }

  await navigate(client, `${adminBase}/marketing/videoChannel/list`);
  await waitFor(client, "document.querySelector('.video-channel')?.innerText.includes('商品列表') && document.querySelector('.el-table__body')", "admin video channel product list");
  await assertText(client, ".video-channel", "商品列表", "admin video channel product tab");
  await assertText(client, ".video-channel", "草稿列表", "admin video channel draft tab visible");
  await assertText(client, ".video-channel", "商品搜索", "admin video channel search field");
  await assertText(client, ".video-channel", "视频号同步", "admin video channel safe warning");
  await assertText(client, ".video-channel", "商品图", "admin video channel image column");
  await assertText(client, ".video-channel", "商品名称", "admin video channel name column");
  await assertText(client, ".video-channel", "售价", "admin video channel price column");
  await assertText(client, ".video-channel", "库存", "admin video channel stock column");
  await assertText(client, ".video-channel", "销量", "admin video channel sales column");
  await assertText(client, ".video-channel", "视频号订单", "admin video channel order count column");
  await assertText(client, ".video-channel", "购买件数", "admin video channel pay num column");
  await assertText(client, ".video-channel", "支付金额", "admin video channel pay amount column");
  await assertText(client, ".video-channel", "主图视频", "admin video channel video column");
  await assertText(client, ".video-channel", "来源", "admin video channel source column");
  await assertText(client, ".video-channel", "同步", "admin video channel sync action disabled");
  await assertText(client, ".video-channel", "发布", "admin video channel publish action disabled");
  checked.push({ area: "admin-video-channel-list", action: "open video channel product list readonly without syncing publishing reviewing logistics or external wechat calls" });

  await navigate(client, `${adminBase}/marketing/videoChannel/draftList`);
  await waitFor(client, "document.querySelector('.video-channel')?.innerText.includes('草稿列表') && document.querySelector('.el-table__body')", "admin video channel draft list");
  await assertText(client, ".video-channel", "商品列表", "admin video channel list tab visible from draft");
  await assertText(client, ".video-channel", "草稿列表", "admin video channel draft tab");
  await assertText(client, ".video-channel", "商品搜索", "admin video channel draft search field");
  await assertText(client, ".video-channel", "草稿类型", "admin video channel draft type filter");
  await assertText(client, ".video-channel", "商品图", "admin video channel draft image column");
  await assertText(client, ".video-channel", "商品名称", "admin video channel draft name column");
  await assertText(client, ".video-channel", "售价", "admin video channel draft price column");
  await assertText(client, ".video-channel", "库存", "admin video channel draft stock column");
  await assertText(client, ".video-channel", "主图视频", "admin video channel draft video column");
  await assertText(client, ".video-channel", "添加时间", "admin video channel draft add time column");
  await assertText(client, ".video-channel", "同步", "admin video channel draft sync action disabled");
  await assertText(client, ".video-channel", "发布", "admin video channel draft publish action disabled");
  checked.push({ area: "admin-video-channel-draft", action: "open video channel draft list readonly without syncing publishing reviewing logistics or external wechat calls" });

  await navigate(client, `${adminBase}/marketing/coupon/list`);
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin coupon list");
  await assertText(client, ".legacy-main", "添加优惠", "admin coupon list create action");
  const couponRecordVisible = await client.evaluate(`document.querySelector('.el-table__body')?.innerText.includes('领取记录') || false`);
  if (couponRecordVisible) {
    await clickText(client, ".el-table__body", "领取记录", "admin coupon receive records dialog");
    await waitFor(client, "document.querySelector('.el-dialog')?.innerText.includes('领取记录')", "admin coupon records dialog body");
    await assertText(client, ".el-dialog", "用户名", "admin coupon records user column");
    await assertText(client, ".el-dialog", "使用状态", "admin coupon records status column");
    await assertText(client, ".el-dialog", "有效期", "admin coupon records valid time column");
    checked.push({ area: "admin-coupon", action: "open receive records dialog without writing" });
    await clickFirst(client, ".el-dialog__headerbtn", "admin close coupon records dialog");
    await waitFor(client, `!Array.from(document.querySelectorAll('.el-dialog')).some((node) => {
      const rect = node.getBoundingClientRect();
      return rect.width > 0 && rect.height > 0 && node.innerText.includes('领取记录');
    })`, "admin coupon records dialog closed");
  } else {
    checked.push({ area: "admin-coupon", action: "receive records dialog", skipped: "no coupon row visible in current database" });
  }
  await clickText(client, ".legacy-main", "添加优惠", "admin coupon create form");
  await waitFor(client, "document.querySelector('.demo-ruleForm')", "admin coupon create form body");
  await assertText(client, ".demo-ruleForm", "优惠劵名称", "admin coupon name field");
  await assertText(client, ".demo-ruleForm", "优惠券面值", "admin coupon money field");
  await assertText(client, ".demo-ruleForm", "领取方式", "admin coupon receive type field");
  await assertText(client, ".demo-ruleForm", "立即创建", "admin coupon create button");
  checked.push({ area: "admin-coupon", action: "open create form without saving" });
  await clickText(client, ".demo-ruleForm", "返回", "admin coupon create back to list");
  await waitFor(client, "document.querySelector('.legacy-main') && document.querySelector('.el-table__body')", "admin coupon list after form");

  await navigate(client, `${adminBase}/marketing/coupon/record`);
  await waitFor(client, "document.querySelector('.divBox') && document.querySelector('.el-table__body')", "admin coupon user record list");
  await assertText(client, ".divBox", "使用状态", "admin coupon record status filter");
  await assertText(client, ".divBox", "领取人", "admin coupon record user filter");
  await assertText(client, ".divBox", "优惠劵", "admin coupon record name filter");
  await assertText(client, ".divBox", "搜索", "admin coupon record search action");
  await assertText(client, ".divBox", "重置", "admin coupon record reset action");
  await assertText(client, ".divBox", "优惠券ID", "admin coupon record coupon id column");
  await assertText(client, ".divBox", "优惠券名称", "admin coupon record coupon name column");
  await assertText(client, ".divBox", "面值", "admin coupon record money column");
  await assertText(client, ".divBox", "最低消费额", "admin coupon record min price column");
  await assertText(client, ".divBox", "开始使用时间", "admin coupon record start time column");
  await assertText(client, ".divBox", "结束使用时间", "admin coupon record end time column");
  await assertText(client, ".divBox", "获取方式", "admin coupon record type column");
  await assertText(client, ".divBox", "是否可用", "admin coupon record usable column");
  await assertText(client, ".divBox", "使用状态", "admin coupon record use status column");
  await fillInputByPlaceholder(client, "请输入优惠劵", "验收只读筛选", "admin coupon record name input");
  await clickText(client, ".divBox", "搜索", "admin coupon record search readonly");
  await waitFor(client, "!document.querySelector('.divBox .el-loading-mask')", "admin coupon record search loading done");
  await assertText(client, ".divBox", "优惠券名称", "admin coupon record columns after search");
  await clickText(client, ".divBox", "重置", "admin coupon record reset readonly");
  await waitFor(client, "!document.querySelector('.divBox .el-loading-mask')", "admin coupon record reset loading done");
  checked.push({ area: "admin-coupon-record", action: "open record list and search reset readonly without issuing coupon or changing coupon user data" });

  await navigate(client, `${appBase}/pages/goods_cate/goods_cate?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.category-panel')", "H5 category panel");
  await waitFor(client, "document.querySelector('.category-panel .category-side button')", "H5 category parent buttons");
  await clickFirst(client, ".category-panel .category-side button", "H5 category parent click");
  await waitFor(client, "document.querySelector('.product-section')", "H5 category product section");
  await waitFor(client, "document.querySelector('.product-section')?.innerText.includes('件')", "H5 category product count");
  checked.push({ area: "h5-category", action: "open legacy category path and select category" });

  if (productKeyword) {
    await navigate(client, `${appBase}/pages/goods/goods_search/index?token=${encodeURIComponent(frontToken)}`);
    await waitFor(client, "document.querySelector('.search-view')", "H5 search view");
    await typeInputByPlaceholder(client, "搜索商品名称 / 关键词", productKeyword, "H5 search keyword input");
    await clickText(client, ".search-view", "搜索", "H5 search submit");
    await waitFor(client, "document.querySelector('.search-view .product-section')", "H5 search product section");
    await assertText(client, ".search-view", "搜索「", "H5 search result title");
    await waitFor(client, "document.querySelector('.search-view .product-card') || document.querySelector('.search-view')?.innerText.includes('暂无商品')", "H5 search result content");
    checked.push({ area: "h5-search", action: "search real product keyword", keyword: productKeyword });
  } else {
    checked.push({ area: "h5-search", action: "search real product keyword", skipped: "front product keyword empty" });
  }

  await navigate(client, `${appBase}/pages/goods/goods_details/index?id=${productId}&token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.detail-panel')", "H5 product detail panel");
  await assertText(client, ".detail-panel", "加入购物车", "H5 product detail actions");
  await clickText(client, ".detail-panel", "用户评价", "H5 product reply entry");
  await waitFor(client, "document.querySelector('.product-reply-view')", "H5 product replies view");
  await assertText(client, ".product-reply-view", "好评率", "H5 product replies content");
  await clickText(client, ".product-reply-view", "中评", "H5 product replies tab");
  await waitFor(client, "document.querySelector('.product-reply-view .reply-tabs .active')?.innerText.includes('中评')", "H5 product replies middle tab active");
  checked.push({ area: "h5-product", action: "detail opens replies and switches tab", productId });

  await navigate(client, `${appBase}/pages/user/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.profile-view')", "H5 profile view");
  await assertText(client, ".profile-view", "我的服务", "H5 profile service panel");
  await clickText(client, ".profile-view", "我的余额", "H5 profile balance entry");
  await waitFor(client, "document.querySelector('.balance-view')", "H5 profile opens balance view");
  await assertText(client, ".balance-view", "我的余额", "H5 balance title");
  await navigate(client, `${appBase}/pages/user/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.profile-view')", "H5 profile view before sign");
  await clickText(client, ".profile-view", "每日签到", "H5 profile sign entry");
  await waitFor(client, "document.querySelector('.sign-view')", "H5 profile opens sign view");
  await assertText(client, ".sign-view", "签到", "H5 sign title");
  await navigate(client, `${appBase}/pages/user/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.profile-view')", "H5 profile view before member");
  await clickText(client, ".profile-view", "会员中心", "H5 profile member entry");
  await waitFor(client, "document.querySelector('.member-level-view')", "H5 profile opens member level view");
  await assertText(client, ".member-level-view", "会员", "H5 member level title");
  checked.push({ area: "h5-profile", action: "service entries open balance sign and member views" });

  await navigate(client, `${appBase}/pages/infos/user_info/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.personal-data-view')", "H5 personal data view");
  await assertText(client, ".personal-data-view", "个人资料", "H5 personal data title");
  await assertText(client, ".personal-data-view", "头像", "H5 personal data avatar field");
  await assertText(client, ".personal-data-view", "头像路径", "H5 personal data avatar path field");
  await assertText(client, ".personal-data-view", "昵称", "H5 personal data nickname field");
  await assertText(client, ".personal-data-view", "手机号码", "H5 personal data phone entry");
  await assertText(client, ".personal-data-view", "ID号", "H5 personal data uid field");
  await assertText(client, ".personal-data-view", "密码", "H5 personal data password entry");
  await assertText(client, ".personal-data-view", "地址管理", "H5 personal data address entry");
  await assertText(client, ".personal-data-view", "保存修改", "H5 personal data save action visible");
  await assertText(client, ".personal-data-view", "退出登录", "H5 personal data logout action visible");
  checked.push({ area: "h5-account-info", action: "open personal data form readonly without saving avatar upload or logout" });

  await navigate(client, `${appBase}/pages/infos/user_phone/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.account-form-view')", "H5 phone change view");
  await assertText(client, ".account-form-view", "更换手机号", "H5 phone change title");
  await assertInputPlaceholder(client, ".account-form-view", "当前手机号码", "H5 phone current input");
  await assertInputPlaceholder(client, ".account-form-view", "填写验证码", "H5 phone captcha input");
  await assertText(client, ".account-form-view", "获取验证码", "H5 phone captcha action visible");
  await assertText(client, ".account-form-view", "下一步", "H5 phone next action visible");
  await assertText(client, ".account-form-view", "当前未配置短信服务商", "H5 phone safety copy");
  checked.push({ area: "h5-account-phone", action: "open phone binding form readonly without sending sms or submitting" });

  await navigate(client, `${appBase}/pages/infos/user_pwd_edit/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.account-form-view')", "H5 password change view");
  await assertText(client, ".account-form-view", "修改密码", "H5 password title");
  await assertText(client, ".account-form-view", "当前手机号", "H5 password current phone field");
  await assertInputPlaceholder(client, ".account-form-view", "6-18位字母加数字", "H5 password new password input");
  await assertInputPlaceholder(client, ".account-form-view", "确认新密码", "H5 password confirm input");
  await assertInputPlaceholder(client, ".account-form-view", "填写验证码", "H5 password captcha input");
  await assertText(client, ".account-form-view", "获取验证码", "H5 password captcha action visible");
  await assertText(client, ".account-form-view", "确认修改", "H5 password submit action visible");
  await assertText(client, ".account-form-view", "沿用老项目密码规则", "H5 password rule copy");
  checked.push({ area: "h5-account-password", action: "open password form readonly without sending sms or submitting" });

  await navigate(client, `${appBase}/pages/users/user_address_list/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.address-view')", "H5 address list view");
  await assertText(client, ".address-view", "收货地址", "H5 address title");
  await assertText(client, ".address-view", "新增", "H5 address create action visible");
  const addressHasItems = await client.evaluate(`Boolean(document.querySelector('.address-view .address-card'))`);
  if (addressHasItems) {
    await assertText(client, ".address-view", "编辑", "H5 address edit action visible");
    await assertText(client, ".address-view", "设为默认", "H5 address default action visible");
    await assertText(client, ".address-view", "删除", "H5 address delete action visible");
  }
  await clickText(client, ".address-view", "新增", "H5 address create form");
  await waitFor(client, "document.querySelector('.address-form')", "H5 address create form visible");
  await assertText(client, ".address-form", "新增地址", "H5 address create title");
  await assertInputPlaceholder(client, ".address-form", "收货人", "H5 address receiver input");
  await assertInputPlaceholder(client, ".address-form", "手机号", "H5 address phone input");
  await assertInputPlaceholder(client, ".address-form", "省", "H5 address province input");
  await assertInputPlaceholder(client, ".address-form", "市", "H5 address city input");
  await assertInputPlaceholder(client, ".address-form", "区/县", "H5 address district input");
  await assertInputPlaceholder(client, ".address-form", "详细地址", "H5 address detail input");
  await assertText(client, ".address-form", "设为默认地址", "H5 address default checkbox visible");
  await assertText(client, ".address-form", "保存地址", "H5 address save action visible");
  await clickText(client, ".address-form", "收起", "H5 close address create form");
  checked.push({ area: "h5-address", action: addressHasItems ? "open address list and create form without setting default deleting or saving" : "open empty address list and create form without saving" });

  await navigate(client, `${appBase}/pages/activity/couponList/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.coupon-center-view')", "H5 coupon center view");
  await assertText(client, ".coupon-center-view", "领取优惠券", "H5 coupon center title");
  await clickText(client, ".coupon-center-view", "商品券", "H5 coupon center product tab");
  await waitFor(client, "document.querySelector('.coupon-center-tabs .active')?.innerText.includes('商品券')", "H5 coupon center product tab active");
  await waitFor(client, "document.querySelector('.coupon-center-view')?.innerText.includes('暂无可领取优惠券') || document.querySelector('.coupon-center-view')?.innerText.includes('立即领取') || document.querySelector('.coupon-center-view')?.innerText.includes('已领取')", "H5 coupon center content");
  checked.push({ area: "h5-coupon-center", action: "open legacy coupon center and switch tab without receiving coupon" });

  await navigate(client, `${appBase}/pages/users/user_coupon/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.user-coupon-view')", "H5 user coupon view");
  await assertText(client, ".user-coupon-view", "我的优惠券", "H5 user coupon title");
  await clickText(client, ".user-coupon-view", "已使用", "H5 user coupon used tab");
  await waitFor(client, "document.querySelector('.user-coupon-tabs .active')?.innerText.includes('已使用')", "H5 user coupon used/expired tab active");
  await waitFor(client, "document.querySelector('.user-coupon-view')?.innerText.includes('暂无优惠券') || document.querySelector('.user-coupon-view')?.innerText.includes('已使用') || document.querySelector('.user-coupon-view')?.innerText.includes('过期')", "H5 user coupon used/expired content");
  checked.push({ area: "h5-user-coupon", action: "open my coupons and switch tabs readonly" });

  await navigate(client, `${appBase}/pages/news/news_list/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.article-list-view')", "H5 article list view");
  await assertText(client, ".article-list-view", "消息资讯", "H5 article list title");
  const articleHasCard = await client.evaluate(`Boolean(document.querySelector('.article-list-view .article-card'))`);
  if (articleHasCard) {
    await clickFirst(client, ".article-list-view .article-card", "H5 article detail card");
    await waitFor(client, "document.querySelector('.article-detail-view')", "H5 article detail view");
    await assertText(client, ".article-detail-view", "资讯详情", "H5 article detail title");
    await waitFor(client, "document.querySelector('.article-detail')?.innerText.trim().length > 0", "H5 article detail content");
    checked.push({ area: "h5-article", action: "open article list and detail readonly" });
  } else {
    checked.push({ area: "h5-article", action: "open article list", skipped: "no article row visible in current database" });
  }

  await navigate(client, `${appBase}/pages/users/user_money/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.balance-view')", "H5 balance direct legacy path");
  await assertText(client, ".balance-view", "总资产", "H5 balance total asset");
  await clickText(client, ".balance-view", "充值", "H5 recharge panel open");
  await waitFor(client, "document.querySelector('.recharge-panel')", "H5 recharge panel visible");
  await assertText(client, ".recharge-panel", "余额充值", "H5 recharge panel title");
  await waitFor(client, "document.querySelector('.recharge-panel')?.innerText.includes('立即充值') || document.querySelector('.recharge-panel')?.innerText.includes('充值功能已关闭')", "H5 recharge panel content");
  await clickFirst(client, ".recharge-panel .coupon-head button", "H5 close recharge panel");
  await waitFor(client, "!document.querySelector('.recharge-panel')", "H5 recharge panel closed");
  await clickText(client, ".balance-view", "账单记录", "H5 balance bill entry");
  await waitFor(client, "document.querySelector('.bill-view')", "H5 bill view from balance");
  await assertText(client, ".bill-view", "账单记录", "H5 bill title");
  await clickText(client, ".bill-view", "消费", "H5 bill expense tab");
  await waitFor(client, "document.querySelector('.bill-tabs .active')?.innerText.includes('消费')", "H5 bill expense tab active");
  checked.push({ area: "h5-balance-bill", action: "open balance recharge panel and bill tab without submitting recharge" });

  await navigate(client, `${appBase}/pages/users/user_integral/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.integral-view')", "H5 integral view");
  await assertText(client, ".integral-view", "积分中心", "H5 integral title");
  await assertText(client, ".integral-view", "当前积分", "H5 integral current value");
  await clickText(client, ".integral-view", "分值提升", "H5 integral raise tab");
  await waitFor(client, "document.querySelector('.integral-tabs .active')?.innerText.includes('分值提升')", "H5 integral raise tab active");
  await assertText(client, ".integral-view", "每日签到", "H5 integral sign entry");
  checked.push({ area: "h5-integral", action: "open integral center and switch raise tab without signing or changing points" });

  await navigate(client, `${appBase}/pages/users/user_goods_collection/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.collection-view')", "H5 collection view");
  await assertText(client, ".collection-view", "收藏商品", "H5 collection title");
  const collectionHasItems = await client.evaluate(`Boolean(document.querySelector('.collection-view .collection-item'))`);
  if (collectionHasItems) {
    await clickText(client, ".collection-view", "管理", "H5 collection manage mode");
    await waitFor(client, "document.querySelector('.collection-submit')?.innerText.includes('取消收藏')", "H5 collection manage actions");
    await assertText(client, ".collection-view", "取消收藏", "H5 collection delete action visible");
    await clickText(client, ".collection-view", "取消", "H5 collection cancel manage mode");
    checked.push({ area: "h5-collection", action: "open collection and toggle manage mode without deleting collection" });
  } else {
    checked.push({ area: "h5-collection", action: "open collection", skipped: "no collection item sample in current front account" });
  }

  await navigate(client, `${appBase}/pages/promoter/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.spread-view')", "H5 spread center view");
  await assertText(client, ".spread-view", "当前佣金", "H5 spread commission card");
  await assertText(client, ".spread-view", "立即提现", "H5 spread extract action");
  await assertText(client, ".spread-view", "推广名片", "H5 spread poster entry");
  await assertText(client, ".spread-view", "推广人统计", "H5 spread people entry");
  await assertText(client, ".spread-view", "佣金明细", "H5 spread brokerage entry");
  await assertText(client, ".spread-view", "推广人订单", "H5 spread order entry");
  checked.push({ area: "h5-spread", action: "open spread center readonly without changing commission or submitting extract" });

  await navigate(client, `${appBase}/pages/promoter/user_spread_money/index?type=1&token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.commission-record-view')", "H5 extract records view");
  await assertText(client, ".commission-record-view", "提现总额", "H5 extract records title");
  await waitFor(client, "document.querySelector('.commission-record-view')?.innerText.includes('暂无提现记录') || document.querySelector('.commission-record-view')?.innerText.includes('提现')", "H5 extract records content");
  checked.push({ area: "h5-spread-extract-records", action: "open extract records readonly" });

  await navigate(client, `${appBase}/pages/promoter/user_spread_money/index?type=2&token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.commission-record-view')", "H5 brokerage records view");
  await assertText(client, ".commission-record-view", "佣金明细", "H5 brokerage records title");
  await waitFor(client, "document.querySelector('.commission-record-view')?.innerText.includes('暂无佣金记录') || document.querySelector('.commission-record-view')?.innerText.includes('返佣') || document.querySelector('.commission-record-view')?.innerText.includes('佣金')", "H5 brokerage records content");
  checked.push({ area: "h5-spread-brokerage-records", action: "open brokerage records readonly" });

  await navigate(client, `${appBase}/pages/promoter/promoter-list/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.spread-people-view')", "H5 spread people view");
  await assertText(client, ".spread-people-view", "推广人统计", "H5 spread people title");
  await clickText(client, ".spread-people-view", "二级", "H5 spread people level two tab");
  await waitFor(client, "document.querySelector('.spread-people-tabs .active')?.innerText.includes('二级')", "H5 spread people level two active");
  await clickText(client, ".spread-people-view", "团队", "H5 spread people team sort");
  await waitFor(client, "document.querySelector('.spread-sort-tabs .active')?.innerText.includes('团队')", "H5 spread people team sort active");
  checked.push({ area: "h5-spread-people", action: "open spread people and switch level/sort tabs readonly" });

  await navigate(client, `${appBase}/pages/promoter/promoter-order/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.spread-order-view')", "H5 spread order view");
  await assertText(client, ".spread-order-view", "累积推广订单", "H5 spread order title");
  await waitFor(client, "document.querySelector('.spread-order-view')?.innerText.includes('暂无推广订单') || document.querySelector('.spread-order-view')?.innerText.includes('订单编号')", "H5 spread order content");
  checked.push({ area: "h5-spread-orders", action: "open spread orders readonly" });

  await navigate(client, `${appBase}/pages/promoter/promoter_rank/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.rank-view')", "H5 spread rank view");
  await assertText(client, ".rank-view", "推广人排行", "H5 spread rank title");
  await clickText(client, ".rank-view", "月榜", "H5 spread rank month tab");
  await waitFor(client, "document.querySelector('.rank-tabs .active')?.innerText.includes('月榜')", "H5 spread rank month active");
  checked.push({ area: "h5-spread-rank", action: "open spread rank and switch month tab readonly" });

  await navigate(client, `${appBase}/pages/promoter/commission_rank/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.rank-view')", "H5 brokerage rank view");
  await assertText(client, ".rank-view", "佣金排行", "H5 brokerage rank title");
  await clickText(client, ".rank-view", "月榜", "H5 brokerage rank month tab");
  await waitFor(client, "document.querySelector('.rank-tabs .active')?.innerText.includes('月榜')", "H5 brokerage rank month active");
  checked.push({ area: "h5-brokerage-rank", action: "open brokerage rank and switch month tab readonly" });

  await navigate(client, `${appBase}/pages/promoter/user_spread_code/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.spread-poster-view')", "H5 spread poster view");
  await assertText(client, ".spread-poster-view", "推广名片", "H5 spread poster title");
  await assertText(client, ".spread-poster-view", "推广链接", "H5 spread poster link");
  await waitFor(client, "document.querySelector('.spread-poster-view')?.innerText.includes('复制链接') || document.querySelector('.spread-poster-view')?.innerText.includes('暂无推广海报')", "H5 spread poster content");
  checked.push({ area: "h5-spread-poster", action: "open spread poster readonly without copying link" });

  await navigate(client, `${appBase}/pages/users/user_cash/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.extract-cash-view')", "H5 extract cash view");
  await assertText(client, ".extract-cash-view", "佣金提现", "H5 extract cash title");
  await assertText(client, ".extract-cash-view", "银行卡", "H5 extract cash bank tab");
  await clickText(client, ".extract-cash-view", "微信", "H5 extract cash wechat tab");
  await waitFor(client, "document.querySelector('.extract-tabs .active')?.innerText.includes('微信')", "H5 extract cash wechat active");
  await assertText(client, ".extract-cash-view", "当前可提现金额", "H5 extract cash amount field");
  await assertText(client, ".extract-cash-view", "提现", "H5 extract cash submit visible");
  checked.push({ area: "h5-extract-cash", action: "open extract form and switch method without submitting extract" });

  await navigate(client, `${appBase}/pages/order_addcart/order_addcart?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.cart-view')", "H5 cart view");
  await assertText(client, ".cart-view", "购物车", "H5 cart title");
  if (frontCartCount > 0) {
    await assertText(client, ".cart-view", "立即下单", "H5 cart checkout action");
    await waitFor(client, "document.querySelector('.cart-all')?.innerText.includes('全选(')", "H5 cart all checkbox");
    await clickFirst(client, ".cart-all input", "H5 cart toggle all off");
    await waitFor(client, "document.querySelector('.cart-all')?.innerText.includes('全选(0)')", "H5 cart unselected all state");
    await clickText(client, ".cart-view", "管理", "H5 cart manage mode");
    await assertText(client, ".cart-view", "收藏", "H5 cart collect action");
    await assertText(client, ".cart-view", "删除", "H5 cart delete action");
    await clickText(client, ".cart-view", "取消", "H5 cart cancel manage mode");
    await clickFirst(client, ".cart-all input", "H5 cart toggle all on before checkout");
    await waitFor(client, "document.querySelector('.cart-all')?.innerText.includes('全选(1)') || !document.querySelector('.cart-all')?.innerText.includes('全选(0)')", "H5 cart selected before checkout");
    await clickText(client, ".cart-view", "立即下单", "H5 cart checkout");
    await waitFor(client, "document.querySelector('.checkout-view')", "H5 checkout view from cart");
    await assertText(client, ".checkout-view", "确认订单", "H5 checkout title");
    await assertText(client, ".checkout-view", "快递配送", "H5 checkout shipping tab");
    await assertText(client, ".checkout-view", "提交订单", "H5 checkout submit action");
    await clickText(client, ".checkout-view", "优惠券", "H5 checkout coupon panel");
    await waitFor(client, "document.querySelector('.coupon-panel')", "H5 checkout coupon panel visible");
    await assertText(client, ".coupon-panel", "优惠券", "H5 checkout coupon panel title");
    await waitFor(client, "document.querySelector('.coupon-panel')?.innerText.includes('暂无可用优惠券') || document.querySelector('.coupon-panel')?.innerText.includes('立即使用') || document.querySelector('.coupon-panel')?.innerText.includes('不使用') || document.querySelector('.coupon-panel')?.innerText.includes('领取使用')", "H5 checkout coupon panel content");
    await clickFirst(client, ".coupon-head button", "H5 close checkout coupon panel");
    checked.push({ area: "h5-cart", action: "toggle selection/manage and open checkout without creating order", cartCount: frontCartCount });
  } else {
    checked.push({ area: "h5-cart", action: "cart view", skipped: "no cart item sample in current front account" });
  }

  if (frontOrderNo) {
    await navigate(client, `${appBase}/pages/order/order_details/index?orderId=${encodeURIComponent(frontOrderNo)}&token=${encodeURIComponent(frontOrderToken)}`);
    await waitFor(client, "document.querySelector('.order-detail-view')", "H5 order detail view");
    await assertText(client, ".order-detail-view", "订单详情", "H5 order detail title");
    await assertText(client, ".order-detail-view", "订单编号", "H5 order detail fields");
    checked.push({ area: "h5-order", action: "legacy order detail direct path", orderNo: frontOrderNo, tokenSource: frontOrderTokenSource });

    await navigate(client, `${appBase}/pages/order/order_pay_status/index?orderNo=${encodeURIComponent(frontOrderNo)}&token=${encodeURIComponent(frontOrderToken)}`);
    await waitFor(client, "document.querySelector('.pay-status-view')", "H5 pay status view");
    await assertText(client, ".pay-status-view", "查看订单", "H5 pay status actions");
    await assertText(client, ".pay-status-view", "订单编号", "H5 pay status fields");
    checked.push({ area: "h5-pay-status", action: "legacy pay status direct path", orderNo: frontOrderNo, tokenSource: frontOrderTokenSource });
    await clickText(client, ".pay-status-view", "查看订单", "H5 pay status view order action");
    await waitFor(client, "document.querySelector('.order-detail-view')", "H5 pay status opens order detail");
    await assertText(client, ".order-detail-view", "订单详情", "H5 pay status order detail title");
    await assertText(client, ".order-detail-view", "订单编号", "H5 pay status order detail fields");
    checked.push({ area: "h5-pay-status", action: "view order opens detail", orderNo: frontOrderNo, tokenSource: frontOrderTokenSource });
  } else {
    checked.push({ area: "h5-order", skipped: "no front order sample in current database" });
    checked.push({ area: "h5-pay-status", skipped: "no front order sample in current database" });
  }

  await navigate(client, `${appBase}/pages/users/user_return_list/index?token=${encodeURIComponent(frontToken)}`);
  await waitFor(client, "document.querySelector('.refund-list-view')", "H5 refund list view");
  await assertText(client, ".refund-list-view", "退货列表", "H5 refund list title");
  await clickText(client, ".refund-list-view", "已退款", "H5 refund status tab");
  await waitFor(client, "document.querySelector('.refund-status-tabs .active')?.innerText.includes('已退款')", "H5 refund status active");
  if (refundOrderNo) {
    if (refundToken !== frontToken) {
      await navigate(client, `${appBase}/pages/users/user_return_list/index?token=${encodeURIComponent(refundToken)}`);
      await waitFor(client, "document.querySelector('.refund-list-view')", "H5 refund list view with refund token");
      await clickText(client, ".refund-list-view", "已退款", "H5 refund status tab with refund token");
      await waitFor(client, "document.querySelector('.refund-status-tabs .active')?.innerText.includes('已退款')", "H5 refund status active with refund token");
    }
    await clickFirst(client, ".refund-list-view .return-card", "H5 refund order card");
    await waitFor(client, "document.querySelector('.order-detail-view')", "H5 refund detail view");
    await assertText(client, ".order-detail-view", "售后进度", "H5 refund detail progress");
    checked.push({ area: "h5-refund", action: "list tab and detail", orderNo: refundOrderNo, tokenSource: refundTokenSource });
  } else {
    checked.push({ area: "h5-refund", action: "list tab", skipped: "no refund order sample in current database" });
  }

  const networkFailures = client.networkFailures.filter((item) => (
    !item.url.includes("/favicon.ico") && item.type !== "Image"
  ));
  if (networkFailures.length) {
    throw new Error(`browser interaction network failures: ${JSON.stringify(networkFailures.slice(0, 20), null, 2)}`);
  }

  const failures = consoleFailures(client.events);
  if (failures.length) {
    throw new Error(`browser interaction console failures: ${JSON.stringify(failures.slice(0, 10), null, 2)}`);
  }

  console.log(JSON.stringify({
    browserInteractionSmokeOk: true,
    adminBase,
    appBase,
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
  await deleteTemporaryFrontToken(temporaryOrderToken);
  await deleteTemporaryFrontToken(temporaryRefundToken);
}
