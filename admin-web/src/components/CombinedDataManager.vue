<template>
  <div class="divBox">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item label="数据搜索：">
            <el-input
              v-model="query.keywords"
              placeholder="请输入ID，KEY，组合数据名称，简介"
              class="selWidth"
              size="small"
              clearable
              @keyup.enter="searchGroups"
            />
          </el-form-item>
          <el-button type="primary" size="small" @click="searchGroups">搜索</el-button>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <template #header>
        <div class="clearfix">
          <el-button type="primary" size="small" @click="openGroupEdit(null)">添加数据组</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="groups" size="small" highlight-current-row class="table">
        <el-table-column label="数据组名称" prop="name" min-width="150" />
        <el-table-column label="简介" prop="info" min-width="150" />
        <el-table-column label="表单数据ID" prop="formId" width="110" />
        <el-table-column label="操作" fixed="right" width="210">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDataList(row)">数据列表</el-button>
            <el-divider direction="vertical" />
            <el-button link type="primary" @click="openGroupEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button link type="danger" @click="removeGroup(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="block-pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.limit"
          :page-sizes="[10, 20, 40, 60]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          background
          @size-change="loadGroups"
          @current-change="loadGroups"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="groupDialog.visible"
      :title="groupDialog.mode === 'create' ? '创建数据组' : '编辑数据组'"
      destroy-on-close
      :close-on-click-modal="false"
      width="540px"
    >
      <el-form ref="groupFormRef" :model="groupForm" :rules="groupRules" label-width="100px">
        <el-form-item label="数据组名称：" prop="name">
          <el-input v-model="groupForm.name" placeholder="数据组名称" clearable />
        </el-form-item>
        <el-form-item label="数据简介：" prop="info">
          <el-input v-model="groupForm.info" placeholder="数据简介" clearable />
        </el-form-item>
        <el-form-item label="表单数据ID：" prop="formId">
          <span class="form-id-text">{{ groupForm.formId || '' }}</span>
          <el-button type="primary" size="small" @click="openFormSelector('group')">选择模板数据</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="savingGroup" @click="submitGroup">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="formSelector.visible"
      title="选择表单模板"
      append-to-body
      destroy-on-close
      width="760px"
      class="user-dialog"
    >
      <div class="padding-add padding-24">
        <el-form inline :model="formQuery" @submit.prevent>
          <el-form-item label="关键字：">
            <el-input
              v-model="formQuery.keywords"
              placeholder="请输入id，名称，描述"
              clearable
              class="selWidth"
              size="small"
              @keyup.enter="searchForms"
            />
          </el-form-item>
          <el-button type="primary" size="small" @click="searchForms">搜索</el-button>
        </el-form>
      </div>
      <el-table
        v-loading="formLoading"
        :data="formRows"
        highlight-current-row
        size="small"
        class="table"
        @current-change="selectForm"
      >
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="名称" prop="name" min-width="180" />
        <el-table-column label="描述" prop="info" min-width="220" />
        <el-table-column label="更新时间" prop="updateTime" min-width="180" />
      </el-table>
      <div class="block-pagination">
        <el-pagination
          v-model:current-page="formQuery.page"
          v-model:page-size="formQuery.limit"
          :page-sizes="[10, 20, 40, 60]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="formTotal"
          background
          @size-change="loadForms"
          @current-change="loadForms"
        />
      </div>
      <template #footer>
        <el-button type="primary" class="form-selector-confirm" :disabled="!selectedForm?.id" @click="confirmForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="dataDialog.visible"
      title="组合数据列表"
      destroy-on-close
      :close-on-click-modal="false"
      width="86%"
    >
      <div class="components-container">
        <div class="container">
          <el-form inline :model="dataQuery" @submit.prevent>
            <el-form-item label="状态">
              <el-select v-model="dataQuery.status" placeholder="状态" clearable class="selWidth" @change="searchGroupData">
                <el-option label="显示" :value="1" />
                <el-option label="隐藏" :value="0" />
              </el-select>
            </el-form-item>
          </el-form>
        </div>

        <el-button class="addBtn" type="primary" size="small" @click="openDataEdit(null)">添加数据</el-button>

        <el-table v-loading="dataLoading" :data="dataRows" size="small" class="table data-table">
          <el-table-column label="编号" prop="id" width="80" />
          <el-table-column
            v-for="field in formFields"
            :key="field.key"
            :label="field.label"
            :prop="field.key"
            min-width="150"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <div v-if="field.isImage && row[field.key]" class="group-image-preview">
                <el-image
                  :src="resourceUrl(row[field.key])"
                  :preview-src-list="[resourceUrl(row[field.key])]"
                  fit="contain"
                  preview-teleported
                />
              </div>
              <span v-else>{{ displayValue(row[field.key], field) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="排序" prop="sort" width="90" />
          <el-table-column label="状态" prop="status" width="90">
            <template #default="{ row }">{{ row.status ? '显示' : '隐藏' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDataEdit(row)">编辑</el-button>
              <el-divider direction="vertical" />
              <el-button link type="danger" @click="removeGroupData(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="block-pagination">
          <el-pagination
            v-model:current-page="dataQuery.page"
            v-model:page-size="dataQuery.limit"
            :page-sizes="[10, 20, 40, 60]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="dataTotal"
            background
            @size-change="loadGroupData"
            @current-change="loadGroupData"
          />
        </div>
      </div>
    </el-dialog>

    <el-dialog
      v-model="dataEditDialog.visible"
      :title="dataEditDialog.mode === 'create' ? '添加数据' : '编辑数据'"
      append-to-body
      destroy-on-close
      :close-on-click-modal="false"
      width="700px"
    >
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="120px">
        <el-form-item label="排序：" prop="sort">
          <el-input-number v-model="dataForm.sort" controls-position="right" :min="0" />
        </el-form-item>
        <el-form-item label="状态：" prop="status">
          <el-switch v-model="dataForm.status" />
        </el-form-item>
        <el-form-item
          v-for="field in formFields"
          :key="field.key"
          :label="`${trimColon(field.label)}：`"
          :prop="`fields.${field.key}`"
          :rules="fieldRules(field)"
          :required="field.required"
        >
          <el-input
            v-if="field.component === 'textarea'"
            v-model="dataForm.fields[field.key]"
            type="textarea"
            :rows="3"
            :placeholder="field.placeholder || field.label"
            :style="{ width: field.width }"
          />
          <el-input-number
            v-else-if="field.component === 'number'"
            v-model="dataForm.fields[field.key]"
            controls-position="right"
            class="form-number"
          />
          <el-radio-group
            v-else-if="field.component === 'radio'"
            v-model="dataForm.fields[field.key]"
            :style="{ width: field.width }"
          >
            <el-radio v-for="option in field.options" :key="option.value" :label="option.value">{{ option.label }}</el-radio>
          </el-radio-group>
          <el-checkbox-group
            v-else-if="field.component === 'checkbox'"
            v-model="dataForm.fields[field.key]"
            :style="{ width: field.width }"
          >
            <el-checkbox v-for="option in field.options" :key="option.value" :label="option.value">{{ option.label }}</el-checkbox>
          </el-checkbox-group>
          <el-select
            v-else-if="field.component === 'select'"
            v-model="dataForm.fields[field.key]"
            :placeholder="field.placeholder || field.label"
            clearable
            filterable
            :style="{ width: field.width }"
          >
            <el-option v-for="option in field.options" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
          <el-switch v-else-if="field.component === 'switch'" v-model="dataForm.fields[field.key]" />
          <el-date-picker
            v-else-if="field.component === 'date'"
            v-model="dataForm.fields[field.key]"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="field.placeholder || field.label"
            :style="{ width: field.width }"
          />
          <el-time-picker
            v-else-if="field.component === 'time'"
            v-model="dataForm.fields[field.key]"
            value-format="HH:mm:ss"
            :placeholder="field.placeholder || field.label"
            :style="{ width: field.width }"
          />
          <div v-else-if="field.component === 'image'" class="image-input-wrap" :style="{ width: field.width }">
            <div class="image-input-line">
              <el-input v-model="dataForm.fields[field.key]" :placeholder="field.placeholder || '请输入图片路径'" clearable />
              <el-button type="primary" @click="openAttachmentSelector(field)">选择图片</el-button>
            </div>
            <span class="muted">可从素材库选择，或粘贴已有素材路径；未配置上传服务时仅保存路径。</span>
          </div>
          <el-input
            v-else
            v-model="dataForm.fields[field.key]"
            :placeholder="field.placeholder || field.label"
            :style="{ width: field.width }"
            clearable
          />
          <div v-if="field.isImage && dataForm.fields[field.key]" class="edit-image-preview">
            <img :src="resourceUrl(dataForm.fields[field.key])" alt="" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataEditDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="savingData" @click="submitGroupData">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="attachmentSelector.visible"
      title="素材管理"
      append-to-body
      destroy-on-close
      width="860px"
      class="attachment-picker-dialog"
    >
      <div class="attachment-picker">
        <div class="attachment-picker-tree">
          <el-tree
            :data="attachmentTree"
            :props="attachmentTreeProps"
            node-key="id"
            default-expand-all
            highlight-current
            @node-click="handleAttachmentCategory"
          />
        </div>
        <div class="attachment-picker-main" v-loading="attachmentLoading">
          <div class="attachment-picker-toolbar">
            <span class="muted">选择已有图片素材，不在本地触发上传。</span>
          </div>
          <div class="attachment-picker-grid">
            <button
              v-for="item in attachmentRows"
              :key="item.attId"
              type="button"
              class="attachment-picker-card"
              @click="selectAttachment(item)"
            >
              <img :src="resourceUrl(item.sattDir)" :alt="item.name" />
              <span :title="item.name">{{ item.name }}</span>
            </button>
            <div v-if="!attachmentRows.length" class="attachment-picker-empty">素材为空</div>
          </div>
          <div class="block-pagination">
            <el-pagination
              v-model:current-page="attachmentQuery.page"
              :page-size="attachmentQuery.limit"
              layout="total, prev, pager, next"
              :total="attachmentTotal"
              background
              @current-change="loadAttachments"
            />
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  attachmentList,
  categoryTree,
  systemFormTempList,
  systemFormTempInfo,
  systemGroupDataDelete,
  systemGroupDataList,
  systemGroupDataSave,
  systemGroupDataUpdate,
  systemGroupDelete,
  systemGroupList,
  systemGroupSave,
  systemGroupUpdate
} from '../api';

const IMAGE_TYPES = 'jpg,jpeg,gif,png,bmp,PNG,JPG';

const loading = ref(false);
const savingGroup = ref(false);
const formLoading = ref(false);
const dataLoading = ref(false);
const savingData = ref(false);
const attachmentLoading = ref(false);
const groupFormRef = ref();
const dataFormRef = ref();
const groups = ref([]);
const total = ref(0);
const formRows = ref([]);
const formTotal = ref(0);
const selectedForm = ref(null);
const selectedGroup = ref(null);
const formConf = ref({ fields: [] });
const dataRows = ref([]);
const dataTotal = ref(0);
const attachmentCategories = ref([]);
const attachmentRows = ref([]);
const attachmentTotal = ref(0);

const query = reactive({
  page: 1,
  limit: 10,
  keywords: ''
});

const groupDialog = reactive({
  visible: false,
  mode: 'create'
});

const groupForm = reactive(defaultGroupForm());

const formSelector = reactive({
  visible: false,
  target: 'group'
});

const formQuery = reactive({
  page: 1,
  limit: 10,
  keywords: ''
});

const dataDialog = reactive({
  visible: false
});

const dataQuery = reactive({
  gid: null,
  page: 1,
  limit: 10,
  status: null,
  keywords: ''
});

const dataEditDialog = reactive({
  visible: false,
  mode: 'create',
  row: null
});

const dataForm = reactive(defaultDataForm());

const attachmentSelector = reactive({
  visible: false,
  fieldKey: ''
});

const attachmentQuery = reactive({
  page: 1,
  limit: 18,
  pid: 0,
  attType: IMAGE_TYPES
});

const groupRules = {
  name: [{ required: true, message: '填写数据组名称', trigger: ['blur', 'change'] }],
  info: [{ required: true, message: '填写数据简介', trigger: ['blur', 'change'] }],
  formId: [{ required: true, message: '请选择表单数据', trigger: ['change'] }]
};

const dataRules = {
  sort: [{ required: true, message: '排序不能为空', trigger: ['blur', 'change'] }]
};

const formFields = computed(() => flattenFields(formConf.value?.fields || []));

const attachmentTreeProps = {
  children: 'child',
  label: 'name'
};

const attachmentTree = computed(() => [
  {
    id: 0,
    name: '全部图片',
    child: attachmentCategories.value
  }
]);

function defaultGroupForm() {
  return {
    id: null,
    name: '',
    info: '',
    formId: null
  };
}

function defaultDataForm() {
  return {
    sort: 1,
    status: true,
    fields: {}
  };
}

async function loadGroups() {
  loading.value = true;
  try {
    const data = await systemGroupList({
      page: query.page,
      limit: query.limit,
      keywords: query.keywords || undefined
    });
    groups.value = data?.list || [];
    total.value = data?.total || 0;
  } finally {
    loading.value = false;
  }
}

function searchGroups() {
  query.page = 1;
  loadGroups();
}

function openGroupEdit(row) {
  groupDialog.mode = row?.id ? 'edit' : 'create';
  Object.assign(groupForm, defaultGroupForm(), row ? {
    id: row.id,
    name: row.name || '',
    info: row.info || '',
    formId: row.formId || null
  } : {});
  groupDialog.visible = true;
}

async function submitGroup() {
  const valid = await groupFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  savingGroup.value = true;
  try {
    const payload = {
      id: groupForm.id || undefined,
      name: groupForm.name,
      info: groupForm.info,
      formId: groupForm.formId
    };
    if (groupDialog.mode === 'create') {
      await systemGroupSave(payload);
      ElMessage.success('添加组合数据成功');
    } else {
      await systemGroupUpdate(payload);
      ElMessage.success('编辑组合数据成功');
    }
    groupDialog.visible = false;
    await loadGroups();
  } finally {
    savingGroup.value = false;
  }
}

async function removeGroup(row) {
  await ElMessageBox.confirm('删除当前数据', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  });
  await systemGroupDelete({ id: row.id });
  ElMessage.success('删除数据成功');
  await loadGroups();
}

async function openFormSelector(target) {
  formSelector.target = target;
  selectedForm.value = null;
  formQuery.page = 1;
  formQuery.keywords = '';
  formSelector.visible = true;
  await loadForms();
}

async function loadForms() {
  formLoading.value = true;
  try {
    const data = await systemFormTempList({
      page: formQuery.page,
      limit: formQuery.limit,
      keywords: formQuery.keywords || undefined
    });
    formRows.value = data?.list || [];
    formTotal.value = data?.total || 0;
  } finally {
    formLoading.value = false;
  }
}

function searchForms() {
  formQuery.page = 1;
  loadForms();
}

function selectForm(row) {
  selectedForm.value = row || null;
}

function confirmForm() {
  if (!selectedForm.value?.id) return;
  groupForm.formId = selectedForm.value.id;
  formSelector.visible = false;
}

async function openDataList(row) {
  if (!row.formId || Number(row.formId) <= 0) {
    ElMessage.error('请先关联表单');
    return;
  }
  selectedGroup.value = row;
  dataQuery.gid = row.id;
  dataQuery.page = 1;
  dataQuery.limit = 10;
  dataQuery.status = null;
  dataRows.value = [];
  formConf.value = { fields: [] };
  dataDialog.visible = true;
  await loadFormConfig(row.formId);
  await loadGroupData();
}

async function loadFormConfig(formId) {
  const data = await systemFormTempInfo({ id: formId });
  try {
    formConf.value = typeof data?.content === 'string' ? JSON.parse(data.content || '{}') : (data?.content || { fields: [] });
  } catch {
    formConf.value = { fields: [] };
    ElMessage.error('表单模板解析失败');
  }
}

async function loadGroupData() {
  if (!dataQuery.gid) return;
  dataLoading.value = true;
  try {
    const data = await systemGroupDataList({
      gid: dataQuery.gid,
      page: dataQuery.page,
      limit: dataQuery.limit,
      status: dataQuery.status === null || dataQuery.status === '' ? undefined : dataQuery.status,
      keywords: dataQuery.keywords || undefined
    });
    dataRows.value = (data?.list || []).map(normalizeGroupDataRow);
    dataTotal.value = data?.total || 0;
  } finally {
    dataLoading.value = false;
  }
}

function searchGroupData() {
  dataQuery.page = 1;
  loadGroupData();
}

function openDataEdit(row) {
  dataEditDialog.mode = row?.id ? 'edit' : 'create';
  dataEditDialog.row = row || null;
  Object.assign(dataForm, defaultDataForm(), {
    sort: row?.sort ?? 1,
    status: row?.status ?? true,
    fields: {}
  });
  for (const field of formFields.value) {
    dataForm.fields[field.key] = normalizeFieldValue(row?.[field.key], field);
  }
  dataEditDialog.visible = true;
}

async function submitGroupData() {
  const valid = await dataFormRef.value?.validate().catch(() => false);
  if (!valid || !selectedGroup.value?.id) return;
  savingData.value = true;
  try {
    const payload = {
      gid: selectedGroup.value.id,
      form: {
        id: selectedGroup.value.formId,
        sort: dataForm.sort,
        status: dataForm.status,
        fields: formFields.value.map((field) => ({
          name: field.key,
          title: field.key,
          value: dataForm.fields[field.key]
        }))
      }
    };
    if (dataEditDialog.mode === 'create') {
      await systemGroupDataSave(payload);
      ElMessage.success('添加数据成功');
    } else {
      await systemGroupDataUpdate({ id: dataEditDialog.row.id }, payload);
      ElMessage.success('编辑数据成功');
    }
    dataEditDialog.visible = false;
    await loadGroupData();
  } finally {
    savingData.value = false;
  }
}

async function removeGroupData(row) {
  await ElMessageBox.confirm('删除当前数据', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  });
  await systemGroupDataDelete({ id: row.id });
  ElMessage.success('删除数据成功');
  await loadGroupData();
}

