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

function futureDate(offsetDays) {
  const date = new Date(Date.now() + offsetDays * 24 * 60 * 60 * 1000);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function numberValue(value, fallback = 0) {
  const number = Number(value);
  return Number.isFinite(number) ? number : fallback;
}

function stringValue(value, fallback = "") {
  return value == null || value === "" ? fallback : String(value);
}

function parseManagerRange(manager) {
  if (Number.isFinite(Number(manager?.startTime)) && Number.isFinite(Number(manager?.endTime))) {
    return [Number(manager.startTime), Number(manager.endTime)];
  }
  const text = String(manager?.time || "");
  if (!text.includes(",")) {
    return null;
  }
  const [start, end] = text.split(",", 2).map((part) => Number(String(part).trim().split(":")[0]));
  if (!Number.isFinite(start) || !Number.isFinite(end)) {
    return null;
  }
  return [start, end];
}

function findFreeManagerTime(managers) {
  const occupied = (managers || [])
    .map(parseManagerRange)
    .filter(Boolean);
  for (let hour = 0; hour < 24; hour += 1) {
    const start = hour;
    const end = hour + 1;
    const conflict = occupied.some(([itemStart, itemEnd]) => itemStart < end && itemEnd > start);
    if (!conflict) {
      return `${String(start).padStart(2, "0")}:00,${String(end).padStart(2, "0")}:00`;
    }
  }
  return "";
}

function seckillPayload({ draft, timeId, title, price, quota, sort, marker }) {
  const image = stringValue(draft.image || draft.sliderImage?.[0], "/crmebimage/public/product/2026/06/19/demo/jsy_product_1.jpg");
  const stock = Math.max(quota, Math.min(numberValue(draft.stock, quota), quota + 20));
  const cost = Math.max(0.01, Number((price * 0.55).toFixed(2)));
  const otPrice = Math.max(price + 1, numberValue(draft.otPrice, price + 20));
  return {
    productId: draft.productId || draft.id,
    image,
    images: JSON.stringify([image]),
    imagess: [image],
    title,
    info: "交付验收临时秒杀商品",
    unitName: draft.unitName || "件",
    timeId,
    startTime: futureDate(30),
    stopTime: futureDate(31),
    status: 0,
    num: 1,
    tempId: numberValue(draft.tempId, 0),
    sort,
    price,
    cost,
    otPrice,
    giveIntegral: 0,
    stock,
    quota,
    quotaShow: quota,
    weight: numberValue(draft.weight, 0),
    volume: numberValue(draft.volume, 0),
    postage: 0,
    specType: false,
    content: `<p>${marker}</p>`,
    attr: [
      { attrName: "规格", attrValues: "默认" }
    ],
    attrValue: [
      {
        suk: "默认",
        price,
        cost,
        otPrice,
        quota,
        quotaShow: quota,
        stock,
        image,
        weight: numberValue(draft.weight, 0),
        volume: numberValue(draft.volume, 0),
        barCode: `SMOKE${sort}`,
        attrValue: { "规格": "默认" }
      }
    ]
  };
}

async function firstProduct(token) {
  const data = assertOk("product list", await requestJson(
    `${adminBase}/api/admin/store/product/list?page=1&limit=1&type=1`,
    { headers: { "Authori-zation": token } }
  ));
  const product = data?.list?.[0];
  assertTruthy("product sample", product?.id > 0, data);
  return product;
}

async function ensureManager(token) {
  const name = "验收临时时段-商品闭环";
  const managers = assertOk("seckill manager list", await requestJson(
    `${adminBase}/api/admin/store/seckill/manger/list?page=1&limit=200`,
    { headers: { "Authori-zation": token } }
  ))?.list || [];
  const existing = managers.find((item) => item.name === name);
  if (existing?.id) {
    return { id: existing.id, created: false };
  }
  const time = findFreeManagerTime(managers);
  if (!time) {
    const reusable = managers.find((item) => item?.id > 0);
    assertTruthy("reusable seckill manager", reusable?.id > 0, managers);
    return { id: reusable.id, created: false, reusedExisting: true };
  }
  assertOk("seckill manager create for product smoke", await requestJson(`${adminBase}/api/admin/store/seckill/manger/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({
      name,
      time,
      img: "",
      silderImgs: "[]",
      sort: 930,
      status: "'0'"
    })
  }));
  const created = assertOk("created seckill manager list", await requestJson(
    `${adminBase}/api/admin/store/seckill/manger/list?page=1&limit=10&name=${encodeURIComponent(name)}`,
    { headers: { "Authori-zation": token } }
  ))?.list?.find((item) => item.name === name);
  assertTruthy("created seckill manager found", created?.id > 0, created);
  return { id: created.id, created: true };
}

async function findSeckillByTitle(token, title) {
  const data = assertOk("seckill list by title", await requestJson(
    `${adminBase}/api/admin/store/seckill/list?page=1&limit=10&keywords=${encodeURIComponent(title)}`,
    { headers: { "Authori-zation": token } }
  ));
  return (data?.list || []).find((item) => item.title === title) || null;
}

const login = assertOk("admin login", await requestJson(`${adminBase}/api/admin/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account, pwd })
}));
const token = login.token;
assertTruthy("admin token", token, login);

