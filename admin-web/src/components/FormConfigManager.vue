<template>
  <div class="divBox">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add padding-24">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item label="关键字：">
            <el-input
              v-model="query.keywords"
              placeholder="请输入id，名称，描述"
              clearable
              class="selWidth"
              size="small"
              @keyup.enter="searchList"
            />
          </el-form-item>
          <el-button type="primary" size="small" @click="searchList">搜索</el-button>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card">
      <template #header>
        <div class="clearfix">
          <el-button type="primary" size="small" :icon="Plus" @click="openEditor(null)">创建表单</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="rows" size="small" class="table">
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="名称" prop="name" min-width="180" />
        <el-table-column label="描述" prop="info" min-width="220" />
        <el-table-column label="字段数" width="90">
          <template #default="{ row }">{{ countFields(row.content) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" prop="updateTime" min-width="200" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="EditPen" @click="openEditor(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button link type="danger" @click="deleteForm(row)">删除</el-button>
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
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="editor.visible"
      :title="editor.mode === 'create' ? '创建表单' : '编辑表单'"
      fullscreen
      destroy-on-close
      :close-on-click-modal="false"
      class="form-config-dialog"
    >
      <div class="form-config-editor">
        <el-card shadow="never" class="form-meta-panel">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
            <el-form-item label="表单名称：" prop="name">
              <el-input v-model="form.name" placeholder="表单名称" clearable />
            </el-form-item>
            <el-form-item label="表单描述：" prop="info">
              <el-input v-model="form.info" placeholder="表单描述" clearable />
            </el-form-item>
          </el-form>
        </el-card>

        <el-tabs v-model="activeTab" class="form-config-tabs">
          <el-tab-pane label="字段配置" name="fields">
            <div class="field-toolbar">
              <el-button type="primary" size="small" :icon="Plus" @click="addField">添加字段</el-button>
              <el-button size="small" :icon="DocumentCopy" @click="syncJsonFromFields">生成 JSON</el-button>
            </div>

            <el-table :data="fields" size="small" class="table field-table" row-key="uid">
              <el-table-column label="字段标题" min-width="180">
                <template #default="{ row }">
                  <el-input v-model="row.label" size="small" placeholder="字段标题" @input="markFieldsDirty" />
                </template>
              </el-table-column>
              <el-table-column label="字段变量" min-width="160">
                <template #default="{ row }">
                  <el-input v-model="row.model" size="small" placeholder="例如 name" @input="markFieldsDirty" />
                </template>
              </el-table-column>
              <el-table-column label="组件类型" min-width="150">
                <template #default="{ row }">
                  <el-select v-model="row.tag" size="small" @change="markFieldsDirty">
                    <el-option label="输入框" value="el-input" />
                    <el-option label="文本域" value="el-input-textarea" />
                    <el-option label="数字输入" value="el-input-number" />
                    <el-option label="单选框组" value="el-radio-group" />
                    <el-option label="多选框组" value="el-checkbox-group" />
                    <el-option label="下拉选择" value="el-select" />
                    <el-option label="开关" value="el-switch" />
                    <el-option label="日期选择" value="el-date-picker" />
                    <el-option label="时间选择" value="el-time-picker" />
                    <el-option label="单图上传" value="self-upload" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="占位提示" min-width="180">
                <template #default="{ row }">
                  <el-input v-model="row.placeholder" size="small" placeholder="占位提示" @input="markFieldsDirty" />
                </template>
              </el-table-column>
              <el-table-column label="默认值" min-width="150">
                <template #default="{ row }">
                  <el-switch
                    v-if="row.tag === 'el-switch'"
                    v-model="row.defaultValue"
                    size="small"
                    @change="markFieldsDirty"
                  />
                  <el-input-number
                    v-else-if="row.tag === 'el-input-number'"
                    v-model="row.defaultValue"
                    size="small"
                    controls-position="right"
                    @change="markFieldsDirty"
                  />
                  <el-input
                    v-else
                    v-model="row.defaultValue"
                    size="small"
                    placeholder="默认值"
                    @input="markFieldsDirty"
                  />
                </template>
              </el-table-column>
              <el-table-column label="必填" width="76">
                <template #default="{ row }">
                  <el-switch v-model="row.required" size="small" @change="markFieldsDirty" />
                </template>
              </el-table-column>
              <el-table-column label="选项" min-width="220">
                <template #default="{ row }">
                  <el-input
                    v-if="optionField(row.tag)"
                    v-model="row.optionsText"
                    size="small"
                    placeholder="例：启用=1,禁用=0"
                    @input="markFieldsDirty"
                  />
                  <span v-else class="muted">-</span>
                </template>
              </el-table-column>
              <el-table-column label="宽度" min-width="100">
                <template #default="{ row }">
                  <el-input v-model="row.width" size="small" placeholder="100%" @input="markFieldsDirty" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ $index }">
                  <el-button link type="primary" :disabled="$index === 0" @click="moveField($index, -1)">上移</el-button>
                  <el-divider direction="vertical" />
                  <el-button link type="primary" @click="copyField($index)">复制</el-button>
                  <el-divider direction="vertical" />
                  <el-button link type="danger" @click="removeField($index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="JSON 配置" name="json">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="22"
              resize="none"
              spellcheck="false"
              class="json-editor"
              @input="markJsonDirty"
            />
          </el-tab-pane>

          <el-tab-pane label="表单预览" name="preview">
            <el-form label-width="130px" class="preview-form">
              <el-form-item
                v-for="field in previewFields"
                :key="field.model"
                :label="`${trimColon(field.label)}：`"
                :required="field.required"
              >
                <el-input
                  v-if="field.tag === 'el-input-textarea'"
                  type="textarea"
                  :rows="3"
                  :placeholder="field.placeholder || field.label"
                  :model-value="field.defaultValue"
                  :style="{ width: field.width || '100%' }"
                />
                <el-input-number
                  v-else-if="field.tag === 'el-input-number'"
                  :model-value="Number(field.defaultValue || 0)"
                  controls-position="right"
                />
                <el-radio-group v-else-if="field.tag === 'el-radio-group'" :model-value="field.defaultValue">
                  <el-radio v-for="option in field.options" :key="option.value" :label="option.value">{{ option.label }}</el-radio>
                </el-radio-group>
                <el-checkbox-group v-else-if="field.tag === 'el-checkbox-group'" :model-value="arrayValue(field.defaultValue)">
                  <el-checkbox v-for="option in field.options" :key="option.value" :label="option.value">{{ option.label }}</el-checkbox>
                </el-checkbox-group>
                <el-select v-else-if="field.tag === 'el-select'" :model-value="field.defaultValue" :placeholder="field.placeholder || field.label" :style="{ width: field.width || '100%' }">
                  <el-option v-for="option in field.options" :key="option.value" :label="option.label" :value="option.value" />
                </el-select>
                <el-switch v-else-if="field.tag === 'el-switch'" :model-value="Boolean(field.defaultValue)" />
                <el-date-picker v-else-if="field.tag === 'el-date-picker'" :model-value="field.defaultValue" type="date" value-format="YYYY-MM-DD" />
                <el-time-picker v-else-if="field.tag === 'el-time-picker'" :model-value="field.defaultValue" value-format="HH:mm:ss" />
                <div v-else-if="field.tag === 'self-upload'" class="preview-upload">
                  <el-icon><Picture /></el-icon>
                  <span>图片上传</span>
                </div>
                <el-input v-else :model-value="field.defaultValue" :placeholder="field.placeholder || field.label" :style="{ width: field.width || '100%' }" />
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>

      <template #footer>
        <el-button @click="editor.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { DocumentCopy, EditPen, Picture, Plus } from '@element-plus/icons-vue';
