<template>
  <div class="product-basic-editor">
    <el-skeleton v-if="loading" :rows="10" animated />
    <template v-else>
      <el-tabs v-model="currentTab" class="list-tabs">
        <el-tab-pane label="商品信息" name="0" />
        <el-tab-pane label="其他设置" name="1" />
        <el-tab-pane label="商品详情" name="2" />
      </el-tabs>

      <el-form ref="formRef" :model="form" :rules="rules" class="formValidate mt20" label-width="110px">
        <el-row v-show="currentTab === '0'" :gutter="24">
          <el-col :span="24">
            <el-form-item label="商品名称：" prop="storeName">
              <el-input v-model="form.storeName" maxlength="128" class="from-ipt-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品分类：" prop="cateId">
              <el-cascader
                v-model="form.cateId"
                :options="categoryOptions"
                :props="categoryProps"
                clearable
                class="from-ipt-width"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品关键字：">
              <el-input v-model="form.keyword" maxlength="256" class="from-ipt-width" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品简介：">
              <el-input v-model="form.storeInfo" maxlength="256" class="from-ipt-width" placeholder="请输入商品简介" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="售价：" prop="price">
              <el-input-number v-model="form.price" :min="0" :precision="2" controls-position="right" class="from-ipt-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="会员价：">
              <el-input-number v-model="form.vipPrice" :min="0" :precision="2" controls-position="right" class="from-ipt-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="市场价：">
              <el-input-number v-model="form.otPrice" :min="0" :precision="2" controls-position="right" class="from-ipt-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="成本价：">
              <el-input-number v-model="form.cost" :min="0" :precision="2" controls-position="right" class="from-ipt-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位：">
              <el-input v-model="form.unitName" maxlength="32" class="from-ipt-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="当前库存：">
              <el-input :model-value="detail.stock" disabled class="from-ipt-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主图视频：">
              <el-input v-model="form.videoLink" maxlength="200" class="from-ipt-width" placeholder="请输入视频链接" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="运费模板：" prop="tempId">
              <el-select v-model="form.tempId" class="from-ipt-width" placeholder="请选择">
                <el-option v-for="item in shippingOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="form.videoLink" :span="24">
            <div class="video-preview">
              <video :src="normalizeAsset(form.videoLink)" controls muted />
              <el-button @click="form.videoLink = ''">删除视频</el-button>
            </div>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品封面图：" prop="image">
              <div class="image-edit-row">
                <div v-if="form.image" class="pictrue">
                  <el-image class="image" :src="normalizeAsset(form.image)" :preview-src-list="[normalizeAsset(form.image)]" fit="cover" />
                </div>
                <button v-else type="button" class="upLoad" @click="openPicker('cover')">
                  <el-icon><Camera /></el-icon>
                </button>
                <el-button type="primary" @click="openPicker('cover')">选择图片</el-button>
                <el-button v-if="form.image" @click="form.image = ''">清除</el-button>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品轮播图：">
              <div class="image-list editable">
                <div v-for="(item, index) in form.sliderImages" :key="`${item}-${index}`" class="pictrue sortable-image">
                  <el-image class="image" :src="normalizeAsset(item)" :preview-src-list="normalizedSliderImages" fit="cover" />
                  <button type="button" class="remove-image" @click="removeSlider(index)">×</button>
                </div>
                <button type="button" class="upLoad" @click="openPicker('slider')">
                  <el-icon><Camera /></el-icon>
                </button>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row v-show="currentTab === '1'" :gutter="24">
          <el-col :span="8">
            <el-form-item label="排序：">
              <el-input-number v-model="form.sort" :min="0" controls-position="right" class="from-ipt-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="虚拟销量：">
              <el-input-number v-model="form.ficti" :min="0" controls-position="right" class="from-ipt-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="赠送积分：">
              <el-input-number v-model="form.giveIntegral" :min="0" controls-position="right" class="from-ipt-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="商品状态：">
              <el-switch v-model="form.isShow" active-text="上架" inactive-text="下架" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="佣金设置：">
              <el-radio-group v-model="form.isSub">
                <el-radio :label="false">默认设置</el-radio>
                <el-radio :label="true">单独设置</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品推荐：">
              <el-checkbox v-model="form.isHot">热卖单品</el-checkbox>
              <el-checkbox v-model="form.isBenefit">促销单品</el-checkbox>
              <el-checkbox v-model="form.isBest">精品推荐</el-checkbox>
              <el-checkbox v-model="form.isNew">首发新品</el-checkbox>
              <el-checkbox v-model="form.isGood">优品推荐</el-checkbox>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="活动排序：">
              <div class="activity-sort-list">
                <span v-for="(item, index) in form.activity" :key="item" class="activity-chip">
                  {{ item }}
                  <button type="button" :disabled="index === 0" @click="moveActivity(index, -1)">上移</button>
                  <button type="button" :disabled="index === form.activity.length - 1" @click="moveActivity(index, 1)">下移</button>
                </span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="优惠券：">
              <div class="coupon-select-row">
                <el-tag
                  v-for="item in form.coupons"
                  :key="item.id"
                  class="mr10"
                  closable
                  @close="removeCoupon(item.id)"
                >
                  {{ item.name || `优惠券 ${item.id}` }}
                </el-tag>
                <span v-if="!form.couponIds.length" class="muted-text">暂无优惠券</span>
                <el-button @click="openCouponDialog">选择优惠券</el-button>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row v-show="currentTab === '2'" :gutter="24">
          <el-col :span="24">
            <el-form-item label="商品详情：">
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="14"
                maxlength="200000"
                show-word-limit
                class="from-ipt-width"
                placeholder="请输入商品详情 HTML"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详情预览：">
              <div class="content-preview" v-html="form.content || '无'"></div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="drawer-footer">
        <el-button @click="$emit('cancel')">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
      </div>
    </template>

    <el-dialog v-model="pickerVisible" title="素材管理" width="860px" class="attachment-picker-dialog" append-to-body>
      <div class="attachment-picker">
        <div class="attachment-picker-tree">
          <el-tree
            :data="attachmentTree"
            :props="treeProps"
            node-key="id"
            default-expand-all
            highlight-current
            @node-click="handleAttachmentCategory"
          />
        </div>
        <div class="attachment-picker-main" v-loading="attachmentLoading">
          <div class="attachment-picker-toolbar">
            <el-upload multiple :show-file-list="false" :http-request="handleUpload" accept="image/*">
              <el-button type="primary">上传图片</el-button>
            </el-upload>
          </div>
          <div class="attachment-picker-grid">
            <div
              v-for="item in attachmentRows"
              :key="item.attId"
              class="attachment-picker-card"
              @click="selectAttachment(item)"
            >
              <img :src="normalizeAsset(item.sattDir)" :alt="item.name" />
              <span :title="item.name">{{ item.name }}</span>
            </div>
            <div v-if="!attachmentRows.length" class="attachment-picker-empty">素材为空</div>
          </div>
          <div class="block">
            <el-pagination
              v-model:current-page="attachmentQuery.page"
              :page-size="attachmentQuery.limit"
              layout="prev, pager, next"
              :total="attachmentTotal"
              background
              @current-change="loadAttachments"
            />
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="couponDialogVisible" title="优惠券列表" width="820px" append-to-body>
      <div class="coupon-picker">
        <div class="coupon-picker-toolbar">
          <el-input
            v-model="couponQuery.keywords"
            placeholder="请输入优惠券名称"
            clearable
            class="coupon-search"
            @keyup.enter="loadCoupons(1)"
          />
          <el-button type="primary" @click="loadCoupons(1)">搜索</el-button>
        </div>
        <el-table v-loading="couponLoading" :data="couponRows" border size="small" row-key="id" max-height="390">
          <el-table-column width="54">
            <template #default="{ row }">
              <el-checkbox :model-value="couponPickerIds.includes(row.id)" @change="toggleCoupon(row, $event)" />
            </template>
          </el-table-column>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="优惠券名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="money" label="优惠券面值" width="110" />
          <el-table-column label="最低消费额" width="120">
            <template #default="{ row }">{{ Number(row.minPrice || 0) === 0 ? '不限制' : row.minPrice }}</template>
          </el-table-column>
          <el-table-column label="有效期限" min-width="180">
            <template #default="{ row }">{{ couponTimeText(row) }}</template>
          </el-table-column>
          <el-table-column label="剩余数量" width="100">
            <template #default="{ row }">{{ row.isLimited ? row.lastTotal : '不限量' }}</template>
          </el-table-column>
        </el-table>
        <div class="coupon-picker-footer">
          <el-pagination
            v-model:current-page="couponQuery.page"
            v-model:page-size="couponQuery.limit"
            :page-sizes="[10, 20, 30, 40]"
            layout="total, sizes, prev, pager, next"
            :total="couponTotal"
            background
            @size-change="loadCoupons(1)"
            @current-change="loadCoupons"
          />
          <div>
            <el-button @click="couponDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="confirmCoupons">确定</el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Camera } from '@element-plus/icons-vue';
