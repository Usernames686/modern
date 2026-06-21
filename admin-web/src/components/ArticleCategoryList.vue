<template>
  <div class="divBox">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item label="分类状态：">
            <el-select v-model="query.status" placeholder="状态" class="selWidth" size="small" @change="searchList">
              <el-option label="全部" :value="-1" />
              <el-option label="显示" :value="1" />
              <el-option label="不显示" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="分类名称：">
            <el-input
              v-model="query.name"
              placeholder="请输入名称"
              class="selWidth"
              size="small"
              clearable
              @keyup.enter="searchList"
            />
          </el-form-item>
          <el-button type="primary" size="small" @click="searchList">搜索</el-button>
          <el-button size="small" @click="reset">重置</el-button>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <template #header>
        <div class="clearfix">
          <el-button type="primary" size="small" @click="openCreate">添加文章分类</el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="treeList"
        class="table"
        row-key="id"
        highlight-current-row
        size="small"
        :tree-props="{ children: 'child', hasChildren: 'hasChildren' }"
      >
        <el-table-column prop="name" label="名称" min-width="240">
          <template #default="{ row }">{{ row.name }} | {{ row.id }}</template>
        </el-table-column>
        <el-table-column label="类型" min-width="150">
          <template #default="{ row }">{{ categoryTypeText(row.type) }}</template>
        </el-table-column>
        <el-table-column label="分类图标" min-width="100">
          <template #default="{ row }">
            <el-image
              v-if="row.extra"
              class="category-icon"
              :src="imageUrl(row.extra)"
              :preview-src-list="[imageUrl(row.extra)]"
              fit="cover"
              preview-teleported
            >
              <template #error>
                <div class="store-image-error">图</div>
              </template>
            </el-image>
            <div v-else class="category-icon category-icon-empty">无</div>
          </template>
        </el-table-column>
        <el-table-column label="排序" prop="sort" min-width="150" />
        <el-table-column label="状态" min-width="150">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-text="显示"
              inactive-text="隐藏"
              :active-value="true"
              :inactive-value="false"
              @change="toggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialog.visible"
      :title="dialog.mode === 'create' ? '添加文章分类' : '编辑文章分类'"
      width="540px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px">
        <el-form-item label="分类名称：" prop="name">
          <el-input v-model="form.name" maxlength="20" placeholder="分类名称" />
        </el-form-item>
        <el-form-item label="分类图标(180*180)：">
          <div class="category-icon-field">
            <el-upload
              class="category-upload"
              action="#"
              :show-file-list="false"
              :http-request="uploadCategoryIcon"
              accept="image/*"
            >
              <div class="upLoadPicBox">
                <div v-if="form.extra" class="pictrue">
                  <img :src="imageUrl(form.extra)" alt="" />
                </div>
                <div v-else class="upLoad">
                  <el-icon><Camera /></el-icon>
                </div>
              </div>
            </el-upload>
            <div class="category-icon-actions">
              <el-button type="primary" plain @click="openAttachmentSelector">选择素材</el-button>
              <el-button v-if="form.extra" @click="clearCategoryIcon">清除</el-button>
              <span class="muted">可上传新图，也可从老素材库选择已有图片。</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="排序：" prop="sort">
          <el-input-number v-model="form.sort" controls-position="right" :min="0" />
        </el-form-item>
        <el-form-item label="状态：" prop="status">
          <el-switch
            v-model="form.status"
            active-text="显示"
            inactive-text="隐藏"
            :active-value="true"
            :inactive-value="false"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
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
              <el-image :src="imageUrl(item.sattDir || item.attDir)" fit="cover" />
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
  categoryDelete,
  categorySave,
  categoryTree,
  categoryUpdate,
  categoryUpdateStatus,
  uploadImage
} from '../api';

const CATEGORY_TYPE_ARTICLE = 3;
const CATEGORY_TYPE_ATTACHMENT = 2;
const IMAGE_TYPES = 'jpg,jpeg,gif,png,bmp,PNG,JPG';