import { systemFormTempDelete, systemFormTempInfo, systemFormTempList, systemFormTempSave, systemFormTempUpdate } from '../api';

const loading = ref(false);
const saving = ref(false);
const formRef = ref();
const rows = ref([]);
const total = ref(0);
const activeTab = ref('fields');
const fields = ref([]);
const fieldsDirty = ref(false);
const jsonDirty = ref(false);

const query = reactive({
  page: 1,
  limit: 10,
  keywords: ''
});

const editor = reactive({
  visible: false,
  mode: 'create'
});

const form = reactive(defaultForm());

const rules = {
  name: [{ required: true, message: '填写表单名称', trigger: ['blur', 'change'] }],
  info: [{ required: true, message: '填写表单描述', trigger: ['blur', 'change'] }]
};

const previewFields = computed(() => {
  try {
    return flattenFields(JSON.parse(form.content || '{"fields":[]}').fields || []);
  } catch {
    return fields.value;
  }
});

function defaultForm() {
  return {
    id: null,
    name: '',
    info: '',
    content: prettyJson(defaultContent())
  };
}

function defaultContent() {
  return {
    fields: [],
    formRef: 'elForm',
    formModel: 'formData',
    size: 'medium',
    labelPosition: 'right',
    labelWidth: 100,
    formRules: 'rules',
    gutter: 15,
    disabled: false,
    span: 24,
    formBtns: true
  };
}

