<template>
  <div class="divBox seckill-product-form">
    <div class="page-header">
      <div>
        <h3>{{ isEdit ? `编辑秒杀商品-${seckillId}` : '添加秒杀商品' }}</h3>
        <p>配置秒杀商品、规格 SKU、活动库存和商品详情。</p>
      </div>
      <el-button @click="goBack">返回列表</el-button>
    </div>

    <el-card class="box-card mt14" shadow="never">
      <el-tabs v-model="currentTab">
        <el-tab-pane label="基础信息" name="base" />
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
                title="当前表单已按老 CRMEB 秒杀结构保存 type=1 规格和活动 SKU；编辑多规格时不会再被覆盖成单规格。"
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
              <el-form-item label="商品标题：" prop="title">
                <el-input v-model.trim="form.title" maxlength="249" placeholder="请输入商品名称" class="form-control" />
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
              <el-form-item label="活动时间：" prop="timeId">
                <el-select v-model="form.timeId" placeholder="请选择" class="form-control">
                  <el-option v-for="item in managers" :key="item.id" :label="`${item.name} | ${timeText(item.time)}`" :value="item.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :md="12" :xs="24">
              <el-form-item label="运费模板：" prop="tempId">
                <el-select v-model="form.tempId" placeholder="请选择" class="form-control">
                  <el-option v-for="item in shippingTemplates" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :md="12" :xs="24">
              <el-form-item label="当天参与次数：" prop="num">
                <el-input-number v-model="form.num" :min="1" :step="1" step-strictly controls-position="right" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="秒杀价：" prop="price">
                <el-input-number v-model="form.price" :min="0" :precision="2" controls-position="right" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="成本价：">
                <el-input-number v-model="form.cost" :min="0" :precision="2" controls-position="right" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="原价：" prop="otPrice">
                <el-input-number v-model="form.otPrice" :min="0" :precision="2" controls-position="right" class="form-control" />
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
              <el-form-item label="商品规格：" prop="specType">
                <el-radio-group v-model="form.specType">
                  <el-radio :value="false">单规格</el-radio>
                  <el-radio :value="true">多规格</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="活动规格：" class="sku-form-item">
                <div class="sku-toolbar">
                  <span>已载入 {{ skuRows.length }} 个规格，{{ selectedSkuRows.length }} 个参与秒杀</span>
                  <el-button size="small" :disabled="!form.productId" @click="refreshProductSpecs">重新载入商品规格</el-button>
                </div>
                <el-table :data="skuRows" border size="small" class="sku-table">
                  <el-table-column v-if="form.specType" label="参与" width="72" fixed>
                    <template #default="{ row }">
                      <el-checkbox v-model="row.enabled" />
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
                  <el-table-column label="库存" min-width="110">
                    <template #default="{ row }">
                      <el-input-number v-model="row.stock" :min="0" :step="1" step-strictly controls-position="right" class="table-number" />
                    </template>
                  </el-table-column>
                  <el-table-column label="限量" min-width="120">
                    <template #default="{ row }">
                      <el-input-number v-model="row.quota" :min="1" :max="maxQuota(row)" :step="1" step-strictly controls-position="right" class="table-number" />
                    </template>
                  </el-table-column>
                  <el-table-column label="秒杀价" min-width="130">
                    <template #default="{ row }">
                      <el-input-number v-model="row.price" :min="0" :precision="2" controls-position="right" class="table-number" />
                    </template>
                  </el-table-column>
                  <el-table-column label="原价" min-width="130">
                    <template #default="{ row }">
                      <el-input-number v-model="row.otPrice" :min="0" :precision="2" controls-position="right" class="table-number" />
                    </template>
                  </el-table-column>
                  <el-table-column label="成本价" min-width="130">
                    <template #default="{ row }">
                      <el-input-number v-model="row.cost" :min="0" :precision="2" controls-position="right" class="table-number" />
                    </template>
                  </el-table-column>
                  <el-table-column label="重量" min-width="120">
                    <template #default="{ row }">
                      <el-input-number v-model="row.weight" :min="0" :precision="2" controls-position="right" class="table-number" />
                    </template>
                  </el-table-column>
                  <el-table-column label="体积" min-width="120">
                    <template #default="{ row }">
                      <el-input-number v-model="row.volume" :min="0" :precision="2" controls-position="right" class="table-number" />
                    </template>
                  </el-table-column>
                  <el-table-column label="编码" min-width="140">
                    <template #default="{ row }">
                      <el-input v-model="row.barCode" class="table-input" />
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

        <template v-else>
          <el-form-item label="商品详情：">
            <el-input v-model="form.content" type="textarea" :rows="12" placeholder="可保留老 HTML 内容" />
          </el-form-item>
        </template>

        <el-form-item class="submit-row">
          <el-button @click="currentTab = currentTab === 'base' ? 'detail' : 'base'">
            {{ currentTab === 'base' ? '下一步' : '上一步' }}
          </el-button>
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
import {
  productDetail,
  productList,
  seckillManagerList,
  seckillStoreInfo,
  seckillStoreSave,
  seckillStoreUpdate,
  shippingTemplatesList,
  uploadImage
} from '../api';

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
const managers = ref([]);
const shippingTemplates = ref([]);
const attrRows = ref([]);
const skuRows = ref([]);
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
  id: 0,
  productId: undefined,
  title: '',
  image: '',
  imagesText: '',
  unitName: '件',
  timeId: undefined,
  timeVal: [],
  tempId: 0,
  num: 1,
  price: 0,
  cost: 0,
  otPrice: 0,
  quota: 1,
  weight: 0,
  volume: 0,
  status: 0,
  specType: false,
  content: ''
});

