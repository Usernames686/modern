<template>
  <div class="divBox financial-extract-list">
    <el-card class="box-card" shadow="never">
      <div class="container">
        <el-form inline size="small" label-width="70px" :model="query" @submit.prevent>
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
          <el-form-item label="提现状态：">
            <el-select v-model="query.status" class="selWidth" placeholder="请选择" @change="search">
              <el-option label="全部" value="" />
              <el-option label="审核中" :value="0" />
              <el-option label="已提现" :value="1" />
              <el-option label="已拒绝" :value="-1" />
            </el-select>
          </el-form-item>
          <el-form-item label="提现方式：">
            <el-select v-model="query.extractType" class="selWidth" placeholder="请选择" @change="search">
              <el-option label="全部" value="" />
              <el-option label="银行卡" value="bank" />
              <el-option label="支付宝" value="alipay" />
              <el-option label="微信" value="weixin" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键字：">
            <el-input
              v-model="query.keywords"
              placeholder="微信号/姓名/支付宝账号/银行卡号/失败原因"
              class="keyword-width"
              clearable
              @keyup.enter="search"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
            <el-button :icon="Refresh" @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <div class="extract-cards mt14">
      <div v-for="item in cardLists" :key="item.name" class="extract-card" :style="{ '--accent': item.color }">
        <div class="extract-card-icon">{{ item.short }}</div>
        <div>
          <div class="extract-card-name">{{ item.name }}</div>
          <div class="extract-card-count">{{ moneyText(item.count) }}</div>
        </div>
      </div>
    </div>

    <el-card class="box-card mt14" shadow="never">
      <el-table v-loading="loading" :data="rows" style="width: 100%" size="small" class="table" highlight-current-row>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="用户信息" min-width="180">
          <template #default="{ row }">
            <p>用户昵称：{{ row.nickName || '-' }}</p>
            <p>用户id：{{ row.uid }}</p>
          </template>
        </el-table-column>
        <el-table-column prop="extractPrice" label="提现金额" min-width="120" />
        <el-table-column label="提现方式" min-width="100">
          <template #default="{ row }">{{ extractTypeText(row.extractType) }}</template>
        </el-table-column>
        <el-table-column label="账号" min-width="220">
          <template #default="{ row }">
            <div v-if="row.extractType === 'bank'">
              <p>姓名：{{ row.realName || '-' }}</p>
              <p>卡号：{{ row.bankCode || '-' }}</p>
              <p>开户行：{{ row.bankName || row.bankAddress || '-' }}</p>
            </div>
            <div v-else-if="row.extractType === 'alipay'">
              <p>姓名：{{ row.realName || '-' }}</p>
              <p>支付宝号：{{ row.alipayCode || '-' }}</p>
              <div class="qrcode-row">
                收款码：
                <el-image v-if="row.qrcodeUrl" class="qrcode-img" :src="row.qrcodeUrl" :preview-src-list="[row.qrcodeUrl]" preview-teleported />
                <span v-else>无</span>
              </div>
            </div>
            <div v-else-if="row.extractType === 'weixin'">
              <p>姓名：{{ row.realName || '-' }}</p>
              <p>微信号：{{ row.wechat || '-' }}</p>
              <div class="qrcode-row">
                收款码：
                <el-image v-if="row.qrcodeUrl" class="qrcode-img" :src="row.qrcodeUrl" :preview-src-list="[row.qrcodeUrl]" preview-teleported />
                <span v-else>无</span>
              </div>
            </div>
            <span v-else>已退款</span>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" min-width="200">
          <template #default="{ row }">
            <div>
              <span class="spBlock">{{ statusText(row.status) }}</span>
              <span v-if="Number(row.status) === -1">拒绝原因：{{ row.failMsg || '-' }}</span>
            </div>
            <template v-if="Number(row.status) === 0">
              <el-button type="danger" :icon="Close" size="small" @click="handleFail(row.id)">未通过</el-button>
              <el-button type="primary" :icon="Check" size="small" @click="handlePass(row.id)">通过</el-button>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="200">
          <template #default="{ row }">{{ row.mark || '-' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="150">
          <template #default="{ row }">{{ dateText(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button v-if="Number(row.status) !== 1" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <span v-else>无</span>
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

    <el-dialog v-model="dialogVisible" title="编辑" width="540px" @closed="resetForm">
      <el-form :model="editForm" label-width="100px" size="small">
        <el-form-item label="提现方式：">
          <el-select v-model="editForm.extractType" class="dialog-width" placeholder="请选择">
            <el-option label="银行卡" value="bank" />
            <el-option label="支付宝" value="alipay" />
            <el-option label="微信" value="weixin" />
          </el-select>
        </el-form-item>
        <el-form-item label="提现金额：">
          <el-input-number v-model="editForm.extractPrice" class="dialog-width" :precision="2" :min="0.01" controls-position="right" />
        </el-form-item>
        <el-form-item label="姓名：">
          <el-input v-model="editForm.realName" class="dialog-width" />
        </el-form-item>
        <template v-if="editForm.extractType === 'bank'">
          <el-form-item label="银行卡号：">
            <el-input v-model="editForm.bankCode" class="dialog-width" />
          </el-form-item>
          <el-form-item label="开户行：">
            <el-input v-model="editForm.bankName" class="dialog-width" />
          </el-form-item>
          <el-form-item label="开户地址：">
            <el-input v-model="editForm.bankAddress" class="dialog-width" />
          </el-form-item>
        </template>
        <template v-else-if="editForm.extractType === 'alipay'">
          <el-form-item label="支付宝号：">
            <el-input v-model="editForm.alipayCode" class="dialog-width" />
          </el-form-item>
          <el-form-item label="收款码：">
            <el-input v-model="editForm.qrcodeUrl" class="dialog-width" />
          </el-form-item>
        </template>
        <template v-else-if="editForm.extractType === 'weixin'">
          <el-form-item label="微信号：">
            <el-input v-model="editForm.wechat" class="dialog-width" />
          </el-form-item>
          <el-form-item label="收款码：">
            <el-input v-model="editForm.qrcodeUrl" class="dialog-width" />
          </el-form-item>
        </template>
        <el-form-item label="备注：">
          <el-input v-model="editForm.mark" class="dialog-width" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Check, Close, Refresh, Search } from '@element-plus/icons-vue';
