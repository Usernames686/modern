<template>
  <div class="divBox bargain-product-form">
    <div class="page-header">
      <div>
        <h3>{{ isEdit ? `编辑砍价商品-${bargainId}` : '添加砍价商品' }}</h3>
        <p>配置砍价商品、参与规格、砍价金额和库存校验。</p>
      </div>
      <el-button @click="goBack">返回列表</el-button>
    </div>

    <el-card class="box-card mt14" shadow="never">
      <el-tabs v-model="currentTab">
        <el-tab-pane label="基础信息" name="base" />
        <el-tab-pane label="砍价规则" name="rule" />
        <el-tab-pane label="商品详情" name="detail" />
      </el-tabs>

      <el-form ref="formRef" v-loading="loading" :model="form" :rules="rules" label-width="130px" class="formValidate mt20">
        <template v-if="currentTab === 'base'">
          <el-row :gutter="24">
            <el-col :span="24">
              <el-alert
                type="success"
                show-icon
                :closable="false"
                title="当前表单已按老 CRMEB 砍价结构保存 type=2 规格；多规格商品保持老逻辑：从商品 SKU 中选择一个参与砍价。"
              />
            </el-col>
            <el-col :md="12" :xs="24">
              <el-form-item label="主商品ID：" prop="productId">
                <el-input v-model="form.productId" :disabled="isEdit" placeholder="请选择商品" class="form-control">
                  <template #append>
                    <el-button :disabled="isEdit" @click="openProductDialog">选择</el-button>
                  </template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :md="12" :xs="24">
              <el-form-item label="砍价名称：" prop="title">
                <el-input v-model.trim="form.title" maxlength="249" placeholder="请输入砍价商品名称" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :md="12" :xs="24">
              <el-form-item label="商品主图：" prop="image">
                <el-input v-model.trim="form.image" placeholder="请输入或上传图片路径" class="form-control">
                  <template #append>
                    <el-upload :show-file-list="false" :http-request="uploadMainImage" accept="image/*">
                      <el-button :loading="uploadingImage">上传</el-button>
                    </el-upload>
                  </template>
                </el-input>
                <el-image v-if="form.image" class="preview-image" :src="assetUrl(form.image)" fit="cover" />
              </el-form-item>
            </el-col>
            <el-col :md="12" :xs="24">
              <el-form-item label="单位：" prop="unitName">
                <el-input v-model.trim="form.unitName" maxlength="16" placeholder="请输入单位" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :md="12" :xs="24">
              <el-form-item label="排序：">
                <el-input-number
                  v-model="form.sort"
                  :min="0"
                  :max="9999"
                  :step="1"
                  step-strictly
                  controls-position="right"
                  class="form-control"
                />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="商品轮播图：" prop="imagesText">
                <el-input
                  v-model="form.imagesText"
                  type="textarea"
                  :rows="3"
                  placeholder="每行一个图片/视频路径，保存时会按老 JSON 数组格式写入 images"
                />
              </el-form-item>
            </el-col>
            <el-col :md="12" :xs="24">
              <el-form-item label="活动日期：" prop="timeVal">
                <el-date-picker
                  v-model="form.timeVal"
                  type="daterange"
                  value-format="YYYY-MM-DD"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  class="form-control"
                />
              </el-form-item>
            </el-col>
            <el-col :md="12" :xs="24">
              <el-form-item label="运费模板：" prop="tempId">
                <el-select v-model="form.tempId" placeholder="请选择" class="form-control">
                  <el-option v-for="item in shippingTemplates" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="发起次数：" prop="num">
                <el-input-number v-model="form.num" :min="1" :step="1" step-strictly controls-position="right" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="帮砍次数：" prop="bargainNum">
                <el-input-number v-model="form.bargainNum" :min="1" :step="1" step-strictly controls-position="right" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="砍价人数：" prop="peopleNum">
                <el-input-number v-model="form.peopleNum" :min="1" :step="1" step-strictly controls-position="right" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="起始金额：" prop="price">
                <el-input-number v-model="form.price" :min="0" :precision="2" controls-position="right" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="最低价：" prop="minPrice">
                <el-input-number v-model="form.minPrice" :min="0" :precision="2" controls-position="right" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="成本价：">
                <el-input-number v-model="form.cost" :min="0" :precision="2" controls-position="right" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="限量：" prop="quota">
                <el-input-number v-model="form.quota" :min="1" :step="1" step-strictly controls-position="right" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="重量：">
                <el-input-number v-model="form.weight" :min="0" :precision="2" controls-position="right" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="体积：">
                <el-input-number v-model="form.volume" :min="0" :precision="2" controls-position="right" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="商品属性：" class="sku-form-item" required>
                <div class="sku-toolbar">
                  <span>已载入 {{ skuRows.length }} 个规格，当前选择：{{ selectedSkuRow?.suk || '未选择' }}</span>
                  <el-button size="small" :disabled="!form.productId" @click="refreshProductSpecs">重新载入商品规格</el-button>
                </div>
                <el-table :data="skuRows" border size="small" class="sku-table">
                  <el-table-column label="选择" width="72" fixed>
                    <template #default="{ row }">
                      <el-radio v-model="selectedSkuKey" :value="row.key" @change="selectSku(row)" />
                    </template>
                  </el-table-column>
                  <template v-if="form.specType">
                    <el-table-column v-for="name in attrColumnNames" :key="name" :label="name" min-width="88">
                      <template #default="{ row }">{{ row.attrMap[name] || '-' }}</template>
                    </el-table-column>
                  </template>
                  <el-table-column v-else prop="suk" label="规格" min-width="90" />
                  <el-table-column label="图片" width="86">
                    <template #default="{ row }">
                      <el-image v-if="row.image" class="sku-image" :src="assetUrl(row.image)" fit="cover" />
                      <div v-else class="sku-image empty-image"><el-icon><Camera /></el-icon></div>
                    </template>
                  </el-table-column>
                  <el-table-column label="砍价起始金额" min-width="150">
                    <template #default="{ row }">
                      <el-input-number v-model="row.price" :disabled="row.key !== selectedSkuKey" :min="0" :precision="2" controls-position="right" class="table-number" />
                    </template>
                  </el-table-column>
                  <el-table-column label="砍价最低价" min-width="150">
                    <template #default="{ row }">
                      <el-input-number v-model="row.minPrice" :disabled="row.key !== selectedSkuKey" :min="0" :max="row.price || undefined" :precision="2" controls-position="right" class="table-number" />
                    </template>
                  </el-table-column>
                  <el-table-column label="成本价" min-width="130">
                    <template #default="{ row }">
                      <el-input-number v-model="row.cost" :disabled="row.key !== selectedSkuKey" :min="0" :precision="2" controls-position="right" class="table-number" />
                    </template>
                  </el-table-column>
                  <el-table-column label="原价" min-width="130">
                    <template #default="{ row }">
                      <el-input-number v-model="row.otPrice" :disabled="row.key !== selectedSkuKey" :min="0" :precision="2" controls-position="right" class="table-number" />
                    </template>
                  </el-table-column>
                  <el-table-column label="库存" min-width="100">
                    <template #default="{ row }">{{ row.stock }}</template>
                  </el-table-column>
                  <el-table-column label="限量" min-width="120">
                    <template #default="{ row }">
                      <el-input-number v-model="row.quota" :disabled="row.key !== selectedSkuKey" :min="1" :max="maxQuota(row)" :step="1" step-strictly controls-position="right" class="table-number" />
                    </template>
                  </el-table-column>
                  <el-table-column label="重量" min-width="120">
                    <template #default="{ row }">
                      <el-input-number v-model="row.weight" :disabled="row.key !== selectedSkuKey" :min="0" :precision="2" controls-position="right" class="table-number" />
                    </template>
                  </el-table-column>
                  <el-table-column label="体积" min-width="120">
                    <template #default="{ row }">
                      <el-input-number v-model="row.volume" :disabled="row.key !== selectedSkuKey" :min="0" :precision="2" controls-position="right" class="table-number" />
                    </template>
                  </el-table-column>
                  <el-table-column label="商品编号" min-width="140">
                    <template #default="{ row }">
                      <el-input v-model="row.barCode" :disabled="row.key !== selectedSkuKey" class="table-input" />
                    </template>
                  </el-table-column>
                </el-table>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="活动状态：" prop="status">
                <el-radio-group v-model="form.status">
                  <el-radio :value="0">关闭</el-radio>
                  <el-radio :value="1">开启</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <template v-else-if="currentTab === 'rule'">
          <el-form-item label="砍价规则：">
            <el-input v-model="form.rule" type="textarea" :rows="12" placeholder="可保留老规则文本或 HTML" />
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="商品详情：">
            <el-input v-model="form.content" type="textarea" :rows="12" placeholder="可保留老 HTML 内容" />
          </el-form-item>
        </template>

        <el-form-item class="submit-row">
          <el-button @click="prevOrNext">{{ currentTab === 'detail' ? '上一步' : '下一步' }}</el-button>
          <el-button type="primary" :loading="saving" @click="submit">提交</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog v-model="productDialogVisible" title="选择商品" width="860px" append-to-body>
      <div class="product-search">
        <el-input v-model.trim="productQuery.keywords" placeholder="请输入商品名称 / ID" clearable @keyup.enter="searchProducts" />
        <el-button type="primary" @click="searchProducts">搜索</el-button>
      </div>
      <el-table v-loading="productLoading" :data="productRows" border size="small" @row-dblclick="selectProduct">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="商品图" width="86">
          <template #default="{ row }">
            <el-image v-if="row.image" class="sku-image" :src="assetUrl(row.image)" fit="cover" />
          </template>
        </el-table-column>
        <el-table-column prop="storeName" label="商品名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="price" label="售价" width="100" />
        <el-table-column prop="stock" label="库存" width="100" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :loading="selectingProductId === row.id" @click="selectProduct(row)">选择</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="dialog-pagination">
        <el-pagination
          v-model:current-page="productQuery.page"
          v-model:page-size="productQuery.limit"
          layout="total, prev, pager, next"
          :total="productTotal"
          @current-change="loadProducts"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { Camera } from '@element-plus/icons-vue';
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { bargainInfo, bargainSave, bargainUpdate, productDetail, productList, shippingTemplatesList, uploadImage } from '../api';

