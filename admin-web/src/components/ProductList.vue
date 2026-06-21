<template>
  <div class="divBox relative">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form inline size="small">
          <el-form-item label="商品分类：">
            <el-cascader
              v-model="query.cateId"
              :options="categoryOptions"
              :props="categoryProps"
              clearable
              class="selWidth"
              @change="searchList"
            />
          </el-form-item>
          <el-form-item label="商品搜索：">
            <el-input
              v-model="query.keywords"
              placeholder="请输入商品名称，关键字，商品ID"
              class="selWidth"
              clearable
              @keyup.enter="searchList"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="searchList">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <template #header>
        <div class="clearfix">
          <el-tabs v-model="query.type" @tab-change="searchList">
            <el-tab-pane
              v-for="item in headerCounts"
              :key="item.type"
              :label="`${item.name}(${item.count})`"
              :name="String(item.type)"
            />
          </el-tabs>
          <el-button type="primary" class="mr14" @click="navigateTo('/store/list/creatProduct')">添加商品</el-button>
          <el-button type="success" @click="handleCopy">商品采集</el-button>
          <el-button :loading="exporting" @click="handleExport">导出</el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="tableData"
        class="table"
        style="width: 100%"
        size="small"
        border
        highlight-current-row
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <el-form label-position="left" inline class="demo-table-expand">
              <el-form-item label="商品分类：" label-width="86px">
                <span>{{ row.cateValues || '-' }}</span>
              </el-form-item>
              <el-form-item label="市场价:" label-width="66px">
                <span>{{ row.otPrice }}</span>
              </el-form-item>
              <el-form-item label="成本价:" label-width="66px">
                <span>{{ row.cost }}</span>
              </el-form-item>
              <el-form-item label="收藏:" label-width="54px">
                <span>{{ row.collectCount }}</span>
              </el-form-item>
              <el-form-item label="虚拟销量:" label-width="86px">
                <span>{{ row.ficti }}</span>
              </el-form-item>
            </el-form>
          </template>
        </el-table-column>
        <el-table-column v-if="visibleColumns.includes('ID')" prop="id" label="ID" min-width="60" />
        <el-table-column v-if="visibleColumns.includes('商品图')" label="商品图" min-width="80">
          <template #default="{ row }">
            <el-image class="product-image" :src="row.image" :preview-src-list="[row.image]" fit="cover" />
          </template>
        </el-table-column>
        <el-table-column v-if="visibleColumns.includes('商品名称')" prop="storeName" label="商品名称" min-width="300" show-overflow-tooltip />
        <el-table-column v-if="visibleColumns.includes('商品售价')" prop="price" label="商品售价" min-width="90" />
        <el-table-column v-if="visibleColumns.includes('销量')" prop="sales" label="销量" min-width="90" />
        <el-table-column v-if="visibleColumns.includes('库存')" prop="stock" label="库存" min-width="90" />
        <el-table-column v-if="visibleColumns.includes('排序')" prop="sort" label="排序" min-width="70" />
        <el-table-column v-if="visibleColumns.includes('操作时间')" label="添加时间" min-width="150">
          <template #default="{ row }">{{ formatTime(row.addTime) }}</template>
        </el-table-column>
        <el-table-column v-if="visibleColumns.includes('状态')" label="状态" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-switch
              v-model="row.isShow"
              :disabled="Number(query.type) > 2"
              active-text="上架"
              inactive-text="下架"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #header>
            <span class="operation-header">
              <span>操作</span>
              <el-button class="column-setting" link @click="cardSelectVisible = !cardSelectVisible">
                <el-icon><Setting /></el-icon>
              </el-button>
            </span>
          </template>
          <template #default="{ row }">
            <el-button link type="primary" @click="navigateTo(`/store/list/creatProduct/${row.id}/1`)">详情</el-button>
            <el-divider direction="vertical" />
            <el-button link type="primary" :disabled="query.type === '5'" @click="navigateTo(`/store/list/creatProduct/${row.id}`)">
              编辑
            </el-button>
            <el-divider direction="vertical" />
            <el-button link type="primary" :disabled="query.type === '2' || query.type === '5'" @click="handleStock(row)">
              编辑库存
            </el-button>
            <el-divider direction="vertical" />
            <template v-if="query.type === '5'">
              <el-button link type="primary" @click="handleRestore(row.id)">恢复商品</el-button>
              <el-divider direction="vertical" />
            </template>
            <el-button link type="danger" @click="handleDelete(row.id)">
              {{ query.type === '5' ? '删除' : '加入回收站' }}
            </el-button>
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

    <div v-show="cardSelectVisible" class="card_abs">
      <div class="cell_ht">
        <el-checkbox v-model="checkAll" :indeterminate="isIndeterminate" @change="handleCheckAllChange">全选</el-checkbox>
        <el-button link type="primary" @click="saveColumns">保存</el-button>
      </div>
      <el-checkbox-group v-model="visibleColumns" @change="handleColumnChange">
        <el-checkbox v-for="item in columnData" :key="item" :label="item" class="check_cell">{{ item }}</el-checkbox>
      </el-checkbox-group>
    </div>

    <el-drawer
      v-model="detailDrawerVisible"
      title="商品详情"
      direction="rtl"
      size="86%"
      class="showHeader"
      destroy-on-close
    >
      <ProductDetailDrawer
        v-if="detailDrawerVisible && currentProductId"
        :product-id="currentProductId"
      />
    </el-drawer>

    <el-drawer
      v-model="stockDrawerVisible"
      title="编辑库存"
      direction="rtl"
      size="80%"
      class="showHeader"
      destroy-on-close
    >
      <ProductStockEditor
        v-if="stockDrawerVisible && currentProductId"
        :product-id="currentProductId"
        @success="handleStockSuccess"
      />
    </el-drawer>

    <el-drawer
      v-model="editDrawerVisible"
      :title="currentProductId ? '编辑商品' : '添加商品'"
      direction="rtl"
      size="86%"
      class="showHeader"
      destroy-on-close
    >
      <ProductBasicEditor
        v-if="editDrawerVisible"
        :product-id="currentProductId"
        :initial-product="initialProduct"
        @success="handleEditSuccess"
        @cancel="editDrawerVisible = false"
      />
    </el-drawer>

    <el-dialog
      v-model="copyDialogVisible"
      title="复制淘宝、天猫、京东、苏宁"
      width="820px"
      class="taoBaoModal"
      append-to-body
      destroy-on-close
    >
      <div class="copy-product-box">
        <el-alert
          title="商品采集设置：设置 > 系统设置 > 第三方接口设置 > 采集商品配置"
          type="warning"
          :closable="false"
          show-icon
        />
        <el-form class="copy-product-form" label-width="90px">
          <el-form-item label="商品链接：">
            <el-input
              v-model="copyUrl"
              placeholder="请输入淘宝、天猫、京东、苏宁商品链接"
              clearable
              @keyup.enter="handleCopySubmit"
            />
          </el-form-item>
        </el-form>
        <div v-if="copyConfig" class="copy-product-tip">
          当前采集方式：{{ copyConfig.copyType === '1' ? '一号通' : '99Api' }}
          <template v-if="copyConfig.copyType === '1'">，剩余采集次数：{{ copyConfig.copyNum }}</template>
        </div>
        <div v-else-if="copyConfigError" class="copy-product-tip copy-product-error">
          {{ copyConfigError }}
        </div>
      </div>
      <template #footer>
        <el-button @click="copyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCopySubmit">开始采集</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Setting } from '@element-plus/icons-vue';