async function openAttachmentSelector(field) {
  attachmentSelector.fieldKey = field.key;
  attachmentSelector.visible = true;
  attachmentQuery.page = 1;
  attachmentQuery.pid = 0;
  if (!attachmentCategories.value.length) {
    attachmentCategories.value = await categoryTree({ type: 2, status: -1 });
  }
  await loadAttachments();
}

async function loadAttachments() {
  attachmentLoading.value = true;
  try {
    const data = await attachmentList({ ...attachmentQuery });
    attachmentRows.value = data?.list || [];
    attachmentTotal.value = data?.total || 0;
  } finally {
    attachmentLoading.value = false;
  }
}

function handleAttachmentCategory(data) {
  attachmentQuery.pid = data.id;
  attachmentQuery.page = 1;
  loadAttachments();
}

function selectAttachment(item) {
  if (!attachmentSelector.fieldKey) return;
  dataForm.fields[attachmentSelector.fieldKey] = item.sattDir || item.attDir || '';
  attachmentSelector.visible = false;
  dataFormRef.value?.validateField?.(`fields.${attachmentSelector.fieldKey}`);
}

function normalizeGroupDataRow(item) {
  const parsed = parseValue(item.value);
  const row = {
    id: item.id,
    sort: item.sort ?? parsed.sort ?? 0,
    status: typeof item.status === 'boolean' ? item.status : Boolean(parsed.status),
    rawValue: parsed
  };
  for (const field of parsed.fields || []) {
    row[field.name] = field.value;
  }
  return row;
}

