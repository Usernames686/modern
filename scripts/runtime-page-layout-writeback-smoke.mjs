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

function clone(value) {
  return JSON.parse(JSON.stringify(value));
}

function markerItem(marker, index, extra = {}) {
  return {
    tempid: null,
    name: `验收配置-${marker}-${index}`,
    info: `页面设计写回-${marker}`,
    pic: `http://127.0.0.1:19527/crmebimage/public/product/2025/05/30/f6dc9f3df7b947eabcbe1419f3fcc83ecslaa1ghjs.png`,
    url: `/pages/activity/promotionList/index?title=${encodeURIComponent(marker)}`,
    status: true,
    ...extra
  };
}

function bottomItem(marker, index) {
  return {
    tempid: null,
    name: `验收底部-${index}`,
    link: index === 0 ? "/pages/index/index" : "/pages/user/index",
    checked: "http://127.0.0.1:19527/crmebimage/public/product/2025/05/30/f6dc9f3df7b947eabcbe1419f3fcc83ecslaa1ghjs.png",
    unchecked: "http://127.0.0.1:19527/crmebimage/public/product/2025/05/30/f6dc9f3df7b947eabcbe1419f3fcc83ecslaa1ghjs.png",
    status: true
  };
}

function hasMarker(list, marker) {
  return Array.isArray(list) && list.some((item) => JSON.stringify(item).includes(marker));
}

async function saveAndAssert(token, name, path, payload, readBack) {
  assertOk(`${name} save`, await requestJson(`${adminBase}${path}`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(payload)
  }));
  const latest = await readBack();
  assertTruthy(`${name} readback marker`, hasMarker(latest.list, latest.marker), latest);
  assertTruthy(`${name} asset prefix normalized`, latest.assetOk, latest);
  return latest;
}

async function saveUniqueConfig(token, key, value) {
  assertOk(`restore config ${key}`, await requestJson(
    `${adminBase}/api/admin/system/config/saveuniq?${new URLSearchParams({
      key,
      value: value == null ? "" : String(value)
    })}`,
    {
      method: "POST",
      headers: { "Authori-zation": token }
    }
  ));
}

