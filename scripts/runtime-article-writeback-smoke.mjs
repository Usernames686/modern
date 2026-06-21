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

function articlePayload({ cid, title, marker, hot = true, banner = false }) {
  return {
    cid: String(cid),
    title,
    author: "交付验收",
    imageInput: "/crmebimage/public/product/2026/06/19/demo/jsy_product_1.jpg",
    synopsis: `交付验收临时文章 ${marker}`,
    shareTitle: title,
    shareSynopsis: `交付验收临时分享简介 ${marker}`,
    isHot: hot,
    isBanner: banner,
    content: `<p>${marker}</p><p>本内容由运行时写回冒烟脚本创建，脚本结束会删除。</p>`
  };
}

async function findCategoryByName(token, name) {
  const data = assertOk("article category list by name", await requestJson(
    `${adminBase}/api/admin/category/list?type=3&status=-1&name=${encodeURIComponent(name)}`,
    { headers: { "Authori-zation": token } }
  ));
  return (data || []).find((item) => item.name === name) || null;
}

async function findArticleByTitle(token, title) {
  const data = assertOk("article list by title", await requestJson(
    `${adminBase}/api/admin/article/list?page=1&limit=10&keywords=${encodeURIComponent(title)}`,
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
const createCategoryName = `验收临时文章分类-${stamp}`;
const updateCategoryName = `验收临时文章分类-已更新-${stamp}`;
const createTitle = `验收临时文章-${stamp}`;
const updateTitle = `验收临时文章-已更新-${stamp}`;
const createMarker = `article-create-${stamp}`;
const updateMarker = `article-update-${stamp}`;
let categoryId = null;
let articleId = null;
let articleDeleted = false;
let categoryDeleted = false;

try {
  assertOk("article category create", await requestJson(`${adminBase}/api/admin/category/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({
      pid: 0,
      name: createCategoryName,
      type: 3,
      url: "",
      extra: "/crmebimage/public/product/2026/06/19/demo/jsy_product_1.jpg",
      status: true,
      sort: 981
    })
  }));

  const createdCategory = await findCategoryByName(token, createCategoryName);
  assertTruthy("created article category found", createdCategory?.id > 0, createdCategory);
  categoryId = createdCategory.id;
  assertTruthy("created category type", Number(createdCategory.type) === 3, createdCategory);
  assertTruthy("created category status", createdCategory.status === true || Number(createdCategory.status) === 1, createdCategory);

  assertOk("article category update", await requestJson(`${adminBase}/api/admin/category/update?id=${categoryId}`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({
      id: categoryId,
      pid: 0,
      name: updateCategoryName,
      type: 3,
      url: "",
      extra: "/crmebimage/public/product/2026/06/19/demo/jsy_product_1.jpg",
      status: true,
      sort: 982
    })
  }));

  const categoryInfo = assertOk("article category info after update", await requestJson(
    `${adminBase}/api/admin/category/info?id=${categoryId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("category info name", categoryInfo.name === updateCategoryName, categoryInfo);
  assertTruthy("category info sort", Number(categoryInfo.sort) === 982, categoryInfo);

  assertOk("article create", await requestJson(`${adminBase}/api/admin/article/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(articlePayload({
      cid: categoryId,
      title: createTitle,
      marker: createMarker
    }))
  }));

  const createdArticle = await findArticleByTitle(token, createTitle);
  assertTruthy("created article found", createdArticle?.id > 0, createdArticle);
  articleId = createdArticle.id;
  assertTruthy("created article category", String(createdArticle.cid) === String(categoryId), createdArticle);
  assertTruthy("created article image", String(createdArticle.imageInput || "").includes("crmebimage/"), createdArticle);

  const infoBefore = assertOk("article info before update", await requestJson(
    `${adminBase}/api/admin/article/info?id=${articleId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("article info before title", infoBefore.title === createTitle, infoBefore);
  assertTruthy("article info before content", String(infoBefore.content || "").includes(createMarker), infoBefore);

  assertOk("article update", await requestJson(`${adminBase}/api/admin/article/update?id=${articleId}`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(articlePayload({
      cid: categoryId,
      title: updateTitle,
      marker: updateMarker,
      hot: false,
      banner: true
    }))
  }));

  const infoAfter = assertOk("article info after update", await requestJson(
    `${adminBase}/api/admin/article/info?id=${articleId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("article info after title", infoAfter.title === updateTitle, infoAfter);
  assertTruthy("article info after category", String(infoAfter.cid) === String(categoryId), infoAfter);
  assertTruthy("article info after content", String(infoAfter.content || "").includes(updateMarker), infoAfter);
  assertTruthy("article info after hot", infoAfter.isHot === false || Number(infoAfter.isHot) === 0, infoAfter);
  assertTruthy("article info after banner", infoAfter.isBanner === true || Number(infoAfter.isBanner) === 1, infoAfter);

  const updatedArticle = await findArticleByTitle(token, updateTitle);
  assertTruthy("updated article listed", updatedArticle?.id === articleId, updatedArticle);

  assertOk("article delete", await requestJson(`${adminBase}/api/admin/article/delete?id=${articleId}`, {
    headers: { "Authori-zation": token }
  }));
  articleDeleted = true;

  const infoAfterDelete = await requestJson(`${adminBase}/api/admin/article/info?id=${articleId}`, {
    headers: { "Authori-zation": token }
  });
  assertTruthy("article info after delete is not ok", infoAfterDelete.body?.code !== 200, infoAfterDelete.body);

  const deletedArticleRow = await findArticleByTitle(token, updateTitle);
  assertTruthy("deleted article excluded from list", !deletedArticleRow, deletedArticleRow);

  assertOk("article category delete", await requestJson(`${adminBase}/api/admin/category/delete?id=${categoryId}`, {
    headers: { "Authori-zation": token }
  }));
  categoryDeleted = true;

  const categoryAfterDelete = await requestJson(`${adminBase}/api/admin/category/info?id=${categoryId}`, {
    headers: { "Authori-zation": token }
  });
  assertTruthy("category info after delete is not ok", categoryAfterDelete.body?.code !== 200, categoryAfterDelete.body);

  const deletedCategoryRow = await findCategoryByName(token, updateCategoryName);
  assertTruthy("deleted article category excluded from list", !deletedCategoryRow, deletedCategoryRow);

  console.log(JSON.stringify({
    runtimeArticleWritebackSmokeOk: true,
    adminBase,
    articleCategory: {
      id: categoryId,
      createName: createCategoryName,
      updateName: updateCategoryName,
      deleted: categoryDeleted
    },
    article: {
      id: articleId,
      createTitle,
      updateTitle,
      deleted: articleDeleted
    }
  }, null, 2));
} finally {
  if (articleId && !articleDeleted) {
    try {
      await requestJson(`${adminBase}/api/admin/article/delete?id=${articleId}`, {
        headers: { "Authori-zation": token }
      });
    } catch {
      // Keep the original assertion error visible. Manual cleanup can search by the unique titles above.
    }
  }
  if (categoryId && !categoryDeleted) {
    try {
      await requestJson(`${adminBase}/api/admin/category/delete?id=${categoryId}`, {
        headers: { "Authori-zation": token }
      });
    } catch {
      // Keep the original assertion error visible. Manual cleanup can search by the unique names above.
    }
  }
}