async function loadList() {
  loading.value = true;
  try {
    const data = await systemFormTempList({
      page: query.page,
      limit: query.limit,
      keywords: query.keywords || undefined
    });
    rows.value = data?.list || [];
    total.value = data?.total || 0;
  } finally {
    loading.value = false;
  }
}

function searchList() {
  query.page = 1;
  loadList();
}

async function openEditor(row) {
  activeTab.value = 'fields';
  Object.assign(form, defaultForm());
  fields.value = [];
  fieldsDirty.value = false;
  jsonDirty.value = false;
  editor.mode = row?.id ? 'edit' : 'create';
  if (row?.id) {
    const data = await systemFormTempInfo({ id: row.id });
    Object.assign(form, {
      id: data.id,
      name: data.name || '',
      info: data.info || '',
      content: prettyJson(parseContent(data.content))
    });
    fields.value = flattenFields(parseContent(data.content).fields || []);
  }
  editor.visible = true;
}

function addField() {
  fields.value.push({
    uid: Date.now() + Math.random(),
    label: '字段名称',
    model: `field_${fields.value.length + 1}`,
    tag: 'el-input',
    placeholder: '',
    defaultValue: '',
    required: false,
    optionsText: '选项1=1,选项2=2',
    width: '100%'
  });
  fieldsDirty.value = true;
  syncJsonFromFields(false);
}

function copyField(index) {
  const source = fields.value[index];
  if (!source) return;
  fields.value.splice(index + 1, 0, {
    ...source,
    uid: Date.now() + Math.random(),
    model: `${source.model}_copy`,
    label: `${trimColon(source.label)}副本`
  });
  fieldsDirty.value = true;
  syncJsonFromFields(false);
}

function removeField(index) {
  fields.value.splice(index, 1);
  fieldsDirty.value = true;
  syncJsonFromFields(false);
}

function moveField(index, offset) {
  const next = index + offset;
  if (next < 0 || next >= fields.value.length) return;
  const list = [...fields.value];
  const [item] = list.splice(index, 1);
  list.splice(next, 0, item);
  fields.value = list;
  fieldsDirty.value = true;
  syncJsonFromFields(false);
}

function syncJsonFromFields(showMessage = true) {
  const old = parseContent(form.content);
  const content = {
    ...defaultContent(),
    ...old,
    fields: fields.value.map(toGeneratorField)
  };
  form.content = prettyJson(content);
  fieldsDirty.value = false;
  jsonDirty.value = false;
  if (showMessage) ElMessage.success('已按字段配置生成 JSON');
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  if (fieldsDirty.value && !jsonDirty.value) {
    syncJsonFromFields(false);
  }
  const content = parseContentStrict(form.content);
  if (!content) return;
  saving.value = true;
  try {
    const payload = {
      name: form.name,
      info: form.info,
      content: JSON.stringify(content)
    };
    if (editor.mode === 'create') {
      await systemFormTempSave(payload);
      ElMessage.success('创建表单配置成功');
    } else {
      await systemFormTempUpdate({ id: form.id }, payload);
      ElMessage.success('编辑表单配置成功');
    }
    editor.visible = false;
    fieldsDirty.value = false;
    jsonDirty.value = false;
    await loadList();
  } finally {
    saving.value = false;
  }
}

async function deleteForm(row) {
  await ElMessageBox.confirm(`确定删除表单配置“${row.name}”吗？`, '提示', { type: 'warning' });
  await systemFormTempDelete({ id: row.id });
  ElMessage.success('删除表单配置成功');
  await loadList();
}

function markFieldsDirty() {
  fieldsDirty.value = true;
}

function markJsonDirty() {
  jsonDirty.value = true;
}

function parseContent(value) {
  if (!value) return defaultContent();
  if (typeof value === 'object') return value;
  try {
    return JSON.parse(value);
  } catch {
    return defaultContent();
  }
}

function parseContentStrict(value) {
  try {
    const content = JSON.parse(value);
    if (!Array.isArray(content.fields)) {
      ElMessage.error('JSON 配置必须包含 fields 数组');
      return null;
    }
    return content;
  } catch {
    ElMessage.error('JSON 配置格式错误');
    return null;
  }
}

function prettyJson(value) {
  return JSON.stringify(value || defaultContent(), null, 2);
}

function countFields(content) {
  return flattenFields(parseContent(content).fields || []).length;
}

function flattenFields(rawFields) {
  const result = [];
  for (const field of rawFields || []) {
    const children = field?.__config__?.children;
    if (Array.isArray(children) && children.length) {
      result.push(...flattenFields(children));
      continue;
    }
    const model = field?.__vModel__ || field?.vModel || field?.name;
    if (!model) continue;
    result.push({
      uid: Date.now() + Math.random(),
      label: trimColon(field?.__config__?.label || field?.label || model),
      model,
      tag: normalizeTag(field),
      placeholder: field?.placeholder || '',
      defaultValue: field?.__config__?.defaultValue ?? field?.defaultValue ?? '',
      required: Boolean(field?.__config__?.required),
      optionsText: optionsText(field?.__slot__?.options || field?.options || field?.__options__ || []),
      width: field?.style?.width || '100%'
    });
  }
  return result;
}

