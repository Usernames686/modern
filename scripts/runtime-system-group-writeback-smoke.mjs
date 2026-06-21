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

function formTempContent(marker) {
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
          label: `标题-${marker}`,
          tag: "el-input",
          tagIcon: "input",
          required: true,
          layout: "colFormItem",
          span: 24,
          regList: []
        },
        __slot__: {
          prepend: "",
          append: ""
        },
        __vModel__: "title",
        placeholder: "请输入标题",
        style: {
          width: "50%"
        },
        clearable: true
      },
      {
        __config__: {
          label: `图片-${marker}`,
          tag: "self-upload",
          tagIcon: "upload",
          required: false,
          layout: "colFormItem",
          span: 24,
          regList: []
        },
        __slot__: {
          "list-type": true
        },
        __vModel__: "pic",
        placeholder: "请选择图片"
      }
    ]
  });
}

function groupDataForm(formId, marker, sort, status) {
  return {
    id: formId,
    sort,
    status,
    fields: [
      {
        name: "title",
        title: "标题",
        value: `验收临时组合数据-${marker}`
      },
      {
        name: "pic",
        title: "图片",
        value: "http://127.0.0.1:19527/crmebimage/public/product/2025/05/30/f6dc9f3df7b947eabcbe1419f3fcc83ecslaa1ghjs.png"
      }
    ]
  };
}

async function findFormTempByName(token, name) {
  const data = assertOk("form temp list by name", await requestJson(
    `${adminBase}/api/admin/system/form/temp/list?page=1&limit=20&keywords=${encodeURIComponent(name)}`,
    { headers: { "Authori-zation": token } }
  ));
  return (data?.list || []).find((item) => item.name === name) || null;
}

async function findGroupByName(token, name) {
  const data = assertOk("system group list by name", await requestJson(
    `${adminBase}/api/admin/system/group/list?page=1&limit=20&keywords=${encodeURIComponent(name)}`,
    { headers: { "Authori-zation": token } }
  ));
  return (data?.list || []).find((item) => item.name === name) || null;
}

async function findGroupDataByKeyword(token, gid, keyword) {
  const data = assertOk("system group data list by keyword", await requestJson(
    `${adminBase}/api/admin/system/group/data/list?gid=${gid}&page=1&limit=20&keywords=${encodeURIComponent(keyword)}`,
    { headers: { "Authori-zation": token } }
  ));
  return (data?.list || []).find((item) => {
    try {
      return JSON.stringify(JSON.parse(item.value || "{}")).includes(keyword);
    } catch {
      return String(item.value || "").includes(keyword);
    }
  }) || null;
}

