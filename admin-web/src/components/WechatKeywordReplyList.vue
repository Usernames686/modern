<template>
  <div class="divBox">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item label="回复类型：">
            <el-select v-model="query.type" placeholder="请选择类型" class="selWidth" size="small" clearable @change="search">
              <el-option label="文本消息" value="text" />
              <el-option label="图片消息" value="image" />
              <el-option label="图文消息" value="news" />
              <el-option label="音频消息" value="voice" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键字：">
            <el-input
              v-model="query.keywords"
              placeholder="请输入关键字"
              class="selWidth"
              size="small"
              clearable
              @keyup.enter="search"
            />
          </el-form-item>
          <el-button type="primary" size="small" @click="search">搜索</el-button>
          <el-button size="small" @click="reset">重置</el-button>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <template #header>
        <div class="clearfix">
          <el-button type="primary" size="small" @click="goEdit()">添加关键字</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="rows" size="small" class="table" highlight-current-row>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="keywords" label="关键字" min-width="170" show-overflow-tooltip />
        <el-table-column label="回复类型" min-width="110">
          <template #default="{ row }">{{ typeText(row.type) }}</template>
        </el-table-column>
        <el-table-column label="回复内容" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ contentText(row) }}</template>
        </el-table-column>
        <el-table-column label="是否显示" min-width="150">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-text="显示"
              inactive-text="隐藏"
              :active-value="true"
              :inactive-value="false"
              @change="changeStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="160">
          <template #default="{ row }">{{ dateText(row.updateTime || row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="isSystemReply(row)" @click="goEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button link type="danger" :disabled="isSystemReply(row)" @click="remove(row)">删除</el-button>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { wechatReplyDelete, wechatReplyList, wechatReplyStatus } from '../api';

const loading = ref(false);
const rows = ref([]);
const total = ref(0);

const query = reactive({
  page: 1,
  limit: 20,
  keywords: '',
  type: ''
});

onMounted(loadList);

async function loadList() {
  loading.value = true;
  try {
    const data = await wechatReplyList({
      page: query.page,
      limit: query.limit,
      keywords: query.keywords || undefined,
      type: query.type || undefined
    });
    rows.value = data?.list || [];
    total.value = data?.total || 0;
  } finally {
    loading.value = false;
  }
}

function search() {
  query.page = 1;
  loadList();
}

function reset() {
  query.type = '';
  query.keywords = '';
  query.page = 1;
  loadList();
}

async function changeStatus(row) {
  const next = row.status;
  try {
    await wechatReplyStatus({ id: row.id, status: next });
    ElMessage.success('修改成功');
  } catch (error) {
    row.status = !next;
  }
}

async function remove(row) {
  await ElMessageBox.confirm('删除当前关键字回复?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  });
  await wechatReplyDelete({ id: row.id });
  ElMessage.success('删除成功');
  await loadList();
}

function goEdit(row) {
  const path = row?.id
    ? `/appSetting/publicAccount/wxReply/keyword/save/${row.id}`
    : '/appSetting/publicAccount/wxReply/keyword/save';
  window.history.pushState({}, '', path);
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path } }));
}

function isSystemReply(row) {
  return row.keywords === 'subscribe' || row.keywords === 'default';
}

function typeText(type) {
  return {
    text: '文本消息',
    image: '图片消息',
    news: '图文消息',
    voice: '音频消息'
  }[type] || type || '-';
}

function contentText(row) {
  const data = parseData(row.data);
  if (row.type === 'text') return data.content || '-';
  if (row.type === 'image') return data.srcUrl || data.mediaId || '图片消息';
  if (row.type === 'voice') return data.srcUrl || data.mediaId || '音频消息';
  if (row.type === 'news') return data.articleData?.title || `图文ID：${data.articleId || '-'}`;
  return row.data || '-';
}

function parseData(value) {
  try {
    return JSON.parse(value || '{}');
  } catch {
    return {};
  }
}

function dateText(value) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
}
</script>