function normalizeTag(field) {
  const tag = String(field?.__config__?.tag || field?.tag || 'el-input');
  if (tag === 'el-input' && field?.type === 'textarea') return 'el-input-textarea';
  if (tag === 'el-radio-group' || tag === 'el-checkbox-group' || tag === 'el-select') return tag;
  if (tag === 'el-date-picker' || tag === 'el-time-picker') return tag;
  return tag;
}

function toGeneratorField(field) {
  const tag = field.tag || 'el-input';
  const isTextarea = tag === 'el-input-textarea';
  const normalizedTag = isTextarea ? 'el-input' : tag;
  const options = parseOptions(field.optionsText);
  const item = {
    __config__: {
      label: trimColon(field.label),
      tag: normalizedTag,
      tagIcon: tagIcon(tag),
      layout: 'colFormItem',
      span: 24,
      required: Boolean(field.required),
      defaultValue: normalizeDefaultValue(field, options),
      regList: []
    },
    __vModel__: field.model,
    placeholder: field.placeholder || `请输入${trimColon(field.label)}`,
    style: { width: field.width || '100%' }
  };
  if (isTextarea) item.type = 'textarea';
  if (optionField(tag)) {
    item.__slot__ = { options };
    item.options = options;
    if (tag === 'el-select') {
      item.clearable = true;
      item.filterable = true;
    }
  }
  if (tag === 'el-date-picker') {
    item.type = 'date';
    item.format = 'yyyy-MM-dd';
    item['value-format'] = 'yyyy-MM-dd';
  }
  if (tag === 'el-time-picker') {
    item['value-format'] = 'HH:mm:ss';
  }
  return item;
}

function tagIcon(tag) {
  return {
    'el-input': 'input',
    'el-input-textarea': 'textarea',
    'el-input-number': 'number',
    'el-radio-group': 'radio',
    'el-checkbox-group': 'checkbox',
    'el-select': 'select',
    'el-switch': 'switch',
    'el-date-picker': 'date',
    'el-time-picker': 'time',
    'self-upload': 'upload'
  }[tag] || 'input';
}

function optionField(tag) {
  return ['el-radio-group', 'el-checkbox-group', 'el-select'].includes(tag);
}

function parseOptions(value) {
  const text = String(value || '').trim();
  if (!text) return [];
  return text.split(/[,，\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => {
      const [label, rawValue] = item.split('=');
      return {
        label: String(label || '').trim(),
        value: String(rawValue ?? label ?? '').trim()
      };
    })
    .filter((item) => item.label);
}

function optionsText(options) {
  if (!Array.isArray(options) || !options.length) {
    return '选项1=1,选项2=2';
  }
  return options
    .map((item) => `${item.label ?? item.value}=${item.value ?? item.label}`)
    .join(',');
}

function normalizeDefaultValue(field, options = []) {
  if (field.tag === 'el-switch') return Boolean(field.defaultValue);
  if (field.tag === 'el-input-number') return Number(field.defaultValue || 0);
  if (field.tag === 'el-checkbox-group') return arrayValue(field.defaultValue);
  if (optionField(field.tag) && !field.defaultValue && options.length) return options[0].value;
  return field.defaultValue ?? '';
}

function arrayValue(value) {
  if (Array.isArray(value)) return value;
  if (value === null || value === undefined || value === '') return [];
  return String(value).split(/[,，]/).map((item) => item.trim()).filter(Boolean);
}

function trimColon(value) {
  return String(value || '').replace(/[：:]+$/, '');
}

onMounted(loadList);
</script>

<style scoped>
.padding-24 {
  padding: 20px 24px 0;
}

.form-config-editor {
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 14px;
  min-height: calc(100vh - 150px);
}

.form-meta-panel {
  border-radius: 4px;
}

.form-config-tabs {
  min-height: 0;
}

.field-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.field-table {
  margin-bottom: 12px;
}

.json-editor :deep(textarea) {
  font-family: Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 1.55;
}

.preview-form {
  max-width: 760px;
}

.preview-upload {
  width: 120px;
  height: 84px;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px dashed #c0c4cc;
  border-radius: 4px;
  color: #909399;
  background: #fafafa;
}

.preview-upload .el-icon {
  font-size: 24px;
}
</style>
