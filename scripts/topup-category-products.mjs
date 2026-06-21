import fs from "node:fs/promises";
import path from "node:path";

const adminBase = process.env.CRMEB_ADMIN_WEB || "http://127.0.0.1:19527";
const frontBase = process.env.CRMEB_APP_WEB || "http://127.0.0.1:19528";
const account = process.env.CRMEB_ADMIN_ACCOUNT || "admin";
const pwd = process.env.CRMEB_ADMIN_PWD || "123456";
const timeoutMs = Number(process.env.CRMEB_RUNTIME_CHECK_TIMEOUT || 30000);
const targetPerCategory = Number(process.env.JSY_CATEGORY_TARGET || 10);
const useWikimedia = process.env.JSY_USE_WIKI === "1";
const marker = "JSY_CATEGORY_TOPUP_20260620";
const uploadRoot = path.resolve(process.env.CRMEB_UPLOAD_ROOT || defaultModernUploadRoot());
const imageDir = path.join(uploadRoot, "crmebimage/public/product/2026/06/20/category-topup");
const imagePrefix = "crmebimage/public/product/2026/06/20/category-topup";

function defaultModernUploadRoot() {
  return path.basename(process.cwd()) === "modern" ? "legacy-deps/upload" : "modern/legacy-deps/upload";
}

