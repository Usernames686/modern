<template>
  <div class="divBox">
    <el-card class="box-card">
      <el-tabs v-model="activeTopId" v-loading="loading" @tab-change="handleTopChange">
        <el-tab-pane v-for="tab in treeList" :key="tab.id" :label="tab.name" :name="String(tab.id)">
          <el-tabs
            v-if="childrenOf(tab).length"
            v-model="activeChildFormId"
            type="border-card"
            class="tab-content"
            @tab-change="loadForm"
          >
            <el-tab-pane
              v-for="child in childrenOf(tab)"
              :key="child.id"
              :label="child.name"
              :name="String(child.extra || '')"
            >
              <DynamicSettingForm
                v-if="currentFormId && activeChildFormId === String(child.extra || '')"
                :form-fields="formFields"
                :form-data="formData"
                :saving="saving"
                @submit="submit"
              />
            </el-tab-pane>
          </el-tabs>
          <DynamicSettingForm
            v-else-if="currentFormId && activeTopId === String(tab.id)"
            :form-fields="formFields"
            :form-data="formData"
            :saving="saving"
            @submit="submit"
          />
          <el-empty v-else description="未关联表单配置" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

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
              <img :src="assetUrl(item.sattDir)" :alt="item.name" />
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
import { computed, defineComponent, h, onMounted, reactive, ref, watch } from 'vue';
import { Camera } from '@element-plus/icons-vue';
import {
  ElButton,
  ElCheckbox,
  ElCheckboxGroup,
  ElDatePicker,
  ElForm,
  ElFormItem,
  ElImage,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElOption,
  ElRadio,
  ElRadioGroup,
  ElSelect,
  ElSwitch,
  ElTimePicker,
  ElUpload
} from 'element-plus';
import {
  attachmentList,
  categoryTree,
  systemConfigInfo,
  systemConfigSaveForm,
  systemConfigUploadType,
  systemFormTempInfo,
  uploadFile,
  uploadImage
} from '../api';

const CATEGORY_TYPE_CONFIG = 6;
const IMAGE_TYPES = 'jpg,jpeg,gif,png,bmp,PNG,JPG';

const loading = ref(false);
const saving = ref(false);
const attachmentLoading = ref(false);
const treeList = ref([]);
const activeTopId = ref('');
const activeChildFormId = ref('');
const currentFormId = ref('');
const currentFormName = ref('');
const formFields = ref([]);
const formData = reactive({});
const uploadType = ref(null);
const attachmentCategories = ref([]);
const attachmentRows = ref([]);
const attachmentTotal = ref(0);

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

const DynamicSettingForm = defineComponent({
  props: {
    formFields: { type: Array, required: true },
    formData: { type: Object, required: true },
    saving: { type: Boolean, default: false }
  },
  emits: ['submit'],
  setup(props, { emit }) {
    const formRef = ref();
    async function submitForm() {
      const valid = await formRef.value?.validate().catch(() => false);
      if (!valid) return;
      emit('submit');
    }
    return () => h('div', { class: 'setting-form-wrap' }, [
      h(ElForm, {
        ref: formRef,
        model: props.formData,
        rules: buildFieldRules(props.formFields),
        labelWidth: '300px',
        labelPosition: 'right',
        class: 'setting-form'
      }, () => [
        ...props.formFields.map((field) => h(ElFormItem, { label: field.label, prop: field.model }, () => renderField(field, props.formData))),
        h(ElFormItem, { class: 'setting-actions' }, () => [
          h(ElButton, { type: 'primary', loading: props.saving, onClick: submitForm }, () => '提交')
        ])
      ])
    ]);
  }
});

onMounted(async () => {
  await loadTree();
  systemConfigUploadType().then((data) => {
    uploadType.value = Number(data?.value ?? data ?? 0);
  }).catch(() => {});
});

watch(activeChildFormId, (value) => {
  if (value) loadForm(value);
});

async function loadTree() {
  loading.value = true;
  try {
    const data = await categoryTree({ type: CATEGORY_TYPE_CONFIG, status: 1 });
    treeList.value = data || [];
    if (treeList.value.length) {
      activeTopId.value = String(treeList.value[0].id);
      await selectTop(treeList.value[0]);
    }
  } finally {
    loading.value = false;
  }
}

async function handleTopChange(id) {
  const tab = treeList.value.find((item) => String(item.id) === String(id));
  if (tab) await selectTop(tab);
}