const rules = {
  productId: [{ required: true, message: '主商品ID不能为空', trigger: 'blur' }],
  title: [{ required: true, message: '请输入商品标题', trigger: 'blur' }],
  image: [{ required: true, message: '请填写商品主图', trigger: 'blur' }],
  imagesText: [{ required: true, message: '请填写商品轮播图', trigger: 'blur' }],
  unitName: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  timeId: [{ required: true, message: '请选择活动时间', trigger: 'change' }],
  tempId: [{ required: true, message: '请选择运费模板', trigger: 'change' }],
  timeVal: [{ required: true, type: 'array', min: 2, message: '请选择活动日期', trigger: 'change' }],
  num: [{ required: true, message: '请输入当天参与次数', trigger: 'blur' }],
  price: [{ required: true, message: '请输入秒杀价', trigger: 'blur' }],
  otPrice: [{ required: true, message: '请输入原价', trigger: 'blur' }],
  quota: [{ required: true, message: '请输入限量', trigger: 'blur' }]
};

const routeParts = computed(() => props.path.split('/').filter(Boolean));
const isEdit = computed(() => routeParts.value[3] === 'updeta' && Number(routeParts.value[5]) > 0);
const seckillId = computed(() => (isEdit.value ? Number(routeParts.value[5]) : 0));
const preferredTimeId = computed(() => {
  if (routeParts.value[3] === 'creat' && Number(routeParts.value[4]) > 0) return Number(routeParts.value[4]);
  return 0;
});
const selectedSkuRows = computed(() => skuRows.value.filter((row) => !form.specType || row.enabled));
const attrColumnNames = computed(() => {
  const names = attrRows.value.map((item) => item.attrName).filter(Boolean);
  if (names.length) return names;
  const first = skuRows.value.find((row) => row.attrMap && Object.keys(row.attrMap).length);
  return first ? Object.keys(first.attrMap) : [];
});

onMounted(async () => {
  await Promise.all([loadManagers(), loadShippingTemplates()]);
  if (isEdit.value) {
    await loadInfo();
  } else {
    if (preferredTimeId.value) form.timeId = preferredTimeId.value;
    resetDefaultSku();
  }
});

async function loadManagers() {
  const data = await seckillManagerList({ page: 1, limit: 200 }).catch(() => ({ list: [] }));
  managers.value = data?.list || [];
}

async function loadShippingTemplates() {
  const data = await shippingTemplatesList({ page: 1, limit: 200 }).catch(() => ({ list: [] }));
  shippingTemplates.value = data?.list || [];
}

