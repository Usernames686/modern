<template>
  <div class="divBox financial-recharge-list">
    <el-card class="box-card" shadow="never">
      <div class="container">
        <el-form size="small" label-width="70px" inline :model="query" @submit.prevent>
          <el-form-item label="时间选择：">
            <el-date-picker
              v-model="timeRange"
              value-format="YYYY-MM-DD"
              format="YYYY-MM-DD"
              type="daterange"
              placement="bottom-end"
              placeholder="自定义时间"
              style="width: 250px"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              @change="onChangeTime"
            />
          </el-form-item>
          <el-form-item label="用户ID：">
            <el-input v-model="query.uid" placeholder="用户id" class="selWidth" clearable @keyup.enter="search" />
          </el-form-item>
          <el-form-item label="订单号：">
            <el-input v-model="query.keywords" placeholder="订单号" class="selWidth" clearable @keyup.enter="search" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
            <el-button :icon="Refresh" @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <div class="recharge-cards mt14">
      <div v-for="item in cardLists" :key="item.name" class="recharge-card" :style="{ '--accent': item.color }">
        <div class="recharge-card-icon">{{ item.icon }}</div>
        <div>
          <div class="recharge-card-name">{{ item.name }}</div>
          <div class="recharge-card-count">{{ moneyText(item.count) }}</div>
        </div>
      </div>
    </div>

    <el-card class="box-card mt14" shadow="never">
      <el-table v-loading="loading" :data="rows" style="width: 100%" size="small" class="table" highlight-current-row>
        <el-table-column prop="uid" label="UID" width="70" />
        <el-table-column label="头像" min-width="80">
          <template #default="{ row }">
            <el-image
              v-if="row.avatar"
              class="avatar-img"
              :src="row.avatar"
              :preview-src-list="[row.avatar]"
              preview-teleported
              fit="cover"
            />
            <span v-else>无</span>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="用户昵称" min-width="130" show-overflow-tooltip />
        <el-table-column prop="orderId" label="订单号" min-width="220" show-overflow-tooltip />
        <el-table-column prop="price" label="支付金额" min-width="120" sortable />
        <el-table-column prop="givePrice" label="赠送金额" min-width="120" sortable />
        <el-table-column label="充值类型" min-width="100">
          <template #default="{ row }">{{ rechargeTypeText(row.rechargeType) }}</template>
        </el-table-column>
        <el-table-column label="支付时间" width="180">
          <template #default="{ row }">{{ dateText(row.payTime) || '无' }}</template>
        </el-table-column>
      </el-table>
      <div class="block">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.limit"
          :page-sizes="[20, 40, 60, 80]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { Refresh, Search } from '@element-plus/icons-vue';
import { topUpLogBalance, topUpLogList } from '../api';

const loading = ref(false);
const rows = ref([]);
const total = ref(0);
const timeRange = ref([]);
const cardLists = ref([
  { name: '充值总金额', count: 0, color: '#1890FF', icon: '总' },
  { name: '小程序充值金额', count: 0, color: '#A277FF', icon: '小' },
  { name: '公众号充值金额', count: 0, color: '#EF9C20', icon: '公' }
]);

const query = reactive({
  uid: '',
  dateLimit: '',
  keywords: '',
  page: 1,
  limit: 20
});

onMounted(() => {
  loadList();
  loadStatistics();
});

async function loadList() {
  loading.value = true;
  try {
    const data = await topUpLogList(compactParams(query));
    rows.value = data?.list || [];
    total.value = Number(data?.total || 0);
  } finally {
    loading.value = false;
  }
}

async function loadStatistics() {
  const stat = await topUpLogBalance();
  cardLists.value = [
    { name: '充值总金额', count: stat?.total || 0, color: '#1890FF', icon: '总' },
    { name: '小程序充值金额', count: stat?.routine || 0, color: '#A277FF', icon: '小' },
    { name: '公众号充值金额', count: stat?.weChat || 0, color: '#EF9C20', icon: '公' }
  ];
}

function search() {
  query.page = 1;
  loadList();
}

function reset() {
  query.uid = '';
  query.dateLimit = '';
  query.keywords = '';
  query.page = 1;
  query.limit = 20;
  timeRange.value = [];
  loadList();
}

function onChangeTime(value) {
  query.dateLimit = value?.length === 2 ? value.join(',') : '';
  query.page = 1;
  loadList();
}

function compactParams(source) {
  return Object.fromEntries(
    Object.entries(source).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  );
}

function rechargeTypeText(type) {
  const map = {
    weixin: '微信',
    public: '公众号',
    routine: '小程序',
    h5: 'H5',
    weixinh5: '微信H5',
    balance: '佣金转余额'
  };
  return map[type] || type || '-';
}

function moneyText(value) {
  const number = Number(value || 0);
  return Number.isFinite(number) ? number.toFixed(2) : '0.00';
}

function dateText(value) {
  return value ? String(value).replace('T', ' ') : '';
}
</script>

<style scoped>
.selWidth {
  width: 300px;
}

.recharge-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.recharge-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 92px;
  padding: 18px 20px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
}

.recharge-card-icon {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: var(--accent);
  font-weight: 600;
}

.recharge-card-name {
  color: #606266;
  font-size: 13px;
}

.recharge-card-count {
  margin-top: 8px;
  color: #303133;
  font-size: 22px;
  font-weight: 600;
}

.avatar-img {
  width: 38px;
  height: 38px;
  border-radius: 4px;
}

.block {
  padding-bottom: 20px;
}

@media (max-width: 900px) {
  .recharge-cards {
    grid-template-columns: 1fr;
  }
}
</style>