async function selectTop(tab) {
  const children = childrenOf(tab);
  if (children.length) {
    activeChildFormId.value = String(children[0].extra || '');
    if (activeChildFormId.value) await loadForm(activeChildFormId.value);
  } else if (tab.extra) {
    activeChildFormId.value = '';
    await loadForm(String(tab.extra));
  } else {
    resetFormState();
  }
}

async function loadForm(formId) {
  if (!formId) {
    resetFormState();
    return;
  }
  loading.value = true;
  try {
    const [template, values] = await Promise.all([
      systemFormTempInfo({ id: formId }),
      systemConfigInfo({ formId })
    ]);
    currentFormId.value = String(template.id);
    currentFormName.value = template.name || '';
    const content = JSON.parse(template.content || '{"fields":[]}');
    formFields.value = flattenFields(content.fields || []);
    for (const key of Object.keys(formData)) delete formData[key];
    for (const field of formFields.value) {
      formData[field.model] = normalizeValue(values?.[field.model] ?? field.defaultValue ?? '', field);
    }
  } finally {
    loading.value = false;
  }
}

async function submit() {
  if (!currentFormId.value) return;
  saving.value = true;
  try {
    const fields = formFields.value.map((field) => ({
      name: field.model,
      title: field.label,
      value: Array.isArray(formData[field.model]) ? formData[field.model].join(',') : String(formData[field.model] ?? '')
    }));
    await systemConfigSaveForm({
      id: Number(currentFormId.value),
      sort: 0,
      status: true,
      fields
    });
    ElMessage.success('添加数据成功');
    await loadForm(currentFormId.value);
  } finally {
    saving.value = false;
  }
}

function renderField(field, data) {
  if (field.tag === 'upload-file' || field.tag === 'el-upload') {
    const isImage = isImageUpload(field);
    const currentValue = data[field.model] || '';
    return h('div', { class: 'setting-file-row' }, [
      h(ElUpload, {
        action: '#',
        showFileList: false,
        accept: uploadAccept(field),
        httpRequest: (options) => uploadSettingFile(options, field, data)
      }, () => h(ElButton, { type: 'primary' }, () => field.buttonText || '点击上传')),
      isImage && currentValue
        ? h(ElImage, {
          class: 'setting-preview',
          src: assetUrl(currentValue),
          previewSrcList: [assetUrl(currentValue)],
          previewTeleported: true,
          fit: 'cover'
        })
        : null,
      h(ElInput, {
        modelValue: currentValue,
        placeholder: field.placeholder || '上传后自动回填文件路径',
        clearable: true,
        style: { width: field.width || '50%' },
        onUpdateModelValue: (value) => { data[field.model] = value; }
      }),
      currentValue
        ? h(ElButton, { onClick: () => { data[field.model] = ''; } }, () => '清除')
        : null
    ]);
  }
  if (field.tag === 'self-upload') {
    return h('div', { class: 'setting-upload-row' }, [
      h(ElUpload, {
        action: '#',
        showFileList: false,
        accept: 'image/*',
        httpRequest: (options) => uploadSettingImage(options, field.model, data)
      }, () => h('div', { class: 'upLoadPicBox' }, [
        data[field.model]
          ? h('div', { class: 'pictrue' }, [h('img', { src: assetUrl(data[field.model]), alt: '' })])
          : h('div', { class: 'upLoad' }, [h(Camera)])
      ])),
      data[field.model] ? h(ElImage, {
        class: 'setting-preview',
        src: assetUrl(data[field.model]),
        previewSrcList: [assetUrl(data[field.model])],
        previewTeleported: true,
        fit: 'cover'
      }) : null,
      h(ElButton, { type: 'primary', onClick: () => openAttachmentSelector(field.model) }, () => '选择图片'),
      data[field.model]
        ? h(ElButton, { onClick: () => { data[field.model] = ''; } }, () => '清除')
        : null
    ]);
  }
  if (field.tag === 'el-input-number') {
    return h(ElInputNumber, {
      modelValue: numberValue(data[field.model]),
      min: field.min ?? 0,
      controlsPosition: 'right',
      onUpdateModelValue: (value) => { data[field.model] = value ?? 0; }
    });
  }
  if (field.tag === 'el-radio-group') {
    return h(ElRadioGroup, {
      modelValue: String(data[field.model] ?? ''),
      onUpdateModelValue: (value) => { data[field.model] = value; }
    }, () => field.options.map((option) => h(ElRadio, { value: option.value, label: option.value }, () => option.label)));
  }
  if (field.tag === 'el-checkbox-group') {
    return h(ElCheckboxGroup, {
      modelValue: arrayValue(data[field.model]),
      onUpdateModelValue: (value) => { data[field.model] = Array.isArray(value) ? value : arrayValue(value); },
      style: { width: field.width || '50%' }
    }, () => field.options.map((option) => h(ElCheckbox, { value: option.value, label: option.value }, () => option.label)));
  }
  if (field.tag === 'el-select') {
    return h(ElSelect, {
      modelValue: field.multiple ? arrayValue(data[field.model]) : normalizeOptionValue(data[field.model]),
      placeholder: field.placeholder || '请选择',
      clearable: true,
      filterable: true,
      multiple: field.multiple,
      style: { width: field.width || '50%' },
      onUpdateModelValue: (value) => { data[field.model] = field.multiple ? arrayValue(value) : value; }
    }, () => field.options.map((option) => h(ElOption, {
      label: option.label,
      value: option.value
    })));
  }
  if (field.tag === 'el-switch') {
    return h(ElSwitch, {
      modelValue: Boolean(data[field.model]),
      onUpdateModelValue: (value) => { data[field.model] = value; }
    });
  }
  if (field.tag === 'el-date-picker') {
    return h(ElDatePicker, {
      modelValue: data[field.model] || '',
      type: field.type || 'date',
      valueFormat: field.valueFormat || 'YYYY-MM-DD',
      placeholder: field.placeholder || '请选择日期',
      style: { width: field.width || '50%' },
      onUpdateModelValue: (value) => { data[field.model] = value || ''; }
    });
  }
  if (field.tag === 'el-time-picker' || field.tag === 'time-select') {
    return h(ElTimePicker, {
      modelValue: data[field.model] || '',
      valueFormat: field.valueFormat || 'HH:mm:ss',
      placeholder: field.placeholder || '请选择时间',
      style: { width: field.width || '50%' },
      onUpdateModelValue: (value) => { data[field.model] = value || ''; }
    });
  }
  return h(ElInput, {
    modelValue: data[field.model],
    type: field.type === 'textarea' ? 'textarea' : 'text',
    rows: field.type === 'textarea' ? 4 : undefined,
    placeholder: field.placeholder || '',
    clearable: true,
    style: { width: field.width || '50%' },
    onUpdateModelValue: (value) => { data[field.model] = value; }
  });
}

