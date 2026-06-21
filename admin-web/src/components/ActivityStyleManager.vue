<template>
  <div class="divBox activity-style">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form :model="query" size="small" inline @submit.prevent>
          <el-form-item label="创建时间：">
            <el-date-picker
              v-model="timeRange"
              type="datetimerange"
              range-separator="-"
              value-format="YYYY-MM-DD HH:mm:ss"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :default-time="defaultTime"
              class="time-picker"
              @change="handleTimeChange"
            />
          </el-form-item>
          <el-form-item label="活动状态：">
            <el-select v-model="query.runningStatus" placeholder="请选择" clearable class="selWidth">
              <el-option label="未开始" :value="0" />
              <el-option label="进行中" :value="1" />
              <el-option label="已结束" :value="-1" />
            </el-select>
          </el-form-item>
          <el-form-item label="活动名称：">
            <el-input v-model="query.name" placeholder="请输入活动名称" clearable class="selWidth" @keyup.enter="search" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="search">搜索</el-button>
            <el-button @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14" :body-style="{ padding: '20px' }" shadow="never">
      <el-button type="primary" @click="openCreate">{{ isAtmosphere ? '添加氛围图' : '添加活动边框' }}</el-button>

      <el-table v-loading="loading" :data="tableData" class="mt20" size="small" highlight-current-row>
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column prop="name" label="活动名称" min-width="150" show-overflow-tooltip />
        <el-table-column :label="isAtmosphere ? '氛围图' : '活动边框'" min-width="100">
          <template #default="{ row }">
            <el-image
              v-if="row.style"
              class="style-image"
              :src="assetUrl(row.style)"
              :preview-src-list="[assetUrl(row.style)]"
              preview-teleported
              fit="cover"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="使用范围" min-width="120">
          <template #default="{ row }">{{ methodText(row.method) }}</template>
        </el-table-column>
        <el-table-column label="活动日期" min-width="260">
          <template #default="{ row }">{{ dateText(row.starttime) }} - {{ dateText(row.endtime) }}</template>
        </el-table-column>
        <el-table-column label="活动状态" min-width="90">
          <template #default="{ row }">
            <el-tag v-if="row.runningStatus === 0" class="tag-background not-start">未开始</el-tag>
            <el-tag v-else-if="row.runningStatus === 1" class="tag-background doing">进行中</el-tag>
            <el-tag v-else-if="row.runningStatus === -1" class="tag-background end">已结束</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="{ row }">{{ dateText(row.createtime) }}</template>
        </el-table-column>
        <el-table-column label="是否开启" fixed="right" min-width="110">
          <template #default="{ row }">
            <el-switch
              :model-value="Boolean(row.status)"
              active-text="开启"
              inactive-text="关闭"
              @change="changeStatus(row, $event)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button link type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog
      v-model="dialogVisible"
      :title="`${form.id ? '编辑' : '添加'}${isAtmosphere ? '氛围图' : '活动边框'}`"
      width="760px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-tabs v-model="currentTab" class="list-tabs">
        <el-tab-pane name="1" label="基础设置" />
        <el-tab-pane name="2" label="使用范围" />
      </el-tabs>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="formValidate mt20">
        <div v-show="currentTab === '1'">
          <el-form-item label="活动名称：" prop="name">
            <el-input v-model="form.name" size="small" class="form-control" placeholder="请输入活动名称" />
          </el-form-item>
          <el-form-item label="活动时间：" prop="timeVal">
            <el-date-picker
              v-model="form.timeVal"
              :default-time="defaultTime"
              type="datetimerange"
              range-separator="至"
              value-format="YYYY-MM-DD HH:mm:ss"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              class="form-control"
              @change="handleFormTimeChange"
            />
            <p class="desc">{{ `设置活动${isAtmosphere ? '氛围图' : '边框'}在商城展示时间` }}</p>
          </el-form-item>
          <el-form-item :label="isAtmosphere ? '活动氛围图：' : '活动边框：'" prop="style">
            <div class="style-upload-wrap">
              <el-upload action="#" :show-file-list="false" :http-request="uploadStyle" accept="image/*">
                <div class="upLoadPicBox mb10">
                  <div v-if="form.style" class="pictrue"><img :src="assetUrl(form.style)" alt="" /></div>
                  <div v-else class="upLoad">
                    <el-icon><Camera /></el-icon>
                  </div>
                </div>
              </el-upload>
              <div class="style-upload-actions">
                <el-button size="small" type="primary" plain @click="openAttachmentSelector">选择素材</el-button>
                <el-button v-if="form.style" size="small" @click="clearStyle">清除</el-button>
              </div>
            </div>
            <p class="desc">{{ isAtmosphere ? '750*100px' : '750*750px' }}</p>
          </el-form-item>
          <el-form-item label="是否开启：">
            <el-switch v-model="form.status" :active-value="true" :inactive-value="false" active-text="开启" inactive-text="关闭" />
          </el-form-item>
        </div>

        <div v-show="currentTab === '2'">
          <el-form-item label-width="0" prop="method">
            <el-radio-group v-model="form.method" @change="handleMethodChange">
              <el-radio :label="0">全部商品参与</el-radio>
              <el-radio :label="1">指定商品参与</el-radio>
              <el-radio :label="3">指定分类参与</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="form.method === 1" label-width="0" prop="products">
            <div class="scope-box">
              <div class="scope-actions">
                <el-button size="small" type="primary" @click="openProductPicker">添加商品</el-button>
                <el-button size="small" :disabled="!selectedProductRows.length" @click="clearSelectedProducts">批量删除</el-button>
                <span class="scope-count">已选 {{ selectedProductRows.length }} 个商品</span>
              </div>
              <el-table :data="selectedProductRows" size="small" border class="scope-table">
                <el-table-column prop="id" label="ID" width="70" />
                <el-table-column label="商品图" width="80">
                  <template #default="{ row }">
                    <el-image
                      v-if="row.image"
                      class="product-thumb"
                      :src="assetUrl(row.image)"
                      :preview-src-list="[assetUrl(row.image)]"
                      preview-teleported
                      fit="cover"
                    />
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column prop="storeName" label="商品名称" min-width="220" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.storeName || `商品 ID：${row.id}` }}</template>
                </el-table-column>
                <el-table-column prop="price" label="售价" width="90" />
                <el-table-column prop="stock" label="库存" width="90" />
                <el-table-column label="操作" width="80">
                  <template #default="{ row }">
                    <el-button link type="danger" @click="removeSelectedProduct(row.id)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-form-item>
          <el-form-item v-if="form.method === 3" label="选择分类：" prop="products">
            <el-cascader
              v-model="categorySelection"
              class="form-control"
              :props="categoryProps"
              :options="categoryOptions"
              filterable
              clearable
              collapse-tags
              collapse-tags-tooltip
              :show-all-levels="false"
              @change="handleCategoryChange"
            />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <el-button v-if="currentTab === '2'" @click="currentTab = '1'">上一步</el-button>
        <el-button v-if="currentTab === '1'" type="primary" @click="nextStep">下一步</el-button>
        <el-button v-if="currentTab === '2'" type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="productDialogVisible" title="选择商品" width="900px" append-to-body destroy-on-close>
      <div class="product-picker-search">
        <el-input
          v-model="productQuery.keywords"
          class="picker-input"
          clearable
          placeholder="请输入商品名称，关键字，商品ID"
          @keyup.enter="searchPickerProducts"
        />
        <el-button type="primary" @click="searchPickerProducts">搜索</el-button>
      </div>
      <el-table
        ref="productTableRef"
        v-loading="productLoading"
        :data="productRows"
        size="small"
        border
        @selection-change="handleProductSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="商品图" width="80">
          <template #default="{ row }">
            <el-image class="product-thumb" :src="assetUrl(row.image)" :preview-src-list="[assetUrl(row.image)]" fit="cover" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column prop="storeName" label="商品名称" min-width="260" show-overflow-tooltip />
        <el-table-column prop="price" label="售价" width="90" />
        <el-table-column prop="stock" label="库存" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.isShow ? 'success' : 'info'">{{ row.isShow ? '上架' : '下架' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="block">
        <el-pagination
          v-model:current-page="productQuery.page"
          v-model:page-size="productQuery.limit"
          :page-sizes="[10, 20, 40]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="productTotal"
          background
          @size-change="loadPickerProducts"
          @current-change="loadPickerProducts"
        />
      </div>
      <template #footer>
        <el-button @click="productDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmProductPicker">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="attachmentDialogVisible" title="选择素材" width="860px" append-to-body destroy-on-close>
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
        <div class="attachment-picker-main">
          <div class="attachment-picker-toolbar">
            <span class="muted">选择已有素材，不触发上传或第三方服务</span>
          </div>
          <div v-loading="attachmentLoading" class="attachment-picker-grid">
            <button
              v-for="item in attachmentRows"
              :key="item.id"
              type="button"
              class="attachment-picker-card"
              @click="selectAttachment(item)"
            >
              <img :src="assetUrl(item.sattDir || item.attDir)" alt="" />
              <span :title="item.name">{{ item.name || item.realName || item.attName || '-' }}</span>
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
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { Camera } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  activityStyleDelete,
  activityStyleInfo,
  activityStyleList,
  activityStyleSave,
  activityStyleStatus,
  activityStyleUpdate,
  attachmentList,
  categoryTree,
  productList,
  productListByIds,
  uploadImage
} from '../api';

