const adminBase = process.env.CRMEB_ADMIN_WEB || "http://127.0.0.1:19527";
const appBase = process.env.CRMEB_APP_WEB || "http://127.0.0.1:19528";
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

function assertOk(name, result) {
  if (!result.response.ok || result.body?.code !== 200) {
    throw new Error(`${name} failed: ${JSON.stringify({
      status: result.response.status,
      code: result.body?.code,
      message: result.body?.message,
      raw: result.body?.raw
    })}`);
  }
  return result.body.data;
}

function firstRow(name, data) {
  const row = Array.isArray(data?.list) ? data.list[0] : null;
  if (!row?.id) {
    throw new Error(`${name} returned no row with id`);
  }
  return row;
}

function assertFields(name, data, fields) {
  const missing = fields.filter((field) => data?.[field] === undefined);
  if (missing.length) {
    throw new Error(`${name} missing fields: ${missing.join(", ")}`);
  }
}

async function get(name, url, token) {
  return assertOk(name, await requestJson(url, {
    headers: token ? { "Authori-zation": token } : {}
  }));
}

const loginData = assertOk("admin login", await requestJson(`${adminBase}/api/admin/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account, pwd })
}));
const token = loginData.token;

const adminProducts = await get("admin product list", `${adminBase}/api/admin/store/product/list?page=1&limit=1&type=1`, token);
const adminProduct = firstRow("admin product list", adminProducts);
const productInfo = await get("admin product info", `${adminBase}/api/admin/store/product/info/${adminProduct.id}`, token);
assertFields("admin product info", productInfo, ["id", "storeName", "image", "price", "stock", "attr", "attrValue", "content"]);

const orders = await get("admin order list", `${adminBase}/api/admin/store/order/list?page=1&limit=1&status=all&type=2`, token);
const order = firstRow("admin order list", orders);
const orderNo = order.orderId || order.orderNo;
if (!orderNo) {
  throw new Error("admin order list row missing orderId/orderNo");
}
const orderInfo = await get("admin order info", `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(orderNo)}`, token);
assertFields("admin order info", orderInfo, ["id", "orderId", "productList", "refundStatus", "deliveryType", "statusStr"]);
await get("admin order status list", `${adminBase}/api/admin/store/order/status/list?orderNo=${encodeURIComponent(orderNo)}&page=1&limit=5`, token);
await get("admin order status num", `${adminBase}/api/admin/store/order/status/num?type=2`, token);

const seckillList = await get("admin seckill list", `${adminBase}/api/admin/store/seckill/list?page=1&limit=1`, token);
const seckill = firstRow("admin seckill list", seckillList);
const seckillInfo = await get("admin seckill info", `${adminBase}/api/admin/store/seckill/info?id=${seckill.id}`, token);
assertFields("admin seckill info", seckillInfo, ["id", "productId", "title", "attr", "attrValue", "content", "sliderImage"]);

const bargainList = await get("admin bargain list", `${adminBase}/api/admin/store/bargain/list?page=1&limit=1`, token);
const bargain = firstRow("admin bargain list", bargainList);
const bargainInfo = await get("admin bargain info", `${adminBase}/api/admin/store/bargain/info?id=${bargain.id}`, token);
assertFields("admin bargain info", bargainInfo, ["id", "productId", "title", "attr", "attrValue", "content", "sliderImage", "minPrice"]);

const combinationList = await get("admin combination list", `${adminBase}/api/admin/store/combination/list?page=1&limit=1`, token);
const combination = firstRow("admin combination list", combinationList);
const combinationInfo = await get("admin combination info", `${adminBase}/api/admin/store/combination/info?id=${combination.id}`, token);
assertFields("admin combination info", combinationInfo, ["id", "productId", "title", "attr", "attrValue", "content", "sliderImage", "startTimeStr", "stopTimeStr"]);

const pageDiyList = await get("admin pagediy list", `${adminBase}/api/admin/pagediy/list?page=1&limit=1`, token);
const pageDiy = firstRow("admin pagediy list", pageDiyList);
const pageDiyInfo = await get("admin pagediy info", `${adminBase}/api/admin/pagediy/info/${pageDiy.id}`, token);
assertFields("admin pagediy info", pageDiyInfo, ["id", "name", "title", "value", "isDefault"]);

const frontProducts = await get("front product list", `${appBase}/api/front/products?page=1&limit=1`);
const frontProduct = firstRow("front product list", frontProducts);
const frontDetail = await get("front product detail", `${appBase}/api/front/product/detail/${frontProduct.id}`);
assertFields("front product detail", frontDetail, ["productInfo", "productAttr", "productValue", "reply", "replyList", "recommend"]);
await get("front reply list", `${appBase}/api/front/reply/list/${frontProduct.id}?page=1&limit=5`);
await get("front reply config", `${appBase}/api/front/reply/config/${frontProduct.id}`);
await get("front category", `${appBase}/api/front/category`);
await get("front page diy default", `${appBase}/api/front/page/diy/default`);

console.log(JSON.stringify({
  runtimeDeepSmokeOk: true,
  adminBase,
  appBase,
  checked: {
    productId: adminProduct.id,
    orderNo,
    seckillId: seckill.id,
    bargainId: bargain.id,
    combinationId: combination.id,
    pageDiyId: pageDiy.id,
    frontProductId: frontProduct.id
  }
}, null, 2));