import {
  categoryTree,
  productCopy,
  productCopyConfig,
  productDelete,
  productExport,
  productHeaders,
  productList,
  productOffShell,
  productPutOnShell,
  productRestore
} from '../api';
import ProductStockEditor from './ProductStockEditor.vue';
import ProductDetailDrawer from './ProductDetailDrawer.vue';
import ProductBasicEditor from './ProductBasicEditor.vue';

const categoryProps = {
  children: 'child',
  label: 'name',
  value: 'id',
  emitPath: false
};

const query = reactive({
  page: 1,
  limit: 20,
  cateId: '',
  keywords: '',
  type: '1'
});

const loading = ref(false);
const exporting = ref(false);
const tableData = ref([]);
const total = ref(0);
const headerCounts = ref([]);
const categoryOptions = ref([]);
const cardSelectVisible = ref(false);
const detailDrawerVisible = ref(false);
const stockDrawerVisible = ref(false);
const editDrawerVisible = ref(false);
const copyDialogVisible = ref(false);
const currentProductId = ref(0);
const copyUrl = ref('');
const copyConfig = ref(null);
const copyConfigError = ref('');
const initialProduct = ref(null);
const columnData = ['ID', '商品图', '商品名称', '商品售价', '销量', '库存', '排序', '状态', '操作时间'];
const visibleColumns = ref(loadColumns());
const checkAll = ref(visibleColumns.value.length === columnData.length);
const isIndeterminate = ref(visibleColumns.value.length > 0 && visibleColumns.value.length < columnData.length);

function loadColumns() {
  const saved = localStorage.getItem('goods_stroge');
  if (!saved) return [...columnData];
  try {
    const parsed = JSON.parse(saved);
    return Array.isArray(parsed) && parsed.length ? parsed : [...columnData];
  } catch {
    return [...columnData];
  }
}

function searchList() {
  query.page = 1;
  loadHeaders();
  loadList();
}

function handleReset() {
  query.cateId = '';
  query.keywords = '';
  searchList();
}

async function loadHeaders() {
  headerCounts.value = await productHeaders({ ...query });
}