function parseValue(value) {
  if (!value) return { fields: [] };
  if (typeof value === 'object') return value;
  try {
    return JSON.parse(value);
  } catch {
    return { fields: [] };
  }
}

function flattenFields(fields) {
  const result = [];
  for (const field of fields || []) {
    const children = field?.__config__?.children;
    if (Array.isArray(children) && children.length) {
      result.push(...flattenFields(children));
      continue;
    }
    const key = field?.__vModel__ || field?.vModel || field?.name;
    if (!key) continue;
    result.push({
      key,
      label: trimColon(field?.__config__?.label || field?.label || key),
      component: componentType(field),
      required: Boolean(field?.__config__?.required),
      defaultValue: field?.__config__?.defaultValue ?? field?.defaultValue ?? '',
      placeholder: field?.placeholder || '',
      width: field?.style?.width || '100%',
      options: fieldOptions(field),
      isImage: isImageLikeField(field, key)
    });
  }
  return result;
}

function componentType(field) {
  const tag = String(field?.__config__?.tag || field?.tag || '').toLowerCase();
  const element = String(field?.__config__?.tagIcon || '').toLowerCase();
  if (tag.includes('textarea') || field?.type === 'textarea') return 'textarea';
  if (tag.includes('radio') || element.includes('radio')) return 'radio';
  if (tag.includes('checkbox') || element.includes('checkbox')) return 'checkbox';
  if (tag.includes('select') || element.includes('select')) return 'select';
  if (tag.includes('input-number') || element.includes('number')) return 'number';
  if (tag.includes('switch') || element.includes('switch')) return 'switch';
  if (tag.includes('date-picker') || element.includes('date')) return 'date';
  if (tag.includes('time-picker') || element.includes('time')) return 'time';
  if (tag.includes('upload') || tag.includes('self-upload') || element.includes('upload')) return 'image';
  return 'input';
}

