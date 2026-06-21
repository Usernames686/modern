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

function formContent(marker, required = true) {
  return JSON.stringify({
    formRef: "elForm",
    formModel: "formData",
    size: "medium",
    labelPosition: "right",
    labelWidth: 120,
    formRules: "rules",
    gutter: 15,
    disabled: false,
    span: 24,
    formBtns: true,
    fields: [
      {
        __config__: {
          label: `验收字段-${marker}`,
          labelWidth: null,
          showLabel: true,
          changeTag: true,
          tag: "el-input",
          tagIcon: "input",
          required,
          layout: "colFormItem",
          span: 24,
          regList: []
        },
        __slot__: {
          prepend: "",
          append: ""
        },
        __vModel__: `acceptance_field_${marker}`,
        placeholder: "请输入验收字段",
        style: {
          width: "50%"
        },
        clearable: true,
        readonly: false,
        disabled: false
      }
    ]
  });
}

async function findFormByName(token, name) {
  const data = assertOk("form temp list by name", await requestJson(
    `${adminBase}/api/admin/system/form/temp/list?page=1&limit=20&keywords=${encodeURIComponent(name)}`,
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
const createName = `验收临时表单配置-${stamp}`;
const updateName = `验收临时表单配置-已更新-${stamp}`;
let formId = null;
let deleted = false;

try {
  assertOk("form temp create", await requestJson(`${adminBase}/api/admin/system/form/temp/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({
      name: createName,
      info: "验收临时表单配置",
      content: formContent("create")
    })
  }));
  const created = await findFormByName(token, createName);
  assertTruthy("created form temp found", created?.id > 0, created);
  formId = created.id;

  const beforeInfo = assertOk("form temp info before update", await requestJson(
    `${adminBase}/api/admin/system/form/temp/info?id=${formId}`,
    { headers: { "Authori-zation": token } }
  ));
  const beforeContent = JSON.parse(beforeInfo.content);
  assertTruthy("form temp create field count", Array.isArray(beforeContent.fields) && beforeContent.fields.length === 1, beforeInfo);
  assertTruthy("form temp create marker", beforeContent.fields[0]?.__vModel__ === "acceptance_field_create", beforeContent);

  assertOk("form temp update", await requestJson(`${adminBase}/api/admin/system/form/temp/update?id=${formId}`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({
      name: updateName,
      info: "验收临时表单配置已更新",
      content: formContent("update", false)
    })
  }));
  const afterInfo = assertOk("form temp info after update", await requestJson(
    `${adminBase}/api/admin/system/form/temp/info?id=${formId}`,
    { headers: { "Authori-zation": token } }
  ));
  const afterContent = JSON.parse(afterInfo.content);
  assertTruthy("form temp update name", afterInfo.name === updateName, afterInfo);
  assertTruthy("form temp update info", afterInfo.info === "验收临时表单配置已更新", afterInfo);
  assertTruthy("form temp update marker", afterContent.fields[0]?.__vModel__ === "acceptance_field_update", afterContent);
  assertTruthy("form temp update required", afterContent.fields[0]?.__config__?.required === false, afterContent);

  assertOk("form temp delete", await requestJson(`${adminBase}/api/admin/system/form/temp/delete?id=${formId}`, {
    headers: { "Authori-zation": token }
  }));
  deleted = true;

  const infoAfterDelete = await requestJson(`${adminBase}/api/admin/system/form/temp/info?id=${formId}`, {
    headers: { "Authori-zation": token }
  });
  assertTruthy("form temp info after delete is not ok", infoAfterDelete.body?.code !== 200, infoAfterDelete.body);
  assertTruthy("deleted form temp excluded from list", !(await findFormByName(token, updateName)), updateName);

  console.log(JSON.stringify({
    runtimeFormTempWritebackSmokeOk: true,
    adminBase,
    formTemp: {
      id: formId,
      createName,
      updateName,
      deleted
    }
  }, null, 2));
} finally {
  if (formId && !deleted) {
    await requestJson(`${adminBase}/api/admin/system/form/temp/delete?id=${formId}`, {
      headers: { "Authori-zation": token }
    }).catch(() => {});
  }
}