import { extractApplyBalance, extractApplyList, extractApplyStatus, extractApplyUpdate } from '../api';

const loading = ref(false);
const saving = ref(false);
const rows = ref([]);
const total = ref(0);
const timeRange = ref([]);
const dialogVisible = ref(false);
const cardLists = ref([]);
const query = reactive({
  extractType: '',
  status: '',
  dateLimit: '',
  keywords: '',
  page: 1,
  limit: 20
});
const editForm = reactive(defaultEditForm());

onMounted(() => {
  loadList();
  loadBalance();
});

async function loadList() {
  loading.value = true;
  try {
    const data = await extractApplyList(compactParams(query));
    rows.value = data?.list || [];
    total.value = Number(data?.total || 0);
  } finally {
    loading.value = false;
  }
}

async function loadBalance() {
  const data = await extractApplyBalance(compactParams({ dateLimit: query.dateLimit }));
  cardLists.value = [
    { name: '待提现金额', count: data?.toBeWithdrawn || 0, color: '#1890FF', short: '待' },
    { name: '佣金总金额', count: data?.commissionTotal || 0, color: '#A277FF', short: '佣' },
    { name: '已提现金额', count: data?.withdrawn || 0, color: '#EF9C20', short: '已' },
    { name: '未提现金额', count: data?.unDrawn || 0, color: '#1BBE6B', short: '未' }
  ];
}

function search() {
  query.page = 1;
  loadList();
  loadBalance();
}

function reset() {
  query.extractType = '';
  query.status = '';
  query.dateLimit = '';
  query.keywords = '';
  query.page = 1;
  query.limit = 20;
  timeRange.value = [];
  loadList();
  loadBalance();
}

function onChangeTime(value) {
  query.dateLimit = value?.length === 2 ? value.join(',') : '';
  query.page = 1;
  loadList();
  loadBalance();
}

function handleEdit(row) {
  Object.assign(editForm, defaultEditForm(), row, {
    extractPrice: Number(row.extractPrice || 0)
  });
  dialogVisible.value = true;
}

async function submitEdit() {
  saving.value = true;
  try {
    await extractApplyUpdate(compactParams(editForm));
    ElMessage.success('编辑成功');
    dialogVisible.value = false;
    loadList();
  } finally {
    saving.value = false;
  }
}

async function handleFail(id) {
  const result = await ElMessageBox.prompt('未通过', '拒绝原因', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputValue: '输入信息不完整或有误!',
    inputPlaceholder: '请输入原因',
    inputValidator: (value) => Boolean(value) || '请输入原因'
  }).catch(() => null);
  if (!result) return;
  await extractApplyStatus({ id, status: -1, backMessage: result.value });
  ElMessage.success('提交成功');
  loadList();
  loadBalance();
}

async function handlePass(id) {
  await ElMessageBox.confirm('审核通过吗', '提示', { type: 'warning' });
  await extractApplyStatus({ id, status: 1 });
  ElMessage.success('操作成功');
  loadList();
  loadBalance();
}

function resetForm() {
  Object.assign(editForm, defaultEditForm());
}

function defaultEditForm() {
  return {
    id: '',
    realName: '',
    extractType: 'bank',
    bankCode: '',
    bankAddress: '',
    alipayCode: '',
    extractPrice: 0,
    mark: '',
    wechat: '',
    bankName: '',
    qrcodeUrl: ''
  };
}

function compactParams(source) {
  return Object.fromEntries(
    Object.entries(source).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  );
}

function extractTypeText(type) {
  return { bank: '银行卡', alipay: '支付宝', weixin: '微信' }[type] || type || '-';
}

function statusText(status) {
  return { '-1': '已拒绝', 0: '审核中', 1: '已提现' }[status] || '-';
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
  width: 350px;
}

.keyword-width {
  width: 350px;
}

.dialog-width {
  width: 360px;
}

.extract-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.extract-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 92px;
  padding: 18px 20px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
}

.extract-card-icon {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: var(--accent);
  font-weight: 600;
}

.extract-card-name {
  color: #606266;
  font-size: 13px;
}

.extract-card-count {
  margin-top: 8px;
  color: #303133;
  font-size: 22px;
  font-weight: 600;
}

.qrcode-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.qrcode-img {
  width: 36px;
  height: 36px;
  border-radius: 4px;
}

.spBlock {
  display: block;
  margin-bottom: 8px;
}

.block {
  padding-bottom: 20px;
}

@media (max-width: 1100px) {
  .extract-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .extract-cards {
    grid-template-columns: 1fr;
  }
}
</style>
