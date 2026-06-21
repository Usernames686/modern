<template>
  <div class="storeEdit">
    <el-skeleton v-if="loading" :rows="5" animated />
    <el-form v-else>
      <el-table :data="rows" border class="tabNumWidth" size="small">
        <template v-if="detail.specType">
          <el-table-column v-for="name in attrColumnNames" :key="name" :label="name" min-width="80">
            <template #default="{ row }">
              <span class="priceBox">{{ row.attrMap[name] || '-' }}</span>
            </template>
          </el-table-column>
        </template>
        <el-table-column label="图片" min-width="80">
          <template #default="{ row }">
            <div class="upLoadPicBox">
              <div v-if="row.image" class="pictrue tabPic">
                <el-image class="preview-src" :src="row.image" :preview-src-list="[row.image]" fit="cover" />
              </div>
              <div v-else class="upLoad tabPic">
                <el-icon><Camera /></el-icon>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="售价" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.price" disabled class="priceBox" />
          </template>
        </el-table-column>
        <el-table-column label="成本价" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.cost" disabled class="priceBox" />
          </template>
        </el-table-column>
        <el-table-column label="原价" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.otPrice" disabled class="priceBox" />
          </template>
        </el-table-column>
        <el-table-column label="库存" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.stock" disabled class="priceBox" />
          </template>
        </el-table-column>
        <el-table-column label="商品编号" min-width="130">
          <template #default="{ row }">
            <el-input v-model="row.barCode" disabled class="priceBox" />
          </template>
        </el-table-column>
        <el-table-column label="重量（KG）" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.weight" disabled class="priceBox" />
          </template>
        </el-table-column>
        <el-table-column label="体积(m³)" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.volume" disabled class="priceBox" />
          </template>
        </el-table-column>
        <el-table-column label="增加库存" min-width="140" fixed="right">
          <template #default="{ row }">
            <el-input-number
              v-model="row.addStock"
              controls-position="right"
              :min="0"
              :step="1"
              step-strictly
            />
          </template>
        </el-table-column>
      </el-table>
    </el-form>
    <el-button class="confirm-button" type="primary" :loading="saving" @click="submit">确认</el-button>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Camera } from '@element-plus/icons-vue';
import { productDetail, stockAdd } from '../api';

const props = defineProps({
  productId: {
    type: Number,
    required: true
  }
});

const emit = defineEmits(['success']);
const loading = ref(false);
const saving = ref(false);
const rows = ref([]);
const detail = reactive({
  specType: false,
  attr: []
});

const attrColumnNames = computed(() => {
  const names = detail.attr.map((item) => item.attrName).filter(Boolean);
  if (names.length) return names;
  const first = rows.value.find((row) => row.attrMap && Object.keys(row.attrMap).length);
  return first ? Object.keys(first.attrMap) : [];
});

function parseAttrValue(value) {
  if (!value) return {};
  if (typeof value === 'object') return value;
  try {
    return JSON.parse(value);
  } catch {
    return {};
  }
}

function normalizeRows(attrValue = []) {
  return attrValue.map((item) => ({
    ...item,
    attrMap: parseAttrValue(item.attrValue),
    addStock: 0
  }));
}

async function loadDetail() {
  loading.value = true;
  try {
    const data = await productDetail(props.productId);
    detail.specType = Boolean(data.specType);
    detail.attr = data.attr || [];
    rows.value = normalizeRows(data.attrValue || []);
  } finally {
    loading.value = false;
  }
}

async function submit() {
  saving.value = true;
  try {
    await stockAdd({
      id: props.productId,
      attrValueList: rows.value.map((row) => ({
        id: row.id,
        addStock: row.addStock || 0,
        version: row.version || 0
      }))
    });
    ElMessage.success('保存成功');
    emit('success');
  } finally {
    saving.value = false;
  }
}

onMounted(loadDetail);
</script>

<style scoped>
.storeEdit {
  padding: 0 35px;
}

.pictrue,
.upLoad {
  width: 36px;
  height: 36px;
}

.pictrue :deep(img) {
  width: 36px;
  height: 36px;
  object-fit: cover;
}

.upLoad {
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #dcdfe6;
  color: #909399;
}

.priceBox {
  width: 100%;
}

.confirm-button {
  margin-top: 20px;
}
</style>
