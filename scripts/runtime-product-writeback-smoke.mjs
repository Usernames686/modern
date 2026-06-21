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

function flattenCategories(items, out = []) {
  for (const item of items || []) {
    out.push(item);
    flattenCategories(item.child, out);
  }
  return out;
}

function productPayload({ id, name, keyword, cateId, tempId, marker, price, sort }) {
  return {
    id,
    storeName: name,
    storeInfo: `交付验收商品简介 ${marker}`,
    keyword,
    cateId: String(cateId),
    image: "crmebimage/public/product/2026/06/19/demo/jsy_product_1.jpg",
    videoLink: "",
    sliderImages: [
      "crmebimage/public/product/2026/06/19/demo/jsy_product_1.jpg",
      "crmebimage/public/product/2026/06/19/demo/jsy_product_2.jpg"
    ],
    price,
    vipPrice: price,
    otPrice: price + 20,
    cost: Math.max(0, price - 40),
    unitName: "件",
    giveIntegral: 0,
    tempId,
    sort,
    ficti: 0,
    isShow: false,
    isSub: false,
    isHot: false,
    isBenefit: false,
    isBest: false,
    isNew: false,
    isGood: false,
    activity: ["默认", "秒杀", "砍价", "拼团"],
    content: `<p>${marker}</p><p>商品编辑写回验收</p>`,
    couponIds: []
  };
}