const stamp = Date.now();
const createTitle = `验收临时秒杀商品-${stamp}`;
const updateTitle = `验收临时秒杀商品-已更新-${stamp}`;
const createMarker = `seckill-product-create-${stamp}`;
const updateMarker = `seckill-product-update-${stamp}`;
let seckillId = null;
let manager = null;
let deleted = false;

try {
  manager = await ensureManager(token);
  const product = await firstProduct(token);
  const draft = assertOk("seckill product draft", await requestJson(
    `${adminBase}/api/admin/store/seckill/product/detail?id=${product.id}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("draft product id", Number(draft.productId || draft.id) === Number(product.id), draft);

  assertOk("seckill product create", await requestJson(`${adminBase}/api/admin/store/seckill/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(seckillPayload({
      draft,
      timeId: manager.id,
      title: createTitle,
      price: 9.9,
      quota: 2,
      sort: 941,
      marker: createMarker
    }))
  }));

  const created = await findSeckillByTitle(token, createTitle);
  assertTruthy("created seckill product found", created?.id > 0, created);
  seckillId = created.id;
  assertTruthy("created seckill product closed", Number(created.status || 0) === 0, created);
  assertTruthy("created seckill product quota", Number(created.quota || 0) === 2, created);

  const infoBefore = assertOk("seckill product info before update", await requestJson(
    `${adminBase}/api/admin/store/seckill/info?id=${seckillId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("info before update title", infoBefore.title === createTitle, infoBefore);
  assertTruthy("info before update attr", Array.isArray(infoBefore.attr), infoBefore);
  assertTruthy("info before update attrValue", Array.isArray(infoBefore.attrValue) && infoBefore.attrValue.length > 0, infoBefore);
  assertTruthy("info before update content", String(infoBefore.content || "").includes(createMarker), infoBefore);

  assertOk("seckill product update", await requestJson(`${adminBase}/api/admin/store/seckill/update?id=${seckillId}`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(seckillPayload({
      draft,
      timeId: manager.id,
      title: updateTitle,
      price: 8.8,
      quota: 3,
      sort: 942,
      marker: updateMarker
    }))
  }));

  const infoAfter = assertOk("seckill product info after update", await requestJson(
    `${adminBase}/api/admin/store/seckill/info?id=${seckillId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("info after update id", infoAfter.id === seckillId, infoAfter);
  assertTruthy("info after update title", infoAfter.title === updateTitle, infoAfter);
  assertTruthy("info after update price", Number(infoAfter.price) === 8.8, infoAfter);
  assertTruthy("info after update quota", Number(infoAfter.quota) === 3, infoAfter);
  assertTruthy("info after update content", String(infoAfter.content || "").includes(updateMarker), infoAfter);
  assertTruthy("info after update attrValue", Array.isArray(infoAfter.attrValue) && Number(infoAfter.attrValue[0]?.quota || 0) === 3, infoAfter);

  const updated = await findSeckillByTitle(token, updateTitle);
  assertTruthy("updated seckill product listed", updated?.id === seckillId, updated);

  assertOk("seckill product delete", await requestJson(`${adminBase}/api/admin/store/seckill/delete?id=${seckillId}`, {
    headers: { "Authori-zation": token }
  }));
  deleted = true;

  const infoAfterDelete = await requestJson(`${adminBase}/api/admin/store/seckill/info?id=${seckillId}`, {
    headers: { "Authori-zation": token }
  });
  assertTruthy("info after delete is not ok", infoAfterDelete.body?.code !== 200, infoAfterDelete.body);

  const deletedListRow = await findSeckillByTitle(token, updateTitle);
  assertTruthy("deleted seckill product excluded from list", !deletedListRow, deletedListRow);

  console.log(JSON.stringify({
    runtimeSeckillProductWritebackSmokeOk: true,
    adminBase,
    seckillProduct: {
      id: seckillId,
      productId: product.id,
      managerId: manager.id,
      createTitle,
      updateTitle,
      deleted
    }
  }, null, 2));
} finally {
  if (seckillId && !deleted) {
    try {
      await requestJson(`${adminBase}/api/admin/store/seckill/delete?id=${seckillId}`, {
        headers: { "Authori-zation": token }
      });
    } catch {
      // Keep the original assertion error visible. Manual cleanup can search by the unique titles above.
    }
  }
  if (manager?.created) {
    await requestJson(`${adminBase}/api/admin/store/seckill/manger/delete?id=${manager.id}`, {
      headers: { "Authori-zation": token }
    }).catch(() => {});
  }
}
