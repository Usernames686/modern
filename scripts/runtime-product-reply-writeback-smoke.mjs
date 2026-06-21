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

async function findReplyByComment(token, marker) {
  const data = assertOk("product reply list by nickname", await requestJson(
    `${adminBase}/api/admin/store/product/reply/list?page=1&limit=20&nickname=${encodeURIComponent(marker)}`,
    { headers: { "Authori-zation": token } }
  ));
  return (data?.list || []).find((item) => item.comment === marker || item.nickname === marker) || null;
}

function frontListContains(list, marker) {
  return Array.isArray(list) && list.some((item) => item.comment === marker || item.nickname === marker);
}

const login = assertOk("admin login", await requestJson(`${adminBase}/api/admin/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account, pwd })
}));
const token = login.token;
assertTruthy("admin token", token, login);

const productId = Number(process.env.CRMEB_REPLY_PRODUCT_ID || 96);
const stamp = Date.now();
const marker = `验收临时商品评价-${stamp}`;
let replyId = null;
let deleted = false;

try {
  assertOk("product reply create", await requestJson(`${adminBase}/api/admin/store/product/reply/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({
      productId,
      unique: `smoke-${stamp}`,
      productScore: 5,
      serviceScore: 4,
      comment: marker,
      pics: JSON.stringify([
        "http://127.0.0.1:19527/crmebimage/public/product/2025/05/30/f6dc9f3df7b947eabcbe1419f3fcc83ecslaa1ghjs.png"
      ]),
      avatar: "http://127.0.0.1:19527/crmebimage/public/product/2025/05/30/f6dc9f3df7b947eabcbe1419f3fcc83ecslaa1ghjs.png",
      nickname: marker,
      sku: "验收规格"
    })
  }));

  const created = await findReplyByComment(token, marker);
  assertTruthy("created product reply found", created?.id > 0, created);
  replyId = created.id;
  assertTruthy("created reply product", Number(created.productId) === productId, created);
  assertTruthy("created reply score", Number(created.productScore) === 5 && Number(created.serviceScore) === 4, created);
  assertTruthy("created reply image normalized", String(created.avatar || "").startsWith("crmebimage/"), created);

  assertOk("product reply merchant comment", await requestJson(`${adminBase}/api/admin/store/product/reply/comment`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({
      ids: replyId,
      merchantReplyContent: `商家回复-${marker}`
    })
  }));
  const afterComment = assertOk("product reply info after merchant comment", await requestJson(
    `${adminBase}/api/admin/store/product/reply/info/${replyId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("merchant reply content", afterComment.merchantReplyContent === `商家回复-${marker}`, afterComment);
  assertTruthy("merchant reply flag", Number(afterComment.isReply) === 1, afterComment);

  const frontList = assertOk("front reply list contains created", await requestJson(
    `${appBase}/api/front/reply/list/${productId}?page=1&limit=20&type=1`
  ));
  assertTruthy("front reply list marker", frontListContains(frontList.list, marker), frontList);
  const frontConfig = assertOk("front reply config after create", await requestJson(
    `${appBase}/api/front/reply/config/${productId}`
  ));
  assertTruthy("front reply config count", Number(frontConfig.sumCount) > 0 && Number(frontConfig.goodCount) > 0, frontConfig);

  assertOk("product reply delete", await requestJson(
    `${adminBase}/api/admin/store/product/reply/delete/${replyId}`,
    { headers: { "Authori-zation": token } }
  ));
  deleted = true;

  const infoAfterDelete = assertOk("product reply info after delete", await requestJson(
    `${adminBase}/api/admin/store/product/reply/info/${replyId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("product reply soft deleted", Number(infoAfterDelete.isDel) === 1, infoAfterDelete);

  const frontListAfterDelete = assertOk("front reply list excludes deleted", await requestJson(
    `${appBase}/api/front/reply/list/${productId}?page=1&limit=50&type=1`
  ));
  assertTruthy("front reply marker removed", !frontListContains(frontListAfterDelete.list, marker), frontListAfterDelete);

  console.log(JSON.stringify({
    runtimeProductReplyWritebackSmokeOk: true,
    adminBase,
    appBase,
    productReply: {
      id: replyId,
      productId,
      marker,
      deleted
    }
  }, null, 2));
} finally {
  if (replyId && !deleted) {
    await requestJson(`${adminBase}/api/admin/store/product/reply/delete/${replyId}`, {
      headers: { "Authori-zation": token }
    }).catch(() => {});
  }
}
