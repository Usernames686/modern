<template>
  <div class="divBox seckill-config-manager">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form :model="query" size="small" inline @submit.prevent>
          <el-form-item label="秒杀状态：">
            <el-select v-model="query.status" placeholder="请选择" clearable class="selWidth" @change="search">
              <el-option label="关闭" value="'0'" />
              <el-option label="开启" value="'1'" />
            </el-select>
          </el-form-item>
          <el-form-item label="秒杀名称：">
            <el-input v-model="query.name" placeholder="请输入秒杀名称" clearable class="selWidth" @keyup.enter="search" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
            <el-button :icon="Refresh" @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14" :body-style="{ padding: '20px' }" shadow="never">
      <el-button type="primary" :icon="Plus" @click="openCreate">添加秒杀配置</el-button>

      <el-table v-loading="loading" :data="rows" class="mt20" size="small" highlight-current-row>
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column label="秒杀名称" min-width="130">
          <template #default="{ row }">
            <el-button link type="primary" @click="navigateToProducts(row.id)">{{ row.name }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="秒杀时段" min-width="130">
          <template #default="{ row }">{{ timeText(row.time) }}</template>
        </el-table-column>
        <el-table-column label="轮播图" min-width="200">
          <template #default="{ row }">
            <div v-if="sliderImages(row).length" class="slider-list">
              <el-image
                v-for="item in sliderImages(row)"
                :key="item.sattDir || item.url || item.name"
                class="slider-image"
                :src="assetUrl(item.sattDir || item.url)"
                :preview-src-list="[assetUrl(item.sattDir || item.url)]"
                preview-teleported
                fit="cover"
              />
            </div>
            <span v-else>无</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="140">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status"
              active-value="'1'"
              inactive-value="'0'"
              active-text="开启"
              inactive-text="关闭"
              @change="changeStatus(row, $event)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button link type="danger" :icon="Delete" @click="remove(row)">删除</el-button>
            <el-divider direction="vertical" />
            <el-button link type="primary" :icon="Plus" @click="addProduct(row)">添加商品</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="block">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.limit"
          :page-sizes="[10, 20, 30, 40]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑数据' : '添加数据'" width="700px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="秒杀名称：" prop="name">
          <el-input v-model="form.name" placeholder="请输入秒杀名称" />
        </el-form-item>
        <el-form-item label="秒杀时段：" prop="timeRange">
          <el-time-picker
            v-model="form.timeRange"
            is-range
            format="HH:mm"
            value-format="HH:mm"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            class="form-control"
          />
        </el-form-item>
        <el-form-item label="轮播图：" prop="sliderList">
          <div class="slider-actions">
            <el-upload action="#" :http-request="uploadSlider" :show-file-list="false" accept="image/*">
              <el-button :icon="Upload">上传图片</el-button>
            </el-upload>
            <el-button type="primary" plain @click="openAttachmentSelector">选择素材</el-button>
            <span class="muted">可上传新图，也可从老素材库选择已有图片。</span>
          </div>
          <div v-if="form.sliderList.length" class="form-slider-list">
            <div v-for="(item, index) in form.sliderList" :key="item.sattDir || index" class="form-slider-item">
              <img :src="assetUrl(item.sattDir)" alt="" />
              <el-button link type="danger" @click="removeSlider(index)">删除</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="排序：">
          <el-input-number v-model="form.sort" :min="0" :max="999999" />
        </el-form-item>
        <el-form-item label="状态：" prop="status">
          <el-switch v-model="form.status" active-value="'1'" inactive-value="'0'" active-text="开启" inactive-text="关闭" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="attachmentDialogVisible" title="选择轮播图" width="860px" append-to-body destroy-on-close>
      <div class="attachment-picker">
        <aside class="attachment-tree">
          <el-tree
            :data="attachmentTree"
            node-key="id"
            :props="attachmentTreeProps"
            default-expand-all
            highlight-current
            @node-click="handleAttachmentCategory"
          />
        </aside>
        <section class="attachment-main">
          <div class="attachment-picker-toolbar">
            <span class="muted">选择已有素材追加到秒杀配置轮播图，已选 {{ form.sliderList.length }} 张。</span>
          </div>
          <div v-loading="attachmentLoading" class="attachment-grid">
            <button
              v-for="item in attachmentRows"
              :key="item.attId || item.id"
              type="button"
              class="attachment-item"
              :class="{ selected: isSelectedAttachment(item) }"
              @click="selectAttachment(item)"
            >
              <el-image :src="assetUrl(item.sattDir || item.attDir)" fit="cover" />
              <span>{{ item.name || item.attName || '图片' }}</span>
            </button>
            <div v-if="!attachmentLoading && !attachmentRows.length" class="attachment-empty">暂无图片</div>
          </div>
          <div class="attachment-pagination">
            <el-pagination
              v-model:current-page="attachmentQuery.page"
              v-model:page-size="attachmentQuery.limit"
              layout="total, prev, pager, next"
              :total="attachmentTotal"
              background
              @current-change="loadAttachments"
            />
          </div>
        </section>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { Delete, Edit, Plus, Refresh, Search, Upload } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  attachmentList,
  categoryTree,
  seckillManagerDelete,
  seckillManagerInfo,
  seckillManagerList,
  seckillManagerSave,
  seckillManagerStatus,
  seckillManagerUpdate,
  uploadImage
} from '../api';

