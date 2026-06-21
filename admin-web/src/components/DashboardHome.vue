<template>
  <section class="dashboard-home" v-loading="loading">
    <el-row :gutter="14" class="metric-row">
      <el-col v-for="item in topCards" :key="item.key" :xs="24" :sm="12" :lg="6">
        <el-card shadow="never" class="metric-card">
          <div class="metric-head">
            <span>{{ item.title }}</span>
            <el-tag size="small">今日</el-tag>
          </div>
          <div class="metric-value">{{ item.value }}</div>
          <div class="metric-foot">
            <span>昨日数据</span>
            <span>{{ item.yesterday }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="14">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-title">快捷入口</div>
          </template>
          <div class="quick-grid">
            <button
              v-for="item in quickEntries"
              :key="item.path"
              type="button"
              class="quick-item"
              @click="go(item.path)"
            >
              <span class="quick-icon" :style="{ backgroundColor: item.color }">
                <el-icon><component :is="item.icon" /></el-icon>
              </span>
              <span>{{ item.title }}</span>
            </button>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-title">经营数据</div>
          </template>
          <div class="business-grid">
            <button
              v-for="item in businessCards"
              :key="item.key"
              type="button"
              class="business-item"
              @click="go(item.path)"
            >
              <strong>{{ item.value }}</strong>
              <span>{{ item.title }}</span>
            </button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="panel-card chart-card">
      <template #header>
        <div class="chart-header">
          <div class="panel-title">订单统计</div>
          <el-radio-group v-model="orderRange" size="small" @change="loadOrderChart">
            <el-radio-button label="last30">30天</el-radio-button>
            <el-radio-button label="week">周</el-radio-button>
            <el-radio-button label="month">月</el-radio-button>
            <el-radio-button label="year">年</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <h4 class="chart-subtitle">订单量趋势</h4>
      <div class="legend-row">
        <span><i class="legend-dot price-dot"></i>订单金额</span>
        <span><i class="legend-dot count-dot"></i>订单数</span>
        <span v-if="hasPreOrderData"><i class="legend-dot pre-dot"></i>上期数据</span>
      </div>
      <div class="chart-wrap">
        <svg viewBox="0 0 640 240" role="img" aria-label="订单统计">
          <line x1="36" y1="200" x2="620" y2="200" class="axis-line" />
          <g v-for="bar in orderBars" :key="bar.key">
            <rect :x="bar.x - 7" :y="bar.preY" width="6" :height="bar.preHeight" class="pre-bar" />
            <rect :x="bar.x" :y="bar.y" width="10" :height="bar.height" class="price-bar" />
            <text v-if="bar.showLabel" :x="bar.x + 5" y="222" text-anchor="middle" class="axis-label">{{ bar.label }}</text>
          </g>
          <polyline v-if="orderLinePoints" :points="orderLinePoints" class="count-line" />
          <polyline v-if="preOrderLinePoints" :points="preOrderLinePoints" class="pre-line" />
        </svg>
      </div>
    </el-card>

    <el-card shadow="never" class="panel-card chart-card">
      <template #header>
        <div class="panel-title">用户统计</div>
      </template>
      <div class="legend-row">
        <span><i class="legend-dot count-dot"></i>人数（人）</span>
      </div>
      <div class="chart-wrap">
        <svg viewBox="0 0 640 220" role="img" aria-label="用户统计">
          <line x1="36" y1="180" x2="620" y2="180" class="axis-line" />
          <g v-for="point in userPoints" :key="point.key">
            <circle :cx="point.x" :cy="point.y" r="3" class="user-point" />
            <text v-if="point.showLabel" :x="point.x" y="202" text-anchor="middle" class="axis-label">{{ point.label }}</text>
          </g>
          <polyline v-if="userLinePoints" :points="userLinePoints" class="count-line" />
        </svg>
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import {
  ChatLineSquare,
  Goods,
  Money,
  Operation,
  Setting,
  ShoppingCart,
  Tickets,
  User
} from '@element-plus/icons-vue';
import { dashboardHomeIndex, dashboardOperatingData, dashboardOrderChart, dashboardUserChart } from '../api';

const loading = ref(false);
const orderRange = ref('last30');
const summary = reactive({
  sales: 0,
  yesterdaySales: 0,
  pageviews: 0,
  yesterdayPageviews: 0,
  orderNum: 0,
  yesterdayOrderNum: 0,
  newUserNum: 0,
  yesterdayNewUserNum: 0
});
const business = reactive({
  notShippingOrderNum: 0,
  refundingOrderNum: 0,
  notWriteOffOrderNum: 0,
  vigilanceInventoryNum: 0,
  onSaleProductNum: 0,
  notSaleProductNum: 0,
  notAuditNum: 0,
  totalRechargeAmount: 0
});
const orderChart = reactive({
  price: {},
  quality: {},
  prePrice: {},
  preQuality: {}
});
const userChart = ref({});

const quickEntries = [
  { title: '用户管理', path: '/user/index', icon: User, color: '#1890ff' },
  { title: '系统设置', path: '/operation/setting', icon: Setting, color: '#1bbe6b' },
  { title: '商品管理', path: '/store/index', icon: Goods, color: '#ef9c20' },
  { title: '订单管理', path: '/order/index', icon: ShoppingCart, color: '#f56c6c' },
  { title: '一号通', path: '/operation/onePass', icon: ChatLineSquare, color: '#8b6be8' },
  { title: '文章管理', path: '/content/articleManager', icon: Tickets, color: '#d49b00' },
  { title: '分销管理', path: '/distribution/index', icon: Operation, color: '#12aab5' },
  { title: '优惠券', path: '/marketing/coupon/list', icon: Money, color: '#1677d2' }
];

const topCards = computed(() => [
  {
    key: 'sales',
    title: '销售额',
    value: money(summary.sales),
    yesterday: `${money(summary.yesterdaySales)} 元`
  },
  {
    key: 'pageviews',
    title: '用户访问量',
    value: number(summary.pageviews),
    yesterday: number(summary.yesterdayPageviews)
  },
  {
    key: 'orderNum',
    title: '订单量',
    value: number(summary.orderNum),
    yesterday: `${number(summary.yesterdayOrderNum)} 单`
  },
  {
    key: 'newUserNum',
    title: '新增用户',
    value: number(summary.newUserNum),
    yesterday: `${number(summary.yesterdayNewUserNum)} 人`
  }
]);

const businessCards = computed(() => [
  { key: 'notShippingOrderNum', title: '待发货订单', value: number(business.notShippingOrderNum), path: '/order/index' },
  { key: 'refundingOrderNum', title: '退款中订单', value: number(business.refundingOrderNum), path: '/order/index' },
  { key: 'notWriteOffOrderNum', title: '待核销订单', value: number(business.notWriteOffOrderNum), path: '/order/index' },
  { key: 'vigilanceInventoryNum', title: '库存预警', value: number(business.vigilanceInventoryNum), path: '/store/index' },
  { key: 'onSaleProductNum', title: '上架商品', value: number(business.onSaleProductNum), path: '/store/index' },
  { key: 'notSaleProductNum', title: '仓库商品', value: number(business.notSaleProductNum), path: '/store/index' },
  { key: 'notAuditNum', title: '提现待审核', value: number(business.notAuditNum), path: '/financial/commission/template' },
  { key: 'totalRechargeAmount', title: '账户充值', value: money(business.totalRechargeAmount), path: '/financial/record/charge' }
]);
const hasPreOrderData = computed(() => Object.keys(orderChart.prePrice || {}).length || Object.keys(orderChart.preQuality || {}).length);
const orderRows = computed(() => chartRows(orderChart.price, orderChart.quality, orderChart.prePrice, orderChart.preQuality));
const orderMaxPrice = computed(() => Math.max(1, ...orderRows.value.map((item) => item.price), ...orderRows.value.map((item) => item.prePrice)));
const orderMaxCount = computed(() => Math.max(1, ...orderRows.value.map((item) => item.count), ...orderRows.value.map((item) => item.preCount)));
const orderBars = computed(() => {
  const rows = orderRows.value;
  const total = Math.max(rows.length - 1, 1);
  const step = rows.length > 1 ? 560 / total : 0;
  const every = Math.max(1, Math.ceil(rows.length / 8));
  return rows.map((item, index) => {
    const x = rows.length > 1 ? 48 + step * index : 320;
    const height = Math.round((item.price / orderMaxPrice.value) * 150);
    const preHeight = Math.round((item.prePrice / orderMaxPrice.value) * 150);
    return {
      key: item.label,
      label: item.label,
      x,
      y: 200 - height,
      height,
      preY: 200 - preHeight,
      preHeight,
      showLabel: index % every === 0 || index === rows.length - 1
    };
  });
});
const orderLinePoints = computed(() => linePoints(orderRows.value.map((item) => item.count), orderMaxCount.value, 48, 200, 560, 150));
const preOrderLinePoints = computed(() => linePoints(orderRows.value.map((item) => item.preCount), orderMaxCount.value, 41, 200, 560, 150));
const userRows = computed(() => Object.entries(userChart.value || {}).map(([label, value]) => ({ label, value: Number(value || 0) })));
const userMax = computed(() => Math.max(1, ...userRows.value.map((item) => item.value)));
const userPoints = computed(() => {
  const rows = userRows.value;
  const total = Math.max(rows.length - 1, 1);
  const step = rows.length > 1 ? 560 / total : 0;
  const every = Math.max(1, Math.ceil(rows.length / 8));
  return rows.map((item, index) => ({
    key: item.label,
    label: item.label,
    x: rows.length > 1 ? 48 + step * index : 320,
    y: 180 - Math.round((item.value / userMax.value) * 130),
    showLabel: index % every === 0 || index === rows.length - 1
  }));
});
const userLinePoints = computed(() => userPoints.value.map((item) => `${item.x},${item.y}`).join(' '));

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const [homeData, operatingData] = await Promise.all([
      dashboardHomeIndex(),
      dashboardOperatingData()
    ]);
    Object.assign(summary, homeData || {});
    Object.assign(business, operatingData || {});
    await Promise.all([loadOrderChart(), loadUserChart()]);
  } finally {
    loading.value = false;
  }
}

