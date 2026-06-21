<template>
  <div class="divBox">
    <el-card v-if="!routeMode" shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item label="文章分类：">
            <el-select v-model="query.cid" placeholder="请选择" class="selWidth" size="small" clearable>
              <el-option label="全部" value="" />
              <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="String(item.id)" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键词：">
            <el-input
              v-model="query.keywords"
              placeholder="请输入文章标题/作者/简介"
              class="selWidth"
              size="small"
              clearable
              @keyup.enter="search"
            />
          </el-form-item>
          <el-button type="primary" size="small" @click="search">搜索</el-button>
          <el-button size="small" @click="reset">重置</el-button>
        </el-form>
      </div>
    </el-card>

    <el-card v-if="!routeMode" class="box-card mt14">
      <template #header>
        <div class="clearfix">
          <el-button type="primary" size="small" @click="openCreate">添加文章</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" size="small" class="table">
        <el-table-column prop="id" label="ID" min-width="70" />
        <el-table-column label="图片" min-width="90">
          <template #default="{ row }">
            <el-image
              v-if="row.imageInput"
              class="article-cover"
              :src="assetUrl(row.imageInput)"
              :preview-src-list="[assetUrl(row.imageInput)]"
              preview-teleported
              fit="cover"
            >
              <template #error>
                <div class="store-image-error">图</div>
              </template>
            </el-image>
            <div v-else class="article-cover article-cover-empty">无</div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="文章分类" min-width="110">
          <template #default="{ row }">{{ categoryName(row.cid) }}</template>
        </el-table-column>
        <el-table-column prop="visit" label="浏览量" min-width="90" />
        <el-table-column prop="author" label="作者" min-width="100" show-overflow-tooltip />
        <el-table-column prop="synopsis" label="文章简介" min-width="220" show-overflow-tooltip />
        <el-table-column label="更新时间" min-width="150">
          <template #default="{ row }">{{ dateText(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.limit"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialog.visible"
      :title="form.id ? '编辑文章' : '添加文章'"
      :width="routeMode ? '900px' : '760px'"
      destroy-on-close
      :close-on-click-modal="false"
      :show-close="!routeMode"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" v-loading="saving">
        <el-form-item label="标题：" prop="title">
          <el-input v-model="form.title" maxlength="200" placeholder="请填写文章标题" />
        </el-form-item>
        <el-form-item label="作者：" prop="author">
          <el-input v-model="form.author" maxlength="50" placeholder="请填写文章作者" />
        </el-form-item>
        <el-form-item label="文章分类：" prop="cid">
          <el-select v-model="form.cid" placeholder="请选择文章分类" class="form-control">
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="String(item.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="图文封面：" prop="imageInput">
          <div class="cover-field">
            <el-upload action="#" :show-file-list="false" :http-request="uploadCover" accept="image/*">
              <div class="upLoadPicBox">
                <div v-if="form.imageInput" class="pictrue"><img :src="assetUrl(form.imageInput)" alt="" /></div>
                <div v-else class="upLoad">
                  <el-icon><Camera /></el-icon>
                </div>
              </div>
            </el-upload>
            <div class="cover-actions">
              <el-button type="primary" plain @click="openAttachmentSelector">选择素材</el-button>
              <el-button v-if="form.imageInput" @click="clearCover">清除</el-button>
              <span class="muted">可上传新图，也可从老素材库选择已有图片。</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="文章简介：" prop="synopsis">
          <el-input
            v-model="form.synopsis"
            type="textarea"
            maxlength="200"
            show-word-limit
            :rows="3"
            placeholder="请填写文章简介"
          />
        </el-form-item>
        <el-form-item label="文章内容：" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            placeholder="请填写文章内容，支持保留原 HTML 内容"
          />
        </el-form-item>
        <el-form-item label="是否Banner：" prop="isBanner">
          <el-switch v-model="form.isBanner" active-text="是" inactive-text="否" />
        </el-form-item>
        <el-form-item label="是否热门：" prop="isHot">
          <el-switch v-model="form.isHot" active-text="是" inactive-text="否" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelForm">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">{{ routeMode ? '保存' : '确定' }}</el-button>
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
  articleDelete,
  articleInfo,
  articleList,
  articleSave,
  articleUpdate,
  categoryTree,
  uploadImage
} from '../api';

const CATEGORY_TYPE_ARTICLE = 3;
const CATEGORY_TYPE_ATTACHMENT = 2;
const IMAGE_TYPES = 'jpg,jpeg,gif,png,bmp,PNG,JPG';

const props = defineProps({
  mode: {
    type: String,
    default: 'list'
  },
  articleId: {
    type: [String, Number],
    default: ''
  }
});

const loading = ref(false);
const saving = ref(false);
const attachmentLoading = ref(false);
const tableData = ref([]);
const total = ref(0);
const categoryOptions = ref([]);
const attachmentCategories = ref([]);
const attachmentRows = ref([]);
const attachmentTotal = ref(0);
const formRef = ref();

const query = reactive({
  cid: '',
  keywords: '',
  page: 1,
  limit: 20
});

const dialog = reactive({
  visible: false
});
const attachmentDialogVisible = ref(false);

