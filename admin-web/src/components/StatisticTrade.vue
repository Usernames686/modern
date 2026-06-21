<template>
  <div class="statistic-page">
    <el-card shadow="never" class="ivu-mt statistic-filter">
      <el-radio-group v-model="query.dateLimit" size="small" @change="loadRangeData">
        <el-radio-button v-for="item in dateOptions" :key="item.value" :label="item.value">
          {{ item.label }}
        </el-radio-button>
      </el-radio-group>
    </el-card>

    <div class="stat-card-grid">
      <el-card v-for="item in totalCards" :key="item.key" shadow="never" class="stat-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </el-card>
    </div>

    <div class="stat-card-grid mt14">
      <el-card v-for="item in overviewCards" :key="item.key" shadow="never" class="stat-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </el-card>
    </div>

    <el-card shadow="never" class="box-card mt14 statistic-chart-card">
      <template #header>
        <div class="statistic-card-header">
          <span>交易趋势</span>
          <span class="muted-text">支付金额 / 支付订单数</span>
        </div>
      </template>
      <div class="statistic-chart">
        <div v-for="row in chartRows" :key="row.label" class="statistic-chart-row">
          <span class="chart-date">{{ row.label }}</span>
          <span class="chart-track amount"><i :style="{ width: row.amountPercent + '%' }"></i></span>
          <span class="chart-track pay"><i :style="{ width: row.numPercent + '%' }"></i></span>
          <span class="chart-value">￥{{ money(row.amount) }} / {{ row.num }} 单</span>
        </div>
        <el-empty v-if="!loading && chartRows.length === 0" description="暂无趋势数据" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { statisticTradeData, statisticTradeOverview, statisticTradeTrend } from '../api';

const dateOptions = [
  { label: '今天', value: 'today' },
  { label: '昨天', value: 'yesterday' },
  { label: '最近7天', value: 'lately7' },
  { label: '最近30天', value: 'lately30' },
  { label: '本月', value: 'month' },
  { label: '本年', value: 'year' }
];

const query = reactive({ dateLimit: 'lately30' });
const loading = ref(false);
const total = ref({});
const overview = ref({});
const trend = ref({});

const totalCards = computed(() => [
  { key: 'orderNum', label: '累计订单数', value: number(total.value.orderNum) },
  { key: 'payOrderNum', label: '累计支付订单', value: number(total.value.payOrderNum) },
  { key: 'payOrderAmount', label: '累计支付金额', value: `￥${money(total.value.payOrderAmount)}` },
  { key: 'refundAmount', label: '累计退款金额', value: `￥${money(total.value.refundAmount)}` },
  { key: 'payUserNum', label: '累计成交用户', value: number(total.value.payUserNum) },
  { key: 'customerPrice', label: '累计客单价', value: `￥${money(total.value.customerPrice)}` }
]);

const overviewCards = computed(() => [
  { key: 'orderNum', label: '下单数', value: number(overview.value.orderNum) },
  { key: 'payOrderNum', label: '支付订单数', value: number(overview.value.payOrderNum) },
  { key: 'waitPayOrderNum', label: '待支付订单', value: number(overview.value.waitPayOrderNum) },
  { key: 'payOrderAmount', label: '支付金额', value: `￥${money(overview.value.payOrderAmount)}` },
  { key: 'refundingOrderNum', label: '退款中订单', value: number(overview.value.refundingOrderNum) },
  { key: 'customerPrice', label: '客单价', value: `￥${money(overview.value.customerPrice)}` }
]);

const chartRows = computed(() => {
  const amountMap = trend.value.payOrderAmount || {};
  const numMap = trend.value.payOrderNum || {};
  const labels = Array.from(new Set([...Object.keys(amountMap), ...Object.keys(numMap)]));
  const maxAmount = Math.max(...labels.map((label) => Number(amountMap[label] || 0)), 0);
  const maxNum = Math.max(...labels.map((label) => Number(numMap[label] || 0)), 0);
  return labels.map((label) => {
    const amount = Number(amountMap[label] || 0);
    const num = Number(numMap[label] || 0);
    return {
      label,
      amount,
      num,
      amountPercent: percent(amount, maxAmount),
      numPercent: percent(num, maxNum)
    };
  });
});

function percent(value, max) {
  if (!max) return 0;
  return Math.max(4, Math.round((Number(value || 0) / max) * 100));
}

function number(value) {
  return Number(value || 0);
}

function money(value) {
  return Number(value || 0).toFixed(2);
}

async function loadRangeData() {
  loading.value = true;
  try {
    const [overviewData, trendData] = await Promise.all([
      statisticTradeOverview({ dateLimit: query.dateLimit }),
      statisticTradeTrend({ dateLimit: query.dateLimit })
    ]);
    overview.value = overviewData || {};
    trend.value = trendData || {};
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  const [totalData] = await Promise.all([statisticTradeData(), loadRangeData()]);
  total.value = totalData || {};
});
</script>
