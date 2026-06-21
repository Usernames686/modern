<template>
  <div class="divBox bargain-manager">
    <template v-if="isRecordPage">
      <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
        <div class="padding-add">
          <el-form :model="recordQuery" size="small" inline @submit.prevent>
            <el-form-item label="时间选择：">
              <el-date-picker
                v-model="recordTime"
                value-format="YYYY-MM-DD"
                format="YYYY-MM-DD"
                type="daterange"
                range-separator="-"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                class="date-picker"
                @change="handleRecordTimeChange"
              />
            </el-form-item>
            <el-form-item label="砍价状态：">
              <el-select v-model="recordQuery.status" placeholder="请选择" clearable class="selWidth" @change="searchRecords">
                <el-option label="进行中" :value="1" />
                <el-option label="未完成" :value="2" />
                <el-option label="已成功" :value="3" />
              </el-select>
            </el-form-item>
          </el-form>
        </div>
      </el-card>

      <el-card class="box-card mt14" :body-style="{ padding: '20px' }" shadow="never">
        <el-table v-loading="recordLoading" :data="recordRows" size="small" highlight-current-row>
          <el-table-column prop="id" label="ID" min-width="60" />
          <el-table-column label="头像" min-width="80">
            <template #default="{ row }">
              <el-image
                v-if="row.avatar"
                class="avatar-image"
                :src="assetUrl(row.avatar)"
                :preview-src-list="[assetUrl(row.avatar)]"
                preview-teleported
                fit="cover"
              />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="发起用户" prop="nickname" min-width="110" show-overflow-tooltip />
          <el-table-column label="开启时间" prop="addTime" min-width="160" />
          <el-table-column label="砍价商品" prop="title" min-width="280" show-overflow-tooltip />
          <el-table-column label="最低价" prop="bargainPriceMin" min-width="90" />
          <el-table-column label="当前价" prop="nowPrice" min-width="90" />
          <el-table-column label="总砍价次数" prop="peopleNum" min-width="110" />
          <el-table-column label="剩余砍价次数" prop="num" min-width="120" />
          <el-table-column label="结束时间" prop="dataTime" min-width="160" />
          <el-table-column label="砍价状态" min-width="110">
            <template #default="{ row }">
              <el-tag :type="recordStatusTag(row.status)" effect="light">{{ recordStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :icon="View" @click="openHelp(row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="block">
          <el-pagination
            v-model:current-page="recordQuery.page"
            v-model:page-size="recordQuery.limit"
            :page-sizes="[10, 20, 30, 40]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="recordTotal"
            background
            @size-change="loadRecords"
            @current-change="loadRecords"
          />
        </div>
      </el-card>
    </template>

    <template v-else>
      <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
        <div class="padding-add">
          <el-form :model="goodsQuery" size="small" inline @submit.prevent>
            <el-form-item label="砍价状态：">
              <el-select v-model="goodsQuery.status" placeholder="请选择" clearable class="selWidth" @change="searchGoods">
                <el-option label="关闭" :value="0" />
                <el-option label="开启" :value="1" />
              </el-select>
            </el-form-item>
            <el-form-item label="商品搜索：">
              <el-input
                v-model="goodsQuery.keywords"
                placeholder="请输入商品名称、ID"
                clearable
                class="selWidth"
                @keyup.enter="searchGoods"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="searchGoods">搜索</el-button>
              <el-button :icon="Refresh" @click="resetGoods">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-card>

      <el-card class="box-card mt14" :body-style="{ padding: '20px' }" shadow="never">
        <div class="table-actions">
          <el-button type="primary" :icon="Plus" @click="handleCreate">添加砍价商品</el-button>
          <el-button :icon="Download" :loading="exporting" @click="handleExport">导出</el-button>
        </div>

        <el-table v-loading="goodsLoading" :data="goodsRows" class="mt20" size="small" highlight-current-row>
          <el-table-column prop="id" label="ID" min-width="60" />
          <el-table-column label="砍价图片" min-width="90">
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
          <el-table-column label="砍价名称" prop="title" min-width="280" show-overflow-tooltip />
          <el-table-column label="砍价价格" prop="price" min-width="90" />
          <el-table-column label="最低价" prop="minPrice" min-width="90" />
          <el-table-column label="参与人数" prop="countPeopleAll" min-width="100" />
          <el-table-column label="帮忙砍价人数" prop="countPeopleHelp" min-width="120" />
          <el-table-column label="砍价成功人数" prop="countPeopleSuccess" min-width="120" />
          <el-table-column label="限量" prop="quotaShow" min-width="80" />
          <el-table-column label="限量剩余" prop="surplusQuota" min-width="100" />
          <el-table-column label="活动时间" min-width="190">
            <template #default="{ row }">{{ row.startTime || '-' }} ~ {{ row.stopTime || '-' }}</template>
          </el-table-column>
          <el-table-column label="砍价状态" fixed="right" min-width="110">
            <template #default="{ row }">
              <el-switch
                :model-value="Boolean(row.status)"
                active-text="开启"
                inactive-text="关闭"
                @change="changeGoodsStatus(row, $event)"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
              <el-divider direction="vertical" />
              <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
              <el-divider direction="vertical" />
              <el-button link type="danger" :icon="Delete" @click="removeGoods(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="block">
          <el-pagination
            v-model:current-page="goodsQuery.page"
            v-model:page-size="goodsQuery.limit"
            :page-sizes="[10, 20, 30, 40]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="goodsTotal"
            background
            @size-change="loadGoods"
            @current-change="loadGoods"
          />
        </div>
      </el-card>
    </template>

    <el-dialog v-model="helpVisible" title="查看详情" width="650px" destroy-on-close>
      <el-table v-loading="helpLoading" :data="helpRows" size="small">
        <el-table-column prop="uid" label="用户id" min-width="70" />
        <el-table-column label="用户头像" min-width="90">
          <template #default="{ row }">
            <el-image
              v-if="row.avatar"
              class="avatar-image"
              :src="assetUrl(row.avatar)"
              :preview-src-list="[assetUrl(row.avatar)]"
              preview-teleported
              fit="cover"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="用户名称" prop="nickname" min-width="120" show-overflow-tooltip />
        <el-table-column label="砍价金额" prop="price" min-width="100" />
        <el-table-column label="砍价时间" prop="addTime" min-width="170" />
      </el-table>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="砍价商品详情" size="620px" destroy-on-close>
      <el-descriptions v-if="detail" :column="1" border class="detail-descriptions">
        <el-descriptions-item label="商品ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="关联商品">{{ detail.productId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="砍价名称">{{ detail.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="活动时间">{{ detail.startTime || '-' }} ~ {{ detail.stopTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="价格">
          砍价价格 {{ detail.price ?? 0 }} / 最低价 {{ detail.minPrice ?? 0 }} / 成本 {{ detail.cost ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="砍价设置">
          砍价人数 {{ detail.peopleNum ?? 0 }} / 帮砍次数 {{ detail.bargainNum ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="活动数据">
          参与 {{ detail.countPeopleAll ?? 0 }} / 帮砍 {{ detail.countPeopleHelp ?? 0 }} / 成功 {{ detail.countPeopleSuccess ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="库存">
          库存 {{ detail.stock ?? 0 }} / 销量 {{ detail.sales ?? 0 }} / 限量 {{ detail.quotaShow ?? 0 }} / 剩余 {{ detail.surplusQuota ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="物流">
          运费 {{ detail.postage ?? 0 }} / 模板 {{ detail.tempId || '-' }} / 重量 {{ detail.weight ?? 0 }} / 体积 {{ detail.volume ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="添加时间">{{ detail.addTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="砍价图片">
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
        <el-descriptions-item label="砍价规则">{{ detail.rule || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Delete, Download, Edit, Plus, Refresh, Search, View } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  bargainDelete,
  bargainExport,
  bargainInfo,
  bargainList,
  bargainStatus,
  bargainUserHelp,
  bargainUserList
} from '../api';

const props = defineProps({
  path: {
    type: String,
    default: ''
  }
});

const isRecordPage = computed(() => props.path.includes('/marketing/bargain/bargainList'));

const goodsLoading = ref(false);
const goodsRows = ref([]);
const goodsTotal = ref(0);
const exporting = ref(false);
const detailVisible = ref(false);
const detail = ref(null);

const recordLoading = ref(false);
const recordRows = ref([]);
const recordTotal = ref(0);
const recordTime = ref([]);

const helpVisible = ref(false);
const helpLoading = ref(false);
const helpRows = ref([]);

const goodsQuery = reactive({
  page: 1,
  limit: 20,
  keywords: '',
  status: ''
});

const recordQuery = reactive({
  page: 1,
  limit: 20,
  dateLimit: '',
  status: ''
});

watch(
  () => props.path,
  () => {
    if (isRecordPage.value) {
      loadRecords();
    } else {
      loadGoods();
    }
  }
);

onMounted(() => {
  if (isRecordPage.value) {
    loadRecords();
  } else {
    loadGoods();
  }
});

async function loadGoods() {
  goodsLoading.value = true;
  try {
    const data = await bargainList(compactParams(goodsQuery));
    goodsRows.value = data?.list || [];
    goodsTotal.value = Number(data?.total || 0);
  } finally {
    goodsLoading.value = false;
  }
}

function searchGoods() {
  goodsQuery.page = 1;
  loadGoods();
}

function resetGoods() {
  goodsQuery.page = 1;
  goodsQuery.limit = 20;
  goodsQuery.keywords = '';
  goodsQuery.status = '';
  loadGoods();
}

async function changeGoodsStatus(row, value) {
  const old = row.status;
  row.status = value;
  try {
    await bargainStatus({ id: row.id, status: value });
    ElMessage.success('修改成功');
  } catch {
    row.status = old;
  }
}

async function removeGoods(row) {
  await ElMessageBox.confirm('删除该商品吗', '提示', { type: 'warning' });
  await bargainDelete({ id: row.id });
  ElMessage.success('删除成功');
  if (goodsRows.value.length === 1 && goodsQuery.page > 1) goodsQuery.page -= 1;
  loadGoods();
}

async function openDetail(row) {
  detail.value = await bargainInfo({ id: row.id });
  detailVisible.value = true;
}

function handleCreate() {
  navigateTo('/marketing/bargain/creatBargain');
}

function handleEdit(row) {
  navigateTo(`/marketing/bargain/creatBargain/${row.id}`);
}

function navigateTo(path) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path } }));
}

async function handleExport() {
  exporting.value = true;
  try {
    const data = await bargainExport(compactParams(goodsQuery));
    if (data?.fileName) {
      window.open(data.fileName, '_blank');
      ElMessage.success('导出成功');
    }
  } finally {
    exporting.value = false;
  }
}

async function loadRecords() {
  recordLoading.value = true;
  try {
    const data = await bargainUserList(compactParams(recordQuery));
    recordRows.value = data?.list || [];
    recordTotal.value = Number(data?.total || 0);
  } finally {
    recordLoading.value = false;
  }
}

function searchRecords() {
  recordQuery.page = 1;
  loadRecords();
}

function handleRecordTimeChange(value) {
  recordQuery.dateLimit = value?.length === 2 ? value.join(',') : '';
  searchRecords();
}

async function openHelp(row) {
  helpVisible.value = true;
  helpLoading.value = true;
  try {
    helpRows.value = await bargainUserHelp(row.id);
  } finally {
    helpLoading.value = false;
  }
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
  return `/crmebimage/${value.replace(/^\/+/, '')}`;
}

function recordStatusText(value) {
  if (Number(value) === 1) return '进行中';
  if (Number(value) === 2) return '未完成';
  if (Number(value) === 3) return '已成功';
  return '-';
}

function recordStatusTag(value) {
  if (Number(value) === 1) return 'success';
  if (Number(value) === 2) return 'warning';
  if (Number(value) === 3) return 'info';
  return 'danger';
}
</script>

<style scoped>
.bargain-manager .padding-add {
  padding: 20px 20px 0;
}

.selWidth {
  width: 180px;
}

.date-picker {
  width: 260px;
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

.product-image,
.avatar-image {
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