const props = defineProps({
  path: {
    type: String,
    default: ''
  }
});

const IMAGE_TYPES = 'jpg,jpeg,gif,png,bmp,PNG,JPG';
const defaultTime = [new Date(2000, 0, 1, 0, 0, 0), new Date(2000, 0, 1, 23, 59, 59)];
const loading = ref(false);
const saving = ref(false);
const attachmentLoading = ref(false);
const dialogVisible = ref(false);
const currentTab = ref('1');
const tableData = ref([]);
const total = ref(0);
const timeRange = ref([]);
const formRef = ref();
const categoryOptions = ref([]);
const categorySelection = ref([]);
const productDialogVisible = ref(false);
const productTableRef = ref();
const productRows = ref([]);
const productTotal = ref(0);
const productLoading = ref(false);
const productSelection = ref([]);
const selectedProductRows = ref([]);
const attachmentDialogVisible = ref(false);
const attachmentCategories = ref([]);
const attachmentRows = ref([]);
const attachmentTotal = ref(0);

const categoryProps = {
  children: 'child',
  label: 'name',
  value: 'id',
  emitPath: false,
  multiple: true,
  checkStrictly: false
};

const isAtmosphere = computed(() => props.path.includes('/marketing/atmosphere'));
const activityType = computed(() => isAtmosphere.value);
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