const props = defineProps({
  path: {
    type: String,
    default: ''
  }
});

const currentTab = ref('base');
const formRef = ref();
const loading = ref(false);
const saving = ref(false);
const uploadingImage = ref(false);
const shippingTemplates = ref([]);
const attrRows = ref([]);
const skuRows = ref([]);
const selectedSkuKey = ref('');
const productDialogVisible = ref(false);
const productLoading = ref(false);
const productRows = ref([]);
const productTotal = ref(0);
const selectingProductId = ref(0);
const productQuery = reactive({
  page: 1,
  limit: 10,
  keywords: ''
});

const form = reactive({
  productId: undefined,
  title: '',
  image: '',
  imagesText: '',
  unitName: '件',
  timeVal: [],
  tempId: 0,
  num: 1,
  bargainNum: 2,
  peopleNum: 2,
  price: 0,
  minPrice: 0,
  cost: 0,
  quota: 1,
  sort: 0,
  weight: 0,
  volume: 0,
  status: 0,
  specType: false,
  rule: '',
  content: ''
});

const rules = {
  productId: [{ required: true, message: '主商品ID不能为空', trigger: 'blur' }],
  title: [{ required: true, message: '请输入砍价名称', trigger: 'blur' }],
  image: [{ required: true, message: '请填写商品主图', trigger: 'blur' }],
  imagesText: [{ required: true, message: '请填写商品轮播图', trigger: 'blur' }],
  unitName: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  tempId: [{ required: true, message: '请选择运费模板', trigger: 'change' }],
  timeVal: [{ required: true, type: 'array', min: 2, message: '请选择活动日期', trigger: 'change' }],
  num: [{ required: true, message: '请输入发起次数', trigger: 'blur' }],
  bargainNum: [{ required: true, message: '请输入帮砍次数', trigger: 'blur' }],
  peopleNum: [{ required: true, message: '请输入砍价人数', trigger: 'blur' }],
  price: [{ required: true, message: '请输入起始金额', trigger: 'blur' }],
  minPrice: [{ required: true, message: '请输入最低价', trigger: 'blur' }],
  quota: [{ required: true, message: '请输入限量', trigger: 'blur' }]
};

