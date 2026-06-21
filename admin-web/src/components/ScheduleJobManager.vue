<template>
  <div class="divBox schedule-page">
    <el-card class="box-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="定时任务" name="jobs">
          <div class="clearfix schedule-toolbar">
            <el-button type="primary" size="small" @click="openEdit(null)">添加定时任务</el-button>
          </div>

          <el-table v-loading="jobLoading" :data="jobs" size="small" class="table">
            <el-table-column prop="jobId" label="任务id" min-width="70" />
            <el-table-column prop="beanName" label="定时任务类名" min-width="160" show-overflow-tooltip />
            <el-table-column prop="methodName" label="方法名" min-width="150" show-overflow-tooltip />
            <el-table-column prop="cronExpression" min-width="140" label="cron表达式" show-overflow-tooltip />
            <el-table-column prop="params" label="参数" min-width="110" show-overflow-tooltip />
            <el-table-column label="状态" min-width="120">
              <template #default="{ row }">
                <el-switch
                  v-model="row.status"
                  :active-value="0"
                  :inactive-value="1"
                  active-text="正常"
                  inactive-text="暂停"
                  :loading="switchingId === row.jobId"
                  @change="changeStatus(row)"
                />
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="170" show-overflow-tooltip />
            <el-table-column prop="createTime" label="创建时间" min-width="170" />
            <el-table-column fixed="right" width="170" label="操作">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="row.status === 0" @click="openEdit(row)">编辑</el-button>
                <el-divider direction="vertical" />
                <el-button link type="primary" @click="triggerJob(row)">触发</el-button>
                <el-divider direction="vertical" />
                <el-button link type="danger" :disabled="row.status === 0" @click="removeJob(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="任务日志" name="logs">
          <el-form inline :model="logQuery" class="log-search" @submit.prevent>
            <el-form-item label="任务id：">
              <el-input-number v-model="logQuery.jobId" size="small" controls-position="right" :min="1" clearable />
            </el-form-item>
            <el-form-item label="任务类名：">
              <el-input v-model="logQuery.beanName" size="small" clearable class="selWidth" />
            </el-form-item>
            <el-form-item label="方法名：">
              <el-input v-model="logQuery.methodName" size="small" clearable class="selWidth" />
            </el-form-item>
            <el-button type="primary" size="small" @click="searchLogs">搜索</el-button>
          </el-form>

          <el-table v-loading="logLoading" :data="logs" size="small" class="table">
            <el-table-column prop="jobId" label="任务id" min-width="70" />
            <el-table-column prop="logId" label="任务日志id" min-width="95" />
            <el-table-column label="定时任务类名" min-width="160" prop="beanName" show-overflow-tooltip />
            <el-table-column min-width="130" label="方法名" prop="methodName" show-overflow-tooltip />
            <el-table-column prop="params" label="参数" min-width="110" show-overflow-tooltip />
            <el-table-column prop="times" label="耗时(单位：毫秒)" min-width="130" />
            <el-table-column prop="error" label="失败信息" min-width="240" show-overflow-tooltip />
            <el-table-column prop="createTime" label="创建时间" min-width="170" />
          </el-table>

          <div class="block-pagination">
            <el-pagination
              v-model:current-page="logQuery.page"
              v-model:page-size="logQuery.limit"
              :page-sizes="[20, 40, 60, 80]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="logTotal"
              background
              @size-change="loadLogs"
              @current-change="loadLogs"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog
      v-model="editDialog.visible"
      title="定时任务"
      width="540px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item required label="定时任务类名：" prop="beanName">
          <el-input v-model.trim="form.beanName" placeholder="请输入定时任务名称" />
        </el-form-item>
        <el-form-item required label="cron表达式：" prop="cronExpression">
          <el-input v-model.trim="form.cronExpression" placeholder="请输入cron表达式" />
        </el-form-item>
        <el-form-item required label="方法名：" prop="methodName">
          <el-input v-model.trim="form.methodName" placeholder="请输入定时任务方法名" />
        </el-form-item>
        <el-form-item label="参数：" prop="params">
          <el-input v-model.trim="form.params" placeholder="请输入定时任务参数" />
        </el-form-item>
        <el-form-item label="备注：" prop="remark">
          <el-input v-model.trim="form.remark" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitJob">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  scheduleJobAdd,
  scheduleJobDelete,
  scheduleJobList,
  scheduleJobLogList,
  scheduleJobStart,
  scheduleJobSuspend,
  scheduleJobTrigger,
  scheduleJobUpdate
} from '../api';

