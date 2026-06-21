import fs from "node:fs/promises";
import path from "node:path";

const adminBase = process.env.CRMEB_ADMIN_WEB || "http://127.0.0.1:19527";
const frontBase = process.env.CRMEB_APP_WEB || "http://127.0.0.1:19528";
const account = process.env.CRMEB_ADMIN_ACCOUNT || "admin";
const pwd = process.env.CRMEB_ADMIN_PWD || "123456";
const marker = "JSY_CATEGORY_FILL_20260620";
const timeoutMs = Number(process.env.CRMEB_RUNTIME_CHECK_TIMEOUT || 30000);
const uploadRoot = path.resolve(process.env.CRMEB_UPLOAD_ROOT || defaultModernUploadRoot());
const imageDir = path.join(uploadRoot, "crmebimage/public/product/2026/06/20/category-fill");
const imagePrefix = "crmebimage/public/product/2026/06/20/category-fill";

function defaultModernUploadRoot() {
  return path.basename(process.cwd()) === "modern" ? "legacy-deps/upload" : "modern/legacy-deps/upload";
}

const categoryFillPlan = {
  275: { prefix: "厨房电器", unit: "件", source: "kolz", ids: ["32", "33", "34"] },
  277: { prefix: "破壁机", unit: "台", source: "dummyjson", ids: [51, 61] },
  825: { prefix: "厨房电器", unit: "件", source: "kolz", ids: ["32", "34", "35"] },
  285: {
    prefix: "母婴腰凳",
    unit: "件",
    source: "static",
    products: [
      {
        id: "off-baby-ready-feed-bottle",
        title: "Hipp Baby Milk Ready To Feed Bottle",
        price: 32,
        image: "https://images.openfoodfacts.org/images/products/406/230/032/9409/front_en.4.400.jpg",
        description: "OpenFoodFacts baby product record."
      },
      {
        id: "off-super-baby-bottle",
        title: "Super Baby Bottle",
        price: 29,
        image: "https://images.openfoodfacts.org/images/products/489/506/051/1117/front_en.3.400.jpg",
        description: "OpenFoodFacts baby bottle product record."
      }
    ]
  },
  286: {
    prefix: "奶粉",
    unit: "罐",
    source: "static",
    products: [
      {
        id: "off-infant-formula-3041092010550",
        title: "Infant Formula",
        price: 168,
        image: "https://images.openfoodfacts.org/images/products/304/109/201/0550/front_en.14.400.jpg",
        description: "OpenFoodFacts infant formula product record."
      },
      {
        id: "off-infant-formula-6111018909170",
        title: "Nestle Infant Formula",
        price: 189,
        image: "https://images.openfoodfacts.org/images/products/611/101/890/9170/front_ar.5.400.jpg",
        description: "OpenFoodFacts infant formula product record."
      }
    ]
  },
  287: {
    prefix: "儿童玩具",
    unit: "件",
    source: "static",
    products: [
      {
        id: "off-toy-story-gummies",
        title: "Toy Story 4 Kids Gummies",
        price: 25,
        image: "https://images.openfoodfacts.org/images/products/075/942/700/0130/front_en.3.400.jpg",
        description: "OpenFoodFacts kids Toy Story product record."
      },
      {
        id: "off-baby-candy-bottle-pop",
        title: "Bottle Pop Baby Candy",
        price: 19,
        image: "https://images.openfoodfacts.org/images/products/004/111/600/4773/front_en.5.400.jpg",
        description: "OpenFoodFacts kids candy product record."
      }
    ]
  },
  806: { prefix: "奶瓶", unit: "个", source: "openfoodfacts", query: "baby bottle" },
  289: { prefix: "骑行配件", unit: "件", source: "dummyjson", ids: [144, 150, 152] },
  290: { prefix: "游泳设备", unit: "件", source: "dummyjson", ids: [137, 140, 153] },
  292: { prefix: "美容仪", unit: "件", source: "kolz", ids: ["1", "5", "7"] },
  295: { prefix: "修容", unit: "件", source: "kolz", ids: ["3", "6", "7"] },
  296: { prefix: "美妆工具", unit: "件", source: "kolz", ids: ["1", "2", "5"] },
  297: { prefix: "按摩仪", unit: "件", source: "kolz", ids: ["44", "45", "46"] },
  805: { prefix: "音响", unit: "台", source: "dummyjson", ids: [99, 103] },
  301: { prefix: "纸品湿巾", unit: "件", source: "dummyjson", ids: [41] },
  302: { prefix: "办公文具", unit: "件", source: "kolz", ids: ["33", "34", "35"] },
  746: { prefix: "童鞋", unit: "双", source: "kolz", ids: ["23"] },
  800: { prefix: "枕头", unit: "件", source: "kolz", ids: ["31", "36", "37"] }
};

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
  return { "Content-Type": "application/json", "Authori-zation": token };
}

