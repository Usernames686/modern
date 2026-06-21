<template>
  <div class="divBox">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form :inline="true" :model="query">
          <el-form-item label="时间选择：">
            <el-date-picker
              v-model="timeVal"
              value-format="YYYY-MM-DD"
              format="YYYY-MM-DD"
              size="small"
              type="daterange"
              placement="bottom-end"
              class="selWidth"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              @change="onchangeTime"
            />
          </el-form-item>
          <el-form-item label="评价状态：" class="mr10">
            <el-select
              v-model="query.isReply"
              placeholder="请选择评价状态"
              size="small"
              class="selWidth"
              clearable
              @change="searchList"
            >
              <el-option label="已回复" value="1" />
              <el-option label="未回复" value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="商品搜索：" class="mr10">
            <el-input v-model="query.productSearch" placeholder="请输入商品名称" class="selWidth" size="small" clearable />
          </el-form-item>
          <el-form-item label="用户名称：">
            <el-input v-model="query.nickname" placeholder="请输入用户名称" class="selWidth" size="small" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small" @click="searchList">搜索</el-button>
            <el-button size="small" @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <template #header>
        <div class="clearfix">
          <el-button type="primary" size="small" @click="openCreate">添加虚拟评论</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" style="width: 100%" size="small">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="商品信息" min-width="400" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="demo-image__preview acea-row row-middle" v-if="row.storeProduct">
              <el-image
                style="width: 30px; height: 30px"
                :src="assetUrl(row.storeProduct.image)"
                :preview-src-list="[assetUrl(row.storeProduct.image)]"
                class="mr10"
                fit="cover"
                preview-teleported
              />
              <div class="info line1">{{ row.storeProduct.storeName }}</div>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="用户名称" min-width="100" />
        <el-table-column prop="productScore" label="商品评分" min-width="90" />
        <el-table-column prop="serviceScore" label="服务评分" min-width="90" />
        <el-table-column label="评价内容" min-width="230">
          <template #default="{ row }">
            <div class="mb5 content_font">{{ row.comment || '-' }}</div>
            <template v-if="row.pics?.length">
              <div class="demo-image__preview comment-pics">
                <el-image
                  v-for="(item, index) in row.pics"
                  :key="index"
                  :src="assetUrl(item)"
                  :preview-src-list="row.pics.map(assetUrl)"
                  class="mr5 comment-thumb"
                  fit="cover"
                  preview-teleported
                />
              </div>
            </template>
          </template>
        </el-table-column>
        <el-table-column prop="merchantReplyContent" label="回复内容" min-width="250">
          <template #default="{ row }">{{ row.merchantReplyContent || '-' }}</template>
        </el-table-column>
        <el-table-column label="评价时间" min-width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="reply(row.id)">回复</el-button>
            <el-divider direction="vertical" />
            <el-button link type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="block">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.limit"
          :page-sizes="[20, 40, 60, 80]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <el-dialog v-model="createDialogVisible" title="添加评论" width="760px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" v-loading="saving">
        <el-form-item label="商品：" prop="productId">
          <div class="upLoadPicBox" @click="openProductDialog">
            <div v-if="form.productId" class="pictrue"><img :src="assetUrl(selectedProduct.image)" /></div>
            <div v-else class="upLoad">
              <el-icon><Camera /></el-icon>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="用户名称：" prop="nickname">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="评价文字：" prop="comment">
          <el-input v-model="form.comment" type="textarea" />
        </el-form-item>
        <el-form-item label="商品分数：" prop="productScore" class="productScore">
          <el-rate v-model="form.productScore" style="margin-top: 8px" />
        </el-form-item>
        <el-form-item label="服务分数：" prop="serviceScore" class="productScore">
          <el-rate v-model="form.serviceScore" style="margin-top: 8px" />
        </el-form-item>
        <el-form-item label="用户头像：" prop="avatar">
          <div class="comment-image-field">
            <el-upload action="#" :show-file-list="false" :http-request="uploadAvatar" accept="image/*">
              <div class="upLoadPicBox">
                <div v-if="form.avatar" class="pictrue"><img :src="assetUrl(form.avatar)" /></div>
                <div v-else class="upLoad">
                  <el-icon><Camera /></el-icon>
                </div>
              </div>
            </el-upload>
            <div class="comment-image-actions">
              <el-button type="primary" plain @click="openAttachmentSelector('avatar')">选择素材</el-button>
              <el-button v-if="form.avatar" @click="clearAvatar">清除</el-button>
              <span class="muted">可上传新图，也可从老素材库选择已有图片。</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="评价图片：" prop="pics">
          <div class="acea-row comment-pic-field">
            <div v-for="(item, index) in pics" :key="index" class="pictrue comment-upload-pic">
              <img :src="assetUrl(item)" />
              <el-icon class="btndel" @click="removePic(index)"><CircleCloseFilled /></el-icon>
            </div>
            <el-upload
              v-if="pics.length < 10"
              action="#"
              :show-file-list="false"
              :http-request="uploadReplyPic"
              accept="image/*"
              multiple
            >
              <div class="upLoadPicBox">
                <div class="upLoad"><el-icon><Camera /></el-icon></div>
              </div>
            </el-upload>
            <el-button v-if="pics.length < 10" type="primary" plain @click="openAttachmentSelector('pics')">选择素材</el-button>
            <span class="muted">最多 10 张，可上传新图或从老素材库选择。</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetForm">重置</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="productDialogVisible" title="商品列表" width="900px">
      <div class="demo-input-suffix acea-row product-picker-search">
        <span class="seachTiele">商品搜索：</span>
        <el-input v-model="productQuery.keywords" placeholder="请输入商品名称/ID" class="selWidth" clearable @keyup.enter="searchProducts" />
        <el-button class="ml30" type="primary" size="small" @click="searchProducts">搜索</el-button>
      </div>
      <el-table v-loading="productLoading" :data="productRows" size="small" border row-key="id" @row-click="chooseProduct">
        <el-table-column label="商品图" width="80">
          <template #default="{ row }"><img class="product-thumb" :src="assetUrl(row.image)" /></template>
        </el-table-column>
        <el-table-column prop="storeName" label="商品名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="price" label="售价" width="90" />
        <el-table-column prop="stock" label="库存" width="90" />
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="chooseProduct(row)">选择</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="block">
        <el-pagination
          v-model:current-page="productQuery.page"
          v-model:page-size="productQuery.limit"
          :page-sizes="[10, 20, 40]"
          layout="total, sizes, prev, pager, next"
          :total="productTotal"
          background
          @size-change="loadProducts"
          @current-change="loadProducts"
        />
      </div>
    </el-dialog>

    <el-dialog v-model="attachmentDialogVisible" :title="attachmentMode === 'avatar' ? '选择头像' : '选择评价图片'" width="860px" append-to-body destroy-on-close>
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
            <span class="muted">{{ attachmentMode === 'avatar' ? '选择一张图片作为用户头像。' : `选择已有素材加入评价图片，已选 ${pics.length}/10 张。` }}</span>
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
import { Camera, CircleCloseFilled } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  attachmentList,
  categoryTree,
  productList,
  productReplyComment,
  productReplyDelete,
  productReplyList,
  productReplySave,
  uploadImage
} from '../api';