const routeParts = computed(() => props.path.split('/').filter(Boolean));
const bargainId = computed(() => Number(routeParts.value[3] || 0));
const isEdit = computed(() => bargainId.value > 0);
const selectedSkuRow = computed(() => skuRows.value.find((row) => row.key === selectedSkuKey.value) || null);
const attrColumnNames = computed(() => {
  const names = attrRows.value.map((item) => item.attrName).filter(Boolean);
  if (names.length) return names;
  const first = skuRows.value.find((row) => row.attrMap && Object.keys(row.attrMap).length);
  return first ? Object.keys(first.attrMap) : [];
});

onMounted(async () => {
  await loadShippingTemplates();
  if (isEdit.value) {
    await loadInfo();
  } else {
    resetDefaultSku();
  }
});

async function loadShippingTemplates() {
  const data = await shippingTemplatesList({ page: 1, limit: 200 }).catch(() => ({ list: [] }));
  shippingTemplates.value = data?.list || [];
}

async function loadInfo() {
  loading.value = true;
  try {
    const data = await bargainInfo({ id: bargainId.value });
    form.productId = data.productId || undefined;
    form.title = data.storeName || data.title || '';
    form.image = data.image || '';
    form.imagesText = parseImages(data.sliderImage || data.images).join('\n');
    form.unitName = data.unitName || '件';
    form.timeVal = data.startTimeStr && data.stopTimeStr ? [data.startTimeStr, data.stopTimeStr] : [];
    form.tempId = Number(data.tempId || 0);
    form.num = Number(data.num || 1);
    form.bargainNum = Number(data.bargainNum || 1);
    form.peopleNum = Number(data.peopleNum || 1);
    form.price = Number(data.price || 0);
    form.minPrice = Number(data.minPrice || 0);
    form.cost = Number(data.cost || 0);
    form.quota = Number(data.quotaShow || data.quota || data.stock || 1);
    form.sort = Number(data.sort || 0);
    form.weight = Number(data.weight || 0);
    form.volume = Number(data.volume || 0);
    form.status = Number(data.status || 0);
    form.specType = Boolean(data.specType);
    form.rule = data.rule || '';
    form.content = data.content || '';
    attrRows.value = normalizeAttrs(data.attr || [], form.specType);
    skuRows.value = normalizeSkuRows(data.attrValue || []);
    if (!skuRows.value.length) resetDefaultSku();
    const selected = skuRows.value.find((row) => row.id) || skuRows.value[0];
    if (selected) selectSku(selected);
  } finally {
    loading.value = false;
  }
}

