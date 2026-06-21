<template>
  <div class="divBox">
    <el-card class="box-card">
      <el-form ref="formRef" :model="ruleForm" :rules="rules" label-width="150px" class="demo-ruleForm">
        <el-form-item label="优惠劵名称：" prop="name">
          <el-input v-model="ruleForm.name" style="width: 350px" placeholder="请输入优惠券名称" />
        </el-form-item>
        <el-form-item label="优惠劵类型：">
          <el-radio-group v-model="ruleForm.useType" @change="handleUseTypeChange">
            <el-radio :label="1">通用券</el-radio>
            <el-radio :label="2">商品券</el-radio>
            <el-radio :label="3">品类券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="ruleForm.useType === 3" label="选择品类：" prop="primaryKey">
          <el-cascader
            v-model="ruleForm.primaryKey"
            :options="categoryOptions"
            :props="categoryProps"
            clearable
            class="selWidth"
            :show-all-levels="false"
          />
        </el-form-item>
        <el-form-item v-if="ruleForm.useType === 2" label="商品：" prop="checked">
          <div class="acea-row">
            <template v-if="ruleForm.checked.length">
              <div v-for="(item, index) in ruleForm.checked" :key="item.id" class="pictrue">
                <img :src="assetUrl(item.image)" />
                <el-icon class="btndel" @click="handleRemove(index)"><CircleCloseFilled /></el-icon>
              </div>
            </template>
            <div class="upLoadPicBox" @click="openProductDialog">
              <div class="upLoad">
                <el-icon class="cameraIconfont"><Camera /></el-icon>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="优惠券面值：" prop="money">
          <el-input-number v-model="ruleForm.money" controls-position="right" :max="99999.99" :min="1" />
        </el-form-item>
        <el-form-item label="使用门槛：">
          <el-radio-group v-model="threshold">
            <el-radio :label="false">无门槛</el-radio>
            <el-radio :label="true">有门槛</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="threshold" label="优惠券最低消费：" prop="minPrice">
          <el-input-number v-model="ruleForm.minPrice" controls-position="right" :min="1" />
        </el-form-item>
        <el-form-item label="使用有效期：">
          <el-radio-group v-model="ruleForm.isFixedTime">
            <el-radio :label="false">天数</el-radio>
            <el-radio :label="true">时间段</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!ruleForm.isFixedTime" label="使用有效期限（天）：" prop="day">
          <el-input-number v-model="ruleForm.day" controls-position="right" :min="0" :max="999" />
        </el-form-item>
        <el-form-item v-if="ruleForm.isFixedTime" label="使用有效期限：" prop="termTime">
          <el-date-picker
            v-model="termTime"
            style="width: 550px"
            type="datetimerange"
            range-separator="至"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
        <el-form-item label="领取是否限时：" prop="isForever">
          <el-radio-group v-model="ruleForm.isForever">
            <el-radio :label="true">限时</el-radio>
            <el-radio :label="false">不限时</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="ruleForm.isForever" label="领取时间：">
          <el-date-picker
            v-model="isForeverTime"
            style="width: 550px"
            type="datetimerange"
            range-separator="至"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
        <el-form-item label="领取方式：" prop="type">
          <el-radio-group v-model="ruleForm.type">
            <el-radio :label="1">手动领取</el-radio>
            <el-radio :label="2">新人券</el-radio>
            <el-radio :label="3">赠送券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否限量：" prop="isLimited">
          <el-radio-group v-model="ruleForm.isLimited">
            <el-radio :label="true">限量</el-radio>
            <el-radio :label="false">不限量</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="ruleForm.isLimited" label="发布数量：" prop="total">
          <el-input-number v-model="ruleForm.total" controls-position="right" :min="1" />
        </el-form-item>
        <el-form-item label="排序：" prop="sort">
          <el-input-number v-model="ruleForm.sort" controls-position="right" :min="0" />
        </el-form-item>
        <el-form-item label="状态：" prop="status">
          <el-radio-group v-model="ruleForm.status">
            <el-radio :label="true">开启</el-radio>
            <el-radio :label="false">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submitForm">{{ submitText }}</el-button>
          <el-button @click="$emit('back')">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog v-model="productDialogVisible" title="商品列表" width="900px">
      <div class="demo-input-suffix acea-row product-picker-search">
        <span class="seachTiele">商品搜索：</span>
        <el-input v-model="productQuery.keywords" placeholder="请输入商品名称/ID" class="selWidth" clearable @keyup.enter="searchProducts" />
        <el-button class="ml30" type="primary" size="small" @click="searchProducts">搜索</el-button>
      </div>
      <el-table
        ref="productTableRef"
        v-loading="productLoading"
        :data="productRows"
        size="small"
        border
        row-key="id"
        @selection-change="handleProductSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column label="商品图" width="80">
          <template #default="{ row }">
            <img class="product-thumb" :src="assetUrl(row.image)" />
          </template>
        </el-table-column>
        <el-table-column prop="storeName" label="商品名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="price" label="售价" width="90" />
        <el-table-column prop="stock" label="库存" width="90" />
      </el-table>
      <div class="block">
        <el-pagination
          v-model:current-page="productQuery.page"
          v-model:page-size="productQuery.limit"
          :page-sizes="[10, 20, 40]"
          layout="total, sizes, prev, pager, next"
          :total="productTotal"
          background
          @size-change="loadProducts"
          @current-change="loadProducts"
        />
      </div>
      <template #footer>
        <el-button @click="productDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmProducts">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Camera, CircleCloseFilled } from '@element-plus/icons-vue';