import {
  attachmentList,
  categoryTree,
  couponSendList,
  productCreate,
  productDetail,
  productUpdate,
  shippingTemplatesList,
  uploadImage
} from '../api';

const props = defineProps({
  productId: {
    type: Number,
    default: 0
  },
  initialProduct: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['success', 'cancel']);

const formRef = ref();
const currentTab = ref('0');
const loading = ref(false);
const saving = ref(false);
const pickerVisible = ref(false);
const couponDialogVisible = ref(false);
const pickerTarget = ref('cover');
const categoryOptions = ref([]);
const shippingOptions = ref([]);
const attachmentCategories = ref([]);
const attachmentRows = ref([]);
const attachmentTotal = ref(0);
const attachmentLoading = ref(false);
const couponRows = ref([]);
const couponTotal = ref(0);
const couponLoading = ref(false);
const couponPickerIds = ref([]);
const couponPickerRows = ref([]);
const detail = reactive({});

const form = reactive({
  id: props.productId,
  storeName: '',
  storeInfo: '',
  keyword: '',
  cateId: [],
  image: '',
  videoLink: '',
  sliderImages: [],
  price: 0,
  vipPrice: 0,
  otPrice: 0,
  cost: 0,
  unitName: '件',
  giveIntegral: 0,
  tempId: '',
  sort: 32760,
  ficti: 0,
  isShow: true,
  isSub: false,
  isHot: false,
  isBenefit: false,
  isBest: false,
  isNew: false,
  isGood: false,
  activity: ['默认', '秒杀', '砍价', '拼团'],
  content: '',
  couponIds: [],
  coupons: []
});

const attachmentQuery = reactive({
  page: 1,
  limit: 12,
  pid: 0,
  attType: 'jpg,jpeg,gif,png,bmp,PNG,JPG'
});

const couponQuery = reactive({
  page: 1,
  limit: 10,
  keywords: '',
  type: 3
});

const rules = {
  storeName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  cateId: [{ required: true, message: '请选择商品分类', trigger: 'change', type: 'array', min: 1 }],
  image: [{ required: true, message: '请选择商品封面图', trigger: 'change' }],
  price: [{ required: true, message: '请输入售价', trigger: 'change' }],
  tempId: [{ required: true, message: '请选择运费模板', trigger: 'change' }]
};

const categoryProps = {
  children: 'child',
  label: 'name',
  value: 'id',
  emitPath: false,
  multiple: true
};

const treeProps = {
  children: 'child',
  label: 'name'
};

const attachmentTree = computed(() => [
  {
    id: 0,
    name: '全部图片',
    child: attachmentCategories.value
  }
]);

const normalizedSliderImages = computed(() => form.sliderImages.map(normalizeAsset));
const isCreateMode = computed(() => !props.productId);

function normalizeAsset(value) {
  if (!value) return '';
  if (value.startsWith('http://') || value.startsWith('https://') || value.startsWith('/')) {
    return value;
  }
  return `/${value}`;
}

function parseSliderImages(value) {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : Object.values(parsed);
  } catch {
    return [];
  }
}

