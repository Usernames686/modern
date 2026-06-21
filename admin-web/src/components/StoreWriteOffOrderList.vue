<template>
  <div class="divBox">
    <el-card class="box-card">
      <div class="clearfix">
        <div class="container filter-container">
          <el-form size="small" label-width="75px" :inline="true" @submit.prevent>
            <el-form-item label="时间选择：">
              <el-date-picker
                v-model="timeValue"
                value-format="YYYY-MM-DD"
                format="YYYY-MM-DD"
                type="daterange"
                placement="bottom-end"
                placeholder="自定义时间"
                class="writeoff-date"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                @change="handleTimeChange"
              />
            </el-form-item>
            <el-form-item label="选择门店：">
              <el-select
                v-model="query.storeId"
                filterable
                placeholder="请选择"
                class="selWidth"
                clearable
                @change="searchList"
              >
                <el-option v-for="item in storeSelectList" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="关键字：">
              <el-input
                v-model="query.keywords"
                placeholder="请输入姓名、电话、订单ID"
                class="selWidth"
                clearable
                @keyup.enter="searchList"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="small" @click="searchList">搜索</el-button>
              <el-button size="small" @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </el-card>

    <div class="writeoff-cards mt14">
      <div v-for="item in cardLists" :key="item.name" class="writeoff-card" :style="{ borderTopColor: item.color }">
        <div class="writeoff-card-icon" :style="{ color: item.color, backgroundColor: item.bg }">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div class="writeoff-card-meta">
          <span>{{ item.name }}</span>
          <strong>{{ item.count }}</strong>
        </div>
      </div>
    </div>

    <el-card class="box-card mt14">
      <el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%"
        size="small"
        class="table"
        highlight-current-row
        border
      >
        <el-table-column label="订单号" prop="orderId" min-width="210" />
        <el-table-column prop="realName" label="用户信息" min-width="110" />
        <el-table-column label="推荐人信息" min-width="120">
          <template #default="{ row }">{{ row.spreadInfo?.name || '-' }}</template>
        </el-table-column>
        <el-table-column label="商品信息" min-width="420" show-overflow-tooltip>
          <template #default="{ row }">
            <div v-if="row.productList?.length">
              <div v-for="item in row.productList" :key="item.id" class="tabBox acea-row row-middle">
                <div class="demo-image__preview mr10">
                  <el-image :src="item.info.image" :preview-src-list="[item.info.image]" fit="cover">
                    <template #error>
                      <div class="store-image-error">
                        <el-icon><Picture /></el-icon>
                      </div>
                    </template>
                  </el-image>
                </div>
                <div class="text_overflow">
                  <span class="tabBox_tit mr10">{{ item.info.productName }} | {{ item.info.sku || '-' }}</span>
                  <span class="tabBox_pice">{{ money(item.info.price) }} x {{ item.info.payNum || 0 }}</span>
                </div>
              </div>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="payPrice" label="实际支付" min-width="100">
          <template #default="{ row }">{{ money(row.payPrice) }}</template>
        </el-table-column>
        <el-table-column prop="clerkName" label="核销员" min-width="110">
          <template #default="{ row }">{{ row.clerkName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="storeName" label="核销门店" min-width="150" />
        <el-table-column label="支付状态" min-width="90">
          <template #default="{ row }">{{ row.paid ? '已支付' : '未支付' }}</template>
        </el-table-column>
        <el-table-column label="订单状态" min-width="110">
          <template #default="{ row }">{{ row.statusStr?.value || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
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
import { computed, onMounted, reactive, ref } from 'vue';
import { Document, Money, Picture, RefreshLeft, Tickets } from '@element-plus/icons-vue';
import { storePointList, writeOffOrderList } from '../api';

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const timeValue = ref([]);
const storeSelectList = ref([]);
const stats = reactive({
  total: 0,
  orderTotalPrice: '0.00',
  refundTotal: 0,
  refundTotalPrice: '0.00'
});

const query = reactive({
  keywords: '',
  storeId: '',
  dateLimit: '',
  page: 1,
  limit: 20
});

const cardLists = computed(() => [
  { name: '订单数量', count: stats.total, color: '#1890FF', bg: '#e8f3ff', icon: Document },
  { name: '订单金额', count: money(stats.orderTotalPrice), color: '#A277FF', bg: '#f1ebff', icon: Money },
  { name: '退款总单数', count: stats.refundTotal, color: '#EF9C20', bg: '#fff4e2', icon: Tickets },
  { name: '退款总金额', count: money(stats.refundTotalPrice), color: '#1BBE6B', bg: '#e7f8ef', icon: RefreshLeft }
]);

async function loadStores() {
  const data = await storePointList({ page: 1, limit: 999, status: 1, keywords: '' });
  storeSelectList.value = data?.list || [];
}

async function loadList() {
  loading.value = true;
  try {
    const data = await writeOffOrderList({ ...query });
    tableData.value = data?.list?.list || [];
    total.value = data?.list?.total || 0;
    stats.total = data?.total || 0;
    stats.orderTotalPrice = data?.orderTotalPrice || '0.00';
    stats.refundTotal = data?.refundTotal || 0;
    stats.refundTotalPrice = data?.refundTotalPrice || '0.00';
  } finally {
    loading.value = false;
  }
}

function searchList() {
  query.page = 1;
  loadList();
}

function handleReset() {
  query.dateLimit = '';
  query.storeId = '';
  query.keywords = '';
  timeValue.value = [];
  query.page = 1;
  loadList();
}

function handleTimeChange(value) {
  timeValue.value = value || [];
  query.dateLimit = Array.isArray(value) && value.length === 2 ? value.join(',') : '';
  query.page = 1;
  loadList();
}

function money(value) {
  const number = Number(value || 0);
  return `￥${number.toFixed(2)}`;
}

function formatTime(value) {
  if (!value) return '';
  return String(value).replace('T', ' ').slice(0, 19);
}

onMounted(async () => {
  await Promise.all([loadStores(), loadList()]);
});
</script>
