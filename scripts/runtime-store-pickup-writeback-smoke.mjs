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

function storePayload({ name, introduction, phone, detailedAddress, latitude, longitude, dayTime }) {
  return {
    name,
    introduction,
    phone,
    address: "陕西省,西安市,雁塔区",
    detailedAddress,
    image: "crmebimage/public/product/2025/05/30/f6dc9f3df7b947eabcbe1419f3fcc83ecslaa1ghjs.png",
    latitude,
    longitude,
    validTime: "",
    dayTime
  };
}

async function findStoreByName(token, name, status = 1) {
  const data = assertOk("store pickup list by name", await requestJson(
    `${adminBase}/api/admin/system/store/list?page=1&limit=20&status=${status}&keywords=${encodeURIComponent(name)}`,
    { headers: { "Authori-zation": token } }
  ));
  return (data?.list || []).find((item) => item.name === name) || null;
}

async function findAvailableAdmin(token) {
  const admins = assertOk("system admin list", await requestJson(
    `${adminBase}/api/admin/system/admin/list?page=1&limit=100&status=true`,
    { headers: { "Authori-zation": token } }
  ));
  const staff = assertOk("store staff list", await requestJson(
    `${adminBase}/api/admin/system/store/staff/list?page=1&limit=100`,
    { headers: { "Authori-zation": token } }
  ));
  const usedAdminIds = new Set((staff?.list || []).map((item) => Number(item.uid)).filter(Boolean));
  return (admins?.list || []).find((item) => item.id > 0 && item.isDel !== true && item.status !== false && !usedAdminIds.has(Number(item.id))) || null;
}

async function findStaffByName(token, name, storeId) {
  const data = assertOk("store staff list by store", await requestJson(
    `${adminBase}/api/admin/system/store/staff/list?page=1&limit=100&storeId=${storeId}`,
    { headers: { "Authori-zation": token } }
  ));
  return (data?.list || []).find((item) => item.staffName === name) || null;
}