async function loadList() {
  loading.value = true;
  try {
    const data = await productList({ ...query });
    tableData.value = data.list || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

function formatTime(timestamp) {
  if (!timestamp) return '-';
  const date = new Date(timestamp * 1000);
  const pad = (value) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function handleDetail(row) {
  currentProductId.value = row.id;
  detailDrawerVisible.value = true;
}

function handleStock(row) {
  currentProductId.value = row.id;
  stockDrawerVisible.value = true;
}

function handleEdit(row) {
  currentProductId.value = row.id;
  initialProduct.value = null;
  editDrawerVisible.value = true;
}

function handleCreate() {
  currentProductId.value = 0;
  initialProduct.value = null;
  editDrawerVisible.value = true;
}

function handleCopy() {
  copyUrl.value = '';
  copyConfigError.value = '';
  copyDialogVisible.value = true;
  loadCopyConfig();
}

async function loadCopyConfig() {
  try {
    copyConfig.value = await productCopyConfig();
  } catch (error) {
    copyConfig.value = null;
    copyConfigError.value = error.message || '请先进行采集商品配置';
  }
}

async function handleCopySubmit() {
  if (!copyUrl.value.trim()) {
    ElMessage.warning('请输入链接地址！');
    return;
  }
  try {
    const data = await productCopy({ url: copyUrl.value.trim() });
    currentProductId.value = 0;
    initialProduct.value = data?.info || null;
    editDrawerVisible.value = true;
    copyDialogVisible.value = false;
    ElMessage.success('采集成功，请补充分类、运费模板后保存');
  } catch (error) {
    return;
  }
}

async function handleExport() {
  exporting.value = true;
  try {
    const data = await productExport({
      cateId: query.cateId,
      keywords: query.keywords,
      type: query.type
    });
    if (data?.fileName) {
      window.location.href = data.fileName;
      ElMessage.success('导出成功');
    }
  } finally {
    exporting.value = false;
  }
}

async function handleStockSuccess() {
  stockDrawerVisible.value = false;
  await Promise.all([loadHeaders(), loadList()]);
}

async function handleEditSuccess() {
  editDrawerVisible.value = false;
  initialProduct.value = null;
  await Promise.all([loadHeaders(), loadList()]);
}

async function handleStatusChange(row) {
  try {
    if (row.isShow) {
      await productPutOnShell(row.id);
      ElMessage.success('上架成功');
    } else {
      await productOffShell(row.id);
      ElMessage.success('下架成功');
    }
    await Promise.all([loadHeaders(), loadList()]);
  } catch (error) {
    row.isShow = !row.isShow;
  }
}

async function handleDelete(id) {
  const isRecycle = query.type === '5';
  const message = isRecycle ? `删除 id 为 ${id} 的商品吗？` : `将 id 为 ${id} 的商品加入回收站吗？`;
  try {
    await ElMessageBox.confirm(message, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch {
    return;
  }
  await productDelete(id, isRecycle ? 'delete' : 'recycle');
  ElMessage.success('操作成功');
  if (tableData.value.length === 1 && query.page > 1) {
    query.page -= 1;
  }
  await Promise.all([loadHeaders(), loadList()]);
}

async function handleRestore(id) {
  try {
    await ElMessageBox.confirm('恢复商品', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch {
    return;
  }
  await productRestore(id);
  ElMessage.success('操作成功');
  await Promise.all([loadHeaders(), loadList()]);
}

function handleCheckAllChange(value) {
  visibleColumns.value = value ? [...columnData] : [];
  isIndeterminate.value = false;
}

function handleColumnChange(value) {
  checkAll.value = value.length === columnData.length;
  isIndeterminate.value = value.length > 0 && value.length < columnData.length;
}

function saveColumns() {
  localStorage.setItem('goods_stroge', JSON.stringify(visibleColumns.value));
  cardSelectVisible.value = false;
  ElMessage.success('保存成功');
}

function navigateTo(path) {
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path } }));
}

onMounted(async () => {
  categoryOptions.value = await categoryTree({ status: -1, type: 1 });
  await Promise.all([loadHeaders(), loadList()]);
});
</script>

<style scoped>
.card_abs {
  position: absolute;
  padding-bottom: 15px;
  top: 260px;
  right: 40px;
  width: 200px;
  background: #fff;
  z-index: 10;
  box-shadow: 0 0 14px rgba(0, 0, 0, 0.1);
}

.cell_ht {
  height: 50px;
  padding: 15px 20px;
  box-sizing: border-box;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.check_cell {
  width: 100%;
  padding: 15px 20px 0;
}

.operation-header {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.column-setting {
  font-size: 14px;
  color: #606266;
}

.copy-product-box {
  padding: 4px 0;
}

.copy-product-form {
  margin-top: 18px;
}

.copy-product-tip {
  color: #909399;
  line-height: 1.7;
  padding-left: 90px;
}

.copy-product-error {
  color: #e6a23c;
}
</style>
