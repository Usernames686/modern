import fs from "node:fs/promises";
import path from "node:path";

const adminBase = process.env.CRMEB_ADMIN_WEB || "http://127.0.0.1:19527";
const account = process.env.CRMEB_ADMIN_ACCOUNT || "admin";
const pwd = process.env.CRMEB_ADMIN_PWD || "123456";
const marker = "JSY_DUMMYJSON_PRODUCT";
const importLimit = Number(process.env.JSY_IMPORT_LIMIT || 100);
const timeoutMs = Number(process.env.CRMEB_RUNTIME_CHECK_TIMEOUT || 30000);
const imagesPerProduct = Number(process.env.JSY_IMAGES_PER_PRODUCT || 1);
const uploadRoot = path.resolve(process.env.CRMEB_UPLOAD_ROOT || defaultModernUploadRoot());
const imageDir = path.join(uploadRoot, "crmebimage/public/product/2026/06/20/dummyjson");
const imagePrefix = "crmebimage/public/product/2026/06/20/dummyjson";

function defaultModernUploadRoot() {
  return path.basename(process.cwd()) === "modern" ? "legacy-deps/upload" : "modern/legacy-deps/upload";
}

const categoryMap = new Map([
  ["beauty", { cateId: 293, prefix: "美妆", unit: "件" }],
  ["fragrances", { cateId: 294, prefix: "香水", unit: "瓶" }],
  ["furniture", { cateId: 300, prefix: "家居", unit: "件" }],
  ["groceries", { cateId: 300, prefix: "日用", unit: "件" }],
  ["home-decoration", { cateId: 300, prefix: "家居", unit: "件" }],
  ["kitchen-accessories", { cateId: 276, prefix: "厨房", unit: "件" }],
  ["laptops", { cateId: 804, prefix: "数码", unit: "台" }],
  ["mens-shirts", { cateId: 744, prefix: "男装", unit: "件" }],
  ["mens-shoes", { cateId: 796, prefix: "男鞋", unit: "双" }],
  ["mens-watches", { cateId: 300, prefix: "腕表", unit: "块" }],
  ["mobile-accessories", { cateId: 298, prefix: "数码配件", unit: "件" }],
  ["motorcycle", { cateId: 802, prefix: "出行", unit: "件" }],
  ["skin-care", { cateId: 291, prefix: "护肤", unit: "件" }],
  ["smartphones", { cateId: 804, prefix: "手机", unit: "台" }],
  ["sports-accessories", { cateId: 803, prefix: "运动", unit: "件" }],
  ["sunglasses", { cateId: 798, prefix: "配饰", unit: "副" }],
  ["tablets", { cateId: 804, prefix: "平板", unit: "台" }],
  ["tops", { cateId: 745, prefix: "女装", unit: "件" }],
  ["vehicle", { cateId: 802, prefix: "出行", unit: "件" }],
  ["womens-bags", { cateId: 801, prefix: "女包", unit: "个" }],
  ["womens-dresses", { cateId: 745, prefix: "女装", unit: "件" }],
  ["womens-jewellery", { cateId: 293, prefix: "饰品", unit: "件" }],
  ["womens-shoes", { cateId: 797, prefix: "女鞋", unit: "双" }],
  ["womens-watches", { cateId: 300, prefix: "腕表", unit: "块" }]
]);

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
      body = { raw: text.slice(0, 300) };
    }
    return { response, body };
  } finally {
    clearTimeout(timer);
  }
}

function assertOk(name, result) {
  if (result.body?.code !== 200) {
    throw new Error(`${name} failed: ${JSON.stringify({
      status: result.response.status,
      code: result.body?.code,
      message: result.body?.message,
      raw: result.body?.raw
    })}`);
  }
  return result.body.data;
}

function authHeaders(token) {
  return {
    "Content-Type": "application/json",
    "Authori-zation": token
  };
}

async function downloadImage(url, productId, index) {
  const ext = extensionFromUrl(url);
  const fileName = `dummyjson_${productId}_${index + 1}.${ext}`;
  const target = path.join(imageDir, fileName);
  try {
    await fs.access(target);
    return `${imagePrefix}/${fileName}`;
  } catch {
  }
  const response = await fetch(encodeURI(url), {
    redirect: "follow",
    headers: { "User-Agent": "CRMEB Modern product import" },
    signal: AbortSignal.timeout(timeoutMs)
  });
  const type = response.headers.get("content-type") || "";
  if (!response.ok || !type.startsWith("image/")) {
    throw new Error(`download image failed ${response.status}: ${url}`);
  }
  await fs.mkdir(imageDir, { recursive: true });
  await fs.writeFile(target, Buffer.from(await response.arrayBuffer()));
  return `${imagePrefix}/${fileName}`;
}

function extensionFromUrl(url) {
  const clean = url.split("?")[0].toLowerCase();
  if (clean.endsWith(".png")) return "png";
  if (clean.endsWith(".jpg") || clean.endsWith(".jpeg")) return "jpg";
  return "webp";
}

