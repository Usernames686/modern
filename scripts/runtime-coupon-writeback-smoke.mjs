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

function couponPayload({ id, name, money, minPrice, day, total, sort }) {
  return {
    id,
    name,
    money,
    isLimited: true,
    total,
    useType: 1,
    primaryKey: "",
    minPrice,
    isForever: false,
    receiveStartTime: "",
    receiveEndTime: "",
    isFixedTime: false,
    useStartTime: "",
    useEndTime: "",
    day,
    type: 3,
    sort,
    status: false
  };
}

async function findCouponByName(token, name) {
  const data = assertOk("coupon list by name", await requestJson(
    `${adminBase}/api/admin/marketing/coupon/list?page=1&limit=10&name=${encodeURIComponent(name)}`,
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
const createName = `验收临时券-${stamp}`;
const updateName = `验收临时券-已更新-${stamp}`;
let couponId = null;
let deleted = false;

try {
  assertOk("coupon create", await requestJson(`${adminBase}/api/admin/marketing/coupon/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(couponPayload({
      name: createName,
      money: 2,
      minPrice: 0,
      day: 7,
      total: 5,
      sort: 11
    }))
  }));

  const created = await findCouponByName(token, createName);
  assertTruthy("created coupon found", created?.id > 0, created);
  couponId = created.id;
  assertTruthy("created coupon hidden from front receive", created.type === 3 && created.status === false, created);
  assertTruthy("created coupon total", created.total === 5 && created.lastTotal === 5, created);

  const infoBefore = assertOk("coupon info before update", await requestJson(
    `${adminBase}/api/admin/marketing/coupon/info?id=${couponId}`,
    { method: "POST", headers: { "Authori-zation": token } }
  ));
  assertTruthy("info before update name", infoBefore?.coupon?.name === createName, infoBefore);
  assertTruthy("info before update id", infoBefore?.coupon?.id === couponId, infoBefore);

  assertOk("coupon update", await requestJson(`${adminBase}/api/admin/marketing/coupon/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(couponPayload({
      id: couponId,
      name: updateName,
      money: 3.5,
      minPrice: 10,
      day: 15,
      total: 8,
      sort: 22
    }))
  }));

  const infoAfter = assertOk("coupon info after update", await requestJson(
    `${adminBase}/api/admin/marketing/coupon/info?id=${couponId}`,
    { method: "POST", headers: { "Authori-zation": token } }
  ));
  const coupon = infoAfter?.coupon || {};
  assertTruthy("info after update id", coupon.id === couponId, infoAfter);
  assertTruthy("info after update name", coupon.name === updateName, infoAfter);
  assertTruthy("info after update money", Number(coupon.money) === 3.5, infoAfter);
  assertTruthy("info after update minPrice", Number(coupon.minPrice) === 10, infoAfter);
  assertTruthy("info after update day", coupon.day === 15, infoAfter);
  assertTruthy("info after update total", coupon.total === 8, infoAfter);
  assertTruthy("info after update status", coupon.status === false, infoAfter);

  const updatedListRow = await findCouponByName(token, updateName);
  assertTruthy("updated coupon listed", updatedListRow?.id === couponId, updatedListRow);
  assertTruthy("updated coupon list total", updatedListRow.total === 8 && updatedListRow.lastTotal === 8, updatedListRow);

  assertOk("coupon delete", await requestJson(`${adminBase}/api/admin/marketing/coupon/delete?id=${couponId}`, {
    method: "POST",
    headers: { "Authori-zation": token }
  }));
  deleted = true;

  const infoAfterDelete = await requestJson(`${adminBase}/api/admin/marketing/coupon/info?id=${couponId}`, {
    method: "POST",
    headers: { "Authori-zation": token }
  });
  assertTruthy("info after delete is not ok", infoAfterDelete.body?.code !== 200, infoAfterDelete.body);

  const deletedListRow = await findCouponByName(token, updateName);
  assertTruthy("deleted coupon excluded from list", !deletedListRow, deletedListRow);

  console.log(JSON.stringify({
    runtimeCouponWritebackSmokeOk: true,
    adminBase,
    coupon: {
      id: couponId,
      createName,
      updateName,
      deleted
    }
  }, null, 2));
} finally {
  if (couponId && !deleted) {
    try {
      await requestJson(`${adminBase}/api/admin/marketing/coupon/delete?id=${couponId}`, {
        method: "POST",
        headers: { "Authori-zation": token }
      });
    } catch {
      // Keep the original assertion error visible. Manual cleanup can search by the unique names above.
    }
  }
}