async function loadOrderChart() {
  const data = await dashboardOrderChart(orderRange.value);
  Object.assign(orderChart, {
    price: data?.price || {},
    quality: data?.quality || {},
    prePrice: data?.prePrice || {},
    preQuality: data?.preQuality || {}
  });
}

async function loadUserChart() {
  userChart.value = await dashboardUserChart();
}

function go(path) {
  if (!path) return;
  if (window.location.pathname !== path) {
    window.history.pushState({}, '', path);
  }
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path } }));
}

function number(value) {
  const num = Number(value || 0);
  return Number.isFinite(num) ? String(num) : '0';
}

function money(value) {
  const num = Number(value || 0);
  return Number.isFinite(num) ? num.toFixed(2) : '0.00';
}

function chartRows(priceMap = {}, countMap = {}, prePriceMap = {}, preCountMap = {}) {
  const labels = [...new Set([
    ...Object.keys(priceMap || {}),
    ...Object.keys(countMap || {}),
    ...Object.keys(prePriceMap || {}),
    ...Object.keys(preCountMap || {})
  ])];
  return labels.map((label) => ({
    label,
    price: Number(priceMap?.[label] || 0),
    count: Number(countMap?.[label] || 0),
    prePrice: Number(prePriceMap?.[label] || 0),
    preCount: Number(preCountMap?.[label] || 0)
  }));
}

