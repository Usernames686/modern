<template>
  <div class="divBox wechat-template">
    <el-card class="box-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="公众号模板消息" name="wechat" />
        <el-tab-pane label="小程序公共模板" name="public" />
        <el-tab-pane label="我的订阅模板" name="mine" />
      </el-tabs>

      <div class="filter-bar">
        <el-input
          v-model="keywords"
          clearable
          placeholder="请输入模板名称 / 编号 / ID"
          class="filter-input"
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-select v-if="activeTab === 'wechat'" v-model="templateType" placeholder="模板类型" clearable class="filter-select">
          <el-option label="公众号模板消息" :value="1" />
          <el-option label="小程序订阅消息" :value="0" />
        </el-select>
        <el-select v-if="activeTab === 'wechat'" v-model="status" placeholder="状态" clearable class="filter-select">
          <el-option label="开启" :value="1" />
          <el-option label="关闭" :value="0" />
        </el-select>
        <el-select v-if="activeTab === 'public'" v-model="publicType" placeholder="订阅类型" clearable class="filter-select">
          <el-option label="一次性订阅" :value="2" />
          <el-option label="长期订阅" :value="3" />
        </el-select>
        <el-select v-if="activeTab === 'mine'" v-model="mineStatus" placeholder="状态" clearable class="filter-select">
          <el-option label="开启" :value="true" />
          <el-option label="关闭" :value="false" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="reload">搜索</el-button>
        <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
      </div>

      <div class="action-row">
        <template v-if="activeTab === 'wechat'">
          <el-button type="primary" :icon="Plus" @click="openWechatDialog()">添加模板</el-button>
          <el-button :icon="RefreshRight" @click="safeSyncWechat">同步微信模版消息</el-button>
          <el-button :icon="RefreshRight" @click="safeSyncRoutine">同步小程序订阅消息</el-button>
        </template>
        <template v-else-if="activeTab === 'mine'">
          <el-button type="primary" :icon="Plus" @click="openMineDialog()">添加我的模板</el-button>
          <el-button :icon="RefreshRight" @click="safeSyncMine">一键同步我的模板到小程序</el-button>
        </template>
        <el-alert
          v-if="syncTip"
          :title="syncTip"
          type="warning"
          show-icon
          :closable="false"
          class="sync-tip"
        />
      </div>

      <el-table v-loading="loading" :data="tableData" size="small" border>
        <template v-if="activeTab === 'wechat'">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="类型" width="130">
            <template #default="{ row }">{{ row.type ? '公众号模板' : '小程序订阅' }}</template>
          </el-table-column>
          <el-table-column prop="name" label="模板名" min-width="160" />
          <el-table-column prop="tempKey" label="模板编号" min-width="160" show-overflow-tooltip />
          <el-table-column prop="tempId" label="模板ID" min-width="220" show-overflow-tooltip />
          <el-table-column prop="content" label="模板内容" min-width="260" show-overflow-tooltip />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-switch
                :model-value="Number(row.status || 0)"
                :active-value="1"
                :inactive-value="0"
                @change="changeWechatStatus(row, $event)"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openWechatDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="deleteWechat(row)">删除</el-button>
            </template>
          </el-table-column>
        </template>

        <template v-else-if="activeTab === 'public'">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="tid" label="TID" width="120" />
          <el-table-column prop="title" label="模板标题" min-width="240" />
          <el-table-column label="订阅类型" width="140">
            <template #default="{ row }">{{ publicTypeText(row.type) }}</template>
          </el-table-column>
          <el-table-column prop="categoryId" label="类目ID" width="120" />
          <el-table-column prop="updateTime" label="更新时间" min-width="170" />
        </template>

        <template v-else>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="tid" label="TID" width="120" />
          <el-table-column prop="title" label="模板标题" min-width="180" />
          <el-table-column prop="tempId" label="模板ID" min-width="180" show-overflow-tooltip />
          <el-table-column prop="kid" label="关键词ID" min-width="140" show-overflow-tooltip />
          <el-table-column prop="sceneDesc" label="场景描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="type" label="应用场景" min-width="140" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-switch :model-value="Boolean(row.status)" @change="changeMineStatus(row, $event)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openMineDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="deleteMine(row)">删除</el-button>
            </template>
          </el-table-column>
        </template>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="limit"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="reload"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <el-dialog v-model="wechatDialogVisible" :title="wechatForm.id ? '编辑模板' : '添加模板'" width="720px" :close-on-click-modal="false">
      <el-form :model="wechatForm" label-width="96px">
        <el-form-item label="模板类型">
          <el-radio-group v-model="wechatForm.type">
            <el-radio :label="1">公众号模板消息</el-radio>
            <el-radio :label="0">小程序订阅消息</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="模板名" required>
          <el-input v-model="wechatForm.name" />
        </el-form-item>
        <el-form-item label="模板编号" required>
          <el-input v-model="wechatForm.tempKey" />
        </el-form-item>
        <el-form-item label="模板ID">
          <el-input v-model="wechatForm.tempId" />
        </el-form-item>
        <el-form-item label="模板内容" required>
          <el-input v-model="wechatForm.content" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="wechatForm.status">
            <el-radio :label="1">开启</el-radio>
            <el-radio :label="0">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="wechatDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitWechat">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="mineDialogVisible" :title="mineForm.id ? '编辑我的模板' : '添加我的模板'" width="720px" :close-on-click-modal="false">
      <el-form :model="mineForm" label-width="96px">
        <el-form-item label="TID" required>
          <el-input-number v-model="mineForm.tid" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="模板标题" required>
          <el-input v-model="mineForm.title" />
        </el-form-item>
        <el-form-item label="关键词ID">
          <el-input v-model="mineForm.kid" placeholder="多个用英文逗号分隔" />
        </el-form-item>
        <el-form-item label="模板ID">
          <el-input v-model="mineForm.tempId" />
        </el-form-item>
        <el-form-item label="应用场景">
          <el-input v-model="mineForm.type" />
        </el-form-item>
        <el-form-item label="场景描述">
          <el-input v-model="mineForm.sceneDesc" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="扩展字段">
          <el-input v-model="mineForm.extra" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="mineForm.status">
            <el-radio :label="true">开启</el-radio>
            <el-radio :label="false">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mineDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitMine">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Refresh, RefreshRight, Search } from '@element-plus/icons-vue';