function parseCateIds(value) {
  if (!value) return [];
  return String(value)
    .split(',')
    .map((item) => Number(item.trim()))
    .filter((item) => Number.isFinite(item) && item > 0);
}

function stringifyCateIds(value) {
  if (!Array.isArray(value)) return value ? String(value) : '';
  return [...new Set(value.map((item) => Number(item)).filter((item) => Number.isFinite(item) && item > 0))].join(',');
}

async function loadDetail() {
  if (isCreateMode.value) {
    Object.assign(detail, { stock: 0 });
    form.id = 0;
    if (props.initialProduct) {
      Object.assign(form, {
        storeName: props.initialProduct.storeName || '',
        storeInfo: props.initialProduct.storeInfo || '',
        keyword: props.initialProduct.keyword || '',
        cateId: parseCateIds(props.initialProduct.cateId),
        image: props.initialProduct.image || '',
        videoLink: props.initialProduct.videoLink || '',
        sliderImages: Array.isArray(props.initialProduct.sliderImages) ? props.initialProduct.sliderImages : [],
        price: Number(props.initialProduct.price || 0),
        vipPrice: Number(props.initialProduct.vipPrice || 0),
        otPrice: Number(props.initialProduct.otPrice || 0),
        cost: Number(props.initialProduct.cost || 0),
        unitName: props.initialProduct.unitName || '件',
        giveIntegral: Number(props.initialProduct.giveIntegral || 0),
        tempId: props.initialProduct.tempId || '',
        sort: Number(props.initialProduct.sort || 0),
        ficti: Number(props.initialProduct.ficti || 0),
        isShow: Boolean(props.initialProduct.isShow),
        isSub: Boolean(props.initialProduct.isSub),
        isHot: Boolean(props.initialProduct.isHot),
        isBenefit: Boolean(props.initialProduct.isBenefit),
        isBest: Boolean(props.initialProduct.isBest),
        isNew: Boolean(props.initialProduct.isNew),
        isGood: Boolean(props.initialProduct.isGood),
        activity: normalizeActivity(props.initialProduct.activity),
        content: props.initialProduct.content || '',
        couponIds: Array.isArray(props.initialProduct.couponIds) ? props.initialProduct.couponIds : [],
        coupons: Array.isArray(props.initialProduct.coupons) ? props.initialProduct.coupons : []
      });
    }
    return;
  }
  loading.value = true;
  try {
    const data = await productDetail(props.productId);
    Object.assign(detail, data);
    Object.assign(form, {
      id: data.id,
      storeName: data.storeName || '',
      storeInfo: data.storeInfo || '',
      keyword: data.keyword || '',
      cateId: parseCateIds(data.cateId),
      image: data.image || '',
      videoLink: data.videoLink || '',
      sliderImages: parseSliderImages(data.sliderImage),
      price: Number(data.price || 0),
      vipPrice: Number(data.vipPrice || 0),
      otPrice: Number(data.otPrice || 0),
      cost: Number(data.cost || 0),
      unitName: data.unitName || '件',
      giveIntegral: Number(data.giveIntegral || 0),
      tempId: data.tempId || '',
      sort: Number(data.sort || 0),
      ficti: Number(data.ficti || 0),
      isShow: Boolean(data.isShow),
      isSub: Boolean(data.isSub),
      isHot: Boolean(data.isHot),
      isBenefit: Boolean(data.isBenefit),
      isBest: Boolean(data.isBest),
      isNew: Boolean(data.isNew),
      isGood: Boolean(data.isGood),
      activity: normalizeActivity(data.activity),
      content: data.content || '',
      couponIds: Array.isArray(data.couponIds) ? data.couponIds : [],
      coupons: Array.isArray(data.coupons) ? data.coupons : []
    });
  } finally {
    loading.value = false;
  }
}

