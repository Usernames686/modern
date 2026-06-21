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
        <em v-if="item.ratio">环比 {{ item.ratio }}</em>
      </el-card>
    </div>

    <el-row :gutter="14" class="mt14">
      <el-col :span="14">
        <el-card shadow="never" class="box-card statistic-chart-card">
          <template #header>
            <div class="statistic-card-header">
              <span>用户趋势</span>
              <span class="muted-text">注册 / 活跃 / 充值</span>
            </div>
          </template>
          <div class="statistic-chart">
            <div v-for="row in chartRows" :key="row.date" class="statistic-chart-row">
              <span class="chart-date">{{ row.date.slice(5) }}</span>
              <span class="chart-track register"><i :style="{ width: row.registerPercent + '%' }"></i></span>
              <span class="chart-track pay"><i :style="{ width: row.activePercent + '%' }"></i></span>
              <span class="chart-track visit"><i :style="{ width: row.rechargePercent + '%' }"></i></span>
              <span class="chart-value">{{ row.registerNum }} / {{ row.activeUserNum }} / {{ row.rechargeUserNum }}</span>
            </div>
            <el-empty v-if="!loading && chartRows.length === 0" description="暂无趋势数据" />
          </div>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="never" class="box-card">
          <template #header>
            <div class="statistic-card-header">
              <span>用户分布</span>
              <span class="muted-text">渠道 / 性别 / 地区</span>
            </div>
          </template>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="渠道" name="channel">
              <el-table :data="channelRows" size="small" border>
                <el-table-column prop="channel" label="渠道" />
                <el-table-column prop="num" label="数量" width="100" />
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="性别" name="sex">
              <el-table :data="sexRows" size="small" border>
                <el-table-column prop="name" label="性别" />
                <el-table-column prop="num" label="数量" width="100" />
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="地区" name="area">
              <el-table :data="areaRows" size="small" border max-height="330">
                <el-table-column prop="name" label="地区" />
                <el-table-column prop="num" label="数量" width="100" />
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import {
  statisticUserAreaData,
  statisticUserChannelData,
  statisticUserOverview,
  statisticUserOverviewList,
  statisticUserSexData,
  statisticUserTotalData
} from '../api';

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
const activeTab = ref('channel');
const total = ref({});
const overview = ref({});
const overviewRows = ref([]);
const channelRows = ref([]);
const sexRows = ref([]);
const areaRows = ref([]);

const totalCards = computed(() => [
  { key: 'registerNum', label: '累计用户', value: number(total.value.registerNum) },
  { key: 'enabledUserNum', label: '正常用户', value: number(total.value.enabledUserNum) },
  { key: 'promoterNum', label: '推广员', value: number(total.value.promoterNum) },
  { key: 'userBalance', label: '用户余额', value: `￥${money(total.value.userBalance)}` },
  { key: 'brokerageBalance', label: '佣金余额', value: `￥${money(total.value.brokerageBalance)}` },
  { key: 'integralBalance', label: '积分余额', value: number(total.value.integralBalance) }
]);

const overviewCards = computed(() => [
  { key: 'registerNum', label: '注册用户数', value: number(overview.value.registerNum), ratio: overview.value.registerNumRatio },
  { key: 'activeUserNum', label: '活跃用户数', value: number(overview.value.activeUserNum), ratio: overview.value.activeUserNumRatio },
  { key: 'rechargeUserNum', label: '充值用户数', value: number(overview.value.rechargeUserNum), ratio: overview.value.rechargeUserNumRatio },
  { key: 'pageviews', label: '浏览量', value: number(overview.value.pageviews) },
  { key: 'orderPayUserNum', label: '成交用户数', value: number(overview.value.orderPayUserNum) },
  { key: 'customerPrice', label: '客单价', value: `￥${money(overview.value.customerPrice)}` }
]);

const chartRows = computed(() => {
  const rows = overviewRows.value || [];
  const maxRegister = Math.max(...rows.map((row) => number(row.registerNum)), 0);
  const maxActive = Math.max(...rows.map((row) => number(row.activeUserNum)), 0);
  const maxRecharge = Math.max(...rows.map((row) => number(row.rechargeUserNum)), 0);
  return rows.map((row) => ({
    ...row,
    registerPercent: percent(row.registerNum, maxRegister),
    activePercent: percent(row.activeUserNum, maxActive),
    rechargePercent: percent(row.rechargeUserNum, maxRecharge)
  }));
});

function percent(value, max) {
  if (!max) return 0;
  return Math.max(4, Math.round((number(value) / max) * 100));
}

function number(value) {
  return Number(value || 0);
}

function money(value) {
  return Number(value || 0).toFixed(2);
}

async function loadStaticData() {
  const [totalData, channelData, sexData, areaData] = await Promise.all([
    statisticUserTotalData(),
    statisticUserChannelData(),
    statisticUserSexData(),
    statisticUserAreaData()
  ]);
  total.value = totalData || {};
  channelRows.value = channelData || [];
  sexRows.value = sexData || [];
  areaRows.value = areaData || [];
}

async function loadRangeData() {
  loading.value = true;
  try {
    const [overviewData, listData] = await Promise.all([
      statisticUserOverview({ dateLimit: query.dateLimit }),
      statisticUserOverviewList({ dateLimit: query.dateLimit })
    ]);
    overview.value = overviewData || {};
    overviewRows.value = listData || [];
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  await Promise.all([loadStaticData(), loadRangeData()]);
});
</script>