function openProductDialog() {
  productDialogVisible.value = true;
  if (!productRows.value.length) loadProducts();
}

function searchProducts() {
  productQuery.page = 1;
  loadProducts();
}

async function loadProducts() {
  productLoading.value = true;
  try {
    const data = await productList({ ...productQuery });
    productRows.value = data?.list || [];
    productTotal.value = Number(data?.total || 0);
  } finally {
    productLoading.value = false;
  }
}

async function selectProduct(row) {
  if (!row?.id) return;
  selectingProductId.value = row.id;
  try {
    const data = await productDetail(row.id);
    applyProductDetail(data);
    productDialogVisible.value = false;
    ElMessage.success('商品规格已载入');
  } finally {
    selectingProductId.value = 0;
  }
}

async function refreshProductSpecs() {
  if (!form.productId) {
    ElMessage.warning('请先选择主商品');
    return;
  }
  const data = await productDetail(Number(form.productId));
  applyProductDetail(data, true);
  ElMessage.success('商品规格已重新载入');
}

function applyProductDetail(data, keepBase = false) {
  form.productId = data.id;
  if (!keepBase) {
    form.title = data.storeName || form.title;
    form.image = data.image || form.image;
    form.imagesText = parseImages(data.sliderImage).join('\n') || data.image || form.imagesText;
    form.unitName = data.unitName || form.unitName || '件';
    form.sort = Number(data.sort || form.sort || 0);
    form.tempId = Number(data.tempId || form.tempId || 0);
    form.content = data.content || form.content;
  }
  form.specType = Boolean(data.specType);
  attrRows.value = normalizeAttrs(data.attr || [], form.specType);
  skuRows.value = normalizeSkuRows(data.attrValue || []);
  if (!skuRows.value.length) resetDefaultSku();
  selectSku(skuRows.value[0]);
}

function selectSku(row) {
  if (!row) return;
  selectedSkuKey.value = row.key;
  form.image = row.image || form.image;
  form.price = number(row.price, form.price);
  form.minPrice = number(row.minPrice, Math.max(0, form.price - 0.01));
  form.cost = number(row.cost, form.cost);
  form.quota = number(row.quota, form.quota);
  form.weight = number(row.weight, form.weight);
  form.volume = number(row.volume, form.volume);
}

