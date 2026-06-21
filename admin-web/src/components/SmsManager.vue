<template>
  <div class="divBox">
    <el-card v-if="view === 'config'" class="box-card">
      <template #header>
        <div class="clearfix">短信账户</div>
      </template>
      <el-alert
        title="当前未配置短信服务商，短信发送、充值和签名同步功能需配置后启用。"
        type="warning"
        :closable="false"
        show-icon
        class="mb20"
      />
      <el-descriptions :column="1" border>
        <el-descriptions-item label="短信账户名称">{{ passInfo.account || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前剩余条数">{{ passInfo.sms?.num ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="运行状态">{{ passInfo.message || '短信服务未配置' }}</el-descriptions-item>
      </el-descriptions>
      <div class="mt20">
        <el-button type="primary" @click="go('/operation/systemSms/template')">短信模板</el-button>
        <el-button @click="go('/operation/systemSms/message')">短信开关</el-button>
        <el-button @click="view = 'record'">发送记录</el-button>
      </div>
    </el-card>

    <el-card v-else-if="view === 'template'" class="box-card">
      <template #header>
        <div class="sms-card-header">
          <div class="sms-card-title">
            <el-button class="mr12" @click="go('/operation/onePass')">返回</el-button>
            <el-button type="primary" @click="openTemplateDialog">添加短信模板</el-button>
          </div>
          <el-alert
            class="sms-alert"
            title="模板会保存到系统配置；外部审核同步需完成短信服务商配置后启用。"
            type="warning"
            :closable="false"
            show-icon
          />
        </div>
      </template>

      <el-table v-loading="loading" :data="templateRows" size="small" highlight-current-row>
        <el-table-column prop="id" label="ID" min-width="70" />
        <el-table-column prop="tempId" label="模板ID" min-width="110" />
        <el-table-column prop="title" label="模板名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="content" label="模板内容" min-width="440" show-overflow-tooltip />
        <el-table-column label="模板类型" min-width="100">
          <template #default="{ row }">{{ tempTypeText(row.tempType) }}</template>
        </el-table-column>
        <el-table-column label="模板状态" min-width="100">
          <template #default="{ row }">{{ row.status === 1 ? '可用' : '不可用' }}</template>
        </el-table-column>
        <el-table-column prop="tempKey" label="审核结果" min-width="120">
          <template #default="{ row }">{{ row.tempKey || '待服务商审核' }}</template>
        </el-table-column>
      </el-table>
      <div class="block">
        <el-pagination
          v-model:current-page="templateQuery.page"
          v-model:page-size="templateQuery.limit"
          :page-sizes="[20, 40, 60, 80]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="templateTotal"
          @size-change="loadTemplates"
          @current-change="loadTemplates"
        />
      </div>
    </el-card>

    <el-card v-else-if="view === 'message'" class="box-card">
      <template #header>
        <div class="clearfix">短信开关</div>
      </template>
      <el-form ref="configFormRef" :model="smsConfig" label-width="230px" class="sms-config-form">
        <el-form-item v-for="item in switchFields" :key="item.key" :label="item.label">
          <el-radio-group v-model="smsConfig[item.key]">
            <el-radio label="0">关闭</el-radio>
            <el-radio label="1">开启</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="验证码有效时间 (分钟)：">
          <el-input-number v-model="smsCodeExpire" :min="1" :step="1" step-strictly />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="saveConfig">提交</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-else-if="view === 'pay'" class="box-card">
      <template #header>
        <div class="clearfix">短信购买</div>
      </template>
      <el-tabs v-model="payType">
        <el-tab-pane label="短信" name="sms" />
        <el-tab-pane label="商品采集" name="copy" />
        <el-tab-pane label="物流查询" name="expr_query" />
        <el-tab-pane label="电子面单打印" name="expr_dump" />
      </el-tabs>
      <el-alert
        title="短信购买属于第三方平台支付能力，需完成服务商配置后生成支付码。"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-empty description="暂无可购买套餐" />
    </el-card>

    <el-card v-else class="box-card">
      <template #header>
        <div class="sms-card-header">
          <div class="sms-card-title">短信发送记录</div>
          <el-form inline :model="recordQuery" @submit.prevent>
            <el-form-item label="手机号：">
              <el-input v-model="recordQuery.phone" size="small" clearable placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="模板ID：">
              <el-input v-model="recordQuery.template" size="small" clearable placeholder="请输入模板ID" />
            </el-form-item>
            <el-button type="primary" size="small" @click="searchRecords">搜索</el-button>
            <el-button size="small" @click="resetRecords">重置</el-button>
          </el-form>
        </div>
      </template>
      <el-table v-loading="loading" :data="recordRows" size="small" highlight-current-row>
        <el-table-column prop="id" label="ID" min-width="70" />
        <el-table-column prop="uid" label="短信账号" min-width="120" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="content" label="短信内容" min-width="320" show-overflow-tooltip />
        <el-table-column prop="template" label="模板ID" min-width="110" />
        <el-table-column label="发送状态" min-width="110">
          <template #default="{ row }">{{ resultText(row.resultcode) }}</template>
        </el-table-column>
        <el-table-column prop="memo" label="平台返回" min-width="180" show-overflow-tooltip />
        <el-table-column label="发送时间" min-width="160">
          <template #default="{ row }">{{ dateText(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <div class="block">
        <el-pagination
          v-model:current-page="recordQuery.page"
          v-model:page-size="recordQuery.limit"
          :page-sizes="[20, 40, 60, 80]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="recordTotal"
          @size-change="loadRecords"
          @current-change="loadRecords"
        />
      </div>
    </el-card>

    <el-dialog v-model="templateDialog.visible" title="添加模板" width="540px" :close-on-click-modal="false">
      <el-form ref="templateFormRef" :model="templateForm" :rules="templateRules" label-width="100px">
        <el-form-item label="模板名称：" prop="title">
          <el-input v-model="templateForm.title" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板内容：" prop="content">
          <el-input
            v-model="templateForm.content"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="请输入模板内容"
          />
        </el-form-item>
        <el-form-item label="模板类型：" prop="type">
          <el-radio-group v-model="templateForm.type">
            <el-radio :label="1">验证码</el-radio>
            <el-radio :label="2">通知</el-radio>
            <el-radio :label="3">推广</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitTemplate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import {
  smsPassInfo,
  smsRecords,
  smsTemplateApply,
  smsTemplates,
  systemConfigInfo,
  systemConfigSaveForm
} from '../api';

const props = defineProps({
  path: {
    type: String,
    default: ''
  }
});

const loading = ref(false);
const saving = ref(false);
const payType = ref('sms');
const passInfo = ref({});
const templateRows = ref([]);
const templateTotal = ref(0);
const recordRows = ref([]);
const recordTotal = ref(0);
const templateFormRef = ref();
const smsCodeExpire = ref(3);

const templateQuery = reactive({ page: 1, limit: 20 });
const recordQuery = reactive({ page: 1, limit: 20, phone: '', template: '' });
const smsConfig = reactive({});
const templateDialog = reactive({ visible: false });
const templateForm = reactive(defaultTemplateForm());

const view = computed(() => {
  if (props.path.includes('/template')) return 'template';
  if (props.path.includes('/message')) return 'message';
  if (props.path.includes('/pay')) return 'pay';
  if (props.path.includes('/record')) return 'record';
  return 'config';
});

const switchFields = [
  { key: 'lowerOrderSwitch', label: '支付成功提醒（用户）：' },
  { key: 'deliverGoodsSwitch', label: '发货提醒（用户）：' },
  { key: 'priceRevisionSwitch', label: '改价短信提醒（用户）：' },
  { key: 'adminLowerOrderSwitch', label: '用户下单管理员提醒：' },
  { key: 'adminPaySuccessSwitch', label: '用户支付成功管理员提醒：' },
  { key: 'adminConfirmTakeOverSwitch', label: '用户退款管理员提醒：' },
  { key: 'adminRefundSwitch', label: '用户确认收货管理员短信提醒：' }
];

const templateRules = {
  title: [{ required: true, message: '模板标题不能为空', trigger: 'blur' }],
  content: [{ required: true, message: '模板内容不能为空', trigger: 'blur' }],
  type: [{ required: true, message: '模板类型不能为空', trigger: 'change' }]
};

onMounted(loadByView);
watch(() => props.path, loadByView);

async function loadByView() {
  if (view.value === 'template') {
    await loadTemplates();
  } else if (view.value === 'message') {
    await loadConfig();
  } else if (view.value === 'record') {
    await loadRecords();
  } else {
    await loadPassInfo();
  }
}

async function loadPassInfo() {
  passInfo.value = await smsPassInfo();
}

async function loadTemplates() {
  loading.value = true;
  try {
    const data = await smsTemplates(templateQuery);
    templateRows.value = data?.data || data?.list || [];
    templateTotal.value = Number(data?.count ?? data?.total ?? 0);
  } finally {
    loading.value = false;
  }
}

async function loadConfig() {
  loading.value = true;
  try {
    const data = await systemConfigInfo({ formId: 111 });
    for (const item of switchFields) {
      smsConfig[item.key] = String(data?.[item.key] ?? '0');
    }
    smsCodeExpire.value = Number(data?.sms_code_expire || 3);
  } finally {
    loading.value = false;
  }
}

async function saveConfig() {
  saving.value = true;
  try {
    const fields = [
      ...switchFields.map((item) => ({ name: item.key, title: item.key, value: String(smsConfig[item.key] ?? '0') })),
      { name: 'sms_code_expire', title: 'sms_code_expire', value: String(smsCodeExpire.value || 1) }
    ];
    await systemConfigSaveForm({ id: 111, sort: 0, status: true, fields });
    ElMessage.success('操作成功');
    await loadConfig();
  } finally {
    saving.value = false;
  }
}

async function loadRecords() {
  loading.value = true;
  try {
    const data = await smsRecords({
      page: recordQuery.page,
      limit: recordQuery.limit,
      phone: recordQuery.phone || undefined,
      template: recordQuery.template || undefined
    });
    recordRows.value = data?.list || [];
    recordTotal.value = Number(data?.total || 0);
  } finally {
    loading.value = false;
  }
}

function searchRecords() {
  recordQuery.page = 1;
  loadRecords();
}

function resetRecords() {
  recordQuery.page = 1;
  recordQuery.phone = '';
  recordQuery.template = '';
  loadRecords();
}

function openTemplateDialog() {
  Object.assign(templateForm, defaultTemplateForm());
  templateDialog.visible = true;
}

async function submitTemplate() {
  const valid = await templateFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    await smsTemplateApply({ ...templateForm });
    ElMessage.success('新增成功');
    templateDialog.visible = false;
    await loadTemplates();
  } finally {
    saving.value = false;
  }
}

function go(path) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path } }));
}

function tempTypeText(value) {
  return { 1: '验证码', 2: '通知', 3: '推广' }[value] || '-';
}

function resultText(value) {
  return {
    100: '成功',
    130: '失败',
    131: '空号',
    132: '停机',
    133: '关机',
    134: '无状态'
  }[value] || (value == null ? '-' : String(value));
}

function dateText(value) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
}

function defaultTemplateForm() {
  return {
    title: '',
    content: '',
    type: 2
  };
}
</script>

<style scoped>
.sms-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.sms-card-title {
  display: flex;
  align-items: center;
  white-space: nowrap;
}

.sms-alert {
  flex: 1;
}

.sms-config-form {
  max-width: 760px;
}

.mb20 {
  margin-bottom: 20px;
}

.mt20 {
  margin-top: 20px;
}

.mr12 {
  margin-right: 12px;
}
</style>