async function loadShippingTemplates() {
  const data = await shippingTemplatesList({ page: 1, limit: 9999 });
  shippingOptions.value = data?.list || [];
}

async function openPicker(target) {
  pickerTarget.value = target;
  pickerVisible.value = true;
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
    attachmentTotal.value = data?.total || 0;
  } finally {
    attachmentLoading.value = false;
  }
}

function handleAttachmentCategory(data) {
  attachmentQuery.pid = data.id;
  attachmentQuery.page = 1;
  loadAttachments();
}

async function handleUpload(option) {
  const formData = new FormData();
  formData.append('multipart', option.file);
  try {
    await uploadImage(formData, { model: 'product', pid: attachmentQuery.pid || 0 });
    ElMessage.success('上传成功');
    await loadAttachments();
    option.onSuccess?.();
  } catch (error) {
    option.onError?.(error);
  }
}

function selectAttachment(item) {
  if (pickerTarget.value === 'cover') {
    form.image = item.sattDir;
  } else if (!form.sliderImages.includes(item.sattDir)) {
    form.sliderImages.push(item.sattDir);
  }
  pickerVisible.value = false;
  formRef.value?.validateField?.('image');
}

function removeSlider(index) {
  form.sliderImages.splice(index, 1);
}

function normalizeActivity(value) {
  const fallback = ['默认', '秒杀', '砍价', '拼团'];
  if (!Array.isArray(value) || !value.length) return fallback;
  const allowed = new Set(fallback);
  const list = value.filter((item) => allowed.has(item));
  return list.length ? list : fallback;
}

function moveActivity(index, direction) {
  const target = index + direction;
  if (target < 0 || target >= form.activity.length) return;
  const next = [...form.activity];
  const [item] = next.splice(index, 1);
  next.splice(target, 0, item);
  form.activity = next;
}

function removeCoupon(id) {
  form.couponIds = form.couponIds.filter((item) => item !== id);
  form.coupons = form.coupons.filter((item) => item.id !== id);
}

async function openCouponDialog() {
  couponPickerIds.value = [...form.couponIds];
  couponPickerRows.value = [...form.coupons];
  couponDialogVisible.value = true;
  await loadCoupons(1);
}

