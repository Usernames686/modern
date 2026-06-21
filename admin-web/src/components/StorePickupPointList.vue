<template>
  <div class="divBox">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item label="关键字：">
            <el-input
              v-model="query.keywords"
              placeholder="请输入提货点名称/电话"
              class="selWidth"
              size="small"
              clearable
              @keyup.enter="search"
            />
          </el-form-item>
          <el-button type="primary" size="small" @click="search">搜索</el-button>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <template #header>
        <div class="clearfix store-point-header">
          <el-tabs v-model="query.status" @tab-change="handleTabChange">
            <el-tab-pane :label="`显示中的提货点(${headerCount.show ?? 0})`" name="1" />
            <el-tab-pane :label="`隐藏中的提货点(${headerCount.hide ?? 0})`" name="0" />
            <el-tab-pane :label="`回收站的提货点(${headerCount.recycle ?? 0})`" name="2" />
          </el-tabs>
          <el-button type="primary" size="small" @click="openCreate">添加提货点</el-button>
        </div>
      </template>

      <el-table v-loading="loading" size="small" :data="tableData" border>
        <el-table-column prop="id" label="ID" min-width="80" />
        <el-table-column prop="image" label="提货点图片" min-width="100">
          <template #default="{ row }">
            <el-image
              v-if="row.image"
              style="width: 36px; height: 36px"
              :src="assetUrl(row.image)"
              :preview-src-list="[assetUrl(row.image)]"
              fit="cover"
            >
              <template #error>
                <div class="store-image-error">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="提货点名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="phone" label="提货点电话" min-width="120" />
        <el-table-column prop="detailedAddress" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="dayTime" label="营业时间" min-width="180" />
        <el-table-column prop="isShow" label="是否显示" min-width="130">
          <template #default="{ row }">
            <el-switch
              v-if="query.status !== '2'"
              v-model="row.isShow"
              active-text="显示"
              inactive-text="隐藏"
              @change="handleStatusChange(row)"
            />
            <span v-else>{{ row.isShow ? '显示' : '隐藏' }}</span>
          </template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row.id)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button v-if="query.status === '2'" link type="primary" @click="handleRecovery(row)">恢复</el-button>
            <template v-if="query.status === '2'">
              <el-divider direction="vertical" />
            </template>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="block">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.limit"
          :page-sizes="[20, 40, 60, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '修改提货点' : '添加提货点'"
      width="750px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        v-loading="formLoading"
        :model="form"
        :rules="rules"
        label-width="110px"
        class="demo-ruleForm"
        @submit.prevent
      >
        <el-form-item label="提货点名称：" prop="name">
          <el-input v-model="form.name" maxlength="40" placeholder="请输入提货点名称" class="dialogWidth" />
        </el-form-item>
        <el-form-item label="提货点简介：">
          <el-input v-model="form.introduction" maxlength="100" placeholder="请输入提货点简介" class="dialogWidth" />
        </el-form-item>
        <el-form-item label="提货点手机号：" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入提货点手机号" class="dialogWidth" />
        </el-form-item>
        <el-form-item label="提货点地址：" prop="address">
          <el-input v-model="form.address" placeholder="请输入省市区，多个层级请用英文逗号分隔" class="dialogWidth" />
        </el-form-item>
        <el-form-item label="详细地址：" prop="detailedAddress">
          <el-input v-model="form.detailedAddress" placeholder="请输入详细地址" class="dialogWidth" />
        </el-form-item>
        <el-form-item label="提货点营业：" prop="dayTime">
          <el-time-picker
            v-model="dayTimeRange"
            is-range
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            placeholder="请选择时间营业时间"
            value-format="HH:mm:ss"
            @change="handleTimeChange"
          />
        </el-form-item>
        <el-form-item label="提货点logo：" prop="image" required>
          <div class="store-logo-field">
            <el-upload
              class="store-logo-uploader"
              :show-file-list="false"
              :http-request="handleLogoUpload"
              accept="image/*"
            >
              <div class="upLoadPicBox">
                <div v-if="form.image" class="pictrue"><img :src="assetUrl(form.image)" alt="" /></div>
                <div v-else class="upLoad">
                  <el-icon><Camera /></el-icon>
                </div>
              </div>
            </el-upload>
            <div class="store-logo-actions">
              <el-button type="primary" plain @click="openAttachmentSelector">选择素材</el-button>
              <el-button v-if="form.image" @click="clearLogo">清除</el-button>
              <span class="muted">可上传新图，也可从老素材库选择已有图片。</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="经纬度：" prop="latitude">
          <el-input v-model="form.latitude" placeholder="请输入经纬度，例如：108.948024,34.263161" class="dialogWidth" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="formLoading" @click="submitForm">
          {{ form.id ? '修改' : '提交' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="attachmentDialogVisible" title="选择图片" width="860px" append-to-body destroy-on-close>
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
          <div v-loading="attachmentLoading" class="attachment-grid">
            <button
              v-for="item in attachmentRows"
              :key="item.attId || item.id"
              type="button"
              class="attachment-item"
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
import { ElMessage, ElMessageBox } from 'element-plus';
import { Camera, Picture } from '@element-plus/icons-vue';
import {
  attachmentList,
  categoryTree,
  storePointCompletelyDelete,
  storePointCount,
  storePointDelete,
  storePointInfo,
  storePointList,
  storePointRecovery,
  storePointSave,
  storePointUpdate,
  storePointUpdateStatus,
  uploadImage
} from '../api';

const CATEGORY_TYPE_ATTACHMENT = 2;
const IMAGE_TYPES = 'jpg,jpeg,gif,png,bmp,PNG,JPG';

const loading = ref(false);
const formLoading = ref(false);
const attachmentLoading = ref(false);
const dialogVisible = ref(false);
const attachmentDialogVisible = ref(false);
const tableData = ref([]);
const attachmentCategories = ref([]);
const attachmentRows = ref([]);
const total = ref(0);
const attachmentTotal = ref(0);
const formRef = ref();
const dayTimeRange = ref(['', '']);
const headerCount = reactive({
  show: 0,
  hide: 0,
  recycle: 0
});

const query = reactive({
  page: 1,
  limit: 20,
  status: '1',
  keywords: ''
});

const form = reactive(defaultForm());
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
  name: [{ required: true, message: '请输入提货点名称', trigger: 'blur' }],
  phone: [
    { required: true, message: '请填写手机号', trigger: 'blur' },
    { pattern: /^1[3456789]\d{9}$/, message: '手机号格式不正确!', trigger: 'blur' }
  ],
  address: [{ required: true, message: '请选择提货点地址', trigger: 'blur' }],
  detailedAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  dayTime: [{ required: true, message: '请选择提货点营业时间', trigger: 'change' }],
  image: [{ required: true, message: '请上传提货点logo', trigger: 'change' }],
  latitude: [{ required: true, message: '请选择经纬度', trigger: 'blur' }]
};

function defaultForm() {
  return {
    id: 0,
    name: '',
    introduction: '',
    phone: '',
    address: '',
    detailedAddress: '',
    dayTime: '',
    image: '',
    latitude: '',
    longitude: '',
    validTime: ''
  };
}

async function loadCount() {
  const data = await storePointCount({ keywords: query.keywords });
  Object.assign(headerCount, data || {});
}

async function loadList() {
  loading.value = true;
  try {
    const data = await storePointList({ ...query });
    tableData.value = data?.list || [];
    total.value = data?.total || 0;
  } finally {
    loading.value = false;
  }
}

async function search() {
  query.page = 1;
  await Promise.all([loadList(), loadCount()]);
}

function handleTabChange() {
  query.page = 1;
  loadList();
}

async function handleStatusChange(row) {
  try {
    await storePointUpdateStatus({ id: row.id, status: row.isShow });
    ElMessage.success('操作成功');
    await Promise.all([loadList(), loadCount()]);
  } catch {
    row.isShow = !row.isShow;
  }
}

async function handleRecovery(row) {
  try {
    await ElMessageBox.confirm('恢复提货点吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch {
    return;
  }
  await storePointRecovery({ id: row.id });
  ElMessage.success('恢复成功');
  await Promise.all([loadList(), loadCount()]);
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('删除提货点吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch {
    return;
  }
  if (query.status === '2') {
    await storePointCompletelyDelete({ id: row.id });
  } else {
    await storePointDelete({ id: row.id });
  }
  ElMessage.success('删除成功');
  await Promise.all([loadList(), loadCount()]);
}

function openCreate() {
  Object.assign(form, defaultForm());
  dayTimeRange.value = ['', ''];
  dialogVisible.value = true;
}

async function openEdit(id) {
  dialogVisible.value = true;
  formLoading.value = true;
  try {
    const data = await storePointInfo({ id });
    Object.assign(form, defaultForm(), data || {});
    dayTimeRange.value = form.dayTime ? form.dayTime.split(',') : ['', ''];
  } finally {
    formLoading.value = false;
  }
}

function handleTimeChange(value) {
  form.dayTime = Array.isArray(value) ? value.join(',') : '';
}

async function handleLogoUpload(options) {
  const payload = new FormData();
  payload.append('multipart', options.file);
  const data = await uploadImage(payload, { model: 'store', pid: 0 });
  form.image = data?.sattDir || data?.src || data?.url || data?.path || '';
  formRef.value?.validateField?.('image');
  options.onSuccess?.(data);
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
  form.image = item.sattDir || item.attDir || '';
  attachmentDialogVisible.value = false;
  formRef.value?.validateField?.('image');
}

function clearLogo() {
  form.image = '';
  formRef.value?.validateField?.('image');
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  formLoading.value = true;
  try {
    const payload = { ...form };
    delete payload.id;
    if (form.id) {
      await storePointUpdate(payload, form.id);
      ElMessage.success('编辑成功');
    } else {
      await storePointSave(payload);
      ElMessage.success('提交成功');
    }
    dialogVisible.value = false;
    await Promise.all([loadList(), loadCount()]);
  } finally {
    formLoading.value = false;
  }
}

function resetForm() {
  formRef.value?.clearValidate();
  Object.assign(form, defaultForm());
  dayTimeRange.value = ['', ''];
}

function assetUrl(value) {
  if (!value) return '';
  if (/^https?:\/\//.test(value) || value.startsWith('data:') || value.startsWith('/')) return value;
  return `/${value}`;
}

onMounted(async () => {
  await Promise.all([loadCount(), loadList()]);
});
</script>

<style scoped>
.store-logo-field {
  display: flex;
  gap: 14px;
  align-items: center;
  flex-wrap: wrap;
}

.store-logo-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.muted {
  color: #909399;
  font-size: 12px;
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

.attachment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(116px, 1fr));
  gap: 12px;
  min-height: 360px;
}

.attachment-item {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  padding: 8px;
  text-align: left;
}

.attachment-item:hover {
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