async function uploadSettingImage(options, key, data) {
  const body = new FormData();
  body.append('file', options.file);
  try {
    const result = await uploadImage(body, { model: 'system', pid: 0 });
    data[key] = result?.sattDir || result?.src || result?.url || result?.path || '';
    if (!data[key]) ElMessage.warning('上传成功，但未返回图片地址');
  } catch (error) {
    options.onError?.(error);
  }
}

async function uploadSettingFile(options, field, data) {
  const body = new FormData();
  body.append('file', options.file);
  try {
    const upload = isImageUpload(field) ? uploadImage : uploadFile;
    const result = await upload(body, { model: 'system', pid: 0 });
    data[field.model] = result?.sattDir || result?.src || result?.url || result?.path || '';
    if (!data[field.model]) ElMessage.warning('上传成功，但未返回文件地址');
  } catch (error) {
    options.onError?.(error);
  }
}

async function openAttachmentSelector(fieldKey) {
  attachmentSelector.fieldKey = fieldKey;
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
  formData[attachmentSelector.fieldKey] = item.sattDir || item.attDir || '';
  attachmentSelector.visible = false;
}

function buildFieldRules(fields) {
  const rules = {};
  for (const field of fields || []) {
    if (!field.required) continue;
    rules[field.model] = [{
      trigger: ['blur', 'change'],
      validator: (_rule, value, callback) => {
        if (field.tag === 'el-checkbox-group' || (field.tag === 'el-select' && field.multiple)) {
          if (Array.isArray(value) && value.length) callback();
          else callback(new Error(`${field.label}不能为空`));
          return;
        }
        if (field.tag === 'el-switch') {
          callback();
          return;
        }
        if (value === undefined || value === null || value === '') callback(new Error(`${field.label}不能为空`));
        else callback();
      }
    }];
  }
  return rules;
}

function flattenFields(rawFields) {
  const result = [];
  for (const raw of rawFields || []) {
    const children = raw?.__config__?.children;
    if (Array.isArray(children) && children.length) {
      result.push(...flattenFields(children));
      continue;
    }
    const field = normalizeField(raw);
    if (field.model) result.push(field);
  }
  return result;
}

