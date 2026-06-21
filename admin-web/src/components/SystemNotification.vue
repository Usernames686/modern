<template>
  <div class="divBox">
    <el-card class="box-card">
      <el-tabs v-model="currentTab" @tab-change="changeTab">
        <el-tab-pane v-for="item in headerList" :key="item.value" :label="item.label" :name="String(item.value)" />
      </el-tabs>

      <div class="mb20 mt-1">
        <el-button type="primary" :icon="Document" :loading="syncRoutineLoading" @click="syncRoutine">同步小程序订阅消息</el-button>
        <el-button type="primary" :icon="Document" :loading="syncWechatLoading" @click="syncWechat">同步微信模版消息</el-button>
      </div>

      <div class="description">
        <p>小程序经营类目：生活服务 &gt; 百货/超市/便利店</p>
        <p>公众号经营类目：IT科技/互联网|电子商务，IT科技/IT软件与服务</p>
      </div>

      <el-table v-loading="loadingList" :data="tableData" class="mt25" size="small">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="type" label="通知类型" min-width="160" />
        <el-table-column prop="description" label="通知场景说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="mark" label="标识" min-width="160" />
        <el-table-column v-if="currentTab === '1'" label="公众号模板" min-width="130">
          <template #default="{ row }">
            <el-switch
              v-if="row.isWechat !== 0"
              :model-value="row.isWechat"
              :active-value="1"
              :inactive-value="2"
              active-text="启用"
              inactive-text="禁用"
              @change="changeWechat(row)"
            />
            <span v-else>禁用</span>
          </template>
        </el-table-column>
        <el-table-column v-if="currentTab === '1'" label="小程序订阅" min-width="130">
          <template #default="{ row }">
            <el-switch
              v-if="row.isRoutine !== 0"
              :model-value="row.isRoutine"
              :active-value="1"
              :inactive-value="2"
              active-text="启用"
              inactive-text="禁用"
              @change="changeRoutine(row)"
            />
            <span v-else>禁用</span>
          </template>
        </el-table-column>
        <el-table-column label="发送短信" min-width="120">
          <template #default="{ row }">
            <el-switch
              v-if="row.isSms !== 0"
              :model-value="row.isSms"
              :active-value="1"
              :inactive-value="2"
              active-text="启用"
              inactive-text="禁用"
              @change="changeSms(row)"
            />
            <span v-else>禁用</span>
          </template>
        </el-table-column>
        <el-table-column label="设置" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="setting(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="通知详情" width="540px" :close-on-click-modal="false">
      <el-tabs v-model="infoTab" @tab-change="getNotificationDetail">
        <el-tab-pane v-for="item in activeInfoList" :key="item.value" :label="item.label" :name="item.value" />
      </el-tabs>
      <el-form v-loading="loadingDetail" :model="form" label-width="80px">
        <el-form-item label="ID">
          <el-input v-model="form.id" disabled />
        </el-form-item>
        <el-form-item v-if="form.name" label="模板名">
          <el-input v-model="form.name" disabled />
        </el-form-item>
        <el-form-item v-if="showTempId" label="模板ID">
          <el-input v-model="form.tempId" />
        </el-form-item>
        <el-form-item v-if="form.tempKey" label="模板编号">
          <el-input v-model="form.tempKey" disabled />
        </el-form-item>
        <el-form-item v-if="form.title" label="模板说明">
          <el-input v-model="form.title" disabled />
        </el-form-item>
        <el-form-item v-if="form.content" label="模板内容">
          <el-input v-model="form.content" disabled />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="1">开启</el-radio>
            <el-radio label="2">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { Document } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import {
  notificationDetail,
  notificationList,
  notificationRoutineSwitch,
  notificationSmsSwitch,
  notificationUpdate,
  notificationWechatSwitch,
  routineTemplateSync,
  wechatTemplateSync
} from '../api';

const headerList = [
  { label: '通知会员', value: '1' },
  { label: '通知平台', value: '2' }
];
const infoList1 = [{ label: '短信', value: 'sms' }];

