<template>
  <div class="mobile-stat-shell">
    <div class="mobile-stat-detail-page">
      <nav class="mobile-stat-detail-tabs">
        <button
          v-for="item in timeTabs"
          :key="item.value"
          type="button"
          :class="{ on: query.dateLimit === item.value }"
          @click="changeTime(item.value)"
        >
          {{ item.label }}
        </button>
      </nav>

      <section class="mobile-stat-detail-summary">
        <div class="title">{{ currentTimeLabel }}{{ isPrice ? '营业额（元）' : '订单量（份）' }}</div>
        <div class="money">{{ summaryValue }}</div>
        <div class="increase acea-row row-between-wrapper">
          <div>
            {{ currentTimeLabel }}增长率：
            <span :class="summary.increaseTimeStatus === 1 ? 'red' : 'green'">{{ growthText }}%</span>
          </div>
          <div>
            {{ currentTimeLabel }}增长：
            <span :class="summary.increaseTimeStatus === 1 ? 'red' : 'green'">{{ money(summary.increaseTime) }}</span>
          </div>
        </div>
      </section>

      <section class="mobile-stat-chart">
        <div class="company">{{ isPrice ? '单位（元）' : '单位（份）' }}</div>
        <div class="chart-bars">
          <div v-for="item in chartRows" :key="item.time" class="chart-row">
            <span class="chart-label">{{ item.time }}</span>
            <span class="chart-track"><span class="chart-fill" :style="{ width: item.percent + '%' }"></span></span>
            <span class="chart-num">{{ isPrice ? money(item.num) : Number(item.num || 0) }}</span>
          </div>
          <el-empty v-if="!loading && chartRows.length === 0" description="暂无趋势数据" :image-size="70" />
        </div>
      </section>

      <section class="mobile-stat-detail-list">
        <div class="title">详细数据</div>
        <div class="nav acea-row row-between-wrapper">
          <div class="data">日期</div>
          <div class="browse">订单数</div>
          <div class="turnover">成交额</div>
        </div>
        <div class="conter">
          <div v-for="item in rows" :key="item.time" class="item acea-row row-between-wrapper">
            <div class="data">{{ item.time }}</div>
            <div class="browse">{{ item.count }}</div>
            <div class="turnover">{{ money(item.price) }}</div>
          </div>
          <el-empty v-if="!loading && rows.length === 0" description="暂无数据" :image-size="70" />
        </div>
      </section>

      <div class="mobile-stat-loading">
        <span v-if="loading">正在加载...</span>
        <span v-else-if="loaded">没有更多了</span>
        <el-button v-else link type="primary" @click="loadRows">加载更多</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { orderStatisticsData, orderTime } from '../api';

const timeTabs = [
  { label: '今天', value: 'today' },
  { label: '昨天', value: 'yesterday' },
  { label: '最近7天', value: 'lately7' },
  { label: '本月', value: 'month' }
];

const loading = ref(false);
const loaded = ref(false);
const rows = ref([]);
const summary = reactive({
  chart: [],
  growthRate: 0,
  increaseTime: 0,
  increaseTimeStatus: 1,
  time: 0
});
const query = reactive({
  page: 1,
  limit: 10,
  dateLimit: pathPart(3) || 'today',
  type: pathPart(2) === 'price' ? 1 : 2
});

const isPrice = computed(() => Number(query.type) === 1);
const currentTimeLabel = computed(() => timeTabs.find((item) => item.value === query.dateLimit)?.label || '');
const summaryValue = computed(() => isPrice.value ? money(summary.time) : Number(summary.time || 0).toFixed(0));
const growthText = computed(() => {
  const value = Number(summary.growthRate || 0);
  return `${value >= 0 ? '' : '-'}${Math.abs(value).toFixed(2)}`;
});
const chartRows = computed(() => {
  const rows = Array.isArray(summary.chart) ? summary.chart : [];
  const max = Math.max(...rows.map((item) => Number(item.num || 0)), 0);
  return rows.map((item) => ({
    ...item,
    percent: max > 0 ? Math.max(4, Math.round((Number(item.num || 0) / max) * 100)) : 0
  }));
});

function pathPart(index) {
  return window.location.pathname.split('/').filter(Boolean)[index] || '';
}

async function loadSummary() {
  const data = await orderTime({ dateLimit: query.dateLimit, type: query.type });
  Object.assign(summary, data || {});
}

async function loadRows() {
  if (loading.value || loaded.value) return;
  loading.value = true;
  try {
    const list = await orderStatisticsData({
      page: query.page,
      limit: query.limit,
      dateLimit: query.dateLimit
    });
    const data = Array.isArray(list) ? list : [];
    rows.value.push(...data);
    loaded.value = data.length < query.limit;
    query.page += 1;
  } finally {
    loading.value = false;
  }
}

async function changeTime(value) {
  if (query.dateLimit === value) return;
  query.dateLimit = value;
  const typePath = isPrice.value ? 'price' : 'order';
  window.history.pushState({}, '', `/javaMobile/orderStatisticsDetail/${typePath}/${value}`);
  rows.value = [];
  query.page = 1;
  loaded.value = false;
  await Promise.all([loadSummary(), loadRows()]);
}

function money(value) {
  return Number(value || 0).toFixed(2);
}

onMounted(async () => {
  await Promise.all([loadSummary(), loadRows()]);
});
</script>