function normalizeField(raw) {
  const config = raw.__config__ || {};
  const slot = raw.__slot__ || {};
  const options = slot.options || raw.options || config.options || [];
  return {
    model: raw.__vModel__ || raw.vModel || raw.model || '',
    label: stripColon(config.label || raw.label || raw.__vModel__ || ''),
    tag: config.tag || raw.tag || 'el-input',
    required: Boolean(config.required),
    type: raw.type || '',
    placeholder: raw.placeholder || '',
    accept: raw.accept || '',
    buttonText: config.buttonText || '点击上传',
    defaultValue: config.defaultValue ?? raw.__defaultValue__ ?? raw.defaultValue ?? '',
    listType: raw['list-type'] || raw.listType || slot['list-type'] || '',
    min: Number.isFinite(Number(raw.min)) ? Number(raw.min) : undefined,
    width: raw.style?.width || '50%',
    multiple: Boolean(raw.multiple),
    valueFormat: normalizeDateFormat(raw['value-format'] || raw.valueFormat || raw.format || ''),
    options: normalizeOptions(options)
  };
}

function normalizeDateFormat(value) {
  return String(value || '')
    .replace(/yyyy/g, 'YYYY')
    .replace(/dd/g, 'DD');
}

function normalizeOptions(options) {
  if (!Array.isArray(options)) return [];
  return options
    .map((item) => {
      if (typeof item === 'string' || typeof item === 'number') {
        const value = normalizeOptionValue(item);
        return { label: value, value };
      }
      return {
        label: String(item?.label ?? item?.name ?? item?.title ?? item?.value ?? ''),
        value: normalizeOptionValue(item?.value ?? item?.id ?? item?.label ?? '')
      };
    })
    .filter((item) => item.label);
}

function normalizeOptionValue(value) {
  if (value === undefined || value === null) return '';
  return String(value).replace(/^['"]|['"]$/g, '');
}

function normalizeValue(value, field) {
  if (field.tag === 'el-input-number') return numberValue(value);
  if (field.tag === 'el-switch') return value === true || value === 'true' || value === '1' || value === 1;
  if (field.tag === 'el-radio-group') return normalizeOptionValue(value);
  if (field.tag === 'el-checkbox-group') return arrayValue(value);
  if (field.tag === 'el-select') return field.multiple ? arrayValue(value) : normalizeOptionValue(value);
  return value ?? '';
}

function arrayValue(value) {
  if (Array.isArray(value)) return value.map((item) => normalizeOptionValue(item)).filter(Boolean);
  if (value === undefined || value === null || value === '') return [];
  return String(value).split(/[,，]/).map((item) => normalizeOptionValue(item.trim())).filter(Boolean);
}

function numberValue(value) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function isImageUpload(field) {
  const accept = String(field.accept || '').toLowerCase();
  const listType = String(field.listType || '').toLowerCase();
  return field.tag === 'el-upload' && (listType.includes('picture') || accept === 'image' || accept.includes('image/'));
}

function uploadAccept(field) {
  if (field.tag === 'upload-file') return field.accept || '.p12,.pem,.zip,.doc,.docx,.xls,.xlsx,.pdf,.mp3,.wma,.wav,.amr,.mp4';
  if (isImageUpload(field)) return 'image/*';
  return field.accept || '';
}

function childrenOf(tab) {
  return tab?.child || tab?.children || [];
}

function resetFormState() {
  currentFormId.value = '';
  currentFormName.value = '';
  formFields.value = [];
  for (const key of Object.keys(formData)) delete formData[key];
}

function stripColon(value) {
  return String(value || '').replace(/[：:]\s*$/, '：');
}

function assetUrl(value) {
  if (!value) return '';
  if (/^https?:\/\//.test(value) || value.startsWith('data:')) return value;
  return value.startsWith('/') ? value : `/${value}`;
}
</script>

<style scoped>
.tab-content {
  margin-top: 10px;
}

.setting-form-wrap {
  padding-top: 20px;
}

.setting-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.setting-upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.setting-file-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.setting-preview {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  overflow: hidden;
}

.setting-actions {
  padding-top: 4px;
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
  .setting-file-row,
  .setting-upload-row {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .attachment-picker {
    grid-template-columns: 1fr;
  }

  .attachment-picker-tree {
    max-height: 180px;
  }
}
</style>