const CATEGORY_TYPE_ATTACHMENT = 2;
const IMAGE_TYPES = 'jpg,jpeg,gif,png,bmp,PNG,JPG';

const loading = ref(false);
const saving = ref(false);
const attachmentLoading = ref(false);
const rows = ref([]);
const attachmentCategories = ref([]);
const attachmentRows = ref([]);
const total = ref(0);
const attachmentTotal = ref(0);
const dialogVisible = ref(false);
const attachmentDialogVisible = ref(false);
const formRef = ref();

const query = reactive({
  page: 1,
  limit: 20,
  name: '',
  status: ''
});

const form = reactive({
  id: null,
  name: '',
  timeRange: [],
  sliderList: [],
  sort: 0,
  status: "'1'"
});
const attachmentQuery = reactive({
  page: 1,
  limit: 18,
  pid: 0,
  attType: IMAGE_TYPES
});

const attachmentTree = computed(() => [
  {
    id: 0,
    name: '全部图片',
    child: attachmentCategories.value
  }
]);

const attachmentTreeProps = {
  children: 'child',
  label: 'name'
};

const rules = {
  name: [{ required: true, message: '请输入秒杀名称', trigger: 'blur' }],
  timeRange: [{ validator: validateTimeRange, trigger: 'change' }],
  sliderList: [{ validator: validateSlider, trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
};

onMounted(loadList);

async function loadList() {
  loading.value = true;
  try {
    const data = await seckillManagerList(compactParams(query));
    rows.value = data?.list || [];
    total.value = Number(data?.total || 0);
  } finally {
    loading.value = false;
  }
}

function search() {
  query.page = 1;
  loadList();
}

function reset() {
  query.page = 1;
  query.limit = 20;
  query.name = '';
  query.status = '';
  loadList();
}

function openCreate() {
  Object.assign(form, {
    id: null,
    name: '',
    timeRange: [],
    sliderList: [],
    sort: 0,
    status: "'1'"
  });
  dialogVisible.value = true;
}

async function openEdit(row) {
  const data = await seckillManagerInfo({ id: row.id });
  Object.assign(form, {
    id: data.id,
    name: data.name || '',
    timeRange: parseTimeRange(data.time),
    sliderList: parseSlider(data.silderImgs),
    sort: data.sort || 0,
    status: data.status || "'1'"
  });
  dialogVisible.value = true;
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    const payload = {
      name: form.name,
      time: form.timeRange.join(','),
      img: form.sliderList[0]?.sattDir || '',
      silderImgs: JSON.stringify(form.sliderList),
      sort: form.sort,
      status: form.status
    };
    if (form.id) {
      await seckillManagerUpdate({ id: form.id }, payload);
    } else {
      await seckillManagerSave(payload);
    }
    ElMessage.success('操作成功');
    dialogVisible.value = false;
    loadList();
  } finally {
    saving.value = false;
  }
}

async function uploadSlider(option) {
  const body = new FormData();
  body.append('multipart', option.file);
  const result = await uploadImage(body, { model: 'activitystyle', pid: 0 });
  form.sliderList.push(createSliderItem(result));
  formRef.value?.validateField('sliderList');
  ElMessage.success('上传成功');
}

async function openAttachmentSelector() {
  attachmentDialogVisible.value = true;
  attachmentQuery.page = 1;
  attachmentQuery.pid = 0;
  if (!attachmentCategories.value.length) {
    attachmentCategories.value = await categoryTree({ type: CATEGORY_TYPE_ATTACHMENT, status: -1 });
  }
  await loadAttachments();
}

async function loadAttachments() {
  attachmentLoading.value = true;
  try {
    const data = await attachmentList({ ...attachmentQuery });
    attachmentRows.value = data?.list || [];
    attachmentTotal.value = Number(data?.total || 0);
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
  const src = item.sattDir || item.attDir || '';
  if (!src) return;
  if (form.sliderList.some((slider) => slider.sattDir === src)) {
    ElMessage.warning('该图片已选择');
    return;
  }
  form.sliderList.push(createSliderItem(item));
  formRef.value?.validateField('sliderList');
}

function isSelectedAttachment(item) {
  const src = item.sattDir || item.attDir || '';
  return form.sliderList.some((slider) => slider.sattDir === src);
}

function removeSlider(index) {
  form.sliderList.splice(index, 1);
  formRef.value?.validateField('sliderList');
}

function createSliderItem(source) {
  const src = source?.sattDir || source?.url || source?.src || source?.path || source?.attDir || '';
  return {
    attId: source?.attId || source?.id || Date.now(),
    name: source?.name || source?.attName || source?.fileName || '图片',
    attDir: source?.attDir || '',
    sattDir: src,
    attSize: String(source?.attSize || source?.fileSize || ''),
    attType: source?.attType || source?.extName || source?.type || '',
    pid: source?.pid || 0,
    imageType: source?.imageType || 1,
    isCk: false,
    isSelect: true,
    num: 1
  };
}

async function changeStatus(row, value) {
  const old = row.status;
  row.status = value;
  try {
    await seckillManagerStatus(row.id, { status: value });
    ElMessage.success('修改成功');
  } catch {
    row.status = old;
  }
}

async function remove(row) {
  await ElMessageBox.confirm('永久删除该配置', '提示', { type: 'warning' });
  await seckillManagerDelete({ id: row.id });
  ElMessage.success('删除成功');
  if (rows.value.length === 1 && query.page > 1) query.page -= 1;
  loadList();
}

function navigateToProducts(id) {
  const path = `/marketing/seckill/list/${id}`;
  window.history.pushState({}, '', path);
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path } }));
}

