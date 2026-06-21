<template>
  <div class="divBox distribution-promoter-list">
    <el-card :bordered="false" shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form size="small" label-width="75px" :model="query" class="filter-form" @submit.prevent>
          <el-form-item label="时间选择：">
            <el-date-picker
              v-model="timeRange"
              value-format="YYYY-MM-DD"
              format="YYYY-MM-DD"
              type="daterange"
              placement="bottom-end"
              placeholder="自定义时间"
              style="width: 260px"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              @change="onChangeTime"
            />
          </el-form-item>
          <el-form-item label="关键字：">
            <el-input v-model="query.keywords" placeholder="请输入姓名、电话、UID" class="selWidth" clearable @keyup.enter="search" />
          </el-form-item>
          <div class="button-row">
            <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
            <el-button :icon="Refresh" @click="reset">重置</el-button>
          </div>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14" shadow="never">
      <el-table v-loading="loading" :data="rows" style="width: 100%" size="small" class="table" highlight-current-row>
        <el-table-column prop="uid" label="ID" width="60" />
        <el-table-column label="头像" min-width="80">
          <template #default="{ row }">
            <el-image v-if="row.avatar" class="avatar-img" :src="row.avatar" :preview-src-list="[row.avatar]" preview-teleported />
            <span v-else>无</span>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="用户信息" min-width="130" show-overflow-tooltip />
        <el-table-column prop="spreadCount" label="推广用户(一级)数量" sortable min-width="150" />
        <el-table-column prop="spreadOrderNum" label="推广订单数量" sortable min-width="120" />
        <el-table-column prop="spreadOrderTotalPrice" label="推广订单金额" sortable min-width="120" />
        <el-table-column prop="totalBrokeragePrice" label="佣金总金额" sortable min-width="120" />
        <el-table-column prop="extractCountPrice" label="已提现金额" sortable min-width="120" />
        <el-table-column prop="extractCountNum" label="已提现次数" sortable min-width="120" />
        <el-table-column prop="brokeragePrice" label="未提现金额" sortable min-width="120" />
        <el-table-column prop="freezeBrokeragePrice" label="冻结中佣金" sortable min-width="120" />
        <el-table-column label="成为推广员时间" min-width="150">
          <template #default="{ row }">{{ dateText(row.promoterTime) }}</template>
        </el-table-column>
        <el-table-column prop="spreadNickname" label="上级推广人" min-width="150" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openSpreadUsers(row.uid)">推广人</el-button>
            <el-divider direction="vertical" />
            <el-dropdown>
              <span class="el-dropdown-link">更多<el-icon><ArrowDown /></el-icon></span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="openSpreadOrders(row.uid)">推广订单</el-dropdown-item>
                  <el-dropdown-item v-if="row.spreadNickname && row.spreadNickname !== '无'" @click="clearSpreadUser(row)">
                    清除上级推广人
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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

    <el-dialog v-model="dialogVisible" :title="`${dialogTitle}列表`" width="900px">
      <div class="container">
        <el-form size="small" label-width="66px" class="filter-form" @submit.prevent>
          <el-form-item v-if="dialogType === 'order'" label="时间选择：">
            <el-date-picker
              v-model="dialogTimeRange"
              value-format="YYYY-MM-DD"
              format="YYYY-MM-DD"
              type="daterange"
              placement="bottom-end"
              placeholder="自定义时间"
              style="width: 380px"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              @change="onChangeDialogTime"
            />
          </el-form-item>
          <el-form-item label="用户类型：">
            <el-select v-model="dialogQuery.type" class="selWidth" placeholder="请选择用户类型" @change="searchDialog">
              <el-option label="全部" :value="0" />
              <el-option label="一级推广人" :value="1" />
              <el-option label="二级推广人" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键字：">
            <el-input
              v-model="dialogQuery.nickName"
              :placeholder="dialogType === 'order' ? '请输入订单号' : '请输入姓名、电话、UID'"
              class="selWidth"
              clearable
              @keyup.enter="searchDialog"
            />
          </el-form-item>
          <div class="button-row mb30">
            <el-button type="primary" :icon="Search" @click="searchDialog">搜索</el-button>
            <el-button :icon="Refresh" @click="resetDialog">重置</el-button>
          </div>
        </el-form>
      </div>
      <el-table
        v-if="dialogType === 'man'"
        v-loading="dialogLoading"
        :data="dialogRows"
        style="width: 100%"
        size="small"
        class="table"
        highlight-current-row
      >
        <el-table-column prop="uid" label="ID" width="60" />
        <el-table-column label="头像" min-width="80">
          <template #default="{ row }">
            <el-image v-if="row.avatar" class="avatar-img" :src="row.avatar" :preview-src-list="[row.avatar]" preview-teleported />
            <span v-else>无</span>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="用户信息" min-width="130" />
        <el-table-column label="是否推广员" min-width="120">
          <template #default="{ row }">{{ Number(row.isPromoter) === 1 ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="spreadCount" label="推广人数" sortable min-width="120" />
        <el-table-column prop="payCount" label="订单数" sortable min-width="120" />
      </el-table>
      <el-table
        v-else
        v-loading="dialogLoading"
        :data="dialogRows"
        style="width: 100%"
        size="small"
        class="table"
        highlight-current-row
      >
        <el-table-column prop="orderId" label="订单ID" min-width="120" />
        <el-table-column label="用户信息" min-width="150">
          <template #default="{ row }">
            <span>{{ row.realName || '-' }}</span>
            <el-divider direction="vertical" />
            <span>{{ row.userPhone || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" min-width="150">
          <template #default="{ row }">{{ dateText(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column prop="price" label="返佣金额" sortable min-width="120" />
      </el-table>
      <div class="block dialog-block">
        <el-pagination
          v-model:current-page="dialogQuery.page"
          v-model:page-size="dialogQuery.limit"
          :page-sizes="[10, 20, 30, 40]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="dialogTotal"
          @size-change="loadDialog"
          @current-change="loadDialog"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowDown, Refresh, Search } from '@element-plus/icons-vue';
import { promoterList, spreadClear, spreadOrderList, spreadUserList } from '../api';

const loading = ref(false);
const rows = ref([]);
const total = ref(0);
const timeRange = ref([]);
const dialogVisible = ref(false);
const dialogLoading = ref(false);
const dialogRows = ref([]);
const dialogTotal = ref(0);
const dialogType = ref('man');
const dialogTitle = ref('推广人');
const dialogTimeRange = ref([]);
const query = reactive({
  dateLimit: '',
  keywords: '',
  page: 1,
  limit: 20
});
const dialogQuery = reactive({
  page: 1,
  limit: 10,
  dateLimit: '',
  type: 0,
  nickName: '',
  uid: ''
});

onMounted(loadList);

async function loadList() {
  loading.value = true;
  try {
    const data = await promoterList(compactParams(query));
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

function openSpreadUsers(uid) {
  openDialog(uid, 'man', '推广人');
}

function openSpreadOrders(uid) {
  openDialog(uid, 'order', '推广订单');
}

function openDialog(uid, type, title) {
  dialogType.value = type;
  dialogTitle.value = title;
  Object.assign(dialogQuery, {
    page: 1,
    limit: 10,
    dateLimit: '',
    type: 0,
    nickName: '',
    uid
  });
  dialogTimeRange.value = [];
  dialogVisible.value = true;
  loadDialog();
}

async function loadDialog() {
  dialogLoading.value = true;
  try {
    const params = { page: dialogQuery.page, limit: dialogQuery.limit };
    const payload = compactParams({
      dateLimit: dialogQuery.dateLimit,
      type: dialogQuery.type,
      nickName: dialogQuery.nickName,
      uid: dialogQuery.uid
    });
    const data = dialogType.value === 'man'
      ? await spreadUserList(params, payload)
      : await spreadOrderList(params, payload);
    dialogRows.value = data?.list || [];
    dialogTotal.value = Number(data?.total || 0);
  } finally {
    dialogLoading.value = false;
  }
}

function searchDialog() {
  dialogQuery.page = 1;
  loadDialog();
}

function resetDialog() {
  dialogQuery.dateLimit = '';
  dialogQuery.type = 0;
  dialogQuery.nickName = '';
  dialogQuery.page = 1;
  dialogTimeRange.value = [];
  loadDialog();
}

function onChangeDialogTime(value) {
  dialogQuery.dateLimit = value?.length === 2 ? value.join(',') : '';
  dialogQuery.page = 1;
  loadDialog();
}

async function clearSpreadUser(row) {
  await ElMessageBox.confirm(`解除【${row.nickname}】的上级推广人吗`, '提示', { type: 'warning' });
  await spreadClear(row.uid);
  ElMessage.success('清除成功');
  loadList();
}

function compactParams(source) {
  return Object.fromEntries(
    Object.entries(source).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  );
}

function dateText(value) {
  return value ? String(value).replace('T', ' ') : '-';
}
</script>

<style scoped>
.filter-form {
  display: flex;
  flex-wrap: wrap;
}

.selWidth {
  width: 300px;
}

.button-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: 30px;
}

.avatar-img {
  width: 38px;
  height: 38px;
  border-radius: 4px;
}

.el-dropdown-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: var(--el-color-primary);
  cursor: pointer;
  font-size: 12px;
}

.dialog-block {
  padding-bottom: 20px;
}

.mb30 {
  margin-bottom: 30px;
}
</style>