function defaultFieldValue(field) {
  if (field.component === 'switch') return false;
  if (field.component === 'number') return 0;
  if (field.component === 'checkbox') return [];
  if (['radio', 'select'].includes(field.component) && field.options.length) return field.options[0].value;
  if (field.defaultValue !== undefined && field.defaultValue !== null && field.defaultValue !== '') return normalizeFieldValue(field.defaultValue, {
    ...field,
    defaultValue: ''
  });
  return '';
}

function normalizeFieldValue(value, field) {
  const source = value === undefined || value === null ? field.defaultValue : value;
  if (source === undefined || source === null || source === '') return defaultFieldValue({
    ...field,
    defaultValue: ''
  });
  if (field.component === 'switch') return source === true || source === 1 || source === '1' || source === 'true';
  if (field.component === 'number') return Number(source || 0);
  if (field.component === 'checkbox') return arrayValue(source);
  return source;
}

function fieldRules(field) {
  if (!field.required) return [];
  return [{
    trigger: ['blur', 'change'],
    validator: (_rule, value, callback) => {
      if (field.component === 'checkbox') {
        if (Array.isArray(value) && value.length) callback();
        else callback(new Error(`请选择${trimColon(field.label)}`));
        return;
      }
      if (field.component === 'switch') {
        callback();
        return;
      }
      if (value === undefined || value === null || value === '') callback(new Error(`请输入${trimColon(field.label)}`));
      else callback();
    }
  }];
}