function addProduct(row) {
  const path = `/marketing/seckill/creatSeckill/creat/${row.id}`;
  window.history.pushState({}, '', path);
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path } }));
}

function sliderImages(row) {
  return parseSlider(row.silderImgs);
}

function parseSlider(value) {
  if (!value) return [];
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value;
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function parseTimeRange(value) {
  if (!value) return [];
  return String(value).split(',').map((item) => item.trim()).filter(Boolean);
}

function timeText(value) {
  return value ? String(value).split(',').join(' - ') : '-';
}

function assetUrl(value) {
  if (!value) return '';
  if (/^(https?:)?\/\//.test(value) || value.startsWith('data:') || value.startsWith('/')) return value;
  if (value.startsWith('crmebimage/')) return `/${value}`;
  return `/crmebimage/${value.replace(/^\/+/, '')}`;
}

function compactParams(source) {
  const params = {};
  Object.entries(source).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) params[key] = value;
  });
  return params;
}

function validateTimeRange(_rule, value, callback) {
  if (!value || value.length !== 2) {
    callback(new Error('请选择秒杀时间段'));
    return;
  }
  const start = Number(value[0].split(':')[0]);
  const end = Number(value[1].split(':')[0]);
  if (start >= end) {
    callback(new Error('请填写正确的时间范围'));
    return;
  }
  callback();
}

function validateSlider(_rule, value, callback) {
  if (!value || !value.length) {
    callback(new Error('请上传轮播图'));
    return;
  }
  callback();
}
</script>

<style scoped>
.seckill-config-manager .padding-add {
  padding: 20px 20px 0;
}

.selWidth {
  width: 180px;
}

.mt14 {
  margin-top: 14px;
}

.mt20 {
  margin-top: 20px;
}

.block {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.slider-list,
.form-slider-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.slider-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.slider-image {
  width: 36px;
  height: 36px;
  border-radius: 4px;
}

.form-slider-list {
  margin-top: 10px;
}

.form-slider-item {
  width: 92px;
}

.form-slider-item img {
  width: 72px;
  height: 72px;
  display: block;
  border-radius: 4px;
  object-fit: cover;
}

.form-control {
  width: 360px;
}

.attachment-picker {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 16px;
  min-height: 430px;
}

.attachment-tree {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px;
  overflow: auto;
}

.attachment-main {
  min-width: 0;
}

.attachment-picker-toolbar {
  margin-bottom: 10px;
}

.attachment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(116px, 1fr));
  gap: 12px;
  min-height: 330px;
}

.attachment-item {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  padding: 8px;
  text-align: left;
}

.attachment-item:hover,
.attachment-item.selected {
  border-color: #409eff;
  background: #ecf5ff;
}

.attachment-item :deep(.el-image) {
  width: 100%;
  height: 88px;
  display: block;
  background: #f5f7fa;
}

.attachment-item span {
  display: block;
  margin-top: 6px;
  overflow: hidden;
  color: #606266;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-empty {
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

.attachment-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