const plans = {
  275: { prefix: "空气炸锅", unit: "台", dummy: [], photo: "air fryer" },
  277: { prefix: "破壁机", unit: "台", dummy: [], photo: "blender" },
  285: { prefix: "婴儿腰凳背带", unit: "件", dummy: [], photo: "baby carrier" },
  286: { prefix: "婴幼儿奶粉", unit: "罐", off: ["infant formula", "baby milk powder"], photo: "infant formula" },
  287: { prefix: "儿童玩具", unit: "件", dummy: ["sports-accessories"], photo: "children toy" },
  289: { prefix: "骑行配件", unit: "件", dummy: [], photo: "bicycle helmet" },
  290: { prefix: "游泳装备", unit: "件", dummy: [], photo: "swimming goggles" },
  291: { prefix: "洁面清洁仪", unit: "件", dummy: ["skin-care", "beauty"], photo: "facial brush" },
  292: { prefix: "美容仪", unit: "件", dummy: ["beauty", "skin-care"], photo: "beauty device" },
  295: { prefix: "修容彩妆", unit: "件", dummy: ["beauty"], photo: "makeup palette" },
  296: { prefix: "美妆工具", unit: "件", dummy: ["beauty"], photo: "makeup brush" },
  297: { prefix: "按摩仪", unit: "件", dummy: [], photo: "massage gun" },
  301: { prefix: "纸品湿巾", unit: "包", dummy: [], photo: "wet wipes" },
  302: { prefix: "办公文具", unit: "件", dummy: [], photo: "stationery notebook" },
  746: { prefix: "儿童鞋", unit: "双", dummy: ["mens-shoes", "womens-shoes"], photo: "kids sneakers" },
  797: { prefix: "高跟鞋", unit: "双", dummy: ["womens-shoes"], photo: "high heel shoes" },
  798: { prefix: "帽子", unit: "顶", dummy: [], photo: "hat" },
  800: { prefix: "家用枕头", unit: "个", dummy: [], photo: "pillow" },
  801: { prefix: "通勤箱包", unit: "个", dummy: ["womens-bags"], photo: "handbag backpack" },
  805: { prefix: "智能音响", unit: "台", dummy: [], photo: "smart speaker" },
  806: { prefix: "婴儿奶瓶", unit: "个", off: ["baby bottle"], photo: "baby bottle" },
  825: { prefix: "厨房炸锅", unit: "台", dummy: [], photo: "air fryer" }
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

async function loadCategoryCounts() {
  const data = assertOk("front categories", await requestJson(`${frontBase}/api/front/category`));
  const rows = [];
  for (const parent of data || []) {
    for (const child of parent.child || []) {
      const result = await requestJson(`${frontBase}/api/front/products?page=1&limit=1&cid=${child.id}`);
      rows.push({
        parent: parent.name,
        id: Number(child.id),
        name: child.name,
        total: Number(result.body?.data?.total || 0)
      });
    }
  }
  return rows;
}

async function existingKeywords(token) {
  const data = assertOk("existing topup products", await requestJson(
    `${adminBase}/api/admin/store/product/list?page=1&limit=500&type=0&keywords=${encodeURIComponent(marker)}`,
    { headers: { "Authori-zation": token } }
  ));
  return new Set((data.list || []).map((item) => String(item.keyword || "")));
}

async function fetchDummyProducts() {
  const data = await fetch("https://dummyjson.com/products?limit=0", {
    headers: { "User-Agent": "CRMEB Modern product topup" },
    signal: AbortSignal.timeout(timeoutMs)
  }).then((response) => response.json());
  return data.products || [];
}

async function fetchOpenFoodFacts(query, limit = 12) {
  const url = `https://world.openfoodfacts.org/cgi/search.pl?search_terms=${encodeURIComponent(query)}&search_simple=1&action=process&json=1&page_size=${limit}`;
  const data = await fetch(url, {
    headers: { "User-Agent": "CRMEB Modern product topup" },
    signal: AbortSignal.timeout(timeoutMs)
  }).then((response) => response.json());
  return (data.products || [])
    .map((product) => ({
      source: "off",
      sourceId: `off-${product.code || product._id || product.product_name}`,
      title: [product.brands, product.product_name].filter(Boolean).join(" ").trim() || query,
      price: priceCny(15 + stableNumber(product.code || product._id || product.product_name, 45)),
      image: product.image_front_url || product.image_url,
      description: product.generic_name || product.categories || ""
    }))
    .filter((item) => item.title && item.image);
}

async function fetchWikimedia(query, limit = 12) {
  const url = new URL("https://commons.wikimedia.org/w/api.php");
  url.searchParams.set("action", "query");
  url.searchParams.set("generator", "search");
  url.searchParams.set("gsrsearch", query);
  url.searchParams.set("gsrnamespace", "6");
  url.searchParams.set("gsrlimit", String(limit));
  url.searchParams.set("prop", "imageinfo");
  url.searchParams.set("iiprop", "url|mime");
  url.searchParams.set("format", "json");
  url.searchParams.set("origin", "*");
  const data = await fetch(url, {
    headers: { "User-Agent": "CRMEB Modern product topup" },
    signal: AbortSignal.timeout(timeoutMs)
  }).then((response) => response.json());
  return Object.values(data.query?.pages || {})
    .map((page) => {
      const info = page.imageinfo?.[0] || {};
      return {
        source: "commons",
        sourceId: `commons-${page.pageid}`,
        title: cleanCommonsTitle(page.title || query),
        price: priceCny(12 + stableNumber(page.pageid, 88)),
        image: info.url,
        description: `Wikimedia Commons image record: ${page.title || query}`
      };
    })
    .filter((item) => item.image && /\.(jpe?g|png|webp)(\?|$)/i.test(item.image));
}

function dummyCandidates(products, categories) {
  return products
    .filter((product) => categories.includes(product.category))
    .map((product) => ({
      source: "dummyjson",
      sourceId: `dummyjson-${product.id}`,
      title: product.title,
      price: priceCny(product.price),
      image: product.thumbnail || product.images?.[0],
      description: product.description || ""
    }))
    .filter((item) => item.image);
}

async function candidatesFor(plan, dummyProducts) {
  const candidates = [];
  if (plan.dummy?.length) {
    candidates.push(...dummyCandidates(dummyProducts, plan.dummy));
  }
  for (const query of plan.off || []) {
    try {
      candidates.push(...await fetchOpenFoodFacts(query));
    } catch (error) {
      console.warn(`[topup] OpenFoodFacts skipped ${query}: ${error.message}`);
    }
  }
  if (useWikimedia) {
    for (const query of plan.wiki || []) {
      try {
        candidates.push(...await fetchWikimedia(query));
      } catch (error) {
        console.warn(`[topup] Wikimedia skipped ${query}: ${error.message}`);
      }
    }
  }
  candidates.push(...photoFallbackCandidates(plan));
  const seen = new Set();
  return candidates.filter((item) => {
    const key = item.sourceId;
    if (!key || seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function photoFallbackCandidates(plan) {
  const query = plan.photo || plan.prefix || "product";
  const tag = encodeURIComponent(String(query).replace(/\s+/g, ","));
  const suffixes = ["轻便款", "升级款", "基础款", "热卖款", "家用款", "便携款", "精选款", "专业款", "旗舰款", "组合装", "耐用款", "高性价比款"];
  return Array.from({ length: 24 }, (_, index) => ({
    source: "loremflickr",
    sourceId: `lorem-${slug(query)}-${index + 1}`,
    title: suffixes[index % suffixes.length],
    price: priceCny(15 + index * 3),
    image: `https://loremflickr.com/640/640/${tag}/all?lock=${stableNumber(`${query}-${index}`, 100000)}`,
    description: `在线图片源按关键词 ${query} 下载，本地化保存。`
  }));
}

async function downloadImage(url, fileKey) {
  const ext = extensionFromUrl(url);
  const fileName = `${slug(fileKey)}.${ext}`;
  const target = path.join(imageDir, fileName);
  try {
    await fs.access(target);
    return `${imagePrefix}/${fileName}`;
  } catch {
  }
  const response = await fetch(encodeURI(url), {
    redirect: "follow",
    headers: { "User-Agent": "CRMEB Modern product topup" },
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

function slug(value) {
  return String(value).replace(/[^a-z0-9_-]+/gi, "-").replace(/^-+|-+$/g, "").toLowerCase().slice(0, 90);
}

function cleanCommonsTitle(value) {
  return String(value)
    .replace(/^File:/i, "")
    .replace(/\.(jpe?g|png|webp|gif)$/i, "")
    .replace(/[_-]+/g, " ")
    .trim();
}

function stableNumber(value, modulo) {
  const text = String(value || "");
  let hash = 0;
  for (let index = 0; index < text.length; index += 1) {
    hash = (hash * 31 + text.charCodeAt(index)) >>> 0;
  }
  return hash % modulo;
}

function priceCny(value, fallback = 29.9) {
  const number = Number(value || fallback);
  if (!Number.isFinite(number) || number <= 0) return fallback;
  if (number > 1000) return Number((number / 100).toFixed(2));
  return Number(Math.max(9.9, number * 7.2).toFixed(2));
}

function productPayload(candidate, plan, cateId, image, index, templateId) {
  const name = `${plan.prefix} ${candidate.title}`.replace(/\s+/g, " ").slice(0, 120);
  const price = Number(candidate.price || 29.9);
  return {
    storeName: name,
    storeInfo: `${name}，来源公开商品/图片数据，图片已下载到本地。`,
    keyword: `${marker},cate-${cateId},source-${candidate.sourceId}`,
    cateId: String(cateId),
    image,
    videoLink: "",
    sliderImages: [image],
    price,
    vipPrice: price,
    otPrice: Number((price * 1.25).toFixed(2)),
    cost: Number((price * 0.55).toFixed(2)),
    unitName: plan.unit || "件",
    giveIntegral: 0,
    tempId: templateId,
    sort: 32760 - index,
    ficti: 40 + index,
    isShow: true,
    isSub: false,
    isHot: index % 3 === 0,
    isBenefit: false,
    isBest: true,
    isNew: index % 2 === 0,
    isGood: index % 4 === 0,
    activity: ["默认", "秒杀", "砍价", "拼团"],
    content: `<p>${name}</p><p>${candidate.description || ""}</p><p><img src="/${image}" /></p>`,
    couponIds: []
  };
}

async function stockUp(token, productId, stock = 80) {
  const info = assertOk("product info", await requestJson(
    `${adminBase}/api/admin/store/product/info/${productId}`,
    { headers: { "Authori-zation": token } }
  ));
  const row = (info.attrValue || [])[0];
  if (!row?.id) return false;
  const current = Number(row.stock || 0);
  const target = Math.max(30, Number(stock || 80));
  if (current >= target) return true;
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
const existing = await existingKeywords(token);
const dummyProducts = await fetchDummyProducts();
const counts = await loadCategoryCounts();

let created = 0;
let skipped = 0;
let failed = 0;
const results = [];

for (const row of counts.filter((item) => item.total < targetPerCategory)) {
  const plan = plans[row.id] || { prefix: row.name, unit: "件", dummy: ["groceries", "home-decoration"], wiki: [row.name] };
  const needed = targetPerCategory - row.total;
  const pool = await candidatesFor(plan, dummyProducts);
  let categoryCreated = 0;
  for (const candidate of pool) {
    if (categoryCreated >= needed) break;
    const keyword = `${marker},cate-${row.id},source-${candidate.sourceId}`;
    if (existing.has(keyword)) {
      skipped += 1;
      continue;
    }
    try {
      const image = await downloadImage(candidate.image, `${row.id}-${candidate.sourceId}`);
      const body = productPayload(candidate, plan, row.id, image, created, templateId);
      const data = assertOk("product create", await requestJson(`${adminBase}/api/admin/store/product/save`, {
        method: "POST",
        headers: authHeaders(token),
        body: JSON.stringify(body)
      }));
      await stockUp(token, data.id, 80 + created);
      existing.add(keyword);
      created += 1;
      categoryCreated += 1;
      results.push({ cateId: row.id, name: row.name, productId: data.id, product: body.storeName, image: body.image });
    } catch (error) {
      failed += 1;
      results.push({ cateId: row.id, name: row.name, source: candidate.sourceId, error: error.message });
    }
  }
  if (categoryCreated < needed) {
    results.push({ cateId: row.id, name: row.name, warning: `needed ${needed}, created ${categoryCreated}` });
  }
}

const after = await loadCategoryCounts();
console.log(JSON.stringify({
  marker,
  targetPerCategory,
  created,
  skipped,
  failed,
  zeroCount: after.filter((item) => item.total === 0).length,
  belowTarget: after.filter((item) => item.total < targetPerCategory).map((item) => ({
    id: item.id,
    name: item.name,
    total: item.total
  })),
  frontTotal: (await requestJson(`${frontBase}/api/front/products?page=1&limit=1`)).body?.data?.total,
  samples: results.filter((item) => item.productId).slice(0, 20),
  warnings: results.filter((item) => item.warning || item.error).slice(0, 20)
}, null, 2));