const login = assertOk("admin login", await requestJson(`${adminBase}/api/admin/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account, pwd })
}));
const token = login.token;
assertTruthy("admin token", token, login);

const stamp = Date.now();
const createFormName = `验收临时组合表单-${stamp}`;
const createGroupName = `验收临时组合数据组-${stamp}`;
const updateGroupName = `验收临时组合数据组-已更新-${stamp}`;
let formId = null;
let groupId = null;
let groupDataId = null;
let formDeleted = false;
let groupDeleted = false;
let groupDataDeleted = false;

try {
  assertOk("form temp create for group", await requestJson(`${adminBase}/api/admin/system/form/temp/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({
      name: createFormName,
      info: "验收临时组合数据表单",
      content: formTempContent("create")
    })
  }));
  const createdForm = await findFormTempByName(token, createFormName);
  assertTruthy("created group form temp found", createdForm?.id > 0, createdForm);
  formId = createdForm.id;

  assertOk("system group create", await requestJson(
    `${adminBase}/api/admin/system/group/save?${new URLSearchParams({
      name: createGroupName,
      info: "验收临时组合数据组",
      formId: String(formId)
    })}`,
    { method: "POST", headers: { "Authori-zation": token } }
  ));
  const createdGroup = await findGroupByName(token, createGroupName);
  assertTruthy("created system group found", createdGroup?.id > 0, createdGroup);
  groupId = createdGroup.id;

  assertOk("system group update", await requestJson(
    `${adminBase}/api/admin/system/group/update?${new URLSearchParams({
      id: String(groupId),
      name: updateGroupName,
      info: "验收临时组合数据组已更新",
      formId: String(formId)
    })}`,
    { method: "POST", headers: { "Authori-zation": token } }
  ));
  const groupInfo = assertOk("system group info after update", await requestJson(
    `${adminBase}/api/admin/system/group/info?id=${groupId}`,
    { headers: { "Authori-zation": token } }
  ));
  assertTruthy("group info name", groupInfo.name === updateGroupName, groupInfo);
  assertTruthy("group info form id", Number(groupInfo.formId) === Number(formId), groupInfo);

  assertOk("system group data create", await requestJson(`${adminBase}/api/admin/system/group/data/save`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({
      gid: groupId,
      form: groupDataForm(formId, "create", 901, true)
    })
  }));
  const createdData = await findGroupDataByKeyword(token, groupId, "验收临时组合数据-create");
  assertTruthy("created system group data found", createdData?.id > 0, createdData);
  groupDataId = createdData.id;
  const createdValue = JSON.parse(createdData.value);
  assertTruthy("created group data sort", Number(createdData.sort) === 901, createdData);
  assertTruthy("created group data status", createdData.status === true || Number(createdData.status) === 1, createdData);
  assertTruthy("created group data image prefix normalized", createdValue.fields[1]?.value?.startsWith("crmebimage/"), createdValue);

  assertOk("system group data update", await requestJson(`${adminBase}/api/admin/system/group/data/update?id=${groupDataId}`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({
      gid: groupId,
      form: groupDataForm(formId, "update", 902, false)
    })
  }));
  const dataInfo = assertOk("system group data info after update", await requestJson(
    `${adminBase}/api/admin/system/group/data/info?id=${groupDataId}`,
    { headers: { "Authori-zation": token } }
  ));
  const updatedValue = JSON.parse(dataInfo.value);
  assertTruthy("group data info sort", Number(dataInfo.sort) === 902, dataInfo);
  assertTruthy("group data info disabled", dataInfo.status === false || Number(dataInfo.status) === 0, dataInfo);
  assertTruthy("group data info marker", updatedValue.fields[0]?.value === "验收临时组合数据-update", updatedValue);
  assertTruthy("group data info form id", Number(updatedValue.id) === Number(formId), updatedValue);

  assertOk("system group data delete", await requestJson(`${adminBase}/api/admin/system/group/data/delete?id=${groupDataId}`, {
    headers: { "Authori-zation": token }
  }));
  groupDataDeleted = true;
  const dataAfterDelete = await requestJson(`${adminBase}/api/admin/system/group/data/info?id=${groupDataId}`, {
    headers: { "Authori-zation": token }
  });
  assertTruthy("group data info after delete is not ok", dataAfterDelete.body?.code !== 200, dataAfterDelete.body);

  assertOk("system group delete", await requestJson(`${adminBase}/api/admin/system/group/delete?id=${groupId}`, {
    headers: { "Authori-zation": token }
  }));
  groupDeleted = true;
  const groupAfterDelete = await requestJson(`${adminBase}/api/admin/system/group/info?id=${groupId}`, {
    headers: { "Authori-zation": token }
  });
  assertTruthy("group info after delete is not ok", groupAfterDelete.body?.code !== 200, groupAfterDelete.body);

  assertOk("form temp delete for group", await requestJson(`${adminBase}/api/admin/system/form/temp/delete?id=${formId}`, {
    headers: { "Authori-zation": token }
  }));
  formDeleted = true;

  assertTruthy("deleted group excluded from list", !(await findGroupByName(token, updateGroupName)), updateGroupName);
  assertTruthy("deleted form excluded from list", !(await findFormTempByName(token, createFormName)), createFormName);

  console.log(JSON.stringify({
    runtimeSystemGroupWritebackSmokeOk: true,
    adminBase,
    formTemp: {
      id: formId,
      name: createFormName,
      deleted: formDeleted
    },
    systemGroup: {
      id: groupId,
      createName: createGroupName,
      updateName: updateGroupName,
      deleted: groupDeleted
    },
    systemGroupData: {
      id: groupDataId,
      deleted: groupDataDeleted
    }
  }, null, 2));
} finally {
  if (groupDataId && !groupDataDeleted) {
    await requestJson(`${adminBase}/api/admin/system/group/data/delete?id=${groupDataId}`, {
      headers: { "Authori-zation": token }
    }).catch(() => {});
  }
  if (groupId && !groupDeleted) {
    await requestJson(`${adminBase}/api/admin/system/group/delete?id=${groupId}`, {
      headers: { "Authori-zation": token }
    }).catch(() => {});
  }
  if (formId && !formDeleted) {
    await requestJson(`${adminBase}/api/admin/system/form/temp/delete?id=${formId}`, {
      headers: { "Authori-zation": token }
    }).catch(() => {});
  }
}