const query = reactive({
  page: 1,
  limit: 20,
  name: '',
  runningStatus: '',
  starttime: '',
  endtime: ''
});

const productQuery = reactive({
  page: 1,
  limit: 10,
  keywords: '',
  type: '1'
});

const attachmentQuery = reactive({
  page: 1,
  limit: 18,
  pid: 0,
  attType: IMAGE_TYPES
});

const form = reactive(defaultForm());

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  timeVal: [{ type: 'array', required: true, message: '请选择时间', trigger: 'change' }],
  style: [{ required: true, message: '请选择图片', trigger: 'change' }],
  products: [{ validator: validateProducts, trigger: 'blur' }]
};

onMounted(async () => {
  await loadCategoryOptions();
  await loadList();
  openFromPath();
});

watch(() => props.path, async () => {
  reset();
  await loadList();
  openFromPath();
});

function handleTimeChange(value) {
  query.starttime = value?.[0] || '';
  query.endtime = value?.[1] || '';
}

function handleFormTimeChange(value) {
  form.starttime = value?.[0] || '';
  form.endtime = value?.[1] || '';
}

function search() {
  query.page = 1;
  loadList();
}

function reset() {
  timeRange.value = [];
  Object.assign(query, {
    page: 1,
    limit: query.limit || 20,
    name: '',
    runningStatus: '',
    starttime: '',
    endtime: ''
  });
}