const props = defineProps({
  initialTab: {
    type: String,
    default: 'jobs'
  }
});

const activeTab = ref(props.initialTab === 'logs' ? 'logs' : 'jobs');
const jobLoading = ref(false);
const logLoading = ref(false);
const saving = ref(false);
const switchingId = ref(null);
const formRef = ref();
const jobs = ref([]);
const logs = ref([]);
const logTotal = ref(0);

const editDialog = reactive({
  visible: false,
  mode: 'create'
});

const form = reactive(defaultJobForm());

const logQuery = reactive({
  page: 1,
  limit: 20,
  jobId: undefined,
  beanName: '',
  methodName: ''
});

const rules = {
  beanName: [{ required: true, message: 'spring bean名称不能为空', trigger: ['blur', 'change'] }],
  cronExpression: [{ required: true, message: 'cron表达式不能为空', trigger: ['blur', 'change'] }],
  methodName: [{ required: true, message: '方法名不能为空', trigger: ['blur', 'change'] }]
};

function defaultJobForm() {
  return {
    jobId: null,
    beanName: '',
    cronExpression: '',
    methodName: '',
    params: '',
    remark: ''
  };
}

async function loadJobs() {
  jobLoading.value = true;
  try {
    jobs.value = await scheduleJobList() || [];
  } finally {
    jobLoading.value = false;
  }
}

async function loadLogs() {
  logLoading.value = true;
  try {
    const data = await scheduleJobLogList({
      page: logQuery.page,
      limit: logQuery.limit,
      jobId: logQuery.jobId || undefined,
      beanName: logQuery.beanName || undefined,
      methodName: logQuery.methodName || undefined
    });
    logs.value = data?.list || [];
    logTotal.value = data?.total || 0;
  } finally {
    logLoading.value = false;
  }
}

function searchLogs() {
  logQuery.page = 1;
  loadLogs();
}

function handleTabChange(name) {
  if (name === 'logs') loadLogs();
}

function openEdit(row) {
  editDialog.mode = row?.jobId ? 'edit' : 'create';
  Object.assign(form, defaultJobForm(), row ? {
    jobId: row.jobId,
    beanName: row.beanName || '',
    cronExpression: row.cronExpression || '',
    methodName: row.methodName || '',
    params: row.params || '',
    remark: row.remark || ''
  } : {});
  editDialog.visible = true;
}

async function submitJob() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    const payload = {
      jobId: form.jobId || undefined,
      beanName: form.beanName,
      cronExpression: form.cronExpression,
      methodName: form.methodName,
      params: form.params,
      remark: form.remark
    };
    if (editDialog.mode === 'create') {
      await scheduleJobAdd(payload);
      ElMessage.success('操作成功');
    } else {
      await scheduleJobUpdate(payload);
      ElMessage.success('操作成功');
    }
    editDialog.visible = false;
    await loadJobs();
  } finally {
    saving.value = false;
  }
}

async function changeStatus(row) {
  switchingId.value = row.jobId;
  const nextStatus = row.status;
  try {
    if (nextStatus === 1) {
      await scheduleJobSuspend(row.jobId);
    } else {
      await scheduleJobStart(row.jobId);
    }
    ElMessage.success('修改成功');
    await loadJobs();
  } catch (error) {
    row.status = nextStatus === 1 ? 0 : 1;
    throw error;
  } finally {
    switchingId.value = null;
  }
}

async function triggerJob(row) {
  await scheduleJobTrigger(row.jobId);
  ElMessage.success('触发成功');
  if (activeTab.value === 'logs') await loadLogs();
}

async function removeJob(row) {
  await ElMessageBox.confirm('删除当前数据?', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  });
  await scheduleJobDelete(row.jobId);
  ElMessage.success('删除成功');
  await loadJobs();
}

onMounted(async () => {
  await loadJobs();
  if (activeTab.value === 'logs') await loadLogs();
});
</script>

<style scoped>
.schedule-toolbar {
  margin-bottom: 14px;
}

.log-search {
  margin-bottom: 14px;
}

.schedule-page :deep(.el-switch__label) {
  color: #606266;
}
</style>
