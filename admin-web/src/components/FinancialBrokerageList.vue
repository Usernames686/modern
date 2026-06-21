<template>
  <div class="divBox financial-brokerage-list">
    <el-card :bordered="false" shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form label-width="75px" :inline="true" size="small" :model="query" @submit.prevent>
          <el-form-item label="变动类型：">
            <el-select v-model="query.type" class="selWidth" :clearable="!lockType" :disabled="lockType" placeholder="请选择" @change="search">
              <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14" shadow="never">
      <el-table
        v-loading="loading"
        :data="rows"
        style="width: 100%"
        size="small"
        class="table"
        highlight-current-row
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="佣金变动" min-width="100">
          <template #default="{ row }">
            <span :class="Number(row.type) === 1 ? 'color-red' : 'color-green'">
              {{ Number(row.type) === 1 ? '+' : '-' }}{{ moneyText(row.price) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="mark" label="变动信息" min-width="150" show-overflow-tooltip />
        <el-table-column prop="title" label="变动类型" min-width="130" />
        <el-table-column prop="userName" label="用户信息" min-width="150" show-overflow-tooltip />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ dateText(row.updateTime) }}</template>
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
import { brokerageRecordList } from '../api';

const props = defineProps({
  initialType: {
    type: [Number, String],
    default: ''
  },
  lockType: {
    type: Boolean,
    default: false
  }
});

const loading = ref(false);
const rows = ref([]);
const total = ref(0);
const typeOptions = [
  { value: 1, label: '订单返佣' },
  { value: 2, label: '申请提现' },
  { value: 3, label: '提现失败' },
  { value: 4, label: '提现成功' },
  { value: 5, label: '佣金转余额' }
];

const query = reactive({
  type: props.initialType,
  page: 1,
  limit: 20
});

onMounted(loadList);

async function loadList() {
  loading.value = true;
  try {
    const data = await brokerageRecordList(compactParams(query));
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