async function loadList() {
  loading.value = true;
  try {
    const data = await activityStyleList({
      page: query.page,
      limit: query.limit,
      name: query.name || undefined,
      runningStatus: query.runningStatus === '' ? undefined : query.runningStatus,
      starttime: query.starttime || undefined,
      endtime: query.endtime || undefined,
      type: activityType.value
    });
    tableData.value = data?.list || [];
    total.value = Number(data?.total || 0);
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  Object.assign(form, defaultForm(), { type: activityType.value });
  currentTab.value = '1';
  dialogVisible.value = true;
}

async function openEdit(row) {
  const data = row?.id ? await activityStyleInfo({ id: row.id }) : row;
  Object.assign(form, defaultForm(), {
    ...data,
    type: Boolean(data.type),
    status: Boolean(data.status),
    method: Number(data.method ?? 0),
    starttime: dateText(data.starttime),
    endtime: dateText(data.endtime),
    timeVal: [dateText(data.starttime), dateText(data.endtime)]
  });
  await hydrateScopeSelection();
  currentTab.value = '1';
  dialogVisible.value = true;
}

async function openFromPath() {
  const match = props.path.match(/\/(?:border|atmosphere)\/add\/?(\d+)?/);
  if (!match) return;
  if (match[1]) {
    await openEdit({ id: Number(match[1]) });
  } else {
    openCreate();
  }
}

async function nextStep() {
  const valid = await formRef.value?.validateField(['name', 'timeVal', 'style']).catch(() => false);
  if (valid === false) return;
  currentTab.value = '2';
}

async function submit() {
  syncScopeProducts();
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    const payload = {
      id: form.id || undefined,
      name: form.name,
      type: activityType.value,
      starttime: form.starttime,
      endtime: form.endtime,
      style: form.style,
      status: Boolean(form.status),
      method: Number(form.method),
      products: Number(form.method) === 0 ? '' : form.products
    };
    if (form.id) {
      await activityStyleUpdate(payload);
    } else {
      await activityStyleSave(payload);
    }
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    await loadList();
    navigateToList();
  } finally {
    saving.value = false;
  }
}

async function uploadStyle(option) {
  const data = new FormData();
  data.append('multipart', option.file);
  const result = await uploadImage(data, { model: isAtmosphere.value ? 'activitystyle' : 'activitystyle', pid: 0 });
  form.style = result.url;
  ElMessage.success('上传成功');
}

async function openAttachmentSelector() {
  attachmentDialogVisible.value = true;
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
  form.style = item.sattDir || item.attDir || '';
  attachmentDialogVisible.value = false;
  formRef.value?.validateField?.('style');
}

function clearStyle() {
  form.style = '';
  formRef.value?.validateField?.('style');
}

async function changeStatus(row, value) {
  try {
    await activityStyleStatus({ id: row.id, status: value });
    ElMessage.success('修改成功');
    await loadList();
  } catch (error) {
    row.status = !value;
  }
}

async function remove(row) {
  await ElMessageBox.confirm('删除活动后将无法恢复，请谨慎操作!', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  });
  await activityStyleDelete({ id: row.id });
  ElMessage.success('删除成功');
  if (tableData.value.length === 1 && query.page > 1) query.page -= 1;
  await loadList();
}

function validateProducts(rule, value, callback) {
  if (Number(form.method) === 0) {
    callback();
  } else if (Number(form.method) === 1 && selectedProductRows.value.length) {
    callback();
  } else if (Number(form.method) === 3 && categorySelection.value.length) {
    callback();
  } else {
    callback(new Error(form.method === 1 ? '请选择商品' : '请选择分类'));
  }
}

async function loadCategoryOptions() {
  categoryOptions.value = await categoryTree({ status: -1, type: 1 });
}

async function hydrateScopeSelection() {
  const ids = parseIds(form.products);
  selectedProductRows.value = [];
  categorySelection.value = [];
  if (Number(form.method) === 1 && ids.length) {
    selectedProductRows.value = await productListByIds(ids.join(','));
    syncProductIds();
  }
  if (Number(form.method) === 3) {
    categorySelection.value = ids;
    syncCategoryIds();
  }
}

function handleMethodChange() {
  form.products = '';
  selectedProductRows.value = [];
  categorySelection.value = [];
  formRef.value?.clearValidate?.('products');
}

function syncScopeProducts() {
  if (Number(form.method) === 1) syncProductIds();
  if (Number(form.method) === 3) syncCategoryIds();
  if (Number(form.method) === 0) form.products = '';
}

function syncProductIds() {
  form.products = selectedProductRows.value.map((item) => item.id).join(',');
}

