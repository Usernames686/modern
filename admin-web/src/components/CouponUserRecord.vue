<template>
  <div class="divBox">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form :inline="true">
          <el-form-item label="使用状态：" class="mr10">
            <el-select
              v-model="query.status"
              placeholder="请选择使用状态"
              clearable
              class="selWidth"
              @change="searchList"
            >
              <el-option label="已使用" :value="1" />
              <el-option label="未使用" :value="0" />
              <el-option label="已过期" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="领取人：">
            <el-select
              v-model="query.uid"
              class="selWidth"
              reserve-keyword
              remote
              filterable
              :remote-method="remoteMethod"
              :loading="userLoading"
              placeholder="请输入领取人"
              clearable
              @change="searchList"
            >
              <el-option v-for="item in userOptions" :key="item.uid" :label="item.nickname" :value="item.uid" />
            </el-select>
          </el-form-item>
          <el-form-item label="优惠劵：" class="mr10">
            <el-input v-model="query.name" placeholder="请输入优惠劵" class="selWidth" clearable @keyup.enter="searchList" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="searchList">搜索</el-button>
            <el-button size="small" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <el-table v-loading="loading" :data="tableData" style="width: 100%" size="small" border>
        <el-table-column prop="couponId" label="优惠券ID" min-width="80" />
        <el-table-column prop="name" label="优惠券名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="nickname" label="领取人" min-width="130" show-overflow-tooltip />
        <el-table-column prop="money" label="面值" min-width="100" />
        <el-table-column prop="minPrice" label="最低消费额" min-width="120" />
        <el-table-column label="开始使用时间" min-width="150">
          <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束使用时间" min-width="150">
          <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="获取方式" min-width="150">
          <template #default="{ row }">{{ typeText(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="is_fail" label="是否可用" min-width="100">
          <template #default="{ row }">
            <el-icon v-if="row.status === 0" class="usable-icon"><Check /></el-icon>
            <el-icon v-else class="unusable-icon"><Close /></el-icon>
          </template>
        </el-table-column>
        <el-table-column label="使用状态" min-width="100">
          <template #default="{ row }">{{ statusText(row.status) }}</template>
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
import { Check, Close } from '@element-plus/icons-vue';
import { couponUserList, userList } from '../api';

const loading = ref(false);
const userLoading = ref(false);
const tableData = ref([]);
const total = ref(0);
const userOptions = ref([]);

const query = reactive({
  page: 1,
  limit: 20,
  uid: '',
  name: '',
  status: ''
});

function searchList() {
  query.page = 1;
  loadList();
}

function handleReset() {
  query.status = '';
  query.name = '';
  query.uid = '';
  loadList();
}

async function remoteMethod(keyword) {
  if (!keyword) {
    userOptions.value = [];
    return;
  }
  userLoading.value = true;
  try {
    const data = await userList({ keywords: keyword, page: 1, limit: 10 });
    userOptions.value = data?.list || [];
  } finally {
    userLoading.value = false;
  }
}

async function loadList() {
  loading.value = true;
  try {
    const params = {
      page: query.page,
      limit: query.limit,
      uid: query.uid,
      name: query.name
    };
    if (query.status !== '') {
      params.status = query.status;
    }
    const data = await couponUserList(params);
    tableData.value = data?.list || [];
    total.value = data?.total || 0;
  } finally {
    loading.value = false;
  }
}

function typeText(value) {
  const map = {
    receive: '自己领取',
    send: '后台发送',
    give: '满赠',
    new: '新人',
    register: '新人',
    buy: '买赠送'
  };
  return map[value] || value || '-';
}

function statusText(value) {
  const map = {
    0: '未使用',
    1: '已使用',
    2: '已过期'
  };
  return map[value] || '-';
}

function formatTime(value) {
  if (!value) return '';
  return String(value).replace('T', ' ').slice(0, 19);
}

onMounted(loadList);
</script>

<style scoped>
.seachTiele {
  line-height: 35px;
}

.usable-icon {
  color: #0092dc;
  font-size: 14px;
}

.unusable-icon {
  color: #ed5565;
  font-size: 14px;
}
</style>