const login = assertOk("admin login", await requestJson(`${adminBase}/api/admin/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account, pwd })
}));
const token = login.token;
assertTruthy("admin token", token, login);

const stamp = Date.now();
const createStoreName = `验收临时提货点-${stamp}`;
const updateStoreName = `验收临时提货点-已更新-${stamp}`;
const createStaffName = `验收临时核销员-${stamp}`;
const updateStaffName = `验收临时核销员-已更新-${stamp}`;
let storeId = null;
let staffId = null;
let storeDeleted = false;
let staffDeleted = false;

try {
  assertOk("store pickup create", await requestJson(`${adminBase}/api/admin/system/store/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(storePayload({
      name: createStoreName,
      introduction: "验收临时提货点",
      phone: "15829040001",
      detailedAddress: "验收路 1 号",
      latitude: "34.197719",
      longitude: "108.885791",
      dayTime: "08:00:00,18:00:00"
    }))
  }));
  const createdStore = await findStoreByName(token, createStoreName, 1);
  assertTruthy("created store pickup found", createdStore?.id > 0, createdStore);
  storeId = createdStore.id;

  assertOk("store pickup update", await requestJson(`${adminBase}/api/admin/system/store/update?id=${storeId}`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(storePayload({
      name: updateStoreName,
      introduction: "验收临时提货点已更新",
      phone: "15829040002",
      detailedAddress: "验收路 2 号",
      latitude: "34.198719",
      longitude: "108.886791",
      dayTime: "09:00:00,19:00:00"
    }))
  }));
  const updatedInfo = assertOk("store pickup info after update", await requestJson(
    `${adminBase}/api/admin/system/store/info?id=${storeId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("store pickup info name", updatedInfo.name === updateStoreName, updatedInfo);
  assertTruthy("store pickup info phone", updatedInfo.phone === "15829040002", updatedInfo);
  assertTruthy("store pickup info day time", updatedInfo.dayTime === "09:00:00,19:00:00", updatedInfo);

  assertOk("store pickup hide", await requestJson(
    `${adminBase}/api/admin/system/store/update/status?id=${storeId}&status=false`,
    { headers: { "Authori-zation": token } }
  ));
  const hiddenStore = await findStoreByName(token, updateStoreName, 0);
  assertTruthy("hidden store pickup found", hiddenStore?.id === storeId, hiddenStore);

  const admin = await findAvailableAdmin(token);
  assertTruthy("available admin for temporary staff", admin?.id > 0, admin);
  assertOk("store staff create", await requestJson(`${adminBase}/api/admin/system/store/staff/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({
      uid: admin.id,
      avatar: "",
      storeId,
      staffName: createStaffName,
      phone: "15829040003",
      verifyStatus: 1,
      status: 1
    })
  }));
  const createdStaff = await findStaffByName(token, createStaffName, storeId);
  assertTruthy("created store staff found", createdStaff?.id > 0, createdStaff);
  staffId = createdStaff.id;

  assertOk("store staff update", await requestJson(`${adminBase}/api/admin/system/store/staff/update?id=${staffId}`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({
      id: staffId,
      uid: admin.id,
      avatar: "",
      storeId,
      staffName: updateStaffName,
      phone: "15829040004",
      verifyStatus: 0,
      status: 1
    })
  }));
  const staffInfo = assertOk("store staff info after update", await requestJson(
    `${adminBase}/api/admin/system/store/staff/info?id=${staffId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("staff info name", staffInfo.staffName === updateStaffName, staffInfo);
  assertTruthy("staff info phone", staffInfo.phone === "15829040004", staffInfo);
  assertTruthy("staff info verify status", Number(staffInfo.verifyStatus) === 0, staffInfo);

  assertOk("store staff disable", await requestJson(
    `${adminBase}/api/admin/system/store/staff/update/status?id=${staffId}&status=0`,
    { headers: { "Authori-zation": token } }
  ));
  const disabledStaff = assertOk("store staff info after disable", await requestJson(
    `${adminBase}/api/admin/system/store/staff/info?id=${staffId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("staff disabled status", Number(disabledStaff.status) === 0, disabledStaff);

  assertOk("store staff delete", await requestJson(`${adminBase}/api/admin/system/store/staff/delete?id=${staffId}`, {
    headers: { "Authori-zation": token }
  }));
  staffDeleted = true;
  const staffAfterDelete = await requestJson(`${adminBase}/api/admin/system/store/staff/info?id=${staffId}`, {
    headers: { "Authori-zation": token }
  });
  assertTruthy("staff info after delete is not ok", staffAfterDelete.body?.code !== 200, staffAfterDelete.body);

  assertOk("store pickup delete", await requestJson(`${adminBase}/api/admin/system/store/delete?id=${storeId}`, {
    headers: { "Authori-zation": token }
  }));
  const recycledStore = await findStoreByName(token, updateStoreName, 2);
  assertTruthy("recycled store pickup found", recycledStore?.id === storeId, recycledStore);

  assertOk("store pickup recovery", await requestJson(`${adminBase}/api/admin/system/store/recovery?id=${storeId}`, {
    headers: { "Authori-zation": token }
  }));
  const recoveredStore = await findStoreByName(token, updateStoreName, 0);
  assertTruthy("recovered store pickup found", recoveredStore?.id === storeId, recoveredStore);

  assertOk("store pickup delete again", await requestJson(`${adminBase}/api/admin/system/store/delete?id=${storeId}`, {
    headers: { "Authori-zation": token }
  }));
  assertOk("store pickup complete delete", await requestJson(`${adminBase}/api/admin/system/store/completely/delete?id=${storeId}`, {
    headers: { "Authori-zation": token }
  }));
  storeDeleted = true;

  const infoAfterDelete = await requestJson(`${adminBase}/api/admin/system/store/info?id=${storeId}`, {
    headers: { "Authori-zation": token }
  });
  assertTruthy("store info after complete delete is not ok", infoAfterDelete.body?.code !== 200, infoAfterDelete.body);
  assertTruthy("deleted store excluded from show list", !(await findStoreByName(token, updateStoreName, 1)), updateStoreName);
  assertTruthy("deleted store excluded from recycle list", !(await findStoreByName(token, updateStoreName, 2)), updateStoreName);

  console.log(JSON.stringify({
    runtimeStorePickupWritebackSmokeOk: true,
    adminBase,
    storePickup: {
      id: storeId,
      createName: createStoreName,
      updateName: updateStoreName,
      deleted: storeDeleted
    },
    storeStaff: {
      id: staffId,
      uid: admin.id,
      createName: createStaffName,
      updateName: updateStaffName,
      deleted: staffDeleted
    }
  }, null, 2));
} finally {
  if (staffId && !staffDeleted) {
    await requestJson(`${adminBase}/api/admin/system/store/staff/delete?id=${staffId}`, {
      headers: { "Authori-zation": token }
    }).catch(() => {});
  }
  if (storeId && !storeDeleted) {
    await requestJson(`${adminBase}/api/admin/system/store/delete?id=${storeId}`, {
      headers: { "Authori-zation": token }
    }).catch(() => {});
    await requestJson(`${adminBase}/api/admin/system/store/completely/delete?id=${storeId}`, {
      headers: { "Authori-zation": token }
    }).catch(() => {});
  }
}