const login = assertOk("admin login", await requestJson(`${adminBase}/api/admin/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account, pwd })
}));
const token = login.token;
assertTruthy("admin token", token, login);

const stamp = Date.now();
const marker = `runtime-page-layout-${stamp}`;
const originalIndex = assertOk("page layout original index", await requestJson(
  `${adminBase}/api/admin/page/layout/index`,
  { headers: { "Authori-zation": token } }
));
const originalBottom = assertOk("page layout original bottom navigation", await requestJson(
  `${adminBase}/api/admin/page/layout/bottom/navigation/get`,
  { headers: { "Authori-zation": token } }
));
const originalCategory = assertOk("page layout original category config", await requestJson(
  `${adminBase}/api/admin/page/layout/category/config`,
  { headers: { "Authori-zation": token } }
));

const restoreTasks = [];
let restored = false;

async function restoreOriginal() {
  for (const task of restoreTasks.reverse()) {
    await requestJson(`${adminBase}${task.path}`, {
      method: "POST",
      headers: authHeaders(token),
      body: JSON.stringify(task.payload)
    });
  }
  await saveUniqueConfig(token, "category_page_config", originalCategory.categoryPageConfig);
  await saveUniqueConfig(token, "is_show_category", originalCategory.isShowCategory);
  restored = true;
}

try {
  const checks = [];
  const indexKeys = [
    ["indexBanner", "/api/admin/page/layout/index/banner/save", [markerItem(`${marker}-banner`, 1)]],
    ["indexTable", "/api/admin/page/layout/index/table/save", [markerItem(`${marker}-table`, 1, { pic: "", info: "爆款推荐验收" })]],
    ["indexMenu", "/api/admin/page/layout/index/menu/save", [markerItem(`${marker}-menu`, 1)]],
    ["indexNews", "/api/admin/page/layout/index/news/save", [markerItem(`${marker}-news`, 1, { pic: "", info: "快讯验收" })]],
    ["userMenu", "/api/admin/page/layout/user/menu/save", [markerItem(`${marker}-user-menu`, 1)]],
    ["userBanner", "/api/admin/page/layout/user/banner/save", [markerItem(`${marker}-user-banner`, 1)]]
  ];

  for (const [key, path, list] of indexKeys) {
    restoreTasks.push({ path, payload: { [key]: clone(originalIndex[key] || []) } });
    const keyMarker = `${marker}-${key}`;
    const payloadList = clone(list).map((item) => ({
      ...item,
      name: `验收配置-${keyMarker}`,
      info: item.info || keyMarker
    }));
    await saveAndAssert(
      token,
      `page layout ${key}`,
      path,
      { [key]: payloadList },
      async () => {
        const data = assertOk(`page layout ${key} readback`, await requestJson(
          `${adminBase}/api/admin/page/layout/index`,
          { headers: { "Authori-zation": token } }
        ));
        return {
          list: data[key],
          marker: keyMarker,
          assetOk: key === "indexNews" || key === "indexTable" || String(data[key]?.[0]?.pic || "").startsWith("/crmebimage/")
        };
      }
    );
    checks.push(key);
  }

  restoreTasks.push({
    path: "/api/admin/page/layout/bottom/navigation/save",
    payload: {
      bottomNavigationList: clone(originalBottom.bottomNavigationList || []),
      isCustom: originalBottom.isCustom
    }
  });
  await saveAndAssert(
    token,
    "page layout bottom navigation",
    "/api/admin/page/layout/bottom/navigation/save",
    {
      bottomNavigationList: [bottomItem(`${marker}-bottom`, 1), bottomItem(`${marker}-bottom`, 2)],
      isCustom: "1"
    },
    async () => {
      const data = assertOk("page layout bottom navigation readback", await requestJson(
        `${adminBase}/api/admin/page/layout/bottom/navigation/get`,
        { headers: { "Authori-zation": token } }
      ));
      return {
        list: data.bottomNavigationList,
        marker: "验收底部",
        assetOk: String(data.bottomNavigationList?.[0]?.checked || "").startsWith("/crmebimage/")
          && String(data.bottomNavigationList?.[0]?.unchecked || "").startsWith("/crmebimage/")
          && String(data.isCustom) === "1"
      };
    }
  );
  checks.push("bottomNavigation");

  assertOk("page layout category config save", await requestJson(
    `${adminBase}/api/admin/page/layout/category/config/save`,
    {
      method: "POST",
      headers: authHeaders(token),
      body: JSON.stringify({
        categoryPageConfig: "3",
        isShowCategory: "0"
      })
    }
  ));
  const categoryAfterSave = assertOk("page layout category config readback", await requestJson(
    `${adminBase}/api/admin/page/layout/category/config`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("category config style", String(categoryAfterSave.categoryPageConfig) === "3", categoryAfterSave);
  assertTruthy("category config show flag", String(categoryAfterSave.isShowCategory) === "0", categoryAfterSave);
  checks.push("categoryConfig");

  await restoreOriginal();

  const restoredIndex = assertOk("page layout restored index", await requestJson(
    `${adminBase}/api/admin/page/layout/index`,
    { headers: { "Authori-zation": token } }
  ));
  const restoredBottom = assertOk("page layout restored bottom", await requestJson(
    `${adminBase}/api/admin/page/layout/bottom/navigation/get`,
    { headers: { "Authori-zation": token } }
  ));
  const restoredCategory = assertOk("page layout restored category", await requestJson(
    `${adminBase}/api/admin/page/layout/category/config`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("marker removed from index layout", !JSON.stringify(restoredIndex).includes(marker), restoredIndex);
  assertTruthy("marker removed from bottom layout", !JSON.stringify(restoredBottom).includes("验收底部"), restoredBottom);
  assertTruthy(
    "category config restored",
    String(restoredCategory.categoryPageConfig) === String(originalCategory.categoryPageConfig)
      && String(restoredCategory.isShowCategory) === String(originalCategory.isShowCategory),
    { originalCategory, restoredCategory }
  );

  console.log(JSON.stringify({
    runtimePageLayoutWritebackSmokeOk: true,
    adminBase,
    marker,
    checked: checks,
    restored
  }, null, 2));
} finally {
  if (!restored) {
    await restoreOriginal().catch(() => {});
  }
}