async function downloadImage(url, sourceId) {
  const ext = extensionFromUrl(url);
  const safeId = String(sourceId).replace(/[^a-z0-9_-]+/gi, "-").replace(/^-+|-+$/g, "").toLowerCase();
  const fileName = `${safeId}.${ext}`;
  const target = path.join(imageDir, fileName);
  try {
    await fs.access(target);
    return `${imagePrefix}/${fileName}`;
  } catch {
  }
  const response = await fetch(encodeURI(url), {
    redirect: "follow",
    headers: { "User-Agent": "CRMEB Modern category product import" },
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

function priceCny(value, defaultPrice = 29) {
  const number = Number(value || defaultPrice);
  if (number > 1000) {
    return Number((number / 100).toFixed(2));
  }
  return Number(Math.max(9.9, number * 7.2).toFixed(2));
}

async function currentCategoryTotal(cateId) {
  const result = await requestJson(`${frontBase}/api/front/products?page=1&limit=1&cid=${cateId}`);
  return Number(result.body?.data?.total || 0);
}

async function existingMarkers(token) {
  const data = assertOk("existing category fill products", await requestJson(
    `${adminBase}/api/admin/store/product/list?page=1&limit=500&type=0&keywords=${encodeURIComponent(marker)}`,
    { headers: { "Authori-zation": token } }
  ));
  return new Set((data.list || []).map((item) => String(item.keyword || "")));
}

async function loadSourceProducts(plan) {
  if (plan.source === "dummyjson") {
    const products = [];
    for (const id of plan.ids) {
      const product = await fetch(`https://dummyjson.com/products/${id}`, { signal: AbortSignal.timeout(timeoutMs) }).then((r) => r.json());
      products.push({
        sourceId: `dummyjson-${product.id}`,
        title: product.title,
        price: priceCny(product.price),
        image: product.thumbnail || product.images?.[0],
        description: product.description || ""
      });
    }
    return products;
  }
  if (plan.source === "kolz") {
    const list = await fetch("https://kolzsticks.github.io/Free-Ecommerce-Products-Api/main/products.json", {
      signal: AbortSignal.timeout(timeoutMs)
    }).then((r) => r.json());
    return list
      .filter((product) => plan.ids.includes(String(product.id)))
      .map((product) => ({
        sourceId: `kolz-${product.id}`,
        title: product.name,
        price: priceCny(product.priceCents),
        image: product.image,
        description: product.description || product.keywords?.join(", ") || ""
      }));
  }
  if (plan.source === "platzi") {
    const list = await fetch("https://api.escuelajs.co/api/v1/products?offset=0&limit=200", {
      signal: AbortSignal.timeout(timeoutMs)
    }).then((r) => r.json());
    return list
      .filter((product) => plan.ids.includes(Number(product.id)))
      .map((product) => ({
        sourceId: `platzi-${product.id}`,
        title: product.title,
        price: priceCny(product.price),
        image: firstImage(product.images),
        description: product.description || ""
      }));
  }
  if (plan.source === "openfoodfacts") {
    const url = `https://world.openfoodfacts.org/cgi/search.pl?search_terms=${encodeURIComponent(plan.query)}&search_simple=1&action=process&json=1&page_size=8`;
    const data = await fetch(url, { signal: AbortSignal.timeout(timeoutMs) }).then((r) => r.json());
    return (data.products || [])
      .map((product) => ({
        sourceId: `off-${product.code || product._id || product.id || product.product_name}`,
        title: [product.brands, product.product_name].filter(Boolean).join(" ").trim() || plan.query,
        price: priceCny(18 + Math.random() * 20),
        image: product.image_front_url || product.image_url,
        description: product.generic_name || product.categories || ""
      }))
      .filter((product) => product.title && product.image)
      .slice(0, 4);
  }
  return (plan.products || []).map((product) => ({
    sourceId: product.id,
    title: product.title,
    price: priceCny(product.price),
    image: product.image,
    description: product.description || ""
  }));
}

function firstImage(images) {
  if (!Array.isArray(images)) {
    return "";
  }
  const image = images.find(Boolean);
  return String(image || "").replace(/^["']|["']$/g, "");
}

function productPayload(product, plan, cateId, image, index, templateId) {
  const name = `${plan.prefix} ${product.title}`.slice(0, 120);
  const price = Number(product.price || 29.9);
  return {
    storeName: name,
    storeInfo: `${name}，来源公开商品/图片数据，图片、标题按同一来源记录导入。`,
    keyword: `${marker},cate-${cateId},source-${product.sourceId}`,
    cateId: String(cateId),
    image,
    videoLink: "",
    sliderImages: [image],
    price,
    vipPrice: price,
    otPrice: Number((price * 1.25).toFixed(2)),
    cost: Number((price * 0.55).toFixed(2)),
    unitName: plan.unit,
    giveIntegral: 0,
    tempId: templateId,
    sort: 31800 - index,
    ficti: 20 + index,
    isShow: true,
    isSub: false,
    isHot: index % 2 === 0,
    isBenefit: false,
    isBest: true,
    isNew: true,
    isGood: false,
    activity: ["默认", "秒杀", "砍价", "拼团"],
    content: `<p>${name}</p><p>${product.description || ""}</p><p><img src="/${image}" /></p>`,
    couponIds: []
  };
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
const existing = await existingMarkers(token);

let created = 0;
let skipped = 0;
let failed = 0;
const results = [];

for (const [cateId, plan] of Object.entries(categoryFillPlan)) {
  const beforeTotal = await currentCategoryTotal(cateId).catch(() => 0);
  if (beforeTotal > 0) {
    results.push({ cateId: Number(cateId), beforeTotal, created: 0, skipped: "already has products" });
    skipped += 1;
    continue;
  }
  let products = [];
  try {
    products = await loadSourceProducts(plan);
  } catch (error) {
    failed += 1;
    results.push({ cateId: Number(cateId), beforeTotal, error: `source failed: ${error.message}` });
    continue;
  }
  let categoryCreated = 0;
  for (const product of products.slice(0, 4)) {
    const keyword = `${marker},cate-${cateId},source-${product.sourceId}`;
    if (existing.has(keyword) || !product.image) {
      skipped += 1;
      continue;
    }
    try {
      const image = await downloadImage(product.image, `${cateId}-${product.sourceId}`);
      const body = productPayload(product, plan, cateId, image, created, templateId);
      const data = assertOk("product create", await requestJson(`${adminBase}/api/admin/store/product/save`, {
        method: "POST",
        headers: authHeaders(token),
        body: JSON.stringify(body)
      }));
      existing.add(keyword);
      created += 1;
      categoryCreated += 1;
      results.push({ cateId: Number(cateId), id: data.id, name: body.storeName, image: body.image });
    } catch (error) {
      failed += 1;
      results.push({ cateId: Number(cateId), product: product.title, error: error.message });
    }
  }
  if (categoryCreated === 0 && beforeTotal === 0) {
    results.push({ cateId: Number(cateId), beforeTotal, created: 0, warning: "no downloadable source products" });
  }
}

console.log(JSON.stringify({
  marker,
  created,
  skipped,
  failed,
  frontTotal: (await requestJson(`${frontBase}/api/front/products?page=1&limit=1`)).body?.data?.total,
  results
}, null, 2));
