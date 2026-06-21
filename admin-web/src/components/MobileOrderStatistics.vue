<template>
  <div class="mobile-stat-shell">
    <div class="mobile-stat-page">
      <div class="mobile-stat-header acea-row">
        <a v-for="item in statusItems" :key="item.name" class="mobile-stat-header-item" href="javascript:void(0)" @click="navigate(item.path)">
          <div class="num">{{ item.count }}</div>
          <div>{{ item.name }}</div>
        </a>
      </div>

      <div class="mobile-stat-wrapper">
        <div class="mobile-stat-title"><el-icon><DataAnalysis /></el-icon>数据统计</div>
        <div class="mobile-stat-grid acea-row">
          <a v-for="item in dataItems" :key="item.name" class="mobile-stat-grid-item" href="javascript:void(0)" @click="navigate(item.path)">
            <div class="num">{{ item.value }}</div>
            <div>{{ item.name }}</div>
          </a>
        </div>
      </div>

      <div class="mobile-stat-list">
        <div class="mobile-stat-title"><el-icon><TrendCharts /></el-icon>营业趋势</div>
        <div class="mobile-stat-nav acea-row">
          <div class="data">日期</div>
          <div class="browse">订单数</div>
          <div class="turnover">成交额</div>
        </div>
        <div class="mobile-stat-rows">
          <div v-for="item in rows" :key="item.time" class="mobile-stat-row acea-row row-middle">
            <div class="data">{{ item.time }}</div>
            <div class="browse">{{ item.count }}</div>
            <div class="turnover">￥{{ money(item.price) }}</div>
          </div>
          <el-empty v-if="!loading && rows.length === 0" description="暂无数据" :image-size="80" />
        </div>
        <div class="mobile-stat-loading">
          <span v-if="loading">正在加载...</span>
          <span v-else-if="loaded">没有更多了</span>
          <el-button v-else link type="primary" @click="loadList">加载更多</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { DataAnalysis, TrendCharts } from '@element-plus/icons-vue';
import { orderStatistics, orderStatisticsData } from '../api';

const loading = ref(false);
const loaded = ref(false);
const rows = ref([]);
const query = reactive({
  page: 1,
  limit: 10
});

const census = reactive({
  unpaidCount: 0,
  unshippedCount: 0,
  receivedCount: 0,
  verificationCount: 0,
  refundCount: 0,
  todayPrice: '0.00',
  proPrice: '0.00',
  monthPrice: '0.00',
  todayCount: 0,
  proCount: 0,
  monthCount: 0
});

const statusItems = computed(() => [
  { name: '待付款', count: census.unpaidCount, path: '/javaMobile/orderList/unPaid' },
  { name: '待发货', count: census.unshippedCount, path: '/javaMobile/orderList/notShipped' },
  { name: '待收货', count: census.receivedCount, path: '/javaMobile/orderList/spike' },
  { name: '待核销', count: census.verificationCount, path: '/javaMobile/orderList/toBeWrittenOff' },
  { name: '退款', count: census.refundCount, path: '/javaMobile/orderList/refunding' }
]);

const dataItems = computed(() => [
  { name: '今日成交额', value: money(census.todayPrice), path: '/javaMobile/orderStatisticsDetail/price/today' },
  { name: '昨日成交额', value: money(census.proPrice), path: '/javaMobile/orderStatisticsDetail/price/yesterday' },
  { name: '本月成交额', value: money(census.monthPrice), path: '/javaMobile/orderStatisticsDetail/price/month' },
  { name: '今日订单数', value: census.todayCount, path: '/javaMobile/orderStatisticsDetail/order/today' },
  { name: '昨日订单数', value: census.proCount, path: '/javaMobile/orderStatisticsDetail/order/yesterday' },
  { name: '本月订单数', value: census.monthCount, path: '/javaMobile/orderStatisticsDetail/order/month' }
]);

async function loadTop() {
  const data = await orderStatistics();
  Object.assign(census, data || {});
}

async function loadList() {
  if (loading.value || loaded.value) return;
  loading.value = true;
  try {
    const data = await orderStatisticsData({ ...query });
    const list = Array.isArray(data) ? data : [];
    rows.value.push(...list);
    loaded.value = list.length < query.limit;
    query.page += 1;
  } finally {
    loading.value = false;
  }
}

function money(value) {
  return Number(value || 0).toFixed(2);
}

function navigate(path) {
  if (!path) return;
  window.history.pushState({}, '', path);
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path } }));
}

onMounted(async () => {
  await Promise.all([loadTop(), loadList()]);
});
</script>
