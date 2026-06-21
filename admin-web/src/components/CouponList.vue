<template>
  <CouponForm v-if="viewMode === 'form'" :coupon-id="editId" :copy-id="copyId" @back="backToList" @saved="handleSaved" />
  <div v-else class="divBox">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <div class="demo-input-suffix acea-row">
          <span class="seachTiele">状态：</span>
          <el-select v-model="query.status" placeholder="请选择" class="filter-item selWidth mr30" clearable @change="searchList">
            <el-option label="未开启" :value="false" />
            <el-option label="开启" :value="true" />
          </el-select>
          <span class="seachTiele">优惠券名称：</span>
          <el-input v-model="query.name" placeholder="请输入优惠券名称" class="selWidth" clearable @keyup.enter="searchList" />
          <el-button class="ml30" type="primary" size="small" @click="searchList">搜索</el-button>
          <el-button size="small" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <template #header>
        <div class="clearfix">
          <el-button type="primary" @click="openCreate">添加优惠劵</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" style="width: 100%" size="small" border highlight-current-row>
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column prop="name" label="名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" min-width="90">
          <template #default="{ row }">{{ useTypeText(row.useType) }}</template>
        </el-table-column>
        <el-table-column prop="money" label="面值" min-width="100" />
        <el-table-column label="领取方式" min-width="110">
          <template #default="{ row }">{{ couponTypeText(row.type) }}</template>
        </el-table-column>
        <el-table-column label="领取日期" min-width="240">
          <template #default="{ row }">
            <span v-if="row.receiveEndTime">{{ formatTime(row.receiveStartTime) }} - {{ formatTime(row.receiveEndTime) }}</span>
            <span v-else>不限时</span>
          </template>
        </el-table-column>
        <el-table-column label="使用时间" min-width="220">
          <template #default="{ row }">
            <span v-if="row.day">{{ row.day }}天</span>
            <span v-else>{{ formatTime(row.useStartTime) }} - {{ formatTime(row.useEndTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发布数量" min-width="120">
          <template #default="{ row }">
            <span v-if="!row.isLimited">不限量</span>
            <span v-else>发布：{{ row.total }} / 剩余：{{ row.lastTotal }}</span>
          </template>
        </el-table-column>
        <el-table-column label="是否开启" min-width="120">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-text="开启"
              inactive-text="关闭"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="receive(row)">领取记录</el-button>
            <el-divider direction="vertical" />
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button v-if="row.status" link type="primary" @click="openCopy(row)">复制</el-button>
            <template v-if="row.status">
              <el-divider direction="vertical" />
            </template>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
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

    <el-dialog v-model="dialogVisible" title="领取记录" width="980px" @close="handleDialogClose">
      <el-table v-loading="issueLoading" :data="issueData" style="width: 100%">
        <el-table-column prop="id" label="记录ID" min-width="80" />
        <el-table-column prop="nickname" label="用户名" min-width="120" />
        <el-table-column label="用户头像" min-width="80">
          <template #default="{ row }">
            <el-image
              v-if="row.avatar"
              style="width: 36px; height: 36px"
              :src="assetUrl(row.avatar)"
              :preview-src-list="[assetUrl(row.avatar)]"
              fit="cover"
            />
          </template>
        </el-table-column>
        <el-table-column prop="money" label="面值" min-width="90">
          <template #default="{ row }">￥{{ moneyText(row.money) }}</template>
        </el-table-column>
        <el-table-column prop="minPrice" label="最低消费额" min-width="110">
          <template #default="{ row }">￥{{ moneyText(row.minPrice) }}</template>
        </el-table-column>
        <el-table-column label="领取方式" min-width="100">
          <template #default="{ row }">{{ receiveTypeText(row.type) }}</template>
        </el-table-column>
        <el-table-column label="使用状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="领取时间" min-width="180">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="有效期" min-width="210" show-overflow-tooltip>
          <template #default="{ row }">{{ validTimeText(row) }}</template>
        </el-table-column>
        <el-table-column label="使用时间" min-width="180">
          <template #default="{ row }">{{ formatTime(row.useTime) || '-' }}</template>
        </el-table-column>
      </el-table>
      <div class="block">
        <el-pagination
          v-model:current-page="issueQuery.page"
          v-model:page-size="issueQuery.limit"
          :page-sizes="[10, 20, 30, 40]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="issueTotal"
          @size-change="loadIssueList"
          @current-change="loadIssueList"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { couponDelete, couponList, couponUpdateStatus, couponUserList } from '../api';
import CouponForm from './CouponForm.vue';

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const issueLoading = ref(false);
const dialogVisible = ref(false);
const issueData = ref([]);
const issueTotal = ref(0);
const viewMode = ref('list');
const copyId = ref(null);
const editId = ref(null);

const query = reactive({
  page: 1,
  limit: 20,
  status: '',
  name: ''
});

const issueQuery = reactive({
  page: 1,
  limit: 10,
  couponId: ''
});

function searchList() {
  query.page = 1;
  loadList();
}

function handleReset() {
  query.status = '';
  query.name = '';
  searchList();
}

async function loadList() {
  loading.value = true;
  try {
    const params = {
      page: query.page,
      limit: query.limit,
      name: query.name
    };
    if (query.status !== '') {
      params.status = query.status;
    }
    const data = await couponList(params);
    tableData.value = data?.list || [];
    total.value = data?.total || 0;
  } finally {
    loading.value = false;
  }
}

async function handleStatusChange(row) {
  try {
    await couponUpdateStatus({ id: row.id, status: row.status });
    ElMessage.success('修改成功');
    await loadList();
  } catch {
    row.status = !row.status;
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('删除当前数据?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch {
    return;
  }
  await couponDelete({ id: row.id });
  ElMessage.success('删除成功');
  query.page = 1;
  await loadList();
}

function openCreate() {
  copyId.value = null;
  editId.value = null;
  viewMode.value = 'form';
}

function openCopy(row) {
  copyId.value = row.id;
  editId.value = null;
  viewMode.value = 'form';
}

function openEdit(row) {
  editId.value = row.id;
  copyId.value = null;
  viewMode.value = 'form';
}

function backToList() {
  viewMode.value = 'list';
  copyId.value = null;
  editId.value = null;
}

async function handleSaved() {
  backToList();
  query.page = 1;
  await loadList();
}

function receive(row) {
  dialogVisible.value = true;
  issueQuery.page = 1;
  issueQuery.couponId = row.id;
  loadIssueList();
}

function handleDialogClose() {
  dialogVisible.value = false;
}

async function loadIssueList() {
  issueLoading.value = true;
  try {
    const data = await couponUserList(issueQuery);
    issueData.value = data?.list || [];
    issueTotal.value = data?.total || 0;
  } finally {
    issueLoading.value = false;
  }
}

function useTypeText(value) {
  const map = {
    1: '通用券',
    2: '商品券',
    3: '品类券'
  };
  return map[value] || '-';
}

function couponTypeText(value) {
  const map = {
    1: '手动领取',
    2: '新人券',
    3: '赠送券'
  };
  return map[value] || '-';
}

function receiveTypeText(value) {
  const map = {
    receive: '自己领取',
    get: '自己领取',
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
  return map[Number(value)] || '-';
}

function statusTagType(value) {
  const map = {
    0: 'success',
    1: 'info',
    2: 'danger'
  };
  return map[Number(value)] || 'info';
}

function validTimeText(row) {
  const start = row.useStartTimeStr || formatTime(row.startTime).slice(0, 10);
  const end = row.useEndTimeStr || formatTime(row.endTime).slice(0, 10);
  if (start || end) {
    return `${start || '-'} - ${end || '-'}`;
  }
  return row.validStr || '-';
}

function moneyText(value) {
  const number = Number(value || 0);
  return Number.isFinite(number) ? number.toFixed(2) : '0.00';
}

function formatTime(value) {
  if (!value) return '';
  return String(value).replace('T', ' ').slice(0, 19);
}

function assetUrl(value) {
  if (!value) return '';
  if (/^https?:\/\//.test(value) || value.startsWith('/')) return value;
  return `/${value}`;
}

onMounted(loadList);
</script>
