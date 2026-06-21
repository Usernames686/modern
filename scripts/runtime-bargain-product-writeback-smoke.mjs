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

function firstImage(product) {
  const image = stringValue(product.image);
  if (image) return image;
  const slider = stringValue(product.sliderImage || product.images);
  if (slider.startsWith("[")) {
    try {
      const parsed = JSON.parse(slider);
      if (Array.isArray(parsed) && parsed[0]) return String(parsed[0]);
    } catch {
      return "";
    }
  }
  return "/crmebimage/public/product/2026/06/19/demo/jsy_product_1.jpg";
}

function bargainPayload({ product, title, price, minPrice, quota, sort, marker }) {
  const image = firstImage(product);
  const stock = Math.max(quota, Math.min(numberValue(product.stock, quota), quota + 20));
  const cost = Math.max(0.01, Number((price * 0.55).toFixed(2)));
  const otPrice = Math.max(price + 1, numberValue(product.otPrice, price + 20));
  return {
    productId: product.id,
    title,
    storeName: title,
    image,
    images: JSON.stringify([image]),
    imagess: [image],
    unitName: product.unitName || "件",
    startTime: futureDate(32),
    stopTime: futureDate(33),
    status: 0,
    num: 1,
    bargainNum: 1,
    peopleNum: 1,
    tempId: numberValue(product.tempId, 0),
    sort,
    info: "交付验收临时砍价商品",
    price,
    minPrice,
    cost,
    otPrice,
    giveIntegral: 0,
    stock,
    quota,
    quotaShow: quota,
    weight: numberValue(product.weight, 0),
    volume: numberValue(product.volume, 0),
    postage: 0,
    isPostage: 1,
    specType: false,
    content: `<p>${marker}</p>`,
    rule: "交付验收临时规则",
    attr: [
      { attrName: "规格", attrValues: "默认" }
    ],
    attrValue: [
      {
        suk: "默认",
        price,
        minPrice,
        cost,
        otPrice,
        quota,
        quotaShow: quota,
        stock,
        image,
        weight: numberValue(product.weight, 0),
        volume: numberValue(product.volume, 0),
        barCode: `BMOKE${sort}`,
        attrValue: { "规格": "默认" }
      }
    ]
  };
}

async function productSample(token) {
  const data = assertOk("product list", await requestJson(
    `${adminBase}/api/admin/store/product/list?page=1&limit=1&type=1`,
    { headers: { "Authori-zation": token } }
  ));
  const row = data?.list?.[0];
  assertTruthy("product list sample", row?.id > 0, data);
  const info = assertOk("product info", await requestJson(
    `${adminBase}/api/admin/store/product/info/${row.id}`,
    { headers: { "Authori-zation": token } }
  ));
  return { ...row, ...info };
}

async function findBargainByTitle(token, title) {
  const data = assertOk("bargain list by title", await requestJson(
    `${adminBase}/api/admin/store/bargain/list?page=1&limit=10&keywords=${encodeURIComponent(title)}`,
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
const createTitle = `验收临时砍价商品-${stamp}`;
const updateTitle = `验收临时砍价商品-已更新-${stamp}`;
const createMarker = `bargain-product-create-${stamp}`;
const updateMarker = `bargain-product-update-${stamp}`;
let bargainId = null;
let deleted = false;

try {
  const product = await productSample(token);
  assertTruthy("product sample id", product.id > 0, product);

  assertOk("bargain product create", await requestJson(`${adminBase}/api/admin/store/bargain/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(bargainPayload({
      product,
      title: createTitle,
      price: 19.9,
      minPrice: 9.9,
      quota: 2,
      sort: 951,
      marker: createMarker
    }))
  }));

  const created = await findBargainByTitle(token, createTitle);
  assertTruthy("created bargain product found", created?.id > 0, created);
  bargainId = created.id;
  assertTruthy("created bargain product closed", created.status === false || Number(created.status || 0) === 0, created);
  assertTruthy("created bargain product quota", Number(created.quota || 0) === 2 || Number(created.quotaShow || 0) === 2, created);

  const infoBefore = assertOk("bargain product info before update", await requestJson(
    `${adminBase}/api/admin/store/bargain/info?id=${bargainId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("info before update title", infoBefore.title === createTitle, infoBefore);
  assertTruthy("info before update attr", Array.isArray(infoBefore.attr), infoBefore);
  assertTruthy("info before update attrValue", Array.isArray(infoBefore.attrValue) && infoBefore.attrValue.length > 0, infoBefore);
  assertTruthy("info before update content", String(infoBefore.content || "").includes(createMarker), infoBefore);

  assertOk("bargain product update", await requestJson(`${adminBase}/api/admin/store/bargain/update?id=${bargainId}`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(bargainPayload({
      product,
      title: updateTitle,
      price: 18.8,
      minPrice: 8.8,
      quota: 3,
      sort: 952,
      marker: updateMarker
    }))
  }));

  const infoAfter = assertOk("bargain product info after update", await requestJson(
    `${adminBase}/api/admin/store/bargain/info?id=${bargainId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("info after update id", infoAfter.id === bargainId, infoAfter);
  assertTruthy("info after update title", infoAfter.title === updateTitle, infoAfter);
  assertTruthy("info after update price", Number(infoAfter.price) === 18.8, infoAfter);
  assertTruthy("info after update minPrice", Number(infoAfter.minPrice) === 8.8, infoAfter);
  assertTruthy("info after update quota", Number(infoAfter.quota) === 3, infoAfter);
  assertTruthy("info after update content", String(infoAfter.content || "").includes(updateMarker), infoAfter);
  assertTruthy("info after update attrValue", Array.isArray(infoAfter.attrValue) && Number(infoAfter.attrValue[0]?.quota || 0) === 3, infoAfter);

  const updated = await findBargainByTitle(token, updateTitle);
  assertTruthy("updated bargain product listed", updated?.id === bargainId, updated);

  assertOk("bargain product delete", await requestJson(`${adminBase}/api/admin/store/bargain/delete?id=${bargainId}`, {
    headers: { "Authori-zation": token }
  }));
  deleted = true;

  const infoAfterDelete = await requestJson(`${adminBase}/api/admin/store/bargain/info?id=${bargainId}`, {
    headers: { "Authori-zation": token }
  });
  assertTruthy("info after delete is not ok", infoAfterDelete.body?.code !== 200, infoAfterDelete.body);

  const deletedListRow = await findBargainByTitle(token, updateTitle);
  assertTruthy("deleted bargain product excluded from list", !deletedListRow, deletedListRow);

  console.log(JSON.stringify({
    runtimeBargainProductWritebackSmokeOk: true,
    adminBase,
    bargainProduct: {
      id: bargainId,
      productId: product.id,
      createTitle,
      updateTitle,
      deleted
    }
  }, null, 2));
} finally {
  if (bargainId && !deleted) {
    try {
      await requestJson(`${adminBase}/api/admin/store/bargain/delete?id=${bargainId}`, {
        headers: { "Authori-zation": token }
      });
    } catch {
      // Keep the original assertion error visible. Manual cleanup can search by the unique titles above.
    }
  }
}