import { categoryTree, couponInfo, couponSave, productList } from '../api';

const props = defineProps({
  couponId: {
    type: Number,
    default: null
  },
  copyId: {
    type: Number,
    default: null
  }
});

const emit = defineEmits(['back', 'saved']);

const formRef = ref();
const productTableRef = ref();
const loading = ref(false);
const threshold = ref(false);
const termTime = ref([]);
const isForeverTime = ref([]);
const categoryOptions = ref([]);
const productDialogVisible = ref(false);
const productLoading = ref(false);
const productRows = ref([]);
const productTotal = ref(0);
const productSelection = ref([]);
const submitText = computed(() => (props.couponId ? '保存修改' : '立即创建'));

const categoryProps = {
  children: 'child',
  label: 'name',
  value: 'id',
  checkStrictly: true,
  emitPath: false
};

const ruleForm = reactive(defaultForm());
const productQuery = reactive({
  page: 1,
  limit: 10,
  type: 1,
  keywords: ''
});

const rules = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  day: [{ required: true, message: '请输入使用有效期限（天）', trigger: 'blur' }],
  money: [{ required: true, message: '请输入优惠券面值', trigger: 'blur' }],
  primaryKey: [{ required: true, message: '请选择品类', trigger: 'change' }],
  checked: [{ required: true, message: '请至少选择一个商品', trigger: 'change', type: 'array' }],
  total: [{ required: true, message: '请输入发布数量', trigger: 'blur' }],
  minPrice: [{ required: true, message: '请输入最低消费', trigger: 'blur' }]
};

function defaultForm() {
  return {
    useType: 1,
    isFixedTime: false,
    name: '',
    money: 1,
    minPrice: 1,
    day: null,
    isForever: false,
    primaryKey: '',
    type: 2,
    isLimited: false,
    useStartTime: '',
    useEndTime: '',
    receiveStartTime: '',
    receiveEndTime: '',
    sort: 0,
    total: 1,
    status: false,
    checked: []
  };
}

function applyForm(data) {
  Object.assign(ruleForm, defaultForm(), data);
}

async function loadCategorySelect() {
  const data = await categoryTree({ status: -1, type: 1 });
  categoryOptions.value = (data || []).map((item) => ({ ...item, disabled: true }));
}

async function loadInfo() {
  const targetId = props.couponId || props.copyId;
  if (!targetId) return;
  loading.value = true;
  try {
    const data = await couponInfo({ id: targetId });
    const info = data?.coupon || {};
    applyForm({
      useType: info.useType,
      isFixedTime: !!info.isFixedTime,
      isForever: !!info.isForever,
      name: info.name,
      money: info.money,
      minPrice: info.minPrice,
      day: info.day,
      type: info.type,
      isLimited: !!info.isLimited,
      sort: info.sort,
      total: info.total,
      status: !!info.status,
      primaryKey: Number(info.primaryKey) || '',
      checked: data?.product || []
    });
    threshold.value = Number(info.minPrice || 0) !== 0;
    isForeverTime.value = info.isForever ? [info.receiveStartTime, info.receiveEndTime] : [];
    termTime.value = info.isFixedTime && info.useStartTime && info.useEndTime ? [info.useStartTime, info.useEndTime] : [];
  } finally {
    loading.value = false;
  }
}