function localName(product, mapped) {
  return `${mapped.prefix} ${product.title}`.slice(0, 120);
}

function priceCny(usd) {
  return Number(Math.max(9.9, usd * 7.2).toFixed(2));
}

function productPayload(product, mapped, images, index, templateId) {
  const price = priceCny(Number(product.price || 9.99));
  const name = localName(product, mapped);
  return {
    storeName: name,
    storeInfo: `${name}，来源 DummyJSON 公开商品数据，图片、标题、价格来自同一商品记录。`,
    keyword: `${marker},dummyjson-${product.id},${product.category},${product.title}`,
    cateId: String(mapped.cateId),
    image: images[0],
    videoLink: "",
    sliderImages: images,
    price,
    vipPrice: price,
    otPrice: Number((price * 1.25).toFixed(2)),
    cost: Number((price * 0.55).toFixed(2)),
    unitName: mapped.unit,
    giveIntegral: 0,
    tempId: templateId,
    sort: 32700 - index,
    ficti: Math.max(20, Number(product.stock || 50)),
    isShow: true,
    isSub: false,
    isHot: index % 3 === 0,
    isBenefit: index % 5 === 0,
    isBest: true,
    isNew: index % 2 === 0,
    isGood: index % 4 === 0,
    activity: ["默认", "秒杀", "砍价", "拼团"],
    content: `<p>${name}</p><p>${product.description || ""}</p>${images.map((image) => `<p><img src="/${image}" /></p>`).join("")}`,
    couponIds: []
  };
}

async function existingImported(token) {
  const data = assertOk("existing products", await requestJson(
    `${adminBase}/api/admin/store/product/list?page=1&limit=500&type=0&keywords=${encodeURIComponent(marker)}`,
    { headers: { "Authori-zation": token } }
  ));
  return new Set((data.list || []).map((item) => {
    const match = String(item.keyword || "").match(/dummyjson-(\d+)/);
    return match ? Number(match[1]) : null;
  }).filter(Boolean));
}

async function stockUp(token, productId, stock) {
  const info = assertOk("product info", await requestJson(
    `${adminBase}/api/admin/store/product/info/${productId}`,
    { headers: { "Authori-zation": token } }
  ));
  const row = (info.attrValue || [])[0];
  if (!row?.id) {
    return false;
  }
  const current = Number(row.stock || 0);
  const target = Math.max(30, Number(stock || 60));
  if (current >= target) {
    return true;
  }
  assertOk("stock add", await requestJson(`${adminBase}/api/admin/store/product/quick/stock/add`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({
      id: productId,
      attrValueList: [{ id: row.id, addStock: target - current, version: row.version || 0 }]
    })
  }));
  return true;
}

const login = assertOk("admin login", await requestJson(`${adminBase}/api/admin/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account, pwd })
}));
const token = login.token;

const shipping = assertOk("shipping templates", await requestJson(
  `${adminBase}/api/admin/express/shipping/templates/list?page=1&limit=20`,
  { headers: { "Authori-zation": token } }
));
const templateId = shipping?.list?.[0]?.id || 1;
const existing = await existingImported(token);

const productData = await fetch(`https://dummyjson.com/products?limit=${importLimit}&skip=0`, {
  signal: AbortSignal.timeout(timeoutMs)
}).then((response) => response.json());
const products = (productData.products || []).filter((product) => categoryMap.has(product.category));

let created = 0;
let skipped = 0;
let failed = 0;
const samples = [];

for (const product of products) {
  if (existing.has(product.id)) {
    skipped += 1;
    continue;
  }
  const mapped = categoryMap.get(product.category);
  try {
    const remoteImages = [product.thumbnail, ...(product.images || [])].filter(Boolean).filter((value, index, array) => array.indexOf(value) === index);
    const images = [];
    for (let index = 0; index < remoteImages.length && images.length < imagesPerProduct; index += 1) {
      try {
        images.push(await downloadImage(remoteImages[index], product.id, index));
      } catch (error) {
        console.warn(`[dummyjson] image skipped ${product.id} ${product.title}: ${error.message}`);
      }
    }
    if (images.length === 0) {
      throw new Error("no images");
    }
    const body = productPayload(product, mapped, images, created, templateId);
    const data = assertOk("product create", await requestJson(`${adminBase}/api/admin/store/product/save`, {
      method: "POST",
      headers: authHeaders(token),
      body: JSON.stringify(body)
    }));
    await stockUp(token, data.id, product.stock);
    created += 1;
    if (samples.length < 12) {
      samples.push({ id: data.id, sourceId: product.id, name: body.storeName, cateId: body.cateId, price: body.price, image: body.image });
    }
  } catch (error) {
    failed += 1;
    console.error(`[dummyjson] failed ${product.id} ${product.title}: ${error.message}`);
  }
}

const front = await requestJson("http://127.0.0.1:19528/api/front/products?page=1&limit=12");
console.log(JSON.stringify({
  marker,
  fetched: productData.products?.length || 0,
  mappable: products.length,
  created,
  skipped,
  failed,
  frontTotal: front.body?.data?.total,
  samples
}, null, 2));
