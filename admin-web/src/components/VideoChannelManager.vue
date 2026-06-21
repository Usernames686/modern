<template>
  <div class="divBox video-channel">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="商品列表" name="list" />
          <el-tab-pane label="草稿列表" name="draftList" />
        </el-tabs>

        <el-form :model="query" size="small" inline @submit.prevent>
          <el-form-item label="商品搜索：">
            <el-input
              v-model="query.keywords"
              class="selWidth"
              clearable
              placeholder="请输入商品名称，关键字，商品ID"
              @keyup.enter="search"
            />
          </el-form-item>
          <el-form-item v-if="activeTab === 'draftList'" label="草稿类型：">
            <el-select v-model="query.status" class="selWidth" @change="search">
              <el-option label="草稿候选" value="candidate" />
              <el-option label="未上架商品" value="unshelved" />
              <el-option label="带主图视频" value="withVideo" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
            <el-button :icon="Refresh" @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14" shadow="never">
      <el-alert
        class="safe-alert"
        type="warning"
        :closable="false"
        show-icon
        title="当前仅支持本地商品与订单数据查看；视频号同步、发布、审核和物流接口需完成微信服务配置后启用。"
      />

      <el-table
        v-loading="loading"
        :data="tableData"
        class="table"
        style="width: 100%"
        size="small"
        border
        highlight-current-row
      >
        <el-table-column prop="id" label="ID" min-width="70" />
        <el-table-column label="商品图" min-width="90">
          <template #default="{ row }">
            <el-image
              v-if="row.image"
              class="product-image"
              :src="assetUrl(row.image)"
              :preview-src-list="[assetUrl(row.image)]"
              fit="cover"
              preview-teleported
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="storeName" label="商品名称" min-width="280" show-overflow-tooltip />
        <el-table-column prop="keyword" label="关键字" min-width="140" show-overflow-tooltip />
        <el-table-column prop="price" label="售价" min-width="90" />
        <el-table-column prop="stock" label="库存" min-width="80" />
        <el-table-column prop="sales" label="销量" min-width="80" />
        <el-table-column v-if="activeTab === 'list'" prop="orderCount" label="视频号订单" min-width="100" />
        <el-table-column v-if="activeTab === 'list'" prop="payNum" label="购买件数" min-width="90" />
        <el-table-column v-if="activeTab === 'list'" prop="payAmount" label="支付金额" min-width="100" />
        <el-table-column label="主图视频" min-width="110">
          <template #default="{ row }">
            <el-tag v-if="row.videoLink" type="success">已配置</el-tag>
            <el-tag v-else type="info">未配置</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.isShow ? 'success' : 'info'">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="activeTab === 'list' ? '最近下单' : '添加时间'" min-width="170">
          <template #default="{ row }">{{ activeTab === 'list' ? row.lastOrderTime || '-' : row.addTimeText || '-' }}</template>
        </el-table-column>
        <el-table-column prop="sourceText" label="来源" min-width="120" />
        <el-table-column label="操作" fixed="right" width="190">
          <template #default="{ row }">
            <el-button link type="primary" @click="navigateTo(`/store/list/creatProduct/${row.id}/1`)">详情</el-button>
            <el-divider direction="vertical" />
            <el-tooltip :content="row.externalActionText" placement="top">
              <el-button link type="info" disabled>同步</el-button>
            </el-tooltip>
            <el-divider direction="vertical" />
            <el-tooltip :content="row.externalActionText" placement="top">
              <el-button link type="info" disabled>发布</el-button>
            </el-tooltip>
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
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Refresh, Search } from '@element-plus/icons-vue';
import { videoChannelDraftList, videoChannelProductList } from '../api';

const props = defineProps({
  path: {
    type: String,
    default: ''
  }
});

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const activeTab = computed({
  get: () => (props.path.includes('/draftList') ? 'draftList' : 'list'),
  set: () => {}
});

const query = reactive({
  page: 1,
  limit: 20,
  keywords: '',
  status: 'candidate'
});

onMounted(loadList);

watch(() => props.path, () => {
  query.page = 1;
  loadList();
});

async function loadList() {
  loading.value = true;
  try {
    const params = compactParams(query);
    const data = activeTab.value === 'draftList'
      ? await videoChannelDraftList(params)
      : await videoChannelProductList(params);
    tableData.value = data?.list || [];
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
  query.page = 1;
  query.keywords = '';
  query.status = 'candidate';
  loadList();
}

function handleTabChange(name) {
  const path = name === 'draftList' ? '/marketing/videoChannel/draftList' : '/marketing/videoChannel/list';
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path } }));
}

function navigateTo(path) {
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path } }));
}

function compactParams(source) {
  const params = {};
  Object.entries(source).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) params[key] = value;
  });
  return params;
}

function assetUrl(value) {
  if (!value) return '';
  if (/^(https?:)?\/\//.test(value) || value.startsWith('/')) return value;
  return `/${value.replace(/^\/+/, '')}`;
}
</script>

<style scoped>
.video-channel .padding-add {
  padding: 20px 20px 0;
}

.video-channel :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.selWidth {
  width: 240px;
}

.safe-alert {
  margin-bottom: 16px;
}

.product-image {
  width: 48px;
  height: 48px;
  border-radius: 4px;
}

.block {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
