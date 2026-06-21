<template>
  <div class="divBox financial-monitor-list">
    <el-card :bordered="false" shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form inline size="small" label-width="70px" :model="query" @submit.prevent>
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
          <el-form-item label="关键字：">
            <el-input v-model="query.keywords" placeholder="微信昵称/ID" class="selWidth" clearable @keyup.enter="search" />
          </el-form-item>
          <el-form-item label="明细类型：">
            <el-select v-model="query.title" class="selWidth" clearable placeholder="请选择" @change="selectType">
              <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
            <el-button :icon="Refresh" @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14" shadow="never">
      <el-table v-loading="loading" :data="rows" style="width: 100%" size="small" class="table" highlight-current-row>
        <el-table-column prop="uid" label="会员ID" width="90" />
        <el-table-column prop="nickName" label="昵称" min-width="130" show-overflow-tooltip />
        <el-table-column label="金额" min-width="120" sortable>
          <template #default="{ row }">
            <span :class="Number(row.pm) === 1 ? 'color-red' : 'color-green'">
              {{ Number(row.pm) === 1 ? '+' : '-' }}{{ moneyText(row.number) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="明细类型" min-width="110" />
        <el-table-column prop="mark" label="备注" min-width="240" show-overflow-tooltip />
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ dateText(row.createTime) }}</template>
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
import { financeMonitorList } from '../api';

const loading = ref(false);
const rows = ref([]);
const total = ref(0);
const timeRange = ref([]);
const options = [
  { value: 'recharge', label: '充值支付' },
  { value: 'admin', label: '后台操作' },
  { value: 'productRefund', label: '商品退款' },
  { value: 'payProduct', label: '购买商品' }
];

const query = reactive({
  title: '',
  dateLimit: '',
  keywords: '',
  page: 1,
  limit: 20
});

onMounted(loadList);

async function loadList() {
  loading.value = true;
  try {
    const data = await financeMonitorList(compactParams(query));
    rows.value = data?.list || [];
    total.value = Number(data?.total || 0);
  } finally {
    loading.value = false;
  }
}

function search() {
  query.page = 1;
  loadList();
}

function reset() {
  query.title = '';
  query.dateLimit = '';
  query.keywords = '';
  query.page = 1;
  query.limit = 20;
  timeRange.value = [];
  loadList();
}

function selectType() {
  query.page = 1;
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

function moneyText(value) {
  const number = Number(value || 0);
  return Number.isFinite(number) ? number.toFixed(2) : '0.00';
}

function dateText(value) {
  return value ? String(value).replace('T', ' ') : '-';
}
</script>

<style scoped>
.selWidth {
  width: 300px;
}

.color-red {
  color: #f5222d;
}

.color-green {
  color: #7abe5c;
}

.block {
  padding-bottom: 20px;
}
</style>
