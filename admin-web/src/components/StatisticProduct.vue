<template>
  <div class="statistic-page">
    <el-card shadow="never" class="ivu-mt statistic-filter">
      <el-radio-group v-model="query.dateLimit" size="small" @change="loadAll">
        <el-radio-button v-for="item in dateOptions" :key="item.value" :label="item.value">
          {{ item.label }}
        </el-radio-button>
      </el-radio-group>
    </el-card>

    <div class="stat-card-grid">
      <el-card v-for="item in cards" :key="item.key" shadow="never" class="stat-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </el-card>
    </div>

    <el-row :gutter="14" class="mt14">
      <el-col :span="14">
        <el-card shadow="never" class="box-card statistic-chart-card">
          <template #header>
            <div class="statistic-card-header">
              <span>商品趋势</span>
              <span class="muted-text">支付金额 / 支付件数 / 浏览量</span>
            </div>
          </template>
          <div class="statistic-chart">
            <div v-for="row in chartRows" :key="row.label" class="statistic-chart-row">
              <span class="chart-date">{{ row.label }}</span>
              <span class="chart-track amount"><i :style="{ width: row.amountPercent + '%' }"></i></span>
              <span class="chart-track pay"><i :style="{ width: row.payPercent + '%' }"></i></span>
              <span class="chart-track visit"><i :style="{ width: row.visitPercent + '%' }"></i></span>
              <span class="chart-value">￥{{ money(row.amount) }} / {{ row.payNum }} / {{ row.visitNum }}</span>
            </div>
            <el-empty v-if="!loading && chartRows.length === 0" description="暂无趋势数据" />
          </div>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="never" class="box-card">
          <template #header>
            <div class="statistic-card-header">
              <span>商品排行</span>
              <el-select v-model="query.sort" size="small" class="rank-sort" @change="loadRanking">
                <el-option label="按销售额" value="payPrice" />
                <el-option label="按销量" value="payNum" />
                <el-option label="按浏览量" value="browse" />
              </el-select>
            </div>
          </template>
          <el-table v-loading="rankingLoading" :data="rankingRows" size="small" border>
            <el-table-column label="商品" min-width="210">
              <template #default="{ row }">
                <div class="stat-product-cell">
                  <el-image class="stat-product-image" :src="asset(row.image)" fit="cover" />
                  <div>
                    <div class="line2">{{ row.storeName || '-' }}</div>
                    <div class="muted-text">ID: {{ row.id }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="payNum" label="销量" width="70" />
            <el-table-column label="销售额" width="100">
              <template #default="{ row }">￥{{ money(row.payPrice) }}</template>
            </el-table-column>
            <el-table-column prop="browse" label="浏览" width="70" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { statisticProductData, statisticProductRanking, statisticProductTrend } from '../api';

const dateOptions = [
  { label: '今天', value: 'today' },
  { label: '昨天', value: 'yesterday' },
  { label: '最近7天', value: 'lately7' },
  { label: '最近30天', value: 'lately30' },
  { label: '本月', value: 'month' },
  { label: '本年', value: 'year' }
];

const query = reactive({
  dateLimit: 'lately30',
  sort: 'payPrice'
});
const loading = ref(false);
const rankingLoading = ref(false);
const data = ref({});
const trend = ref({});
const rankingRows = ref([]);

const cards = computed(() => [
  { key: 'productCount', label: '商品总数', value: number(data.value.productCount) },
  { key: 'newProductNum', label: '新增商品', value: number(data.value.newProductNum) },
  { key: 'onSaleProductNum', label: '在售商品', value: number(data.value.onSaleProductNum) },
  { key: 'stockNum', label: '商品库存', value: number(data.value.stockNum) },
  { key: 'payProductNum', label: '支付件数', value: number(data.value.payProductNum) },
  { key: 'payAmount', label: '销售额', value: `￥${money(data.value.payAmount)}` },
  { key: 'payUserNum', label: '支付人数', value: number(data.value.payUserNum) },
  { key: 'customerPrice', label: '客单价', value: `￥${money(data.value.customerPrice)}` }
]);

const chartRows = computed(() => {
  const amountMap = trend.value.payAmount || {};
  const payMap = trend.value.payNum || {};
  const visitMap = trend.value.visitNum || {};
  const labels = Array.from(new Set([...Object.keys(amountMap), ...Object.keys(payMap), ...Object.keys(visitMap)]));
  const maxAmount = Math.max(...labels.map((label) => Number(amountMap[label] || 0)), 0);
  const maxPay = Math.max(...labels.map((label) => Number(payMap[label] || 0)), 0);
  const maxVisit = Math.max(...labels.map((label) => Number(visitMap[label] || 0)), 0);
  return labels.map((label) => {
    const amount = Number(amountMap[label] || 0);
    const payNum = Number(payMap[label] || 0);
    const visitNum = Number(visitMap[label] || 0);
    return {
      label,
      amount,
      payNum,
      visitNum,
      amountPercent: percent(amount, maxAmount),
      payPercent: percent(payNum, maxPay),
      visitPercent: percent(visitNum, maxVisit)
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

function asset(value) {
  if (!value) return '';
  if (String(value).startsWith('http') || String(value).startsWith('/')) return value;
  return `/${value}`;
}

async function loadData() {
  data.value = await statisticProductData({ dateLimit: query.dateLimit });
}

async function loadTrend() {
  trend.value = await statisticProductTrend({ dateLimit: query.dateLimit });
}

async function loadRanking() {
  rankingLoading.value = true;
  try {
    rankingRows.value = await statisticProductRanking({
      dateLimit: query.dateLimit,
      sort: query.sort,
      limit: 10
    });
  } finally {
    rankingLoading.value = false;
  }
}

async function loadAll() {
  loading.value = true;
  try {
    await Promise.all([loadData(), loadTrend(), loadRanking()]);
  } finally {
    loading.value = false;
  }
}

onMounted(loadAll);
</script>