async function uploadMainImage(options) {
  uploadingImage.value = true;
  try {
    const body = new FormData();
    body.append('multipart', options.file);
    const data = await uploadImage(body, { model: 'product', pid: 0 });
    form.image = data.url || '';
    if (!form.imagesText.trim()) form.imagesText = form.image;
    ElMessage.success('上传成功');
  } finally {
    uploadingImage.value = false;
  }
}

async function submit() {
  await formRef.value?.validate();
  if (Number(form.minPrice) > Number(form.price)) {
    ElMessage.warning('最低价不能大于起始金额');
    return;
  }
  const selected = selectedSkuRow.value;
  if (!selected) {
    ElMessage.warning('请选择一个商品属性');
    return;
  }
  const minRequired = Number((Number(form.peopleNum || 1) * 0.01 + Number(selected.minPrice || 0)).toFixed(2));
  if (Number(selected.price || 0) < minRequired) {
    ElMessage.warning(`砍价起始金额不能小于${minRequired}`);
    return;
  }
  saving.value = true;
  try {
    const payload = buildPayload();
    if (isEdit.value) {
      await bargainUpdate({ id: bargainId.value }, payload);
      ElMessage.success('编辑成功');
    } else {
      await bargainSave(payload);
      ElMessage.success('新增成功');
    }
    goBack();
  } catch (error) {
    ElMessage.error(error?.message || '提交失败');
  } finally {
    saving.value = false;
  }
}

function buildPayload() {
  const images = form.imagesText.split('\n').map(item => item.trim()).filter(Boolean);
  const row = buildSelectedAttrValue();
  return {
    id: isEdit.value ? bargainId.value : undefined,
    productId: form.productId,
    title: form.title,
    storeName: form.title,
    image: form.image,
    imagess: images,
    images: JSON.stringify(images),
    unitName: form.unitName,
    startTime: form.timeVal?.[0],
    stopTime: form.timeVal?.[1],
    tempId: form.tempId,
    num: form.num,
    bargainNum: form.bargainNum,
    peopleNum: form.peopleNum,
    price: row.price,
    minPrice: row.minPrice,
    cost: row.cost,
    otPrice: row.otPrice,
    stock: row.stock,
    quota: row.quota,
    quotaShow: row.quotaShow,
    sort: form.sort,
    weight: row.weight,
    volume: row.volume,
    status: form.status,
    specType: form.specType,
    attr: buildAttrPayload(),
    rule: form.rule,
    content: form.content,
    attrValue: [row]
  };
}

function buildAttrPayload() {
  if (form.specType) {
    return normalizeAttrs(attrRows.value, true).map((item) => ({
      attrName: item.attrName,
      attrValues: item.attrValues
    }));
  }
  return [{ attrName: '规格', attrValues: '默认' }];
}

function buildSelectedAttrValue() {
  const row = selectedSkuRow.value;
  if (!row) {
    throw new Error('请选择一个商品属性');
  }
  const quota = number(row.quota, 0);
  const stock = number(row.stock, quota);
  if (quota <= 0) throw new Error(`规格「${row.suk || '默认'}」限量必须大于0`);
  if (stock > 0 && quota > stock) throw new Error(`规格「${row.suk || '默认'}」限量不能大于库存`);
  return {
    suk: row.suk || attrSuk(row.attrMap),
    image: row.image || form.image,
    price: number(row.price, form.price),
    minPrice: number(row.minPrice, form.minPrice),
    cost: number(row.cost, form.cost),
    otPrice: number(row.otPrice, row.price || form.price),
    stock,
    quota,
    quotaShow: quota,
    weight: number(row.weight, form.weight),
    volume: number(row.volume, form.volume),
    barCode: row.barCode || '',
    attrValue: JSON.stringify(row.attrMap && Object.keys(row.attrMap).length ? row.attrMap : parseAttrValue(row.attrValue))
  };
}