function parseSliderImages(value) {
  try {
    const parsed = JSON.parse(value || "[]");
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

const login = assertOk("admin login", await requestJson(`${adminBase}/api/admin/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account, pwd })
}));
const token = login.token;
assertTruthy("admin token", token, login);

const categoryTree = assertOk("category tree", await requestJson(
  `${adminBase}/api/admin/category/list/tree?status=-1&type=1`,
  { headers: { "Authori-zation": token } }
));
const category = flattenCategories(categoryTree).find((item) => item.status === true) || flattenCategories(categoryTree)[0];
assertTruthy("category available", category?.id > 0, categoryTree);

const shipping = assertOk("shipping templates", await requestJson(
  `${adminBase}/api/admin/express/shipping/templates/list?page=1&limit=20`,
  { headers: { "Authori-zation": token } }
));
const template = (shipping?.list || [])[0];
assertTruthy("shipping template available", template?.id > 0, shipping);

const stamp = Date.now();
const createName = `验收临时商品-${stamp}`;
const updateName = `验收临时商品-已更新-${stamp}`;
const createMarker = `product-create-${stamp}`;
const updateMarker = `product-update-${stamp}`;
let productId = null;
let deleted = false;

try {
  const created = assertOk("product create", await requestJson(`${adminBase}/api/admin/store/product/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(productPayload({
      name: createName,
      keyword: createMarker,
      cateId: category.id,
      tempId: template.id,
      marker: createMarker,
      price: 12.3,
      sort: 13
    }))
  }));
  productId = created.id;
  assertTruthy("created product id", Number.isInteger(productId) && productId > 0, created);

  const infoBefore = assertOk("product info before update", await requestJson(
    `${adminBase}/api/admin/store/product/info/${productId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("info before name", infoBefore.storeName === createName, infoBefore);
  assertTruthy("info before hidden", infoBefore.isShow === false, infoBefore);
  assertTruthy("info before content", String(infoBefore.content || "").includes(createMarker), infoBefore);
  assertTruthy("info before slider", parseSliderImages(infoBefore.sliderImage).length === 2, infoBefore);

  assertOk("product update", await requestJson(`${adminBase}/api/admin/store/product/update`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(productPayload({
      id: productId,
      name: updateName,
      keyword: updateMarker,
      cateId: category.id,
      tempId: template.id,
      marker: updateMarker,
      price: 18.8,
      sort: 31
    }))
  }));

  const infoAfter = assertOk("product info after update", await requestJson(
    `${adminBase}/api/admin/store/product/info/${productId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("info after id", infoAfter.id === productId, infoAfter);
  assertTruthy("info after name", infoAfter.storeName === updateName, infoAfter);
  assertTruthy("info after keyword", infoAfter.keyword === updateMarker, infoAfter);
  assertTruthy("info after price", Number(infoAfter.price) === 18.8, infoAfter);
  assertTruthy("info after sort", infoAfter.sort === 31, infoAfter);
  assertTruthy("info after category", String(infoAfter.cateId) === String(category.id), infoAfter);
  assertTruthy("info after temp", infoAfter.tempId === template.id, infoAfter);
  assertTruthy("info after content", String(infoAfter.content || "").includes(updateMarker), infoAfter);
  assertTruthy("info after coupons empty", Array.isArray(infoAfter.couponIds) && infoAfter.couponIds.length === 0, infoAfter);

  const listSearch = assertOk("product list after update", await requestJson(
    `${adminBase}/api/admin/store/product/list?page=1&limit=10&type=2&keywords=${encodeURIComponent(updateName)}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy(
    "updated hidden product listed",
    Array.isArray(listSearch.list) && listSearch.list.some((item) => item.id === productId && item.storeName === updateName),
    listSearch
  );

  assertOk("product put on shelf", await requestJson(
    `${adminBase}/api/admin/store/product/putOnShell/${productId}`,
    { headers: { "Authori-zation": token } }
  ));
  const infoAfterPutOn = assertOk("product info after put on", await requestJson(
    `${adminBase}/api/admin/store/product/info/${productId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("product put on is show", infoAfterPutOn.isShow === true, infoAfterPutOn);

  assertOk("product off shelf", await requestJson(
    `${adminBase}/api/admin/store/product/offShell/${productId}`,
    { headers: { "Authori-zation": token } }
  ));
  const infoAfterOff = assertOk("product info after off", await requestJson(
    `${adminBase}/api/admin/store/product/info/${productId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("product off is hidden", infoAfterOff.isShow === false, infoAfterOff);

  const stockRow = (infoAfterOff.attrValue || [])[0];
  let stockAddChecked = false;
  let stockAddSkipped = "";
  if (stockRow?.id > 0) {
    const beforeStock = Number(infoAfterOff.stock || 0);
    const beforeSkuStock = Number(stockRow.stock || 0);
    assertOk("product stock add", await requestJson(`${adminBase}/api/admin/store/product/quick/stock/add`, {
      method: "POST",
      headers: authHeaders(token),
      body: JSON.stringify({
        id: productId,
        attrValueList: [
          {
            id: stockRow.id,
            addStock: 1,
            version: stockRow.version || 0
          }
        ]
      })
    }));
    const infoAfterStock = assertOk("product info after stock", await requestJson(
      `${adminBase}/api/admin/store/product/info/${productId}`,
      { headers: { "Authori-zation": token } }
    ));
    const stockRowAfter = (infoAfterStock.attrValue || [])[0];
    assertTruthy("product stock increased", Number(infoAfterStock.stock || 0) === beforeStock + 1, { beforeStock, infoAfterStock });
    assertTruthy("product sku stock increased", Number(stockRowAfter?.stock || 0) === beforeSkuStock + 1, { beforeSkuStock, stockRowAfter });
    assertTruthy("product sku version increased", Number(stockRowAfter?.version || 0) === Number(stockRow.version || 0) + 1, { before: stockRow, after: stockRowAfter });
    stockAddChecked = true;
  } else {
    stockAddSkipped = "created basic product has no normal SKU attrValue";
  }

  assertOk("product hard delete", await requestJson(
    `${adminBase}/api/admin/store/product/delete/${productId}?type=delete`,
    { headers: { "Authori-zation": token } }
  ));
  deleted = true;

  const infoAfterDelete = await requestJson(`${adminBase}/api/admin/store/product/info/${productId}`, {
    headers: { "Authori-zation": token }
  });
  assertTruthy("info after delete not ok", infoAfterDelete.body?.code !== 200, infoAfterDelete.body);

  const listAfterDelete = assertOk("product list after delete", await requestJson(
    `${adminBase}/api/admin/store/product/list?page=1&limit=10&type=2&keywords=${encodeURIComponent(updateName)}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy(
    "deleted product excluded from list",
    !Array.isArray(listAfterDelete.list) || !listAfterDelete.list.some((item) => item.id === productId),
    listAfterDelete
  );

  console.log(JSON.stringify({
    runtimeProductWritebackSmokeOk: true,
    adminBase,
    product: {
      id: productId,
      createName,
      updateName,
      categoryId: category.id,
      tempId: template.id,
      putOnOffChecked: true,
      stockAddChecked,
      stockAddSkipped,
      deleted
    }
  }, null, 2));
} finally {
  if (productId && !deleted) {
    try {
      await requestJson(`${adminBase}/api/admin/store/product/delete/${productId}?type=delete`, {
        headers: { "Authori-zation": token }
      });
    } catch {
      // Keep the original assertion error visible. Manual cleanup can search by the unique names above.
    }
  }
}