function fieldOptions(field) {
  const options = field?.__slot__?.options || field?.options || field?.__options__ || [];
  if (!Array.isArray(options)) return [];
  return options
    .map((option) => {
      if (typeof option === 'string' || typeof option === 'number') {
        return { label: String(option), value: String(option) };
      }
      const label = option?.label ?? option?.name ?? option?.dictLabel ?? option?.title ?? option?.value;
      const value = option?.value ?? option?.id ?? option?.dictValue ?? option?.label;
      return {
        label: String(label ?? ''),
        value: String(value ?? '')
      };
    })
    .filter((option) => option.label);
}

function arrayValue(value) {
  if (Array.isArray(value)) return value;
  if (value === null || value === undefined || value === '') return [];
  return String(value).split(/[,，]/).map((item) => item.trim()).filter(Boolean);
}

function isImageLikeField(field, key) {
  const lowerKey = String(key || '').toLowerCase();
  const tag = String(field?.__config__?.tag || field?.tag || '').toLowerCase();
  const tagIcon = String(field?.__config__?.tagIcon || '').toLowerCase();
  const label = String(field?.__config__?.label || field?.label || '').toLowerCase();
  return ['img', 'image', 'pic'].includes(lowerKey)
    || lowerKey.includes('image')
    || lowerKey.includes('img')
    || lowerKey.includes('pic')
    || tag.includes('upload')
    || tagIcon.includes('upload')
    || label.includes('图片')
    || label.includes('图标');
}