const loading = ref(false);
const saving = ref(false);
const attachmentLoading = ref(false);
const formRef = ref();
const treeList = ref([]);
const attachmentCategories = ref([]);
const attachmentRows = ref([]);
const attachmentTotal = ref(0);
const attachmentDialogVisible = ref(false);

const query = reactive({
  type: CATEGORY_TYPE_ARTICLE,
  status: -1,
  name: ''
});

const dialog = reactive({
  visible: false,
  mode: 'create'
});

const form = reactive(defaultForm());
const attachmentQuery = reactive({
  page: 1,
  limit: 18,
  pid: 0,
  attType: IMAGE_TYPES
});

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: ['blur', 'change'] }],
  sort: [{ required: true, message: '排序数字为空', trigger: ['blur', 'change'] }]
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

onMounted(loadList);

async function loadList() {
  loading.value = true;
  try {
    const data = await categoryTree({
      type: query.type,
      status: query.status,
      name: query.name || undefined
    });
    treeList.value = data || [];
  } finally {
    loading.value = false;
  }
}

function searchList() {
  loadList();
}

function reset() {
  query.status = -1;
  query.name = '';
  loadList();
}

function openCreate() {
  Object.assign(form, defaultForm());
  dialog.mode = 'create';
  dialog.visible = true;
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    extra: row.extra || '',
    name: row.name || '',
    pid: 0,
    sort: row.sort || 0,
    status: Boolean(row.status),
    type: CATEGORY_TYPE_ARTICLE,
    url: row.url || ''
  });
  dialog.mode = 'edit';
  dialog.visible = true;
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    const payload = {
      id: form.id || undefined,
      extra: form.extra || '',
      name: form.name,
      pid: 0,
      sort: form.sort,
      status: form.status,
      type: CATEGORY_TYPE_ARTICLE,
      url: form.url || ''
    };
    if (dialog.mode === 'create') {
      await categorySave(payload);
      ElMessage.success('创建目录成功');
    } else {
      await categoryUpdate(payload);
      ElMessage.success('更新目录成功');
    }
    dialog.visible = false;
    await loadList();
  } finally {
    saving.value = false;
  }
}

async function toggleStatus(row) {
  try {
    await categoryUpdateStatus(row.id);
    ElMessage.success('修改成功');
    await loadList();
  } catch (error) {
    row.status = !row.status;
  }
}

async function remove(row) {
  await ElMessageBox.confirm('删除当前数据?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  });
  await categoryDelete({ id: row.id });
  ElMessage.success('删除成功');
  await loadList();
}

async function uploadCategoryIcon(options) {
  const data = new FormData();
  data.append('file', options.file);
  try {
    const result = await uploadImage(data, { model: 'store' });
    form.extra = result?.sattDir || result?.src || result?.url || result?.path || '';
    if (!form.extra) {
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
  form.extra = item.sattDir || item.attDir || '';
  attachmentDialogVisible.value = false;
}

function clearCategoryIcon() {
  form.extra = '';
}

function defaultForm() {
  return {
    id: 0,
    extra: '',
    name: '',
    pid: 0,
    sort: 0,
    status: true,
    type: CATEGORY_TYPE_ARTICLE,
    url: ''
  };
}

function categoryTypeText(value) {
  return {
    1: '产品分类',
    2: '附件分类',
    3: '文章分类',
    4: '设置分类',
    5: '菜单分类',
    6: '配置分类',
    7: '秒杀配置'
  }[Number(value)] || '-';
}

function imageUrl(value) {
  if (!value) return '';
  if (/^https?:\/\//.test(value) || value.startsWith('data:')) return value;
  return value.startsWith('/') ? value : `/${value}`;
}
</script>

<style scoped>
.category-icon-field {
  display: flex;
  gap: 14px;
  align-items: center;
  flex-wrap: wrap;
}

.category-icon-actions {
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