const currentTab = ref('1');
const tableData = ref([]);
const loadingList = ref(false);
const loadingDetail = ref(false);
const saving = ref(false);
const syncRoutineLoading = ref(false);
const syncWechatLoading = ref(false);
const dialogVisible = ref(false);
const activeInfoList = ref([]);
const infoTab = ref('');
const currentId = ref(0);
const detailType = ref('');

const form = reactive(defaultForm());

const showTempId = computed(() => infoTab.value !== 'sms' && form.tempId !== undefined);

onMounted(() => loadList());

function changeTab() {
  loadList();
}

async function loadList() {
  loadingList.value = true;
  try {
    tableData.value = await notificationList({ sendType: Number(currentTab.value) });
  } finally {
    loadingList.value = false;
  }
}

async function changeWechat(row) {
  await notificationWechatSwitch(row.id);
  ElMessage.success('修改成功');
  await loadList();
}

async function changeRoutine(row) {
  await notificationRoutineSwitch(row.id);
  ElMessage.success('修改成功');
  await loadList();
}

async function changeSms(row) {
  await notificationSmsSwitch(row.id);
  ElMessage.success('修改成功');
  await loadList();
}

function setting(row) {
  currentId.value = row.id;
  activeInfoList.value = [];
  if (currentTab.value === '1') {
    if (row.isWechat !== 0) activeInfoList.value.push({ label: '公众号模板消息', value: 'wechat' });
    if (row.isRoutine !== 0) activeInfoList.value.push({ label: '小程序订阅消息', value: 'routine' });
    if (row.isSms !== 0) activeInfoList.value.push({ label: '短信', value: 'sms' });
  } else {
    activeInfoList.value = [...infoList1];
  }
  if (!activeInfoList.value.length) {
    ElMessage.warning('当前通知未配置可编辑模板');
    return;
  }
  dialogVisible.value = true;
  infoTab.value = activeInfoList.value[0].value;
  getNotificationDetail(infoTab.value);
}

async function getNotificationDetail(type) {
  detailType.value = typeof type === 'string' ? type : infoTab.value;
  loadingDetail.value = true;
  try {
    const data = await notificationDetail({ id: currentId.value, detailType: detailType.value });
    Object.assign(form, {
      id: data.id || '',
      content: data.content || '',
      name: data.name || '',
      status: String(data.status || 2),
      tempId: data.tempId || '',
      tempKey: data.tempKey || '',
      title: data.title || ''
    });
  } finally {
    loadingDetail.value = false;
  }
}

async function submit() {
  saving.value = true;
  try {
    await notificationUpdate({
      id: currentId.value,
      detailType: detailType.value,
      status: Number(form.status),
      tempId: form.tempId
    });
    ElMessage.success('修改成功');
    dialogVisible.value = false;
    await loadList();
  } finally {
    saving.value = false;
  }
}

async function syncWechat() {
  syncWechatLoading.value = true;
  try {
    const data = await wechatTemplateSync();
    ElMessage({
      message: data?.message || '同步完成',
      type: data?.localMode ? 'warning' : 'success'
    });
  } finally {
    syncWechatLoading.value = false;
  }
}

async function syncRoutine() {
  syncRoutineLoading.value = true;
  try {
    const data = await routineTemplateSync();
    ElMessage({
      message: data?.message || '同步完成',
      type: data?.localMode ? 'warning' : 'success'
    });
  } finally {
    syncRoutineLoading.value = false;
  }
}

function defaultForm() {
  return {
    content: '',
    name: '',
    id: '',
    status: '2',
    tempId: '',
    tempKey: '',
    title: ''
  };
}
</script>

<style scoped>
.mt-1 {
  margin-top: 6px;
}

.description {
  padding: 16px;
  border-radius: 4px;
  margin-bottom: 20px;
  color: #515a6e;
  line-height: 1.5;
  font-size: 14px;
  border: 1px solid #abdcff;
  background-color: #f0faff;
}

.description p {
  margin: 0 0 6px;
}

.description p:last-child {
  margin-bottom: 0;
}
</style>
