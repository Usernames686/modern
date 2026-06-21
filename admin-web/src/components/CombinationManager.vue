<template>
  <div class="divBox combination-manager">
    <template v-if="isRecordPage">
      <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
        <div class="padding-add">
          <el-form :model="recordQuery" size="small" inline @submit.prevent>
            <el-form-item label="时间选择：">
              <el-date-picker
                v-model="recordTime"
                value-format="YYYY-MM-DD"
                format="YYYY-MM-DD"
                type="daterange"
                range-separator="-"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                class="date-picker"
                @change="handleRecordTimeChange"
              />
            </el-form-item>
            <el-form-item label="拼团状态：">
              <el-select v-model="recordQuery.status" placeholder="请选择" clearable class="selWidth" @change="searchRecords">
                <el-option label="进行中" :value="1" />
                <el-option label="已成功" :value="2" />
                <el-option label="未完成" :value="3" />
              </el-select>
            </el-form-item>
          </el-form>
        </div>
      </el-card>

      <div class="stats-row">
        <div class="stat-card one">
          <div class="stat-name">参与人数(人)</div>
          <div class="stat-value">{{ stats.countPeople ?? 0 }}</div>
        </div>
        <div class="stat-card two">
          <div class="stat-name">成团数量(个)</div>
          <div class="stat-value">{{ stats.countTeam ?? 0 }}</div>
        </div>
      </div>

      <el-card class="box-card" :body-style="{ padding: '20px' }" shadow="never">
        <el-table v-loading="recordLoading" :data="recordRows" size="small" highlight-current-row>
          <el-table-column prop="id" label="ID" min-width="60" />
          <el-table-column label="头像" min-width="80">
            <template #default="{ row }">
              <el-image
                v-if="row.avatar"
                class="avatar-image"
                :src="assetUrl(row.avatar)"
                :preview-src-list="[assetUrl(row.avatar)]"
                preview-teleported
                fit="cover"
              />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="开团团长" prop="nickname" min-width="110" show-overflow-tooltip />
          <el-table-column label="开团时间" prop="addTime" min-width="150" />
          <el-table-column label="拼团商品" prop="title" min-width="280" show-overflow-tooltip />
          <el-table-column label="几人团" prop="people" min-width="90" />
          <el-table-column label="几人参加" prop="countPeople" min-width="100" />
          <el-table-column label="结束时间" prop="stopTime" min-width="150" />
          <el-table-column label="拼团状态" min-width="110">
            <template #default="{ row }">
              <el-tag :type="groupStatusTag(row.status)" effect="light">{{ groupStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :icon="View" @click="openPinkOrders(row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="block">
          <el-pagination
            v-model:current-page="recordQuery.page"
            v-model:page-size="recordQuery.limit"
            :page-sizes="[10, 20, 30, 40]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="recordTotal"
            background
            @size-change="loadRecords"
            @current-change="loadRecords"
          />
        </div>
      </el-card>
    </template>

    <template v-else>
      <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
        <div class="padding-add">
          <el-form :model="goodsQuery" size="small" inline @submit.prevent>
            <el-form-item label="拼团状态：">
              <el-select v-model="goodsQuery.isShow" placeholder="请选择" clearable class="selWidth" @change="searchGoods">
                <el-option label="关闭" :value="0" />
                <el-option label="开启" :value="1" />
              </el-select>
            </el-form-item>
            <el-form-item label="商品搜索：">
              <el-input
                v-model="goodsQuery.keywords"
                placeholder="请输入商品名称、ID"
                clearable
                class="selWidth"
                @keyup.enter="searchGoods"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="searchGoods">搜索</el-button>
              <el-button :icon="Refresh" @click="resetGoods">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-card>

      <el-card class="box-card mt14" :body-style="{ padding: '20px' }" shadow="never">
        <div class="table-actions">
          <el-button type="primary" :icon="Plus" @click="handleCreate">添加拼团商品</el-button>
          <el-button :icon="Download" :loading="exporting" @click="handleExport">导出</el-button>
        </div>

        <el-table v-loading="goodsLoading" :data="goodsRows" class="mt20" size="small" highlight-current-row>
          <el-table-column prop="id" label="ID" min-width="60" />
          <el-table-column label="拼团图片" min-width="90">
            <template #default="{ row }">
              <el-image
                v-if="row.image"
                class="product-image"
                :src="assetUrl(row.image)"
                :preview-src-list="[assetUrl(row.image)]"
                preview-teleported
                fit="cover"
              />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="拼团名称" prop="title" min-width="280" show-overflow-tooltip />
          <el-table-column label="原价" prop="otPrice" min-width="90" />
          <el-table-column label="拼团价" prop="price" min-width="90" />
          <el-table-column label="拼团人数" prop="countPeople" min-width="100" />
          <el-table-column label="参与人数" prop="countPeopleAll" min-width="100" />
          <el-table-column label="成团数量" prop="countPeoplePink" min-width="100" />
          <el-table-column label="限量" prop="quotaShow" min-width="80" />
          <el-table-column label="限量剩余" prop="remainingQuota" min-width="100" />
          <el-table-column label="结束时间" min-width="130">
            <template #default="{ row }">{{ formatMaybeTime(row.stopTimeStr || row.stopTime) }}</template>
          </el-table-column>
          <el-table-column label="拼团状态" fixed="right" min-width="110">
            <template #default="{ row }">
              <el-switch
                :model-value="Boolean(row.isShow)"
                active-text="开启"
                inactive-text="关闭"
                @change="changeGoodsStatus(row, $event)"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
          <el-button
            link
            type="primary"
            :icon="row.isShow ? View : Edit"
            @click="row.isShow ? openDetail(row) : handleEdit(row)"
          >
            {{ row.isShow ? '详情' : '编辑' }}
          </el-button>
          <el-divider direction="vertical" />
          <el-button link type="danger" :icon="Delete" @click="removeGoods(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="block">
          <el-pagination
            v-model:current-page="goodsQuery.page"
            v-model:page-size="goodsQuery.limit"
            :page-sizes="[10, 20, 30, 40]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="goodsTotal"
            background
            @size-change="loadGoods"
            @current-change="loadGoods"
          />
        </div>
      </el-card>
    </template>

    <el-dialog v-model="ordersVisible" title="查看详情" width="720px" destroy-on-close>
      <el-table v-loading="ordersLoading" :data="orderRows" size="small">
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column label="头像" min-width="80">
          <template #default="{ row }">
            <el-image
              v-if="row.avatar"
              class="avatar-image"
              :src="assetUrl(row.avatar)"
              :preview-src-list="[assetUrl(row.avatar)]"
              preview-teleported
              fit="cover"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="用户名称" prop="nickname" min-width="110" show-overflow-tooltip />
        <el-table-column label="订单编号" prop="orderId" min-width="190" show-overflow-tooltip />
        <el-table-column label="金额" prop="totalPrice" min-width="90" />
        <el-table-column label="订单状态" min-width="110">
          <template #default="{ row }">
            {{ Number(row.refundStatus || 0) === 0 ? orderStatusText(row.orderStatus) : refundStatusText(row.refundStatus) }}
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="拼团商品详情" size="620px" destroy-on-close>
      <el-descriptions v-if="detail" :column="1" border class="detail-descriptions">
        <el-descriptions-item label="商品ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="关联商品">{{ detail.productId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="拼团名称">{{ detail.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="活动时间">{{ formatMaybeTime(detail.startTimeStr || detail.startTime) }} ~ {{ formatMaybeTime(detail.stopTimeStr || detail.stopTime) }}</el-descriptions-item>
        <el-descriptions-item label="价格">原价 {{ detail.otPrice ?? 0 }} / 拼团价 {{ detail.price ?? 0 }} / 成本 {{ detail.cost ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="拼团设置">
          拼团人数 {{ detail.people ?? 0 }} / 时效 {{ detail.effectiveTime ?? 0 }} 小时 / 单次购买 {{ detail.onceNum ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="活动数据">
          开团 {{ detail.countPeople ?? 0 }} / 参与 {{ detail.countPeopleAll ?? 0 }} / 成团 {{ detail.countPeoplePink ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="库存">
          库存 {{ detail.stock ?? 0 }} / 销量 {{ detail.sales ?? 0 }} / 限量 {{ detail.quotaShow ?? 0 }} / 剩余 {{ detail.remainingQuota ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="物流">
          运费 {{ detail.postage ?? 0 }} / 模板 {{ detail.tempId || '-' }} / 重量 {{ detail.weight ?? 0 }} / 体积 {{ detail.volume ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="添加时间">{{ detail.addTimeStr || '-' }}</el-descriptions-item>
        <el-descriptions-item label="拼团图片">
          <el-image
            v-if="detail.image"
            class="detail-image"
            :src="assetUrl(detail.image)"
            :preview-src-list="[assetUrl(detail.image)]"
            preview-teleported
            fit="cover"
          />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="商品轮播图">
          <div v-if="detailImages.length" class="detail-slider">
            <template v-for="(item, index) in detailImages" :key="`${item}-${index}`">
              <video v-if="isVideo(item)" class="detail-image" :src="assetUrl(item)" muted controls />
              <el-image
                v-else
                class="detail-image"
                :src="assetUrl(item)"
                :preview-src-list="detailImagePreviewList"
                preview-teleported
                fit="cover"
              />
            </template>
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="简介">{{ detail.info || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="detail" class="detail-section">
        <div class="detail-section-title">商品属性</div>
        <el-table :data="detailSkuRows" border size="small" empty-text="后端未返回规格明细">
          <el-table-column prop="suk" label="规格" min-width="110" />
          <el-table-column label="图片" width="78">
            <template #default="{ row }">
              <el-image v-if="row.image" class="sku-image" :src="assetUrl(row.image)" fit="cover" />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="拼团价" min-width="90" />
          <el-table-column prop="otPrice" label="原价" min-width="90" />
          <el-table-column prop="cost" label="成本价" min-width="90" />
          <el-table-column prop="stock" label="库存" min-width="80" />
          <el-table-column prop="quota" label="限量" min-width="80" />
          <el-table-column prop="quotaShow" label="限量显示" min-width="100" />
          <el-table-column prop="barCode" label="商品编号" min-width="120" show-overflow-tooltip />
          <el-table-column prop="weight" label="重量(KG)" min-width="100" />
          <el-table-column prop="volume" label="体积(m³)" min-width="100" />
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Delete, Download, Edit, Plus, Refresh, Search, View } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  combinationDelete,
  combinationExport,
  combinationInfo,
  combinationList,
  combinationPinkList,
  combinationPinkOrders,
  combinationStatistics,
  combinationStatus
} from '../api';

const props = defineProps({
  path: {
    type: String,
    default: ''
  }
});

const isRecordPage = computed(() => props.path.includes('/marketing/groupBuy/groupList'));

const goodsLoading = ref(false);
const goodsRows = ref([]);
const goodsTotal = ref(0);
const exporting = ref(false);
const detailVisible = ref(false);
const detail = ref(null);
const detailSkuRows = computed(() => normalizeDetailSkuRows(detail.value?.attrValue || []));
const detailImages = computed(() => parseImages(detail.value?.sliderImage || detail.value?.images).slice(0, 10));
const detailImagePreviewList = computed(() => detailImages.value.filter((item) => !isVideo(item)).map(assetUrl));

const recordLoading = ref(false);
const recordRows = ref([]);
const recordTotal = ref(0);
const recordTime = ref([]);
const stats = reactive({
  countPeople: 0,
  countTeam: 0
});

const ordersVisible = ref(false);
const ordersLoading = ref(false);
const orderRows = ref([]);

const goodsQuery = reactive({
  page: 1,
  limit: 20,
  keywords: '',
  isShow: ''
});

const recordQuery = reactive({
  page: 1,
  limit: 20,
  dateLimit: '',
  status: ''
});

watch(
  () => props.path,
  () => {
    if (isRecordPage.value) {
      loadStats();
      loadRecords();
    } else {
      loadGoods();
    }
  }
);

onMounted(() => {
  if (isRecordPage.value) {
    loadStats();
    loadRecords();
  } else {
    loadGoods();
  }
});

async function loadGoods() {
  goodsLoading.value = true;
  try {
    const data = await combinationList(compactParams(goodsQuery));
    goodsRows.value = data?.list || [];
    goodsTotal.value = Number(data?.total || 0);
  } finally {
    goodsLoading.value = false;
  }
}

function searchGoods() {
  goodsQuery.page = 1;
  loadGoods();
}

function resetGoods() {
  goodsQuery.page = 1;
  goodsQuery.limit = 20;
  goodsQuery.keywords = '';
  goodsQuery.isShow = '';
  loadGoods();
}

async function changeGoodsStatus(row, value) {
  const old = row.isShow;
  row.isShow = value;
  try {
    await combinationStatus({ id: row.id, isShow: value });
    ElMessage.success('修改成功');
  } catch {
    row.isShow = old;
  }
}

async function removeGoods(row) {
  await ElMessageBox.confirm('永久删除该商品', '提示', { type: 'warning' });
  await combinationDelete({ id: row.id });
  ElMessage.success('删除成功');
  if (goodsRows.value.length === 1 && goodsQuery.page > 1) goodsQuery.page -= 1;
  loadGoods();
}

async function openDetail(row) {
  detail.value = await combinationInfo({ id: row.id });
  detailVisible.value = true;
}

function handleCreate() {
  navigateTo('/marketing/groupBuy/creatGroup');
}

function handleEdit(row) {
  navigateTo(`/marketing/groupBuy/creatGroup/${row.id}`);
}

function navigateTo(path) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path } }));
}

async function handleExport() {
  exporting.value = true;
  try {
    const data = await combinationExport(compactParams(goodsQuery));
    if (data?.fileName) {
      window.open(data.fileName, '_blank');
      ElMessage.success('导出成功');
    }
  } finally {
    exporting.value = false;
  }
}

async function loadStats() {
  const data = await combinationStatistics().catch(() => ({ countPeople: 0, countTeam: 0 }));
  stats.countPeople = data?.countPeople || 0;
  stats.countTeam = data?.countTeam || 0;
}

async function loadRecords() {
  recordLoading.value = true;
  try {
    const data = await combinationPinkList(compactParams(recordQuery));
    recordRows.value = data?.list || [];
    recordTotal.value = Number(data?.total || 0);
  } finally {
    recordLoading.value = false;
  }
}

function searchRecords() {
  recordQuery.page = 1;
  loadRecords();
}

function handleRecordTimeChange(value) {
  recordQuery.dateLimit = value?.length === 2 ? value.join(',') : '';
  searchRecords();
}

async function openPinkOrders(row) {
  ordersVisible.value = true;
  ordersLoading.value = true;
  try {
    orderRows.value = await combinationPinkOrders(row.id);
  } finally {
    ordersLoading.value = false;
  }
}

function compactParams(source) {
  const params = {};
  Object.entries(source).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) params[key] = value;
  });
  return params;
}

function assetUrl(value) {
  if (!value) return '';
  if (/^(https?:)?\/\//.test(value) || value.startsWith('/')) return value;
  return `/crmebimage/${value.replace(/^\/+/, '')}`;
}

function groupStatusText(value) {
  if (Number(value) === 1) return '进行中';
  if (Number(value) === 2) return '已成功';
  if (Number(value) === 3) return '未完成';
  return '-';
}

function groupStatusTag(value) {
  if (Number(value) === 1) return 'success';
  if (Number(value) === 2) return 'info';
  if (Number(value) === 3) return 'danger';
  return 'warning';
}

function orderStatusText(value) {
  const map = {
    0: '待发货',
    1: '待收货',
    2: '待评价',
    3: '已完成'
  };
  return map[Number(value)] || '-';
}

function refundStatusText(value) {
  const map = {
    1: '申请中',
    2: '已退款',
    3: '退款中'
  };
  return map[Number(value)] || '-';
}

function normalizeDetailSkuRows(list) {
  return toArray(list).map((item) => {
    const attrMap = parseAttrValue(item.attrValue);
    return {
      ...item,
      suk: item.suk || Object.values(attrMap).filter(Boolean).join(',') || '默认',
      image: item.image || item.pic || '',
      otPrice: item.otPrice ?? item.ot_price ?? 0,
      quotaShow: item.quotaShow ?? item.quota ?? '-',
      barCode: item.barCode || item.bar_code || ''
    };
  });
}

function toArray(value) {
  if (Array.isArray(value)) return value;
  if (!value) return [];
  if (typeof value === 'object') return Object.values(value);
  return [];
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

function isVideo(value) {
  return /\.mp4($|\?)/i.test(String(value || ''));
}

function formatMaybeTime(value) {
  if (!value) return '-';
  if (typeof value === 'number') return formatDate(new Date(value));
  const text = String(value);
  if (/^\d{13}$/.test(text)) return formatDate(new Date(Number(text)));
  if (/^\d{10}$/.test(text)) return formatDate(new Date(Number(text) * 1000));
  return text;
}

function formatDate(date) {
  if (Number.isNaN(date.getTime())) return '-';
  const pad = (value) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
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
</script>

<style scoped>
.combination-manager .padding-add {
  padding: 20px 20px 0;
}

.selWidth {
  width: 180px;
}

.date-picker {
  width: 260px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(180px, 1fr));
  gap: 14px;
  margin-top: 14px;
  margin-bottom: 14px;
}

.stat-card {
  min-height: 92px;
  border-radius: 6px;
  padding: 18px 22px;
  color: #fff;
}

.stat-card.one {
  background: #1890ff;
}

.stat-card.two {
  background: #a277ff;
}

.stat-name {
  font-size: 14px;
  opacity: 0.92;
}

.stat-value {
  margin-top: 10px;
  font-size: 30px;
  font-weight: 600;
}

.table-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.mt14 {
  margin-top: 14px;
}

.mt20 {
  margin-top: 20px;
}

.block {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.product-image,
.avatar-image {
  width: 36px;
  height: 36px;
  border-radius: 4px;
}

.detail-descriptions {
  margin-right: 8px;
}

.detail-image {
  width: 96px;
  height: 96px;
  border-radius: 4px;
  object-fit: cover;
}

.detail-slider {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-section {
  margin-top: 18px;
}

.detail-section-title {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.sku-image {
  width: 36px;
  height: 36px;
  border-radius: 4px;
}
</style>