function syncCategoryIds() {
  form.products = categorySelection.value.join(',');
}

function handleCategoryChange() {
  syncCategoryIds();
  formRef.value?.validateField?.('products');
}

async function openProductPicker() {
  productDialogVisible.value = true;
  productSelection.value = [...selectedProductRows.value];
  await loadPickerProducts();
}

function searchPickerProducts() {
  productQuery.page = 1;
  loadPickerProducts();
}

async function loadPickerProducts() {
  productLoading.value = true;
  try {
    const data = await productList({ ...productQuery });
    productRows.value = data?.list || [];
    productTotal.value = Number(data?.total || 0);
    await nextTick();
    syncPickerSelection();
  } finally {
    productLoading.value = false;
  }
}

function syncPickerSelection() {
  const table = productTableRef.value;
  if (!table) return;
  table.clearSelection();
  const selectedIds = new Set(productSelection.value.map((item) => item.id));
  productRows.value.forEach((row) => {
    if (selectedIds.has(row.id)) table.toggleRowSelection(row, true);
  });
}

function handleProductSelectionChange(selection) {
  const selectedMap = new Map(productSelection.value.map((item) => [item.id, item]));
  const pageIds = new Set(productRows.value.map((item) => item.id));
  pageIds.forEach((id) => selectedMap.delete(id));
  selection.forEach((item) => selectedMap.set(item.id, item));
  productSelection.value = Array.from(selectedMap.values());
}

function confirmProductPicker() {
  selectedProductRows.value = [...productSelection.value];
  syncProductIds();
  formRef.value?.validateField?.('products');
  productDialogVisible.value = false;
}

function removeSelectedProduct(id) {
  selectedProductRows.value = selectedProductRows.value.filter((item) => item.id !== id);
  syncProductIds();
}

function clearSelectedProducts() {
  selectedProductRows.value = [];
  syncProductIds();
}

function parseIds(value) {
  return String(value || '')
    .split(',')
    .map((item) => Number(item.trim()))
    .filter((item) => Number.isInteger(item) && item > 0);
}

function navigateToList() {
  const listPath = isAtmosphere.value ? '/marketing/atmosphere/list' : '/marketing/border/list';
  if (window.location.pathname !== listPath) {
    window.history.pushState({}, '', listPath);
    window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path: listPath } }));
  }
}

function methodText(value) {
  const map = {
    0: '全部商品参与',
    1: '指定商品参与',
    2: '指定品牌参与',
    3: '指定分类参与',
    4: '指定商户产品'
  };
  return map[Number(value)] || '-';
}

function dateText(value) {
  if (!value) return '';
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
    name: '',
    type: false,
    starttime: '',
    endtime: '',
    timeVal: [],
    style: '',
    status: false,
    method: 0,
    products: ''
  };
}
</script>

<style scoped>
.time-picker {
  width: 310px;
}

.style-image {
  width: 36px;
  height: 36px;
  border-radius: 4px;
}

.form-control {
  width: 420px;
}

.scope-box {
  width: 100%;
}

.scope-actions,
.product-picker-search {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.scope-count {
  color: #909399;
  font-size: 12px;
}

.scope-table {
  width: 100%;
}

.product-thumb {
  width: 44px;
  height: 44px;
  border-radius: 4px;
}

.picker-input {
  width: 260px;
}

.upLoadPicBox {
  width: 104px;
  height: 104px;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  cursor: pointer;
  overflow: hidden;
}

.upLoadPicBox .pictrue,
.upLoadPicBox .pictrue img,
.upLoadPicBox .upLoad {
  width: 100%;
  height: 100%;
}

.upLoadPicBox .pictrue img {
  object-fit: cover;
}

.upLoadPicBox .upLoad {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 24px;
}

.style-upload-wrap {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.style-upload-actions {
  display: flex;
  gap: 8px;
  padding-top: 34px;
}

.desc {
  margin: 6px 0 0;
  color: #999;
  font-size: 12px;
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

.not-start {
  background: #fff7e6;
  color: #d46b08;
}

.doing {
  background: #e6fffb;
  color: #08979c;
}

.end {
  background: #f5f5f5;
  color: #666;
}
</style>