const form = reactive(defaultForm());
const routeMode = computed(() => props.mode === 'form');
const attachmentQuery = reactive({
  page: 1,
  limit: 18,
  pid: 0,
  attType: IMAGE_TYPES
});

const categoryMap = computed(() => {
  const map = new Map();
  categoryOptions.value.forEach((item) => map.set(String(item.id), item.name));
  return map;
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
  title: [{ required: true, message: '请填写文章标题', trigger: 'blur' }],
  author: [{ required: true, message: '请填写文章作者', trigger: 'blur' }],
  cid: [{ required: true, message: '请选择分类', trigger: 'change' }],
  imageInput: [{ required: true, message: '请上传文章图片', trigger: 'change' }],
  synopsis: [{ required: true, message: '请填写文章简介', trigger: 'blur' }],
  content: [{ required: true, message: '请填写文章内容', trigger: 'blur' }]
};

onMounted(async () => {
  await loadCategories();
  if (routeMode.value) {
    if (props.articleId) {
      await openEdit({ id: props.articleId });
    } else {
      openCreate();
    }
  } else {
    await loadList();
  }
});

async function loadCategories() {
  const data = await categoryTree({ type: CATEGORY_TYPE_ARTICLE, status: -1 });
  categoryOptions.value = flattenCategories(data || []);
}

async function loadList() {
  loading.value = true;
  try {
    const data = await articleList({
      cid: query.cid || undefined,
      keywords: query.keywords || undefined,
      page: query.page,
      limit: query.limit
    });
    tableData.value = data?.list || [];
    total.value = data?.total || 0;
  } finally {
    loading.value = false;
  }
}

function search() {
  query.page = 1;
  loadList();
}

function reset() {
  query.cid = '';
  query.keywords = '';
  query.page = 1;
  loadList();
}

function openCreate() {
  Object.assign(form, defaultForm());
  dialog.visible = true;
}

async function openEdit(row) {
  saving.value = true;
  try {
    const data = await articleInfo({ id: row.id });
    Object.assign(form, {
      id: data.id,
      cid: data.cid || '',
      title: data.title || '',
      author: data.author || '',
      imageInput: data.imageInput || '',
      synopsis: data.synopsis || '',
      content: data.content || '',
      isBanner: Boolean(data.isBanner),
      isHot: Boolean(data.isHot)
    });
    dialog.visible = true;
  } finally {
    saving.value = false;
  }
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    const data = {
      cid: form.cid,
      title: form.title,
      author: form.author,
      imageInput: form.imageInput,
      synopsis: form.synopsis,
      shareTitle: form.title,
      shareSynopsis: form.synopsis,
      content: form.content,
      isBanner: form.isBanner,
      isHot: form.isHot
    };
    if (form.id) {
      await articleUpdate({ id: form.id }, data);
      ElMessage.success('编辑成功');
    } else {
      await articleSave(data);
      ElMessage.success('添加成功');
    }
    if (routeMode.value) {
      goBack();
    } else {
      dialog.visible = false;
      await loadList();
    }
  } finally {
    saving.value = false;
  }
}

function cancelForm() {
  if (routeMode.value) {
    goBack();
  } else {
    dialog.visible = false;
  }
}

async function remove(row) {
  await ElMessageBox.confirm('删除当前文章?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  });
  await articleDelete({ id: row.id });
  ElMessage.success('删除成功');
  await loadList();
}

async function uploadCover(options) {
  const data = new FormData();
  data.append('file', options.file);
  try {
    const result = await uploadImage(data, { model: 'article' });
    form.imageInput = result?.sattDir || result?.src || result?.url || result?.path || '';
    if (!form.imageInput) {
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
  form.imageInput = item.sattDir || item.attDir || '';
  attachmentDialogVisible.value = false;
  formRef.value?.validateField?.('imageInput').catch(() => {});
}

function clearCover() {
  form.imageInput = '';
  formRef.value?.validateField?.('imageInput').catch(() => {});
}

function flattenCategories(list, prefix = '') {
  return list.flatMap((item) => {
    const current = {
      ...item,
      name: `${prefix}${item.name}`
    };
    const children = item.child || item.children || [];
    return [current, ...flattenCategories(children, `${prefix}${item.name} / `)];
  });
}

function categoryName(value) {
  return categoryMap.value.get(String(value)) || value || '-';
}

function dateText(value) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
}

function assetUrl(value) {
  if (!value) return '';
  if (/^https?:\/\//.test(value) || value.startsWith('data:')) return value;
  return value.startsWith('/') ? value : `/${value}`;
}

function defaultForm() {
  return {
    id: null,
    cid: '',
    title: '',
    author: '',
    imageInput: '',
    synopsis: '',
    content: '',
    isBanner: false,
    isHot: false
  };
}

function goBack() {
  window.history.pushState({}, '', '/content/articleManager');
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path: '/content/articleManager' } }));
}
</script>

<style scoped>
.article-cover {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  overflow: hidden;
}

.article-cover-empty {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
}

.form-control {
  width: 100%;
}

.cover-field {
  display: flex;
  gap: 14px;
  align-items: center;
  flex-wrap: wrap;
}

.cover-actions {
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
