const adminBase = process.env.CRMEB_ADMIN_WEB || "http://127.0.0.1:19527";
const appBase = process.env.CRMEB_APP_WEB || "http://127.0.0.1:19528";
const account = process.env.CRMEB_ADMIN_ACCOUNT || "admin";
const pwd = process.env.CRMEB_ADMIN_PWD || "123456";
const frontAccount = process.env.CRMEB_FRONT_ACCOUNT || "18888888888";
const frontPwd = process.env.CRMEB_FRONT_PWD || "a123456";
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

const adminLogin = assertOk("admin login", await requestJson(`${adminBase}/api/admin/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account, pwd })
}));
const adminToken = adminLogin.token;

const frontLogin = assertOk("front login", await requestJson(`${appBase}/api/front/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account: frontAccount, password: frontPwd })
}));
const frontToken = frontLogin.token;

const unauthorizedChecks = [
  ["admin info no token", `${adminBase}/api/admin/getAdminInfoByToken`],
  ["admin users no token", `${adminBase}/api/admin/user/list?page=1&limit=1&accessType=0`],
  ["admin order no token", `${adminBase}/api/admin/store/order/list?page=1&limit=1&status=all&type=2`],
  ["front user no token", `${appBase}/api/front/user`],
  ["front cart no token", `${appBase}/api/front/cart/list`],
  ["front pay config no token", `${appBase}/api/front/pay/get/config`]
];

const unauthorizedResults = [];
for (const [name, url] of unauthorizedChecks) {
  const result = await requestJson(url);
  assertCode(name, result, 401);
  unauthorizedResults.push({ name, code: result.body.code, message: result.body.message });
}

const frontSendCode = assertOk("front send code safe", await requestJson(`${appBase}/api/front/sendCode`, {
  method: "POST"
}));
assertTruthy("front send code safe text", /未配置|安全/.test(String(frontSendCode)), frontSendCode);

const adminSmsLogin = assertOk("admin sms login safe", await requestJson(`${adminBase}/api/admin/pass/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json", "Authori-zation": adminToken },
  body: JSON.stringify({ account: "dry-run" })
}));
assertTruthy("admin sms login localMode", adminSmsLogin.localMode === true && adminSmsLogin.status === false, adminSmsLogin);
assertTruthy("admin sms login no external message", /未触发外部请求|第三方/.test(String(adminSmsLogin.message)), adminSmsLogin);

const adminSmsInfo = assertOk("admin sms info safe", await requestJson(`${adminBase}/api/admin/pass/info`, {
  headers: { "Authori-zation": adminToken }
}));
assertTruthy("admin sms info localMode", adminSmsInfo.localMode === true, adminSmsInfo);

const expressSync = assertOk("admin express sync safe", await requestJson(`${adminBase}/api/admin/express/sync/express`, {
  method: "POST",
  headers: { "Authori-zation": adminToken }
}));
assertTruthy("admin express sync localMode", expressSync.localMode === true && expressSync.synced === false, expressSync);
assertTruthy("admin express sync no external message", /未配置|当前读取/.test(String(expressSync.message)), expressSync);

const payConfig = assertOk("front pay config safe", await requestJson(`${appBase}/api/front/pay/get/config`, {
  headers: { "Authori-zation": frontToken }
}));
assertTruthy("front pay config dryRun", payConfig.dryRun === true, payConfig);
assertTruthy(
  "front pay config third-party dryRun",
  Array.isArray(payConfig.payConfig) && payConfig.payConfig.some((item) => item.value === "weixin" && item.dryRun === true)
    && payConfig.payConfig.some((item) => item.value === "alipay" && item.dryRun === true),
  payConfig
);
const unsafePayCopy = (payConfig.payConfig || []).filter((item) =>
  /dry-run|迁移|本地安全模式/i.test(`${item.name || ""} ${item.title || ""} ${item.message || ""}`)
);
assertTruthy("front pay config release copy", unsafePayCopy.length === 0, unsafePayCopy);

console.log(JSON.stringify({
  runtimeGuardSmokeOk: true,
  adminBase,
  appBase,
  unauthorized: unauthorizedResults,
  safeModes: {
    frontSendCode,
    adminSmsLogin,
    adminSmsInfo,
    expressSync,
    payConfig: {
      dryRun: payConfig.dryRun,
      methods: payConfig.payConfig?.map((item) => ({
        value: item.value,
        name: item.name,
        title: item.title,
        dryRun: Boolean(item.dryRun)
      }))
    }
  }
}, null, 2));
