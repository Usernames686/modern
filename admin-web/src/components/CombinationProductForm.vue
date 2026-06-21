<template>
  <div class="divBox combination-product-form">
    <div class="page-header">
      <div>
        <h3>{{ pageTitle }}</h3>
        <p>配置拼团商品、规格 SKU、活动限量和商品详情。</p>
      </div>
      <el-button @click="goBack">返回列表</el-button>
    </div>

    <el-card class="box-card mt14" shadow="never">
      <el-tabs v-model="currentTab">
        <el-tab-pane v-if="!isEdit" label="选择商品" name="select" />
        <el-tab-pane label="基础信息" name="base" />
        <el-tab-pane label="商品详情" name="detail" />
      </el-tabs>

      <el-form ref="formRef" v-loading="loading" :model="form" :rules="rules" :disabled="isReadonly" label-width="160px" class="formValidate mt20">
        <template v-if="currentTab === 'select'">
          <el-form-item label="选择商品：" prop="productId">
            <div class="select-product-box" @click="openProductDialog">
              <el-image v-if="form.image" class="select-product-image" :src="assetUrl(form.image)" fit="cover" />
              <div v-else class="select-product-empty">
                <el-icon><Camera /></el-icon>
              </div>
              <div class="select-product-meta">
                <div class="select-product-title">{{ form.title || '请选择普通商品' }}</div>
                <div class="select-product-tip">
                  {{ form.productId ? `商品ID：${form.productId}，已载入 ${skuRows.length} 个规格` : '先选择商品，再补充拼团活动信息' }}
                </div>
              </div>
            </div>
          </el-form-item>
        </template>

        <template v-else-if="currentTab === 'base'">
          <el-row :gutter="24">
            <el-col :span="24">
              <el-alert
                type="success"
                show-icon
                :closable="false"
                title="当前表单已按老 CRMEB 拼团结构保存规格和活动 SKU；编辑多规格时保留已选择的拼团规格。"
              />
            </el-col>
            <el-col :md="12" :xs="24">
              <el-form-item label="主商品ID：" prop="productId">
                <el-input v-model="form.productId" :disabled="isEdit" placeholder="请选择商品" class="form-control">
                  <template #append>
                    <el-button :disabled="isEdit || isReadonly" @click="openProductDialog">选择</el-button>
                  </template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :md="12" :xs="24">
              <el-form-item label="拼团名称：" prop="title">
                <el-input v-model.trim="form.title" maxlength="249" placeholder="请输入拼团商品名称" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="推荐图：" prop="image">
                <div class="legacy-upload-row">
                  <div class="upLoadPicBox" @click="triggerMainImageUpload">
                    <el-image v-if="form.image" class="pictrue" :src="assetUrl(form.image)" fit="cover" />
                    <div v-else class="upLoad"><el-icon><Camera /></el-icon></div>
                  </div>
                  <div class="upload-control">
                    <el-input v-model.trim="form.image" placeholder="请输入或上传图片路径" class="form-control">
                      <template #append>
                        <el-upload ref="mainImageUploadRef" :show-file-list="false" :http-request="uploadMainImage" accept="image/*">
                          <el-button :loading="uploadingImage">上传</el-button>
                        </el-upload>
                      </template>
                    </el-input>
                  </div>
                </div>
              </el-form-item>
            </el-col>
            <el-col :md="12" :xs="24">
              <el-form-item label="单位：" prop="unitName">
                <el-input v-model.trim="form.unitName" maxlength="32" placeholder="请输入单位" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="商品轮播图：" prop="imagesText">
                <div class="slider-uploader">
                  <div v-for="(item, index) in carouselImages" :key="`${item}-${index}`" class="slider-thumb">
                    <video v-if="isVideo(item)" :src="assetUrl(item)" muted />
                    <el-image v-else :src="assetUrl(item)" fit="cover" />
                    <el-button
                      v-if="!isReadonly"
                      class="slider-remove"
                      type="danger"
                      circle
                      size="small"
                      :icon="Close"
                      @click="removeCarouselImage(index)"
                    />
                  </div>
                  <el-upload
                    v-if="carouselImages.length < 10 && !isReadonly"
                    :show-file-list="false"
                    :http-request="uploadCarouselImage"
                    accept="image/*,video/mp4"
                    multiple
                  >
                    <div class="upLoadPicBox add-slider">
                      <div class="upLoad"><el-icon><Camera /></el-icon></div>
                    </div>
                  </el-upload>
                </div>
                <el-input
                  v-model="form.imagesText"
                  type="textarea"
                  :rows="3"
                  placeholder="每行一个图片/视频路径，保存时会按老 JSON 数组格式写入 images"
                />
                <div class="from-tips">最多 10 张，支持图片和 mp4；上方缩略图会按当前文本顺序保存。</div>
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
                <div class="from-tips">设置活动开启结束时间，用户可以在设置时间内发起参与拼团。</div>
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
              <el-form-item label="拼团人数：" prop="people">
                <el-input-number v-model="form.people" :min="2" :step="1" step-strictly controls-position="right" class="form-control" />
                <div class="from-tips">单次拼团需要参与的用户数。</div>
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="拼团时效(单位 小时)：" prop="effectiveTime">
                <el-input-number v-model="form.effectiveTime" :min="1" :step="1" step-strictly controls-position="right" class="form-control" />
                <div class="from-tips">用户发起拼团后开始计时，超过时效未成团则系统判定拼团失败并按订单流程退款。</div>
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="购买数量限制：" prop="num">
                <el-input-number v-model="form.num" :min="1" :step="1" step-strictly controls-position="right" class="form-control" />
                <div class="from-tips">活动时间内每个用户参与拼团的次数限制。</div>
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="单次购买数量限制：" prop="onceNum">
                <el-input-number v-model="form.onceNum" :min="1" :max="form.num || undefined" :step="1" step-strictly controls-position="right" class="form-control" />
                <div class="from-tips">用户参与拼团时，一次购买最大数量限制。</div>
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="排序：" prop="sort">
                <el-input-number v-model="form.sort" :min="0" :max="9999" :step="1" step-strictly controls-position="right" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="拼团价：" prop="price">
                <el-input-number v-model="form.price" :min="0" :precision="2" controls-position="right" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="原价：" prop="otPrice">
                <el-input-number v-model="form.otPrice" :min="0" :precision="2" controls-position="right" class="form-control" />
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
                <div class="from-tips">活动商品总限量，多规格保存时按 SKU 限量汇总。</div>
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="库存：" prop="stock">
                <el-input-number v-model="form.stock" :min="0" :step="1" step-strictly controls-position="right" class="form-control" />
                <div class="from-tips">单规格可直接设置，多规格会按规格库存汇总。</div>
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="补齐人数：">
                <el-input-number v-model="form.virtualRation" :min="0" :max="Math.max(Number(form.people || 2) - 1, 0)" :step="1" step-strictly controls-position="right" class="form-control" />
                <div class="from-tips">成团时效内未满团时，可按配置补齐虚拟人数。</div>
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
            <el-col :md="8" :xs="24">
              <el-form-item label="是否包邮：">
                <el-radio-group v-model="form.isPostage">
                  <el-radio :value="1">包邮</el-radio>
                  <el-radio :value="0">不包邮</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="邮费：">
                <el-input-number v-model="form.postage" :min="0" :precision="2" controls-position="right" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="活动简介：">
                <el-input v-model.trim="form.info" maxlength="255" placeholder="请输入活动简介" class="form-control" />
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
                  <span>已载入 {{ skuRows.length }} 个规格，{{ selectedSkuRows.length }} 个参与拼团</span>
                  <div class="sku-actions">
                    <el-button size="small" :disabled="!selectedSkuRows.length" @click="applySummaryToSkuRows">按上方价格/限量填充</el-button>
                    <el-button size="small" :disabled="!form.productId" @click="refreshProductSpecs">重新载入商品规格</el-button>
                  </div>
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
                  <el-table-column label="拼团价" min-width="130">
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
                  <el-table-column label="体积(m³)" min-width="120">
                    <template #default="{ row }">
                      <el-input-number v-model="row.volume" :min="0" :precision="2" controls-position="right" class="table-number" />
                    </template>
                  </el-table-column>
                  <el-table-column label="商品编号" min-width="140">
                    <template #default="{ row }">
                      <el-input v-model="row.barCode" class="table-input" />
                    </template>
                  </el-table-column>
                </el-table>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="活动状态：" prop="isShow">
                <el-radio-group v-model="form.isShow">
                  <el-radio :value="0">关闭</el-radio>
                  <el-radio :value="1">开启</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <template v-else>
          <el-form-item label="商品详情：">
            <div v-if="isReadonly" class="content-preview" v-html="form.content || '-'" />
            <el-input v-else v-model="form.content" type="textarea" :rows="12" placeholder="可保留老 HTML 内容" />
          </el-form-item>
        </template>

        <el-form-item class="submit-row" :disabled="false">
          <el-button @click="handleStepChange">{{ stepButtonText }}</el-button>
          <el-button v-if="!isReadonly" type="primary" :loading="saving" @click="submit">提交</el-button>
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
import { Camera, Close } from '@element-plus/icons-vue';
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  combinationProductDetail,
  combinationInfo,
  combinationSave,
  combinationUpdate,
  productDetail,
  productList,
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
const mainImageUploadRef = ref();
const loading = ref(false);
const saving = ref(false);
const uploadingImage = ref(false);
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
  productId: undefined,
  title: '',
  image: '',
  imagesText: '',
  unitName: '件',
  timeVal: [],
  tempId: 0,
  people: 2,
  effectiveTime: 1,
  num: 1,
  onceNum: 1,
  price: 0,
  otPrice: 0,
  cost: 0,
  sort: 0,
  stock: 1,
  quota: 1,
  virtualRation: 0,
  weight: 0,
  volume: 0,
  isPostage: 0,
  postage: 0,
  info: '',
  isShow: 0,
  specType: false,
  content: ''
});

