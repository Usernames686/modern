<template>
  <div class="divBox seckill-product-list">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form :model="query" size="small" inline @submit.prevent>
          <el-form-item label="是否显示：">
            <el-select v-model="query.status" placeholder="请选择" clearable class="selWidth" @change="search">
              <el-option label="关闭" :value="0" />
              <el-option label="开启" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item label="配置名称：">
            <el-select v-model="query.timeId" placeholder="请选择" clearable class="selWidth" @change="search">
              <el-option
                v-for="item in seckillTime"
                :key="item.id"
                :label="`${item.name} - ${timeText(item.time)}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="商品搜索：">
            <el-input
              v-model="query.keywords"
              placeholder="请输入商品ID/名称"
              clearable
              class="selWidth"
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

    <el-card class="box-card mt14" :body-style="{ padding: '20px' }" shadow="never">
      <div class="table-actions">
        <el-button type="primary" :icon="Plus" @click="handleCreate">添加秒杀商品</el-button>
        <el-button :icon="Download" :loading="exporting" @click="handleExport">导出</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" class="mt20" size="small" highlight-current-row>
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column label="配置" min-width="180">
          <template #default="{ row }">
            <div>{{ row.storeSeckillManagerResponse?.name || '-' }}</div>
            <div class="muted">{{ row.startTime || '-' }} - {{ row.stopTime || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="秒杀时段" min-width="130">
          <template #default="{ row }">{{ timeText(row.storeSeckillManagerResponse?.time) }}</template>
        </el-table-column>
        <el-table-column label="商品图片" min-width="90">
          <template #default="{ row }">
            <el-image
              v-if="row.image"
              class="product-image"
              :src="assetUrl(row.image)"
              :preview-src-list="[assetUrl(row.image)]"
              preview-teleported
              fit="cover"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="商品标题" prop="title" min-width="280" show-overflow-tooltip />
        <el-table-column label="原价" prop="otPrice" min-width="90" />
        <el-table-column label="秒杀价" prop="price" min-width="90" />
        <el-table-column label="限量" prop="quotaShow" min-width="80" />
        <el-table-column label="限量剩余" prop="quota" min-width="90" />
        <el-table-column label="秒杀状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.killStatus)" effect="light">{{ row.statusName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" min-width="160" />
        <el-table-column label="状态" fixed="right" min-width="110">
          <template #default="{ row }">
            <el-switch
              :model-value="Number(row.status) === 1"
              active-text="开启"
              inactive-text="关闭"
              @change="changeStatus(row, $event)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
            <el-divider direction="vertical" />
            <el-button link type="primary" @click="edit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button
              link
              type="danger"
              :icon="Delete"
              :disabled="row.killStatus === 2"
              @click="remove(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="block">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.limit"
          :page-sizes="[10, 20, 30, 40]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" title="秒杀商品详情" size="620px" destroy-on-close>
      <el-descriptions v-if="detail" :column="1" border class="detail-descriptions">
        <el-descriptions-item label="商品ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="关联商品">{{ detail.productId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="商品标题">{{ detail.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="配置">
          {{ detail.storeSeckillManagerResponse?.name || '-' }}
          <span class="muted">{{ timeText(detail.storeSeckillManagerResponse?.time) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="活动时间">{{ detail.startTime || '-' }} - {{ detail.stopTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="价格">
          原价 {{ detail.otPrice ?? 0 }} / 秒杀价 {{ detail.price ?? 0 }} / 成本 {{ detail.cost ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="库存">
          库存 {{ detail.stock ?? 0 }} / 销量 {{ detail.sales ?? 0 }} / 限量 {{ detail.quotaShow ?? 0 }} / 剩余 {{ detail.quota ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="物流">
          运费 {{ detail.postage ?? 0 }} / 模板 {{ detail.tempId || '-' }} / 重量 {{ detail.weight ?? 0 }} / 体积 {{ detail.volume ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.statusName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="商品图片">
          <el-image
            v-if="detail.image"
            class="detail-image"
            :src="assetUrl(detail.image)"
            :preview-src-list="[assetUrl(detail.image)]"
            preview-teleported
            fit="cover"
          />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="简介">{{ detail.info || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue';
import { Delete, Download, Plus, Refresh, Search, View } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  seckillExport,
  seckillManagerList,
  seckillStoreDelete,
  seckillStoreInfo,
  seckillStoreList,
  seckillStoreStatus
} from '../api';

const props = defineProps({
  path: {
    type: String,
    default: ''
  }
});

const loading = ref(false);
const exporting = ref(false);
const tableData = ref([]);
const seckillTime = ref([]);
const total = ref(0);
const detailVisible = ref(false);
const detail = ref(null);

const query = reactive({
  page: 1,
  limit: 20,
  timeId: '',
  status: '',
  keywords: ''
});

watch(
  () => props.path,
  () => {
    const timeId = pathTimeId();
    query.timeId = timeId || '';
    query.page = 1;
    loadList();
  }
);

onMounted(async () => {
  const timeId = pathTimeId();
  query.timeId = timeId || '';
  await Promise.all([loadManagers(), loadList()]);
});

async function loadManagers() {
  const data = await seckillManagerList({ page: 1, limit: 200 }).catch(() => ({ list: [] }));
  seckillTime.value = data?.list || [];
}

async function loadList() {
  loading.value = true;
  try {
    const params = compactParams(query);
    const data = await seckillStoreList(params);
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
  query.limit = 20;
  query.timeId = '';
  query.status = '';
  query.keywords = '';
  loadList();
}

async function changeStatus(row, value) {
  const next = value ? 1 : 0;
  const old = row.status;
  row.status = next;
  try {
    await seckillStoreStatus({ id: row.id, status: next });
    ElMessage.success('修改成功');
  } catch {
    row.status = old;
  }
}

async function remove(row) {
  if (row.killStatus === 2) return;
  await ElMessageBox.confirm('永久删除该商品', '提示', { type: 'warning' });
  await seckillStoreDelete({ id: row.id });
  ElMessage.success('删除成功');
  if (tableData.value.length === 1 && query.page > 1) query.page -= 1;
  loadList();
}

async function openDetail(row) {
  const data = await seckillStoreInfo({ id: row.id });
  detail.value = data;
  detailVisible.value = true;
}

function handleCreate() {
  const path = query.timeId ? `/marketing/seckill/creatSeckill/creat/${query.timeId}` : '/marketing/seckill/creatSeckill/creat';
  navigateTo(path);
}

function edit(row) {
  navigateTo(`/marketing/seckill/creatSeckill/updeta/${row.productId || 0}/${row.id}`);
}

function navigateTo(path) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path } }));
}

async function handleExport() {
  exporting.value = true;
  try {
    const data = await seckillExport(compactParams(query));
    if (data?.fileName) {
      window.open(data.fileName, '_blank');
      ElMessage.success('导出成功');
    }
  } finally {
    exporting.value = false;
  }
}

function pathTimeId() {
  const matched = props.path.match(/^\/marketing\/seckill\/list\/(\d+)/);
  return matched ? Number(matched[1]) : 0;
}

function compactParams(source) {
  const params = {};
  Object.entries(source).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) params[key] = value;
  });
  return params;
}

function timeText(value) {
  return value ? String(value).split(',').join(' - ') : '-';
}

function assetUrl(value) {
  if (!value) return '';
  if (/^(https?:)?\/\//.test(value) || value.startsWith('/')) return value;
  return `/crmebimage/${value.replace(/^\/+/, '')}`;
}

function statusTagType(value) {
  if (value === 2) return 'success';
  if (value === 1) return 'warning';
  if (value === -1) return 'info';
  return 'danger';
}
</script>

<style scoped>
.seckill-product-list .padding-add {
  padding: 20px 20px 0;
}

.selWidth {
  width: 180px;
}

.table-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.mt20 {
  margin-top: 20px;
}

.block {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.product-image {
  width: 36px;
  height: 36px;
  border-radius: 4px;
}

.detail-descriptions {
  margin-right: 8px;
}

.detail-image {
  width: 96px;
  height: 96px;
  border-radius: 4px;
}
</style>