function resourceUrl(path) {
  if (!path) return '';
  if (/^https?:\/\//.test(path) || path.startsWith('data:')) return path;
  return path.startsWith('/') ? path : `/${path}`;
}

function displayValue(value, field) {
  if (Array.isArray(value)) return value.map((item) => optionLabel(item, field)).join('，');
  if (field?.options?.length) return optionLabel(value, field);
  if (field?.component === 'switch') return value ? '是' : '否';
  if (value && typeof value === 'object') return JSON.stringify(value);
  return value ?? '';
}

function trimColon(value) {
  return String(value || '').replace(/[：:]+$/, '');
}

function optionLabel(value, field) {
  const match = field?.options?.find((option) => String(option.value) === String(value));
  return match?.label ?? value ?? '';
}

onMounted(loadGroups);
</script>

<style scoped>
.form-id-text {
  display: inline-flex;
  min-width: 54px;
  color: #606266;
}

.padding-24 {
  padding: 0 0 12px;
}

.form-selector-confirm {
  width: 100%;
}

.container {
  margin-bottom: 14px;
}

.addBtn {
  margin-bottom: 20px;
}

.data-table {
  margin-bottom: 20px;
}

.group-image-preview :deep(.el-image) {
  width: 36px;
  height: 36px;
  display: block;
  background: #f5f7f9;
}

.edit-image-preview {
  margin-top: 8px;
}

.edit-image-preview img {
  width: 72px;
  height: 72px;
  object-fit: contain;
  display: block;
  border: 1px solid #ebeef5;
  background: #f5f7f9;
}

.form-number {
  width: 180px;
}

.image-input-wrap {
  display: grid;
  gap: 6px;
}

.image-input-line {
  display: flex;
  gap: 8px;
}

.image-input-line :deep(.el-input) {
  flex: 1;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.attachment-picker {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 16px;
  min-height: 420px;
}

.attachment-picker-tree {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 10px;
  overflow: auto;
}

.attachment-picker-main {
  min-width: 0;
}

.attachment-picker-toolbar {
  min-height: 32px;
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.attachment-picker-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(104px, 1fr));
  gap: 12px;
  min-height: 300px;
}

.attachment-picker-card {
  min-width: 0;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
  padding: 8px;
  cursor: pointer;
  text-align: left;
}

.attachment-picker-card:hover {
  border-color: #409eff;
}

.attachment-picker-card img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  display: block;
  background: #f5f7fa;
}

.attachment-picker-card span {
  display: block;
  margin-top: 6px;
  color: #606266;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.attachment-picker-empty {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  color: #909399;
  background: #fafafa;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
}

@media (max-width: 900px) {
  .attachment-picker {
    grid-template-columns: 1fr;
  }

  .attachment-picker-tree {
    max-height: 180px;
  }
}
</style>
