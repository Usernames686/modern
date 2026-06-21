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

function templatePayload({ name, sort, firstPrice, renewalPrice, freePrice }) {
  return {
    name,
    type: 1,
    appoint: 2,
    sort,
    shippingTemplatesRegionRequestList: [
      {
        cityId: "0",
        title: "[全国]",
        first: 1,
        firstPrice,
        renewal: 1,
        renewalPrice,
        uniqid: `region-${sort}`
      }
    ],
    shippingTemplatesFreeRequestList: [
      {
        cityId: "0",
        title: "[全国]",
        number: 1,
        price: freePrice,
        uniqid: `free-${sort}`
      }
    ]
  };
}

async function findTemplateByName(token, name) {
  const data = assertOk("shipping template list by name", await requestJson(
    `${adminBase}/api/admin/express/shipping/templates/list?page=1&limit=10&keywords=${encodeURIComponent(name)}`,
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
const createName = `验收临时运费模板-${stamp}`;
const updateName = `验收临时运费模板-已更新-${stamp}`;
let templateId = null;
let deleted = false;

try {
  assertOk("shipping template create", await requestJson(`${adminBase}/api/admin/express/shipping/templates/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(templatePayload({
      name: createName,
      sort: 971,
      firstPrice: 6.5,
      renewalPrice: 2.5,
      freePrice: 99
    }))
  }));

  const created = await findTemplateByName(token, createName);
  assertTruthy("created shipping template found", created?.id > 0, created);
  templateId = created.id;
  assertTruthy("created template type", Number(created.type) === 1, created);
  assertTruthy("created template appoint", Number(created.appoint) === 2, created);

  const infoBefore = assertOk("shipping template info before update", await requestJson(
    `${adminBase}/api/admin/express/shipping/templates/info?id=${templateId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("info before update name", infoBefore.name === createName, infoBefore);
  assertTruthy("info before update region", Array.isArray(infoBefore.regionList) && infoBefore.regionList.length === 1, infoBefore);
  assertTruthy("info before update free", Array.isArray(infoBefore.freeList) && infoBefore.freeList.length === 1, infoBefore);
  assertTruthy("info before update first price", Number(infoBefore.regionList[0]?.firstPrice) === 6.5, infoBefore);
  assertTruthy("info before update free price", Number(infoBefore.freeList[0]?.price) === 99, infoBefore);

  assertOk("shipping template update", await requestJson(`${adminBase}/api/admin/express/shipping/templates/update?id=${templateId}`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(templatePayload({
      name: updateName,
      sort: 972,
      firstPrice: 7.5,
      renewalPrice: 3.5,
      freePrice: 129
    }))
  }));

  const infoAfter = assertOk("shipping template info after update", await requestJson(
    `${adminBase}/api/admin/express/shipping/templates/info?id=${templateId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("info after update name", infoAfter.name === updateName, infoAfter);
  assertTruthy("info after update sort", Number(infoAfter.sort) === 972, infoAfter);
  assertTruthy("info after update region", Array.isArray(infoAfter.regionList) && infoAfter.regionList.length === 1, infoAfter);
  assertTruthy("info after update free", Array.isArray(infoAfter.freeList) && infoAfter.freeList.length === 1, infoAfter);
  assertTruthy("info after update first price", Number(infoAfter.regionList[0]?.firstPrice) === 7.5, infoAfter);
  assertTruthy("info after update renewal price", Number(infoAfter.regionList[0]?.renewalPrice) === 3.5, infoAfter);
  assertTruthy("info after update free price", Number(infoAfter.freeList[0]?.price) === 129, infoAfter);

  const regionList = assertOk("shipping region list", await requestJson(
    `${adminBase}/api/admin/express/shipping/region/list?tempId=${templateId}`,
    { headers: { "Authori-zation": token } }
  ));
  const freeList = assertOk("shipping free list", await requestJson(
    `${adminBase}/api/admin/express/shipping/free/list?tempId=${templateId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("region endpoint readback", Array.isArray(regionList) && Number(regionList[0]?.firstPrice) === 7.5, regionList);
  assertTruthy("free endpoint readback", Array.isArray(freeList) && Number(freeList[0]?.price) === 129, freeList);

  assertOk("shipping template delete", await requestJson(`${adminBase}/api/admin/express/shipping/templates/delete?id=${templateId}`, {
    headers: { "Authori-zation": token }
  }));
  deleted = true;

  const infoAfterDelete = await requestJson(`${adminBase}/api/admin/express/shipping/templates/info?id=${templateId}`, {
    headers: { "Authori-zation": token }
  });
  assertTruthy("info after delete is not ok", infoAfterDelete.body?.code !== 200, infoAfterDelete.body);

  const deletedListRow = await findTemplateByName(token, updateName);
  assertTruthy("deleted shipping template excluded from list", !deletedListRow, deletedListRow);

  console.log(JSON.stringify({
    runtimeShippingTemplateWritebackSmokeOk: true,
    adminBase,
    shippingTemplate: {
      id: templateId,
      createName,
      updateName,
      deleted
    }
  }, null, 2));
} finally {
  if (templateId && !deleted) {
    try {
      await requestJson(`${adminBase}/api/admin/express/shipping/templates/delete?id=${templateId}`, {
        headers: { "Authori-zation": token }
      });
    } catch {
      // Keep the original assertion error visible. Manual cleanup can search by the unique names above.
    }
  }
}
