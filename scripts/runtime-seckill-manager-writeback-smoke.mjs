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

function managerPayload({ name, time, sort, status }) {
  return {
    name,
    time,
    img: "",
    silderImgs: "[]",
    sort,
    status
  };
}

function parseManagerRange(manager) {
  if (Number.isFinite(Number(manager?.startTime)) && Number.isFinite(Number(manager?.endTime))) {
    return [Number(manager.startTime), Number(manager.endTime)];
  }
  const text = String(manager?.time || "");
  if (!text.includes(",")) {
    return null;
  }
  const [start, end] = text.split(",", 2).map((part) => Number(String(part).trim().split(":")[0]));
  if (!Number.isFinite(start) || !Number.isFinite(end)) {
    return null;
  }
  return [start, end];
}

function findFreeManagerTime(managers, excludedIds = new Set()) {
  const occupied = (managers || [])
    .filter((manager) => !excludedIds.has(Number(manager?.id)))
    .map(parseManagerRange)
    .filter(Boolean);
  for (let hour = 0; hour < 24; hour += 1) {
    const start = hour;
    const end = hour + 1;
    const conflict = occupied.some(([itemStart, itemEnd]) => itemStart < end && itemEnd > start);
    if (!conflict) {
      return `${String(start).padStart(2, "0")}:00,${String(end).padStart(2, "0")}:00`;
    }
  }
  return "";
}

async function findManagerByName(token, name) {
  const data = assertOk("seckill manager list by name", await requestJson(
    `${adminBase}/api/admin/store/seckill/manger/list?page=1&limit=10&name=${encodeURIComponent(name)}`,
    { headers: { "Authori-zation": token } }
  ));
  return (data?.list || []).find((item) => item.name === name) || null;
}

const login = assertOk("admin login", await requestJson(`${adminBase}/api/admin/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account, pwd })
}));
const token = login.token;
assertTruthy("admin token", token, login);

const stamp = Date.now();
const createName = `验收临时时段-${stamp}`;
const updateName = `验收临时时段-已更新-${stamp}`;
const managersBefore = assertOk("seckill manager list before writeback", await requestJson(
  `${adminBase}/api/admin/store/seckill/manger/list?page=1&limit=200`,
  { headers: { "Authori-zation": token } }
))?.list || [];
const createTime = findFreeManagerTime(managersBefore);
assertTruthy("free seckill manager time", createTime, managersBefore);
let managerId = null;
let deleted = false;

try {
  assertOk("seckill manager create", await requestJson(`${adminBase}/api/admin/store/seckill/manger/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(managerPayload({
      name: createName,
      time: createTime,
      sort: 901,
      status: "'0'"
    }))
  }));

  const created = await findManagerByName(token, createName);
  assertTruthy("created seckill manager found", created?.id > 0, created);
  managerId = created.id;
  assertTruthy("created seckill manager time", created.time === createTime, created);
  assertTruthy("created seckill manager closed", created.status === "'0'" || created.status === "0", created);

  const infoBefore = assertOk("seckill manager info before update", await requestJson(
    `${adminBase}/api/admin/store/seckill/manger/info?id=${managerId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("info before update name", infoBefore.name === createName, infoBefore);
  assertTruthy("info before update id", infoBefore.id === managerId, infoBefore);

  const managersForUpdate = assertOk("seckill manager list before update", await requestJson(
    `${adminBase}/api/admin/store/seckill/manger/list?page=1&limit=200`,
    { headers: { "Authori-zation": token } }
  ))?.list || [];
  const updateTime = findFreeManagerTime(managersForUpdate, new Set([Number(managerId)])) || createTime;

  assertOk("seckill manager update", await requestJson(`${adminBase}/api/admin/store/seckill/manger/update?id=${managerId}`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(managerPayload({
      name: updateName,
      time: updateTime,
      sort: 902,
      status: "'0'"
    }))
  }));

  const infoAfter = assertOk("seckill manager info after update", await requestJson(
    `${adminBase}/api/admin/store/seckill/manger/info?id=${managerId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("info after update id", infoAfter.id === managerId, infoAfter);
  assertTruthy("info after update name", infoAfter.name === updateName, infoAfter);
  assertTruthy("info after update time", infoAfter.time === updateTime, infoAfter);
  assertTruthy("info after update sort", infoAfter.sort === 902, infoAfter);
  assertTruthy("info after update status", infoAfter.status === "'0'" || infoAfter.status === "0", infoAfter);

  const updated = await findManagerByName(token, updateName);
  assertTruthy("updated seckill manager listed", updated?.id === managerId, updated);

  assertOk("seckill manager delete", await requestJson(`${adminBase}/api/admin/store/seckill/manger/delete?id=${managerId}`, {
    headers: { "Authori-zation": token }
  }));
  deleted = true;

  const infoAfterDelete = await requestJson(`${adminBase}/api/admin/store/seckill/manger/info?id=${managerId}`, {
    headers: { "Authori-zation": token }
  });
  assertTruthy("info after delete is not ok", infoAfterDelete.body?.code !== 200, infoAfterDelete.body);

  const deletedListRow = await findManagerByName(token, updateName);
  assertTruthy("deleted seckill manager excluded from list", !deletedListRow, deletedListRow);

  console.log(JSON.stringify({
    runtimeSeckillManagerWritebackSmokeOk: true,
    adminBase,
    seckillManager: {
      id: managerId,
      createName,
      updateName,
      deleted
    }
  }, null, 2));
} finally {
  if (managerId && !deleted) {
    try {
      await requestJson(`${adminBase}/api/admin/store/seckill/manger/delete?id=${managerId}`, {
        headers: { "Authori-zation": token }
      });
    } catch {
      // Keep the original assertion error visible. Manual cleanup can search by the unique names above.
    }
  }
}