function linePoints(values, max, startX, baseY, width, height) {
  if (!values.length) return '';
  const total = Math.max(values.length - 1, 1);
  return values.map((value, index) => {
    const x = values.length > 1 ? startX + (width / total) * index : 320;
    const y = baseY - Math.round((Number(value || 0) / max) * height);
    return `${x},${y}`;
  }).join(' ');
}
</script>

<style scoped>
.dashboard-home {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.metric-row {
  row-gap: 14px;
}

.metric-card,
.panel-card {
  border-radius: 4px;
}

.metric-head,
.metric-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.metric-head {
  color: #303133;
  font-size: 14px;
  font-weight: 500;
}

.metric-value {
  margin: 18px 0;
  color: #303133;
  font-size: 30px;
  font-weight: 600;
  line-height: 1.1;
}

.metric-foot {
  border-top: 1px solid #ebeef5;
  padding-top: 14px;
  color: #606266;
  font-size: 14px;
}

.panel-title {
  border-left: 2px solid #1890ff;
  padding-left: 8px;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.chart-card {
  overflow: hidden;
}

.chart-subtitle {
  margin: 0 0 12px;
  color: #303133;
  font-size: 15px;
  font-weight: 600;
}

.legend-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 8px;
  color: #606266;
  font-size: 13px;
}

.legend-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 6px;
  border-radius: 50%;
}

.price-dot {
  background: #5b8ff9;
}

.count-dot {
  background: #4bcad5;
}

.pre-dot {
  background: #a8abb2;
}

.chart-wrap {
  min-height: 240px;
  overflow-x: auto;
}

.chart-wrap svg {
  min-width: 640px;
  width: 100%;
  height: auto;
}

.axis-line {
  stroke: #e4e7ed;
  stroke-width: 1;
}

.axis-label {
  fill: #909399;
  font-size: 11px;
}

.price-bar {
  fill: #5b8ff9;
  rx: 2;
}

.pre-bar {
  fill: #dcdfe6;
  rx: 2;
}

.count-line,
.pre-line {
  fill: none;
  stroke-width: 2.5;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.count-line {
  stroke: #4bcad5;
}

.pre-line {
  stroke: #a8abb2;
  stroke-dasharray: 5 5;
}

.user-point {
  fill: #4bcad5;
}

.quick-grid,
.business-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.quick-item,
.business-item {
  border: 0;
  background: transparent;
  cursor: pointer;
  font: inherit;
}

.quick-item {
  display: flex;
  min-height: 92px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #303133;
}

.quick-icon {
  display: inline-flex;
  width: 42px;
  height: 42px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #fff;
  font-size: 22px;
}

.business-item {
  min-height: 82px;
  border-radius: 4px;
  color: #303133;
  text-align: center;
}

.business-item:hover,
.quick-item:hover {
  background: #f5f7fa;
}

.business-item strong {
  display: block;
  margin-bottom: 8px;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.business-item span,
.quick-item span:last-child {
  color: #606266;
  font-size: 14px;
}

@media (max-width: 760px) {
  .quick-grid,
  .business-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