function handleUseTypeChange() {
  ruleForm.primaryKey = '';
  ruleForm.checked = [];
}

function handleRemove(index) {
  ruleForm.checked.splice(index, 1);
}

async function openProductDialog() {
  productDialogVisible.value = true;
  productSelection.value = [...ruleForm.checked];
  await loadProducts();
  await nextTick();
  syncProductSelection();
}

function searchProducts() {
  productQuery.page = 1;
  loadProducts();
}

async function loadProducts() {
  productLoading.value = true;
  try {
    const data = await productList(productQuery);
    productRows.value = data?.list || [];
    productTotal.value = data?.total || 0;
    await nextTick();
    syncProductSelection();
  } finally {
    productLoading.value = false;
  }
}

function syncProductSelection() {
  const table = productTableRef.value;
  if (!table) return;
  table.clearSelection();
  const selectedIds = new Set(productSelection.value.map((item) => item.id));
  productRows.value.forEach((row) => {
    if (selectedIds.has(row.id)) {
      table.toggleRowSelection(row, true);
    }
  });
}

function handleProductSelectionChange(selection) {
  const selectedMap = new Map(productSelection.value.map((item) => [item.id, item]));
  const pageIds = new Set(productRows.value.map((item) => item.id));
  pageIds.forEach((id) => selectedMap.delete(id));
  selection.forEach((item) => selectedMap.set(item.id, item));
  productSelection.value = Array.from(selectedMap.values());
}

function confirmProducts() {
  ruleForm.checked = [...productSelection.value];
  productDialogVisible.value = false;
}

async function submitForm() {
  if (ruleForm.isFixedTime && !termTime.value.length) {
    ElMessage.warning('请选择使用有效期限');
    return;
  }
  if (ruleForm.isForever && !isForeverTime.value.length) {
    ElMessage.warning('请选择请选择领取时间');
    return;
  }
  if (ruleForm.useType === 2) {
    ruleForm.primaryKey = ruleForm.checked.map((item) => item.id).join(',');
  }
  if (ruleForm.useType === 1) {
    ruleForm.primaryKey = '';
  }
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  const payload = {
    ...ruleForm,
    id: props.couponId || undefined,
    minPrice: threshold.value ? ruleForm.minPrice : 0,
    total: ruleForm.isLimited ? ruleForm.total : 0,
    useStartTime: ruleForm.isFixedTime ? termTime.value[0] : '',
    useEndTime: ruleForm.isFixedTime ? termTime.value[1] : '',
    day: ruleForm.isFixedTime ? null : ruleForm.day,
    receiveStartTime: ruleForm.isForever ? isForeverTime.value[0] : '',
    receiveEndTime: ruleForm.isForever ? isForeverTime.value[1] : ''
  };
  delete payload.checked;

  loading.value = true;
  try {
    await couponSave(payload);
    ElMessage.success(props.couponId ? '保存成功' : '新增成功');
    emit('saved');
  } finally {
    loading.value = false;
  }
}

function assetUrl(value) {
  if (!value) return '';
  if (/^https?:\/\//.test(value) || value.startsWith('/')) return value;
  return `/${value}`;
}

onMounted(async () => {
  await loadCategorySelect();
  await loadInfo();
});
</script>

<style scoped>
.pictrue {
  width: 60px;
  height: 60px;
  border: 1px dotted rgba(0, 0, 0, 0.1);
  margin-right: 10px;
  position: relative;
  cursor: pointer;
}

.pictrue img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.btndel {
  position: absolute;
  z-index: 1;
  width: 20px !important;
  height: 20px !important;
  left: 43px;
  top: 1px;
  color: #f56c6c;
  cursor: pointer;
}

.upLoadPicBox {
  width: 60px;
  height: 60px;
  border: 1px dashed #c0ccda;
  cursor: pointer;
}

.upLoad {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8c939d;
}

.cameraIconfont {
  font-size: 22px;
}

.product-picker-search {
  margin-bottom: 14px;
}

.product-thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
}
</style>
