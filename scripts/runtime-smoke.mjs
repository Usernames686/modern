const adminBase = process.env.CRMEB_ADMIN_WEB || "http://127.0.0.1:19527";
const appBase = process.env.CRMEB_APP_WEB || "http://127.0.0.1:19528";
const account = process.env.CRMEB_ADMIN_ACCOUNT || "admin";
const pwd = process.env.CRMEB_ADMIN_PWD || "123456";

async function requestJson(url, options = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 10000);
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

function flattenMenus(items, out = []) {
  for (const item of items || []) {
    if (item.childList?.length) {
      flattenMenus(item.childList, out);
    } else if (item.component) {
      out.push({ name: item.name, component: item.component });
    }
  }
  return out;
}

function summarizeData(data) {
  if (Array.isArray(data?.list)) return { count: data.list.length, total: data.total ?? data.count };
  if (Array.isArray(data)) return { count: data.length };
  if (data && typeof data === "object") return { keys: Object.keys(data).slice(0, 8) };
  return { type: typeof data };
}

const login = await requestJson(`${adminBase}/api/admin/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account, pwd })
});
if (login.body?.code !== 200 || !login.body?.data?.token) {
  throw new Error(`admin login failed: ${JSON.stringify(login.body)}`);
}
const token = login.body.data.token;

const menuResult = await requestJson(`${adminBase}/api/admin/getMenus`, {
  headers: { "Authori-zation": token }
});
if (menuResult.body?.code !== 200) {
  throw new Error(`admin menus failed: ${JSON.stringify(menuResult.body)}`);
}
const menuLeaves = flattenMenus(menuResult.body.data);

const checks = [
  ["admin health", `${adminBase}/api/admin/health`],
  ["admin info", `${adminBase}/api/admin/getAdminInfoByToken`, true],
  ["admin products", `${adminBase}/api/admin/store/product/list?page=1&limit=5&type=1`, true],
  ["admin orders", `${adminBase}/api/admin/store/order/list?page=1&limit=5&status=all&type=2`, true],
  ["admin order nums", `${adminBase}/api/admin/store/order/status/num?type=2`, true],
  ["admin users", `${adminBase}/api/admin/user/list?page=1&limit=5&accessType=0`, true],
  ["admin seckill", `${adminBase}/api/admin/store/seckill/list?page=1&limit=5`, true],
  ["admin bargain", `${adminBase}/api/admin/store/bargain/list?page=1&limit=5`, true],
  ["admin combination", `${adminBase}/api/admin/store/combination/list?page=1&limit=5`, true],
  ["admin coupon", `${adminBase}/api/admin/marketing/coupon/list?page=1&limit=5`, true],
  ["admin pagediy", `${adminBase}/api/admin/pagediy/list?page=1&limit=5`, true],
  ["admin layout", `${adminBase}/api/admin/page/layout/index`, true],
  ["admin attachment default pid", `${adminBase}/api/admin/system/attachment/list?page=1&limit=5`, true],
  ["front health", `${appBase}/api/front/health`],
  ["front index", `${appBase}/api/front/index`],
  ["front category", `${appBase}/api/front/category`],
  ["front products", `${appBase}/api/front/products?page=1&limit=5`],
  ["front search", `${appBase}/api/front/search/keyword`],
  ["front seckill", `${appBase}/api/front/seckill/header`],
  ["front combination", `${appBase}/api/front/combination/list?page=1&limit=5`],
  ["front bargain", `${appBase}/api/front/bargain/list?page=1&limit=5`],
  ["front coupons guest", `${appBase}/api/front/coupons?page=1&limit=5`]
];

const results = [];
for (const [name, url, auth] of checks) {
  const { response, body } = await requestJson(url, {
    headers: auth ? { "Authori-zation": token } : {}
  });
  const ok = response.ok && body?.code === 200;
  results.push({
    name,
    ok,
    status: response.status,
    code: body?.code,
    message: body?.message || null,
    ...summarizeData(body?.data)
  });
}

const failed = results.filter((item) => !item.ok);
console.log(JSON.stringify({
  adminBase,
  appBase,
  menuLeaves: menuLeaves.length,
  checks: results
}, null, 2));

if (menuLeaves.length < 50) {
  throw new Error(`admin menu leaf count too low: ${menuLeaves.length}`);
}
if (failed.length) {
  throw new Error(`runtime smoke failed: ${JSON.stringify(failed, null, 2)}`);
}