const rules = {
  productId: [{ required: true, message: '主商品ID不能为空', trigger: 'blur' }],
  title: [{ required: true, message: '请输入拼团名称', trigger: 'blur' }],
  image: [{ required: true, message: '请填写推荐图', trigger: 'blur' }],
  imagesText: [{ required: true, message: '请填写商品轮播图', trigger: 'blur' }],
  unitName: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  tempId: [{ required: true, message: '请选择运费模板', trigger: 'change' }],
  timeVal: [{ required: true, type: 'array', min: 2, message: '请选择活动日期', trigger: 'change' }],
  people: [{ required: true, message: '请输入拼团人数', trigger: 'blur' }],
  effectiveTime: [{ required: true, message: '请输入拼团时效', trigger: 'blur' }],
  num: [{ required: true, message: '请输入购买数量', trigger: 'blur' }],
  onceNum: [{ required: true, message: '请输入单次购买数量', trigger: 'blur' }],
  sort: [{ required: true, message: '请输入排序', trigger: 'blur' }],
  price: [{ required: true, message: '请输入拼团价', trigger: 'blur' }],
  otPrice: [{ required: true, message: '请输入原价', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
  quota: [{ required: true, message: '请输入限量', trigger: 'blur' }]
};

const routeParts = computed(() => props.path.split('/').filter(Boolean));
const combinationId = computed(() => Number(routeParts.value[3] || 0));
const isEdit = computed(() => combinationId.value > 0);
const isReadonly = computed(() => routeParts.value.includes('info'));
const pageTitle = computed(() => {
  if (isReadonly.value) return `拼团商品详情-${combinationId.value}`;
  return isEdit.value ? `编辑拼团商品-${combinationId.value}` : '添加拼团商品';
});
const selectedSkuRows = computed(() => skuRows.value.filter((row) => !form.specType || row.enabled));
const carouselImages = computed(() => parseImages(form.imagesText).slice(0, 10));
const stepButtonText = computed(() => {
  if (currentTab.value === 'select' || currentTab.value === 'base') return '下一步';
  return '上一步';
});
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
    currentTab.value = 'select';
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
    const data = await combinationInfo({ id: combinationId.value });
    form.productId = data.productId || undefined;
    form.title = data.title || '';
    form.image = data.image || '';
    form.imagesText = parseImages(data.sliderImage || data.images).join('\n');
    form.unitName = data.unitName || '件';
    form.timeVal = normalizeDateRange(data.startTimeStr || data.startTime, data.stopTimeStr || data.stopTime);
    form.tempId = Number(data.tempId || 0);
    form.people = Number(data.people || 2);
    form.effectiveTime = Number(data.effectiveTime || 1);
    form.num = Number(data.num || 1);
    form.onceNum = Number(data.onceNum || 1);
    form.price = Number(data.price || 0);
    form.otPrice = Number(data.otPrice || 0);
    form.cost = Number(data.cost || 0);
    form.sort = Number(data.sort || 0);
    form.stock = Number(data.stock || 0);
    form.quota = Number(data.quotaShow || data.quota || data.stock || 1);
    form.virtualRation = Number(data.virtualRation || 0);
    form.weight = Number(data.weight || 0);
    form.volume = Number(data.volume || 0);
    form.isPostage = Number(data.isPostage || 0);
    form.postage = Number(data.postage || 0);
    form.info = data.info || '';
    form.isShow = Number(data.isShow ? 1 : 0);
    form.specType = Boolean(data.specType);
    form.content = data.content || '';
    attrRows.value = normalizeAttrs(data.attr || [], form.specType);
    skuRows.value = normalizeSkuRows(toArray(data.attrValue), true);
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
    const data = await loadCombinationProductDetail(row.id);
    applyProductDetail(data);
    productDialogVisible.value = false;
    if (!isEdit.value) currentTab.value = 'base';
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
  const data = await loadCombinationProductDetail(Number(form.productId));
  applyProductDetail(data, true);
  ElMessage.success('商品规格已重新载入');
}

async function loadCombinationProductDetail(id) {
  return combinationProductDetail(id).catch(() => productDetail(id));
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
    form.info = data.storeInfo || form.info;
    form.sort = Number(data.sort || form.sort || 0);
    form.stock = Number(data.stock || form.stock || 1);
  }
  form.specType = Boolean(data.specType);
  attrRows.value = normalizeAttrs(data.attr || [], form.specType);
  skuRows.value = normalizeSkuRows(toArray(data.attrValue), true);
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

function triggerMainImageUpload() {
  if (isReadonly.value) return;
  mainImageUploadRef.value?.$el?.querySelector('input[type="file"]')?.click();
}

async function uploadCarouselImage(options) {
  if (carouselImages.value.length >= 10) {
    ElMessage.warning('最多选择10张图片');
    return;
  }
  const body = new FormData();
  body.append('multipart', options.file);
  const data = await uploadImage(body, { model: 'product', pid: 0 });
  const url = data.url || data.sattDir || '';
  if (!url) return;
  const next = [...carouselImages.value, url].slice(0, 10);
  form.imagesText = next.join('\n');
  ElMessage.success('上传成功');
}

function removeCarouselImage(index) {
  const next = [...carouselImages.value];
  next.splice(index, 1);
  form.imagesText = next.join('\n');
}

async function submit() {
  if (isReadonly.value) return;
  await formRef.value?.validate();
  saving.value = true;
  try {
    const payload = buildPayload();
    if (isEdit.value) {
      await combinationUpdate({ id: combinationId.value }, payload);
      ElMessage.success('编辑成功');
    } else {
      await combinationSave(payload);
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
    id: isEdit.value ? combinationId.value : undefined,
    productId: Number(form.productId),
    title: form.title,
    image: form.image,
    imagess: images,
    images: JSON.stringify(images),
    sliderImage: JSON.stringify(images),
    unitName: form.unitName,
    startTime: form.timeVal?.[0],
    stopTime: form.timeVal?.[1],
    tempId: form.tempId,
    people: form.people,
    effectiveTime: form.effectiveTime,
    num: form.num,
    onceNum: form.onceNum,
    price: minBy(attrValue, 'price', form.price),
    otPrice: minBy(attrValue, 'otPrice', form.otPrice),
    cost: minBy(attrValue, 'cost', form.cost),
    stock: stock || form.stock || form.quota,
    quota: quota || form.quota,
    quotaShow: quota || form.quota,
    virtualRation: form.virtualRation,
    sort: form.sort,
    weight: form.weight,
    volume: form.volume,
    isPostage: form.isPostage,
    postage: form.postage,
    info: form.info,
    isShow: form.isShow,
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
    throw new Error('请至少选择一个参与拼团的商品规格');
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

function applySummaryToSkuRows() {
  const rows = selectedSkuRows.value;
  rows.forEach((row) => {
    row.price = form.price;
    row.otPrice = form.otPrice;
    row.cost = form.cost;
    row.quota = Math.min(number(form.quota, row.quota), number(row.stock, form.quota) || number(form.quota, row.quota));
    row.weight = form.weight;
    row.volume = form.volume;
  });
  ElMessage.success('已填充当前参与拼团规格');
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
  return toArray(list).map((item) => {
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
  form.stock = rows.reduce((sum, row) => sum + number(row.stock, 0), 0) || form.stock;
  form.weight = number(rows[0].weight, form.weight);
  form.volume = number(rows[0].volume, form.volume);
}

function handleStepChange() {
  if (currentTab.value === 'select') {
    if (!form.productId) {
      ElMessage.warning('请先选择商品');
      return;
    }
    currentTab.value = 'base';
    return;
  }
  currentTab.value = currentTab.value === 'base' ? 'detail' : 'base';
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

function toArray(value) {
  if (Array.isArray(value)) return value;
  if (!value) return [];
  if (typeof value === 'object') return Object.values(value);
  return [];
}

function normalizeDateRange(start, stop) {
  const startValue = normalizeDate(start);
  const stopValue = normalizeDate(stop);
  return startValue && stopValue ? [startValue, stopValue] : [];
}

function normalizeDate(value) {
  if (!value) return '';
  if (typeof value === 'number') return new Date(value).toISOString().slice(0, 10);
  const text = String(value);
  if (/^\d{13}$/.test(text)) return new Date(Number(text)).toISOString().slice(0, 10);
  if (/^\d{10}$/.test(text)) return new Date(Number(text) * 1000).toISOString().slice(0, 10);
  return text.slice(0, 10);
}

function isVideo(value) {
  return /\.mp4($|\?)/i.test(String(value || ''));
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
  const path = '/marketing/groupBuy/groupGoods';
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
.combination-product-form .page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 0 4px;
}

.combination-product-form h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.combination-product-form p {
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

.legacy-upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.upload-control {
  width: min(520px, 100%);
}

.upLoadPicBox,
.pictrue,
.upLoad {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  flex: none;
}

.upLoadPicBox {
  cursor: pointer;
}

.pictrue {
  border: 1px dotted rgba(0, 0, 0, 0.16);
}

.upLoad {
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #dcdfe6;
  color: #909399;
  font-size: 22px;
  background: #fff;
}

.slider-uploader {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
}

.slider-thumb {
  position: relative;
  width: 60px;
  height: 60px;
  border: 1px dotted rgba(0, 0, 0, 0.16);
  border-radius: 4px;
  overflow: hidden;
}

.slider-thumb :deep(.el-image),
.slider-thumb video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.slider-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 18px;
  height: 18px;
}

.add-slider {
  display: block;
}

.select-product-box {
  width: min(460px, 100%);
  min-height: 92px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  background: #fff;
}

.select-product-box:hover {
  border-color: #1890ff;
}

.select-product-image,
.select-product-empty {
  width: 64px;
  height: 64px;
  border-radius: 4px;
  flex: none;
}

.select-product-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #dcdfe6;
  color: #909399;
  font-size: 22px;
}

.select-product-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.select-product-tip,
.from-tips {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
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

.sku-actions {
  display: flex;
  gap: 8px;
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

.content-preview {
  width: 100%;
  min-height: 260px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
  color: #303133;
  line-height: 1.7;
}
</style>