function resetDefaultSku() {
  attrRows.value = [{ attrName: '规格', attrValues: '默认' }];
  skuRows.value = [{
    key: '默认',
    suk: '默认',
    image: form.image,
    stock: form.quota || 1,
    quota: form.quota || 1,
    price: form.price || 0,
    minPrice: form.minPrice || 0,
    cost: form.cost || 0,
    otPrice: form.price || 0,
    weight: form.weight || 0,
    volume: form.volume || 0,
    barCode: '',
    attrMap: { 规格: '默认' },
    attrValue: '{"规格":"默认"}'
  }];
  selectedSkuKey.value = '默认';
}

function normalizeAttrs(list, specType) {
  const rows = (list || [])
    .map((item) => ({
      attrName: item.attrName || item.attr_name || '',
      attrValues: Array.isArray(item.attrValues) ? item.attrValues.join(',') : (item.attrValues || item.attr_values || '')
    }))
    .filter((item) => item.attrName && item.attrValues);
  if (rows.length) return rows;
  return specType ? [] : [{ attrName: '规格', attrValues: '默认' }];
}

function normalizeSkuRows(list) {
  return (list || []).map((item, index) => {
    const attrMap = parseAttrValue(item.attrValue);
    const stock = number(item.stock, number(item.quota, 1));
    const quota = number(item.quotaShow, number(item.quota, stock || 1));
    const suk = item.suk || attrSuk(attrMap);
    return {
      ...item,
      key: `${item.id || index}-${suk}`,
      suk,
      image: item.image || form.image,
      stock,
      quota: Math.min(quota || stock || 1, stock || quota || 1),
      price: number(item.price, 0),
      minPrice: number(item.minPrice, number(item.price, 0)),
      cost: number(item.cost, 0),
      otPrice: number(item.otPrice, number(item.price, 0)),
      weight: number(item.weight, 0),
      volume: number(item.volume, 0),
      barCode: item.barCode || '',
      attrMap,
      attrValue: typeof item.attrValue === 'string' ? item.attrValue : JSON.stringify(attrMap)
    };
  });
}

function parseImages(value) {
  if (!value) return [];
  if (Array.isArray(value)) return value;
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [String(value)];
  } catch {
    return String(value).split(',').map(item => item.trim()).filter(Boolean);
  }
}

function parseAttrValue(value) {
  if (!value) return {};
  if (typeof value === 'object') return value;
  try {
    return JSON.parse(value);
  } catch {
    return {};
  }
}

function attrSuk(attrMap) {
  const values = Object.values(attrMap || {}).filter(Boolean);
  return values.length ? values.join(',') : '默认';
}

function number(value, fallback = 0) {
  const result = Number(value);
  return Number.isFinite(result) ? result : fallback;
}

function maxQuota(row) {
  return row.stock > 0 ? row.stock : undefined;
}

function prevOrNext() {
  if (currentTab.value === 'base') currentTab.value = 'rule';
  else if (currentTab.value === 'rule') currentTab.value = 'detail';
  else currentTab.value = 'rule';
}

function goBack() {
  const path = '/marketing/bargain/bargainGoods';
  window.history.pushState({}, '', path);
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path } }));
}

function assetUrl(value) {
  if (!value) return '';
  if (/^(https?:)?\/\//.test(value) || value.startsWith('/')) return value;
  if (value.startsWith('crmebimage/')) return `/${value}`;
  return `/crmebimage/${value.replace(/^\/+/, '')}`;
}
</script>

<style scoped>
.bargain-product-form .page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 0 4px;
}

.bargain-product-form h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.bargain-product-form p {
  margin: 6px 0 0;
  color: #909399;
  font-size: 13px;
}

.form-control {
  width: 100%;
}

.preview-image {
  width: 72px;
  height: 72px;
  margin-top: 10px;
  border-radius: 4px;
}

.sku-form-item :deep(.el-form-item__content) {
  display: block;
}

.sku-toolbar,
.product-search,
.dialog-pagination {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sku-toolbar {
  justify-content: space-between;
  margin-bottom: 10px;
  color: #606266;
}

.sku-table {
  width: 100%;
}

.sku-image {
  width: 42px;
  height: 42px;
  border-radius: 4px;
}

.empty-image {
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #dcdfe6;
  color: #909399;
}

.table-number,
.table-input {
  width: 100%;
}

.product-search {
  margin-bottom: 12px;
}

.dialog-pagination {
  justify-content: flex-end;
  margin-top: 12px;
}

.submit-row {
  margin-top: 24px;
}
</style>