async function loadInfo() {
  loading.value = true;
  try {
    const data = await seckillStoreInfo({ id: seckillId.value });
    form.id = data.id || 0;
    form.productId = data.productId || undefined;
    form.title = data.storeName || data.title || '';
    form.image = data.image || '';
    form.imagesText = parseImages(data.sliderImage || data.images).join('\n');
    form.unitName = data.unitName || '件';
    form.timeId = Number(data.timeId || 0) || undefined;
    form.timeVal = data.startTimeStr && data.stopTimeStr ? [data.startTimeStr, data.stopTimeStr] : [];
    form.tempId = Number(data.tempId || 0);
    form.num = Number(data.num || 1);
    form.price = Number(data.price || 0);
    form.cost = Number(data.cost || 0);
    form.otPrice = Number(data.otPrice || 0);
    form.quota = Number(data.quotaShow || data.quota || data.stock || 1);
    form.weight = Number(data.weight || 0);
    form.volume = Number(data.volume || 0);
    form.status = Number(data.status || 0);
    form.specType = Boolean(data.specType);
    form.content = data.content || data.description || '';
    attrRows.value = normalizeAttrs(data.attr || [], form.specType);
    skuRows.value = normalizeSkuRows(data.attrValue || [], true);
    if (!skuRows.value.length) resetDefaultSku();
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
    form.tempId = Number(data.tempId || form.tempId || 0);
    form.content = data.content || form.content;
  }
  form.specType = Boolean(data.specType);
  attrRows.value = normalizeAttrs(data.attr || [], form.specType);
  skuRows.value = normalizeSkuRows(data.attrValue || [], true);
  if (!skuRows.value.length) resetDefaultSku();
  syncSummaryFromRows();
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
  saving.value = true;
  try {
    const payload = buildPayload();
    if (isEdit.value) {
      await seckillStoreUpdate({ id: seckillId.value }, payload);
      ElMessage.success('编辑成功');
    } else {
      await seckillStoreSave(payload);
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
  const images = form.imagesText.split('\n').map((item) => item.trim()).filter(Boolean);
  const attrValue = buildAttrValuePayload();
  const stock = attrValue.reduce((sum, row) => sum + number(row.stock, 0), 0);
  const quota = attrValue.reduce((sum, row) => sum + number(row.quota, 0), 0);
  return {
    id: isEdit.value ? seckillId.value : undefined,
    productId: Number(form.productId),
    title: form.title,
    image: form.image,
    imagess: images,
    images: JSON.stringify(images),
    unitName: form.unitName,
    timeId: form.timeId,
    startTime: form.timeVal?.[0],
    stopTime: form.timeVal?.[1],
    tempId: form.tempId,
    num: form.num,
    price: minBy(attrValue, 'price', form.price),
    cost: minBy(attrValue, 'cost', form.cost),
    otPrice: minBy(attrValue, 'otPrice', form.otPrice),
    stock: stock || form.quota,
    quota: quota || form.quota,
    quotaShow: quota || form.quota,
    weight: form.weight,
    volume: form.volume,
    status: form.status,
    specType: form.specType,
    attr: buildAttrPayload(),
    content: form.content,
    attrValue
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

function buildAttrValuePayload() {
  if (!form.specType) {
    const first = skuRows.value[0] || {};
    const quota = Number(form.quota || first.quota || first.stock || 1);
    return [{
      suk: first.suk || '默认',
      image: form.image || first.image || '',
      price: form.price,
      cost: form.cost,
      otPrice: form.otPrice,
      stock: number(first.stock, quota),
      quota,
      quotaShow: quota,
      weight: form.weight,
      volume: form.volume,
      barCode: first.barCode || '',
      attrValue: JSON.stringify(first.attrMap && Object.keys(first.attrMap).length ? first.attrMap : { 规格: '默认' })
    }];
  }
  const rows = selectedSkuRows.value;
  if (!rows.length) {
    throw new Error('请至少选择一个参与秒杀的商品规格');
  }
  return rows.map((row) => {
    const quota = number(row.quota, 0);
    const stock = number(row.stock, quota);
    if (quota <= 0) throw new Error(`规格「${row.suk || '默认'}」限量必须大于0`);
    if (stock > 0 && quota > stock) throw new Error(`规格「${row.suk || '默认'}」限量不能大于库存`);
    return {
      suk: row.suk || attrSuk(row.attrMap),
      image: row.image || form.image,
      price: number(row.price, form.price),
      cost: number(row.cost, form.cost),
      otPrice: number(row.otPrice, form.otPrice),
      stock,
      quota,
      quotaShow: quota,
      weight: number(row.weight, form.weight),
      volume: number(row.volume, form.volume),
      barCode: row.barCode || '',
      attrValue: JSON.stringify(row.attrMap && Object.keys(row.attrMap).length ? row.attrMap : parseAttrValue(row.attrValue))
    };
  });
}

function resetDefaultSku() {
  attrRows.value = [{ attrName: '规格', attrValues: '默认' }];
  skuRows.value = [{
    enabled: true,
    suk: '默认',
    image: form.image,
    stock: form.quota || 1,
    quota: form.quota || 1,
    price: form.price || 0,
    cost: form.cost || 0,
    otPrice: form.otPrice || 0,
    weight: form.weight || 0,
    volume: form.volume || 0,
    barCode: '',
    attrMap: { 规格: '默认' },
    attrValue: '{"规格":"默认"}'
  }];
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

function normalizeSkuRows(list, enabled) {
  return (list || []).map((item) => {
    const attrMap = parseAttrValue(item.attrValue);
    const stock = number(item.stock, number(item.quota, 1));
    const quota = number(item.quotaShow, number(item.quota, stock || 1));
    return {
      ...item,
      enabled,
      suk: item.suk || attrSuk(attrMap),
      image: item.image || form.image,
      stock,
      quota: Math.min(quota || stock || 1, stock || quota || 1),
      price: number(item.price, 0),
      cost: number(item.cost, 0),
      otPrice: number(item.otPrice, 0),
      weight: number(item.weight, 0),
      volume: number(item.volume, 0),
      barCode: item.barCode || '',
      attrMap,
      attrValue: typeof item.attrValue === 'string' ? item.attrValue : JSON.stringify(attrMap)
    };
  });
}

function syncSummaryFromRows() {
  const rows = selectedSkuRows.value.length ? selectedSkuRows.value : skuRows.value;
  if (!rows.length) return;
  form.price = minBy(rows, 'price', form.price);
  form.otPrice = minBy(rows, 'otPrice', form.otPrice);
  form.cost = minBy(rows, 'cost', form.cost);
  form.quota = rows.reduce((sum, row) => sum + number(row.quota, 0), 0) || form.quota;
  form.weight = number(rows[0].weight, form.weight);
  form.volume = number(rows[0].volume, form.volume);
}

function parseImages(value) {
  if (!value) return [];
  if (Array.isArray(value)) return value;
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [String(value)];
  } catch {
    return String(value).split(',').map((item) => item.trim()).filter(Boolean);
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

function minBy(rows, key, fallback) {
  const values = rows.map((row) => number(row[key], 0)).filter((value) => value > 0);
  return values.length ? Math.min(...values) : number(fallback, 0);
}

function number(value, fallback = 0) {
  const result = Number(value);
  return Number.isFinite(result) ? result : fallback;
}

function maxQuota(row) {
  return row.stock > 0 ? row.stock : undefined;
}

function goBack() {
  const path = '/marketing/seckill/list';
  window.history.pushState({}, '', path);
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path } }));
}

function timeText(value) {
  return value ? String(value).split(',').join(' - ') : '-';
}

function assetUrl(value) {
  if (!value) return '';
  if (/^(https?:)?\/\//.test(value) || value.startsWith('/')) return value;
  if (value.startsWith('crmebimage/')) return `/${value}`;
  return `/crmebimage/${value.replace(/^\/+/, '')}`;
}
</script>

<style scoped>
.seckill-product-form .page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 0 4px;
}

.seckill-product-form h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.seckill-product-form p {
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