import {
  myTempAsync,
  myTempDelete,
  myTempList,
  myTempSave,
  myTempStatus,
  myTempUpdate,
  publicTempList,
  routineTemplateSync,
  wechatTemplateDelete,
  wechatTemplateList,
  wechatTemplateSave,
  wechatTemplateStatus,
  wechatTemplateSync,
  wechatTemplateUpdate
} from '../api';

const props = defineProps({
  path: {
    type: String,
    default: ''
  }
});

const activeTab = ref('wechat');
const loading = ref(false);
const saving = ref(false);
const tableData = ref([]);
const total = ref(0);
const page = ref(1);
const limit = ref(20);
const keywords = ref('');
const templateType = ref('');
const status = ref('');
const publicType = ref('');
const mineStatus = ref('');
const syncTip = ref('');
const wechatDialogVisible = ref(false);
const mineDialogVisible = ref(false);

const wechatForm = reactive(defaultWechatForm());
const mineForm = reactive(defaultMineForm());

onMounted(() => {
  syncTabFromPath(props.path);
  loadList();
});

watch(() => props.path, (path) => {
  const next = tabFromPath(path);
  if (next !== activeTab.value) {
    activeTab.value = next;
    handleTabChange();
  }
});

function syncTabFromPath(path) {
  activeTab.value = tabFromPath(path);
}

function tabFromPath(path) {
  if (path?.includes('/publicRoutine/publicRoutineTemplate')) return 'public';
  if (path?.includes('/publicRoutine/routineTemplate')) return 'mine';
  if (path?.includes('/publicRoutine/template/0')) return 'mine';
  if (path?.includes('/publicAccount/wxSubscribe')) return 'mine';
  if (path?.includes('/publicAccount/template/1')) return 'wechat';
  return 'wechat';
}

function handleTabChange() {
  page.value = 1;
  syncTip.value = '';
  loadList();
}

function resetFilters() {
  keywords.value = '';
  templateType.value = '';
  status.value = '';
  publicType.value = '';
  mineStatus.value = '';
  reload();
}

function reload() {
  page.value = 1;
  loadList();
}

