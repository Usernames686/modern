const adminBase = process.env.CRMEB_ADMIN_WEB || "http://127.0.0.1:19527";
const account = process.env.CRMEB_ADMIN_ACCOUNT || "admin";
const pwd = process.env.CRMEB_ADMIN_PWD || "123456";
const timeoutMs = Number(process.env.CRMEB_RUNTIME_CHECK_TIMEOUT || 10000);

async function requestJson(url, options = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, { ...options, signal: controller.signal });
    const text = await response.text();
    let body;
    try {
      body = JSON.parse(text);
    } catch {
      body = { raw: text.slice(0, 200) };
    }
    return { response, body };
  } finally {
    clearTimeout(timer);
  }
}

function assertCode(name, result, code) {
  if (result.body?.code !== code) {
    throw new Error(`${name}: expected code ${code}, got ${JSON.stringify({
      httpStatus: result.response.status,
      code: result.body?.code,
      message: result.body?.message,
      raw: result.body?.raw
    })}`);
  }
}

function assertOk(name, result) {
  assertCode(name, result, 200);
  return result.body.data;
}

function assertTruthy(name, value, context) {
  if (!value) {
    throw new Error(`${name}: assertion failed ${JSON.stringify(context)}`);
  }
}

function authHeaders(token) {
  return {
    "Content-Type": "application/json",
    "Authori-zation": token
  };
}

function menuPayload(marker) {
  return {
    button: [
      {
        name: "商城",
        sub_button: [
          {
            name: "首页",
            type: "view",
            url: "/pages/index/index"
          },
          {
            name: "客服",
            type: "click",
            key: marker
          }
        ]
      },
      {
        name: "个人中心",
        type: "view",
        url: "/pages/user/index"
      },
      {
        name: "小程序",
        type: "miniprogram",
        appid: "wx0000000000000000",
        url: "https://example.com/fallback",
        pagepath: "pages/index/index"
      }
    ]
  };
}

function normalizeButtonList(data) {
  return data?.menu?.button || [];
}

function containsMarker(data, marker) {
  return JSON.stringify(data || {}).includes(marker);
}

const login = assertOk("admin login", await requestJson(`${adminBase}/api/admin/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account, pwd })
}));
const token = login.token;
assertTruthy("admin token", token, login);

const original = assertOk("wechat menu original", await requestJson(
  `${adminBase}/api/admin/wechat/menu/public/get`,
  { headers: { "Authori-zation": token } }
));
const originalMenu = { button: normalizeButtonList(original) };
const marker = `验收临时菜单-${Date.now()}`;
let restored = false;

async function restoreOriginal() {
  assertOk("wechat menu restore", await requestJson(`${adminBase}/api/admin/wechat/menu/public/create`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(originalMenu)
  }));
  restored = true;
}

try {
  assertTruthy("wechat menu local mode", original.localMode === true, original);

  assertOk("wechat menu save", await requestJson(`${adminBase}/api/admin/wechat/menu/public/create`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(menuPayload(marker))
  }));

  const afterSave = assertOk("wechat menu after save", await requestJson(
    `${adminBase}/api/admin/wechat/menu/public/get`,
    { headers: { "Authori-zation": token } }
  ));
  const buttons = normalizeButtonList(afterSave);
  assertTruthy("wechat menu after save local mode", afterSave.localMode === true, afterSave);
  assertTruthy("wechat menu main count", buttons.length === 3, afterSave);
  assertTruthy("wechat menu child count", buttons[0]?.sub_button?.length === 2, afterSave);
  assertTruthy("wechat menu click marker", buttons[0]?.sub_button?.[1]?.key === marker, afterSave);
  assertTruthy("wechat menu view url", buttons[1]?.url === "/pages/user/index", afterSave);
  assertTruthy("wechat menu miniprogram", buttons[2]?.type === "miniprogram" && buttons[2]?.pagepath === "pages/index/index", afterSave);

  await restoreOriginal();
  const afterRestore = assertOk("wechat menu after restore", await requestJson(
    `${adminBase}/api/admin/wechat/menu/public/get`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("wechat menu marker removed", !containsMarker(afterRestore, marker), afterRestore);
  assertTruthy(
    "wechat menu restored button count",
    normalizeButtonList(afterRestore).length === originalMenu.button.length,
    { originalMenu, afterRestore }
  );

  console.log(JSON.stringify({
    runtimeWechatMenuWritebackSmokeOk: true,
    adminBase,
    marker,
    originalButtonCount: originalMenu.button.length,
    restored,
    localMode: afterRestore.localMode
  }, null, 2));
} finally {
  if (!restored) {
    await restoreOriginal().catch(() => {});
  }
}
