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

function pagePayload({ id, name, marker }) {
  return {
    id,
    name,
    title: "交付验收",
    coverImage: "",
    templateName: "runtime-writeback",
    value: JSON.stringify({
      source: "modern-runtime-writeback-smoke",
      marker,
      components: [
        {
          name: "home_title",
          titleConfig: { val: "交付验收标题" },
          marker
        }
      ]
    }),
    status: 0,
    type: 0,
    isShow: 1,
    isBgColor: 0,
    isBgPic: 0,
    isDiy: 0,
    colorPicker: "",
    bgPic: "",
    bgTabVal: 0,
    returnAddress: "0",
    titleBgColor: "1",
    titleColor: "1",
    serviceStatus: 1,
    textPosition: 0
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
const name = `验收临时页-${stamp}`;
const createMarker = `create-${stamp}`;
const updateMarker = `update-${stamp}`;
const copyMarker = updateMarker;
let createdId = null;
let copiedId = null;
let originalDefaultId = null;
let defaultRestored = false;
let deleted = false;
let copiedDeleted = false;

try {
  originalDefaultId = assertOk("page diy original default", await requestJson(`${adminBase}/api/admin/pagediy/getdefault`, {
    headers: { "Authori-zation": token }
  }));
  assertTruthy("original default page id", Number.isInteger(originalDefaultId) && originalDefaultId > 0, originalDefaultId);

  const created = assertOk("page diy create", await requestJson(`${adminBase}/api/admin/pagediy/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(pagePayload({ name, marker: createMarker }))
  }));
  createdId = created.id;
  assertTruthy("created page id", Number.isInteger(createdId) && createdId > 0, created);
  assertTruthy("created page not default", created.isDefault !== 1, created);
  assertTruthy("created value marker", String(created.value || "").includes(createMarker), created);

  const updated = assertOk("page diy update", await requestJson(`${adminBase}/api/admin/pagediy/update`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(pagePayload({ id: createdId, name, marker: updateMarker }))
  }));
  assertTruthy("updated page id", updated.id === createdId, updated);
  assertTruthy("updated value marker", String(updated.value || "").includes(updateMarker), updated);
  assertTruthy("old marker replaced", !String(updated.value || "").includes(createMarker), updated);

  const info = assertOk("page diy info after update", await requestJson(`${adminBase}/api/admin/pagediy/info/${createdId}`, {
    headers: { "Authori-zation": token }
  }));
  assertTruthy("info id after update", info.id === createdId, info);
  assertTruthy("info name after update", info.name === name, info);
  assertTruthy("info value after update", String(info.value || "").includes(updateMarker), info);

  const copied = assertOk("page diy copy", await requestJson(`${adminBase}/api/admin/pagediy/copy/${createdId}`, {
    method: "POST",
    headers: { "Authori-zation": token }
  }));
  copiedId = copied.id;
  assertTruthy("copied page id", Number.isInteger(copiedId) && copiedId > 0 && copiedId !== createdId, copied);
  assertTruthy("copied value marker", String(copied.value || "").includes(copyMarker), copied);
  assertTruthy("copied page not default", copied.isDefault !== 1, copied);

  assertOk("page diy set copied default", await requestJson(`${adminBase}/api/admin/pagediy/setdefault/${copiedId}`, {
    headers: { "Authori-zation": token }
  }));
  const defaultAfterSet = assertOk("page diy default after set", await requestJson(`${adminBase}/api/admin/pagediy/getdefault`, {
    headers: { "Authori-zation": token }
  }));
  assertTruthy("copied default id after set", defaultAfterSet === copiedId, { defaultAfterSet, copiedId });

  const frontDefault = assertOk("front page diy default after set", await requestJson(`${appBase}/api/front/page/diy/default`));
  assertTruthy("front default marker after set", String(frontDefault.value || "").includes(copyMarker), frontDefault);

  assertOk("page diy restore original default", await requestJson(`${adminBase}/api/admin/pagediy/setdefault/${originalDefaultId}`, {
    headers: { "Authori-zation": token }
  }));
  defaultRestored = true;
  const defaultAfterRestore = assertOk("page diy default after restore", await requestJson(`${adminBase}/api/admin/pagediy/getdefault`, {
    headers: { "Authori-zation": token }
  }));
  assertTruthy("original default restored", defaultAfterRestore === originalDefaultId, { defaultAfterRestore, originalDefaultId });

  const listBeforeDelete = assertOk("page diy list before delete", await requestJson(
    `${adminBase}/api/admin/pagediy/list?page=1&limit=10&name=${encodeURIComponent(name)}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy(
    "list includes created page before delete",
    Array.isArray(listBeforeDelete.list) && listBeforeDelete.list.some((item) => item.id === createdId),
    listBeforeDelete
  );

  assertOk("page diy copied delete", await requestJson(`${adminBase}/api/admin/pagediy/delete?id=${copiedId}`, {
    headers: { "Authori-zation": token }
  }));
  copiedDeleted = true;

  assertOk("page diy delete", await requestJson(`${adminBase}/api/admin/pagediy/delete?id=${createdId}`, {
    headers: { "Authori-zation": token }
  }));
  deleted = true;

  const infoAfterDelete = await requestJson(`${adminBase}/api/admin/pagediy/info/${createdId}`, {
    headers: { "Authori-zation": token }
  });
  assertTruthy(
    "info after delete is not ok",
    infoAfterDelete.body?.code !== 200,
    infoAfterDelete.body
  );

  const listAfterDelete = assertOk("page diy list after delete", await requestJson(
    `${adminBase}/api/admin/pagediy/list?page=1&limit=10&name=${encodeURIComponent(name)}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy(
    "list excludes deleted page",
    !Array.isArray(listAfterDelete.list) || !listAfterDelete.list.some((item) => item.id === createdId),
    listAfterDelete
  );
  assertTruthy(
    "list excludes copied page",
    !Array.isArray(listAfterDelete.list) || !listAfterDelete.list.some((item) => item.id === copiedId),
    listAfterDelete
  );

  const frontDefaultAfterCleanup = assertOk("front page diy default after cleanup", await requestJson(`${appBase}/api/front/page/diy/default`));
  assertTruthy(
    "front default marker cleaned",
    !String(frontDefaultAfterCleanup.value || "").includes(createMarker) && !String(frontDefaultAfterCleanup.value || "").includes(updateMarker),
    frontDefaultAfterCleanup
  );

  console.log(JSON.stringify({
    runtimeWritebackSmokeOk: true,
    adminBase,
    appBase,
    pageDiy: {
      id: createdId,
      copiedId,
      originalDefaultId,
      name,
      createMarker,
      updateMarker,
      defaultRestored,
      copiedDeleted,
      deleted
    }
  }, null, 2));
} finally {
  if (originalDefaultId && !defaultRestored) {
    try {
      await requestJson(`${adminBase}/api/admin/pagediy/setdefault/${originalDefaultId}`, {
        headers: { "Authori-zation": token }
      });
    } catch {
      // Keep the original assertion error visible; the unique marker above helps manual cleanup if needed.
    }
  }
  if (copiedId && !copiedDeleted) {
    try {
      await requestJson(`${adminBase}/api/admin/pagediy/delete?id=${copiedId}`, {
        headers: { "Authori-zation": token }
      });
    } catch {
      // Keep the original assertion error visible. Manual cleanup can search by the unique name above.
    }
  }
  if (createdId && !deleted) {
    try {
      await requestJson(`${adminBase}/api/admin/pagediy/delete?id=${createdId}`, {
        headers: { "Authori-zation": token }
      });
    } catch {
      // The smoke must not hide the original assertion error. Manual cleanup can search by the unique name above.
    }
  }
}