const CATEGORY_TYPE_ATTACHMENT = 2;
const IMAGE_TYPES = 'jpg,jpeg,gif,png,bmp,PNG,JPG';

const loading = ref(false);
const saving = ref(false);
const attachmentLoading = ref(false);
const tableData = ref([]);
const attachmentCategories = ref([]);
const attachmentRows = ref([]);
const total = ref(0);
const attachmentTotal = ref(0);
const timeVal = ref([]);
const createDialogVisible = ref(false);
const productDialogVisible = ref(false);
const attachmentDialogVisible = ref(false);
const attachmentMode = ref('avatar');
const productLoading = ref(false);
const productRows = ref([]);
const productTotal = ref(0);
const formRef = ref();
const pics = ref([]);
const selectedProduct = ref({});

const query = reactive({
  page: 1,
  limit: 20,
  isReply: '',
  dateLimit: '',
  nickname: '',
  productSearch: ''
});

const form = reactive(defaultForm());
const productQuery = reactive({
  page: 1,
  limit: 10,
  type: 1,
  keywords: ''
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
  avatar: [{ required: true, message: '请选择用户头像', trigger: 'change' }],
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  comment: [{ required: true, message: '请填写评价内容', trigger: 'blur' }],
  nickname: [{ required: true, message: '请填写用户名称', trigger: 'blur' }],
  pics: [{ validator: validatePics, trigger: 'change' }],
  productScore: [{ required: true, message: '商品分数不能为空', trigger: 'change' }],
  serviceScore: [{ required: true, message: '服务分数不能为空', trigger: 'change' }]
};

function defaultForm() {
  return {
    avatar: '',
    comment: '',
    nickname: '',
    pics: '',
    productId: '',
    productScore: null,
    serviceScore: null,
    sku: '',
    unique: ''
  };
}

async function loadList() {
  loading.value = true;
  try {
    const data = await productReplyList({
      page: query.page,
      limit: query.limit,
      isReply: query.isReply || undefined,
      dateLimit: query.dateLimit || undefined,
      nickname: query.nickname || undefined,
      productSearch: query.productSearch || undefined
    });
    tableData.value = data?.list || [];
    total.value = data?.total || 0;
  } finally {
    loading.value = false;
  }
}

function searchList() {
  createDialogVisible.value = false;
  query.page = 1;
  loadList();
}

function reset() {
  query.isReply = '';
  query.nickname = '';
  query.productSearch = '';
  query.dateLimit = '';
  timeVal.value = [];
  query.page = 1;
  loadList();
}

function onchangeTime(value) {
  timeVal.value = value || [];
  query.dateLimit = value?.length ? value.join(',') : '';
  query.page = 1;
  loadList();
}

async function reply(id) {
  const { value } = await ElMessageBox.prompt('回复内容', '回复', {
    inputType: 'textarea',
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /\\S/,
    inputErrorMessage: '请填写评论内容'
  });
  await productReplyComment({ ids: id, merchantReplyContent: value });
  ElMessage.success('回复成功');
  await loadList();
}

async function remove(id) {
  await ElMessageBox.confirm('确定删除当前评论?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  });
  await productReplyDelete(id);
  ElMessage.success('删除成功');
  if (tableData.value.length === 1 && query.page > 1) {
    query.page -= 1;
  }
  await loadList();
}

function openCreate() {
  resetForm();
  createDialogVisible.value = true;
}

function resetForm() {
  Object.assign(form, defaultForm());
  pics.value = [];
  selectedProduct.value = {};
  formRef.value?.clearValidate?.();
}

function openProductDialog() {
  productDialogVisible.value = true;
  loadProducts();
}

function searchProducts() {
  productQuery.page = 1;
  loadProducts();
}

async function loadProducts() {
  productLoading.value = true;
  try {
    const data = await productList({
      page: productQuery.page,
      limit: productQuery.limit,
      type: productQuery.type,
      keywords: productQuery.keywords || undefined
    });
    productRows.value = data?.list || [];
    productTotal.value = data?.total || 0;
  } finally {
    productLoading.value = false;
  }
}

function chooseProduct(row) {
  selectedProduct.value = row;
  form.productId = row.id;
  const firstAttr = row.attrValue?.[0];
  form.sku = firstAttr?.suk || '';
  form.unique = firstAttr?.id ? String(firstAttr.id) : '';
  productDialogVisible.value = false;
}

async function uploadAvatar(options) {
  const result = await uploadFile(options.file);
  form.avatar = result;
  formRef.value?.validateField?.('avatar');
}

async function uploadReplyPic(options) {
  const result = await uploadFile(options.file);
  if (result && pics.value.length < 10) {
    pics.value.push(result);
    formRef.value?.validateField?.('pics');
  }
}

async function uploadFile(file) {
  const data = new FormData();
  data.append('file', file);
  const result = await uploadImage(data, { model: 'store' });
  return result?.sattDir || result?.url || result?.src || result?.path || '';
}

function removePic(index) {
  pics.value.splice(index, 1);
  formRef.value?.validateField?.('pics');
}

async function openAttachmentSelector(mode) {
  attachmentMode.value = mode;
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
  if (attachmentMode.value === 'avatar') {
    form.avatar = src;
    attachmentDialogVisible.value = false;
    formRef.value?.validateField?.('avatar');
    return;
  }
  if (pics.value.includes(src)) {
    ElMessage.warning('该图片已选择');
    return;
  }
  if (pics.value.length >= 10) {
    ElMessage.warning('评价图片最多 10 张');
    return;
  }
  pics.value.push(src);
  formRef.value?.validateField?.('pics');
}

function isSelectedAttachment(item) {
  const src = item.sattDir || item.attDir || '';
  return attachmentMode.value === 'avatar' ? form.avatar === src : pics.value.includes(src);
}

function clearAvatar() {
  form.avatar = '';
  formRef.value?.validateField?.('avatar');
}

async function submitCreate() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    await productReplySave({
      ...form,
      pics: pics.value.length ? JSON.stringify(pics.value) : ''
    });
    ElMessage.success('新增成功');
    createDialogVisible.value = false;
    await loadList();
  } finally {
    saving.value = false;
  }
}

function assetUrl(value) {
  if (!value) return '';
  if (/^https?:\/\//.test(value) || value.startsWith('data:') || value.startsWith('/')) return value;
  return `/${value}`;
}

function validatePics(_rule, _value, callback) {
  if (!pics.value.length) {
    callback(new Error('请选择评价图片'));
    return;
  }
  callback();
}

function formatTime(value) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
}

onMounted(loadList);
</script>

<style scoped>
.comment-image-field,
.comment-pic-field {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.comment-image-actions {
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
