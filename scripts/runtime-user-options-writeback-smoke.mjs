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

async function findGroupByName(token, name) {
  const data = assertOk("user group list", await requestJson(
    `${adminBase}/api/admin/user/group/list?page=1&limit=200`,
    { headers: { "Authori-zation": token } }
  ));
  return (data?.list || []).find((item) => item.groupName === name) || null;
}

async function findTagByName(token, name) {
  const data = assertOk("user tag list", await requestJson(
    `${adminBase}/api/admin/user/tag/list?page=1&limit=200`,
    { headers: { "Authori-zation": token } }
  ));
  return (data?.list || []).find((item) => item.name === name) || null;
}

async function findLevelByName(token, name) {
  const data = assertOk("user level list", await requestJson(
    `${adminBase}/api/admin/system/user/level/list`,
    { headers: { "Authori-zation": token } }
  ));
  return (data || []).find((item) => item.name === name) || null;
}

function levelPayload({ name, experience, grade, discount, isShow }) {
  return {
    name,
    experience,
    grade,
    discount,
    icon: "/crmebimage/public/product/2026/06/19/demo/jsy_product_1.jpg",
    isShow
  };
}

const login = assertOk("admin login", await requestJson(`${adminBase}/api/admin/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account, pwd })
}));
const token = login.token;
assertTruthy("admin token", token, login);

const stamp = Date.now();
const createGroupName = `验收临时用户分组-${stamp}`;
const updateGroupName = `验收临时用户分组-已更新-${stamp}`;
const createTagName = `验收临时用户标签-${stamp}`;
const updateTagName = `验收临时用户标签-已更新-${stamp}`;
const createLevelName = `验收临时用户等级-${stamp}`;
const updateLevelName = `验收临时用户等级-已更新-${stamp}`;
let groupId = null;
let tagId = null;
let levelId = null;
let groupDeleted = false;
let tagDeleted = false;
let levelDeleted = false;

try {
  assertOk("user group create", await requestJson(`${adminBase}/api/admin/user/group/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({ groupName: createGroupName })
  }));
  const createdGroup = await findGroupByName(token, createGroupName);
  assertTruthy("created user group found", createdGroup?.id > 0, createdGroup);
  groupId = createdGroup.id;

  assertOk("user group update", await requestJson(`${adminBase}/api/admin/user/group/update?id=${groupId}`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({ groupName: updateGroupName })
  }));
  const groupInfo = assertOk("user group info after update", await requestJson(
    `${adminBase}/api/admin/user/group/info?id=${groupId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("group info name", groupInfo.groupName === updateGroupName, groupInfo);

  assertOk("user tag create", await requestJson(`${adminBase}/api/admin/user/tag/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({ name: createTagName })
  }));
  const createdTag = await findTagByName(token, createTagName);
  assertTruthy("created user tag found", createdTag?.id > 0, createdTag);
  tagId = createdTag.id;

  assertOk("user tag update", await requestJson(`${adminBase}/api/admin/user/tag/update?id=${tagId}`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({ name: updateTagName })
  }));
  const tagInfo = assertOk("user tag info after update", await requestJson(
    `${adminBase}/api/admin/user/tag/info?id=${tagId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("tag info name", tagInfo.name === updateTagName, tagInfo);

  assertOk("user level create", await requestJson(`${adminBase}/api/admin/system/user/level/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(levelPayload({
      name: createLevelName,
      experience: 900000,
      grade: 90,
      discount: 95,
      isShow: false
    }))
  }));
  const createdLevel = await findLevelByName(token, createLevelName);
  assertTruthy("created user level found", createdLevel?.id > 0, createdLevel);
  levelId = createdLevel.id;
  assertTruthy("created level grade", Number(createdLevel.grade) === 90, createdLevel);
  assertTruthy("created level hidden", createdLevel.isShow === false || Number(createdLevel.isShow) === 0, createdLevel);

  assertOk("user level update", await requestJson(`${adminBase}/api/admin/system/user/level/update/${levelId}`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(levelPayload({
      name: updateLevelName,
      experience: 910000,
      grade: 91,
      discount: 90,
      isShow: true
    }))
  }));
  const levelInfo = assertOk("user level info after update", await requestJson(
    `${adminBase}/api/admin/system/user/level/info?id=${levelId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("level info name", levelInfo.name === updateLevelName, levelInfo);
  assertTruthy("level info grade", Number(levelInfo.grade) === 91, levelInfo);
  assertTruthy("level info experience", Number(levelInfo.experience) === 910000, levelInfo);
  assertTruthy("level info discount", Number(levelInfo.discount) === 90, levelInfo);
  assertTruthy("level info show", levelInfo.isShow === true || Number(levelInfo.isShow) === 1, levelInfo);

  assertOk("user group delete", await requestJson(`${adminBase}/api/admin/user/group/delete?id=${groupId}`, {
    headers: { "Authori-zation": token }
  }));
  groupDeleted = true;

  assertOk("user tag delete", await requestJson(`${adminBase}/api/admin/user/tag/delete?id=${tagId}`, {
    headers: { "Authori-zation": token }
  }));
  tagDeleted = true;

  assertOk("user level delete", await requestJson(`${adminBase}/api/admin/system/user/level/delete/${levelId}`, {
    method: "POST",
    headers: { "Authori-zation": token }
  }));
  levelDeleted = true;

  assertTruthy("deleted group excluded", !(await findGroupByName(token, updateGroupName)), updateGroupName);
  assertTruthy("deleted tag excluded", !(await findTagByName(token, updateTagName)), updateTagName);
  assertTruthy("deleted level excluded", !(await findLevelByName(token, updateLevelName)), updateLevelName);

  console.log(JSON.stringify({
    runtimeUserOptionsWritebackSmokeOk: true,
    adminBase,
    userGroup: {
      id: groupId,
      createName: createGroupName,
      updateName: updateGroupName,
      deleted: groupDeleted
    },
    userTag: {
      id: tagId,
      createName: createTagName,
      updateName: updateTagName,
      deleted: tagDeleted
    },
    userLevel: {
      id: levelId,
      createName: createLevelName,
      updateName: updateLevelName,
      deleted: levelDeleted
    }
  }, null, 2));
} finally {
  if (groupId && !groupDeleted) {
    await requestJson(`${adminBase}/api/admin/user/group/delete?id=${groupId}`, {
      headers: { "Authori-zation": token }
    }).catch(() => {});
  }
  if (tagId && !tagDeleted) {
    await requestJson(`${adminBase}/api/admin/user/tag/delete?id=${tagId}`, {
      headers: { "Authori-zation": token }
    }).catch(() => {});
  }
  if (levelId && !levelDeleted) {
    await requestJson(`${adminBase}/api/admin/system/user/level/delete/${levelId}`, {
      method: "POST",
      headers: { "Authori-zation": token }
    }).catch(() => {});
  }
}
