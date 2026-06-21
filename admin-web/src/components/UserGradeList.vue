<template>
  <div class="divBox">
    <el-card class="box-card">
      <template #header>
        <div class="clearfix">
          <el-button type="primary" class="mr10" size="small" @click="add">添加用户等级</el-button>
        </div>
      </template>

      <el-table v-loading="listLoading" :data="tableData" style="width: 100%" size="small">
        <el-table-column prop="id" label="ID" min-width="50" />
        <el-table-column label="等级图标" min-width="80">
          <template #default="{ row }">
            <div class="demo-image__preview">
              <el-image
                style="width: 36px; height: 36px"
                :src="assetUrl(row.icon)"
                :preview-src-list="[assetUrl(row.icon)]"
                preview-teleported
                fit="cover"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="等级名称" min-width="100" />
        <el-table-column prop="experience" label="经验" min-width="100" />
        <el-table-column prop="discount" label="享受折扣(%)" min-width="100" />
        <el-table-column label="状态" min-width="130">
          <template #default="{ row }">
            <el-switch
              :model-value="Boolean(row.isShow)"
              active-text="开启"
              inactive-text="关闭"
              :loading="statusLoadingId === row.id"
              @click="onchangeIsShow(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="edit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="`${form.id ? '编辑用户等级' : '添加用户等级'}`"
      width="540px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="demo-ruleForm" v-loading="saving">
        <el-form-item label="等级名称：" prop="name">
          <el-input v-model="form.name" placeholder="请输入等级名称" />
        </el-form-item>
        <el-form-item label="等级：" prop="grade">
          <el-input v-model.number="form.grade" placeholder="请输入等级" />
        </el-form-item>
        <el-form-item label="享受折扣(%)：" prop="discount">
          <el-input-number
            v-model="form.discount"
            controls-position="right"
            :min="1"
            :max="100"
            step-strictly
            placeholder="请输入享受折扣"
          />
        </el-form-item>
        <el-form-item label="经验：" prop="experience">
          <el-input-number
            v-model="form.experience"
            controls-position="right"
            :min="0"
            step-strictly
            placeholder="请输入经验"
          />
        </el-form-item>
        <el-form-item label="图标：" prop="icon">
          <div class="grade-icon-field">
            <el-upload action="#" :show-file-list="false" :http-request="uploadIcon" accept="image/*">
              <div class="upLoadPicBox">
                <div v-if="form.icon" class="pictrue"><img :src="assetUrl(form.icon)" /></div>
                <div v-else class="upLoad">
                  <el-icon><Camera /></el-icon>
                </div>
              </div>
            </el-upload>
            <div class="grade-icon-actions">
              <el-button type="primary" plain @click="openAttachmentSelector">选择素材</el-button>
              <el-button v-if="form.icon" @click="clearIcon">清除</el-button>
              <span class="muted">可上传新图，也可从老素材库选择已有图片。</span>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">确定</el-button>
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
import { Camera } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  attachmentList,
  categoryTree,
  uploadImage,
  userLevelDelete,
  userLevelSave,
  userLevelUpdate,
  userLevelUse,
  userLevels
} from '../api';

const CATEGORY_TYPE_ATTACHMENT = 2;
const IMAGE_TYPES = 'jpg,jpeg,gif,png,bmp,PNG,JPG';

const listLoading = ref(false);
const saving = ref(false);
const attachmentLoading = ref(false);
const dialogVisible = ref(false);
const attachmentDialogVisible = ref(false);
const tableData = ref([]);
const attachmentCategories = ref([]);
const attachmentRows = ref([]);
const attachmentTotal = ref(0);
const formRef = ref();
const statusLoadingId = ref(null);

const form = reactive(defaultForm());
const attachmentQuery = reactive({
  page: 1,
  limit: 18,
  pid: 0,
  attType: IMAGE_TYPES
});
const rules = {
  name: [{ required: true, message: '请输入等级名称', trigger: 'blur' }],
  grade: [
    { required: true, message: '请输入等级', trigger: 'blur' },
    { type: 'number', message: '等级必须为数字值', trigger: 'blur' }
  ],
  discount: [{ required: true, message: '请输入折扣', trigger: 'blur' }],
  experience: [{ required: true, message: '请输入经验', trigger: 'blur' }],
  icon: [{ required: true, message: '请上传图标', trigger: 'change' }]
};
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

onMounted(getList);

async function getList() {
  listLoading.value = true;
  try {
    tableData.value = await userLevels();
  } finally {
    listLoading.value = false;
  }
}

function add() {
  Object.assign(form, defaultForm());
  dialogVisible.value = true;
}

function edit(row) {
  Object.assign(form, {
    id: row.id,
    name: row.name || '',
    grade: row.grade ?? 1,
    discount: row.discount ?? 100,
    experience: row.experience ?? 0,
    icon: row.icon || '',
    isShow: Boolean(row.isShow)
  });
  dialogVisible.value = true;
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    const data = { ...form };
    if (form.id) {
      await userLevelUpdate(form.id, data);
      ElMessage.success('编辑成功');
    } else {
      await userLevelSave(data);
      ElMessage.success('添加成功');
    }
    dialogVisible.value = false;
    await getList();
  } finally {
    saving.value = false;
  }
}

async function handleDelete(id) {
  await ElMessageBox.confirm('删除吗？删除会导致对应用户等级数据清空，请谨慎操作！', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  });
  await userLevelDelete(id);
  ElMessage.success('删除成功');
  await getList();
}

async function onchangeIsShow(row) {
  const nextShow = !Boolean(row.isShow);
  if (!nextShow) {
    await ElMessageBox.confirm('该操作会导致对应用户等级隐藏，请谨慎操作', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
  }
  statusLoadingId.value = row.id;
  try {
    await userLevelUse({ id: row.id, isShow: nextShow });
    ElMessage.success('修改成功');
    await getList();
  } finally {
    statusLoadingId.value = null;
  }
}

async function uploadIcon(options) {
  const data = new FormData();
  data.append('file', options.file);
  try {
    const result = await uploadImage(data, { model: 'user' });
    form.icon = result?.sattDir || result?.src || result?.url || result?.path || '';
    form.isShow = false;
    if (!form.icon) {
      ElMessage.warning('上传成功，但未返回图片地址');
    }
  } catch (error) {
    options.onError?.(error);
  }
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
  form.icon = item.sattDir || item.attDir || '';
  form.isShow = false;
  attachmentDialogVisible.value = false;
  formRef.value?.validateField?.('icon').catch(() => {});
}

function clearIcon() {
  form.icon = '';
  formRef.value?.validateField?.('icon').catch(() => {});
}

function resetForm() {
  formRef.value?.clearValidate();
}

function defaultForm() {
  return {
    id: null,
    name: '',
    grade: 1,
    discount: 100,
    experience: 0,
    icon: '',
    isShow: false
  };
}

function assetUrl(value) {
  if (!value) return '';
  if (/^https?:\/\//.test(value) || value.startsWith('data:')) return value;
  return value.startsWith('/') ? value : `/${value}`;
}
</script>

<style scoped>
.grade-icon-field {
  display: flex;
  gap: 14px;
  align-items: center;
  flex-wrap: wrap;
}

.grade-icon-actions {
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