async function loadCoupons(page = couponQuery.page) {
  couponQuery.page = page;
  couponLoading.value = true;
  try {
    const data = await couponSendList({ ...couponQuery });
    couponRows.value = data?.list || [];
    couponTotal.value = data?.total || 0;
    for (const row of couponRows.value) {
      if (couponPickerIds.value.includes(row.id) && !couponPickerRows.value.some((item) => item.id === row.id)) {
        couponPickerRows.value.push(row);
      }
    }
  } finally {
    couponLoading.value = false;
  }
}

function toggleCoupon(row, checked) {
  if (checked) {
    if (!couponPickerIds.value.includes(row.id)) {
      couponPickerIds.value.push(row.id);
    }
    if (!couponPickerRows.value.some((item) => item.id === row.id)) {
      couponPickerRows.value.push(row);
    }
    return;
  }
  couponPickerIds.value = couponPickerIds.value.filter((id) => id !== row.id);
  couponPickerRows.value = couponPickerRows.value.filter((item) => item.id !== row.id);
}

function confirmCoupons() {
  form.couponIds = [...couponPickerIds.value];
  form.coupons = couponPickerRows.value.filter((item) => form.couponIds.includes(item.id));
  couponDialogVisible.value = false;
}

function couponTimeText(row) {
  if (row.isFixedTime) {
    return `${formatDate(row.useStartTime)} - ${formatDate(row.useEndTime)}`;
  }
  return row.day ? `${row.day} 天` : '不限制';
}

function formatDate(value) {
  if (!value) return '';
  return String(value).replace('T', ' ').slice(0, 19);
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    const payload = {
      ...form,
      cateId: stringifyCateIds(form.cateId),
      sliderImages: form.sliderImages,
      unitName: form.unitName || '件',
      activity: form.activity,
      couponIds: form.couponIds
    };
    if (isCreateMode.value) {
      delete payload.id;
      await productCreate(payload);
      ElMessage.success('添加成功');
    } else {
      await productUpdate(payload);
      ElMessage.success('保存成功');
    }
    emit('success');
  } finally {
    saving.value = false;
  }
}

onMounted(async () => {
  const [categories] = await Promise.all([
    categoryTree({ status: -1, type: 1 }),
    loadShippingTemplates()
  ]);
  categoryOptions.value = categories;
  await loadDetail();
});
</script>

<style scoped>
.product-basic-editor {
  padding: 0 28px 68px;
}

.from-ipt-width {
  width: 100%;
}

.image-edit-row,
.image-list.editable {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.video-preview {
  width: 260px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 0 0 18px 110px;
}

.video-preview video {
  width: 260px;
  height: 146px;
  object-fit: contain;
  border-radius: 4px;
  background: #111;
}

.activity-sort-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.activity-chip {
  min-height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  color: #303133;
}

.activity-chip button {
  border: 0;
  background: transparent;
  color: #409eff;
  cursor: pointer;
}

.activity-chip button:disabled {
  color: #c0c4cc;
  cursor: not-allowed;
}

.coupon-select-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.muted-text {
  color: #909399;
}

.coupon-picker-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.coupon-search {
  width: 260px;
}

.coupon-picker-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 16px;
}

.content-preview {
  width: 100%;
  min-height: 180px;
  max-height: 360px;
  overflow: auto;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
}

.content-preview :deep(img) {
  max-width: 100%;
}

.sortable-image {
  position: relative;
}

.remove-image {
  position: absolute;
  top: -7px;
  right: -7px;
  width: 20px;
  height: 20px;
  border: 0;
  border-radius: 50%;
  color: #fff;
  background: #f56c6c;
  cursor: pointer;
}

.drawer-footer {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 0 28px;
  border-top: 1px solid #ebeef5;
  background: #fff;
}

.attachment-picker {
  min-height: 520px;
  display: grid;
  grid-template-columns: 210px 1fr;
  gap: 16px;
}

.attachment-picker-tree,
.attachment-picker-main {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
}

.attachment-picker-tree {
  padding: 12px 0;
  overflow: auto;
}

.attachment-picker-main {
  padding: 12px;
}

.attachment-picker-toolbar {
  margin-bottom: 12px;
}

.attachment-picker-grid {
  min-height: 386px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(104px, 1fr));
  gap: 12px;
}

.attachment-picker-card {
  height: 132px;
  padding: 6px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  cursor: pointer;
}

.attachment-picker-card:hover {
  border-color: #1890ff;
}

.attachment-picker-card img {
  width: 100%;
  height: 88px;
  display: block;
  object-fit: contain;
  background: #f5f7f9;
}

.attachment-picker-card span {
  display: block;
  margin-top: 6px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 12px;
  color: #606266;
}

.attachment-picker-empty {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}
</style>
