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

function textReplyPayload({ id, keywords, marker, status = true }) {
  return {
    id,
    keywords,
    type: "text",
    status,
    data: JSON.stringify({
      content: `验收临时关键字回复-${marker}`,
      mediaId: "",
      srcUrl: "",
      articleData: {}
    })
  };
}

function newsReplyPayload({ id, keywords, marker, status = false }) {
  return {
    id,
    keywords,
    type: "news",
    status,
    data: JSON.stringify({
      content: "",
      mediaId: "",
      srcUrl: "",
      articleData: {
        title: `验收临时图文-${marker}`,
        imageInput: "http://127.0.0.1:19527/crmebimage/public/product/2025/05/30/f6dc9f3df7b947eabcbe1419f3fcc83ecslaa1ghjs.png",
        synopsis: "公众号回复写回验收",
        url: "/pages/index/index"
      }
    })
  };
}

async function findReplyByKeyword(token, keywords) {
  const data = assertOk("wechat reply list by keyword", await requestJson(
    `${adminBase}/api/admin/wechat/keywords/reply/list?page=1&limit=20&keywords=${encodeURIComponent(keywords)}`,
    { headers: { "Authori-zation": token } }
  ));
  return (data?.list || []).find((item) => item.keywords === keywords) || null;
}

function parseReplyData(reply) {
  try {
    return JSON.parse(reply?.data || "{}");
  } catch {
    return {};
  }
}

const login = assertOk("admin login", await requestJson(`${adminBase}/api/admin/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account, pwd })
}));
const token = login.token;
assertTruthy("admin token", token, login);

const stamp = Date.now();
const createKeyword = `验收临时关键字-${stamp}`;
const updateKeyword = `验收临时关键字-已更新-${stamp}`;
let replyId = null;
let deleted = false;

try {
  assertOk("wechat reply create", await requestJson(`${adminBase}/api/admin/wechat/keywords/reply/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(textReplyPayload({ keywords: createKeyword, marker: "create" }))
  }));

  const created = await findReplyByKeyword(token, createKeyword);
  assertTruthy("created wechat reply found", created?.id > 0, created);
  replyId = created.id;
  const createdData = parseReplyData(created);
  assertTruthy("created text type", created.type === "text", created);
  assertTruthy("created text marker", createdData.content === "验收临时关键字回复-create", createdData);
  assertTruthy("created status", created.status === true || Number(created.status) === 1, created);

  assertOk("wechat reply update to news", await requestJson(`${adminBase}/api/admin/wechat/keywords/reply/update`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(newsReplyPayload({ id: replyId, keywords: updateKeyword, marker: "update" }))
  }));

  const infoAfterUpdate = assertOk("wechat reply info after update", await requestJson(
    `${adminBase}/api/admin/wechat/keywords/reply/info?id=${replyId}`,
    { headers: { "Authori-zation": token } }
  ));
  const updatedData = parseReplyData(infoAfterUpdate);
  assertTruthy("updated keyword", infoAfterUpdate.keywords === updateKeyword, infoAfterUpdate);
  assertTruthy("updated news type", infoAfterUpdate.type === "news", infoAfterUpdate);
  assertTruthy("updated disabled", infoAfterUpdate.status === false || Number(infoAfterUpdate.status) === 0, infoAfterUpdate);
  assertTruthy("updated news title", updatedData.articleData?.title === "验收临时图文-update", updatedData);
  assertTruthy(
    "updated image prefix normalized",
    updatedData.articleData?.imageInput === "crmebimage/public/product/2025/05/30/f6dc9f3df7b947eabcbe1419f3fcc83ecslaa1ghjs.png",
    updatedData
  );

  const infoByKeyword = assertOk("wechat reply info by keyword", await requestJson(
    `${adminBase}/api/admin/wechat/keywords/reply/info/keywords?keywords=${encodeURIComponent(updateKeyword)}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("info by keyword id", infoByKeyword.id === replyId, infoByKeyword);

  assertOk("wechat reply status enable", await requestJson(
    `${adminBase}/api/admin/wechat/keywords/reply/status?id=${replyId}&status=true`,
    { method: "POST", headers: { "Authori-zation": token } }
  ));
  const enabled = assertOk("wechat reply info after status", await requestJson(
    `${adminBase}/api/admin/wechat/keywords/reply/info?id=${replyId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("enabled status", enabled.status === true || Number(enabled.status) === 1, enabled);

  assertOk("wechat reply delete", await requestJson(
    `${adminBase}/api/admin/wechat/keywords/reply/delete?id=${replyId}`,
    { headers: { "Authori-zation": token } }
  ));
  deleted = true;

  const infoAfterDelete = await requestJson(
    `${adminBase}/api/admin/wechat/keywords/reply/info?id=${replyId}`,
    { headers: { "Authori-zation": token } }
  );
  assertTruthy("wechat reply info after delete is not ok", infoAfterDelete.body?.code !== 200, infoAfterDelete.body);
  assertTruthy("deleted keyword excluded from list", !(await findReplyByKeyword(token, updateKeyword)), updateKeyword);

  console.log(JSON.stringify({
    runtimeWechatReplyWritebackSmokeOk: true,
    adminBase,
    wechatReply: {
      id: replyId,
      createKeyword,
      updateKeyword,
      deleted
    }
  }, null, 2));
} finally {
  if (replyId && !deleted) {
    await requestJson(`${adminBase}/api/admin/wechat/keywords/reply/delete?id=${replyId}`, {
      headers: { "Authori-zation": token }
    }).catch(() => {});
  }
}