async function loadList() {
  loading.value = true;
  try {
    const params = { page: page.value, limit: limit.value };
    let data;
    if (activeTab.value === 'wechat') {
      data = await wechatTemplateList({
        ...params,
        keywords: keywords.value || undefined,
        type: templateType.value === '' ? undefined : templateType.value,
        status: status.value === '' ? undefined : status.value
      });
    } else if (activeTab.value === 'public') {
      data = await publicTempList({
        ...params,
        title: keywords.value || undefined,
        type: publicType.value === '' ? undefined : publicType.value
      });
    } else {
      data = await myTempList({
        ...params,
        title: keywords.value || undefined,
        status: mineStatus.value === '' ? undefined : mineStatus.value
      });
    }
    tableData.value = data?.list || data?.data || [];
    total.value = Number(data?.total ?? data?.count ?? 0);
  } finally {
    loading.value = false;
  }
}

function openWechatDialog(row) {
  Object.assign(wechatForm, defaultWechatForm(), row ? {
    ...row,
    type: row.type ? 1 : 0,
    status: Number(row.status || 0)
  } : {});
  wechatDialogVisible.value = true;
}

async function submitWechat() {
  if (!wechatForm.name || !wechatForm.tempKey || !wechatForm.content) {
    ElMessage.warning('请填写模板名、模板编号和模板内容');
    return;
  }
  saving.value = true;
  try {
    const payload = {
      type: Number(wechatForm.type) === 1,
      name: wechatForm.name,
      tempKey: wechatForm.tempKey,
      tempId: wechatForm.tempId,
      content: wechatForm.content,
      status: Number(wechatForm.status)
    };
    if (wechatForm.id) {
      await wechatTemplateUpdate(wechatForm.id, payload);
    } else {
      await wechatTemplateSave(payload);
    }
    ElMessage.success('保存成功');
    wechatDialogVisible.value = false;
    await loadList();
  } finally {
    saving.value = false;
  }
}

async function changeWechatStatus(row, value) {
  await wechatTemplateStatus(row.id, { status: value });
  ElMessage.success('修改成功');
  await loadList();
}

async function deleteWechat(row) {
  await ElMessageBox.confirm(`确定删除模板「${row.name}」吗？`, '提示', { type: 'warning' });
  await wechatTemplateDelete(row.id);
  ElMessage.success('删除成功');
  await loadList();
}

function openMineDialog(row) {
  Object.assign(mineForm, defaultMineForm(), row ? { ...row, status: Boolean(row.status) } : {});
  mineDialogVisible.value = true;
}

async function submitMine() {
  if (!mineForm.tid || !mineForm.title) {
    ElMessage.warning('请填写 TID 和模板标题');
    return;
  }
  saving.value = true;
  try {
    const payload = { ...mineForm };
    delete payload.id;
    if (mineForm.id) {
      await myTempUpdate(mineForm.id, payload);
    } else {
      await myTempSave(payload);
    }
    ElMessage.success('保存成功');
    mineDialogVisible.value = false;
    await loadList();
  } finally {
    saving.value = false;
  }
}

async function changeMineStatus(row, value) {
  await myTempStatus({ id: row.id, status: value });
  ElMessage.success('修改成功');
  await loadList();
}

async function deleteMine(row) {
  await ElMessageBox.confirm(`确定删除模板「${row.title}」吗？`, '提示', { type: 'warning' });
  await myTempDelete({ id: row.id });
  ElMessage.success('删除成功');
  await loadList();
}

async function safeSyncWechat() {
  const data = await wechatTemplateSync();
  syncTip.value = data?.message || '公众号服务未配置，模板同步需配置后启用';
}

async function safeSyncRoutine() {
  const data = await routineTemplateSync();
  syncTip.value = data?.message || '小程序服务未配置，模板同步需配置后启用';
}

async function safeSyncMine() {
  const data = await myTempAsync();
  syncTip.value = data?.message || '小程序服务未配置，模板同步需配置后启用';
}

function publicTypeText(value) {
  if (Number(value) === 2) return '一次性订阅';
  if (Number(value) === 3) return '长期订阅';
  return value || '-';
}

function defaultWechatForm() {
  return {
    id: 0,
    type: 1,
    name: '',
    tempKey: '',
    tempId: '',
    content: '',
    status: 1
  };
}

function defaultMineForm() {
  return {
    id: 0,
    tid: undefined,
    title: '',
    kid: '',
    sceneDesc: '',
    tempId: '',
    extra: '',
    status: true,
    type: ''
  };
}
</script>

<style scoped>
.wechat-template :deep(.el-card__body) {
  padding: 20px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.filter-input {
  width: 260px;
}

.filter-select {
  width: 160px;
}

.action-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.sync-tip {
  max-width: 560px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
