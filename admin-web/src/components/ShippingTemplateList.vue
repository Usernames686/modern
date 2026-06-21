<template>
  <div class="divBox">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form inline :model="query" @submit.prevent>
          <el-form-item label="模板名称：">
            <el-input
              v-model="query.keywords"
              placeholder="请输入模板名称"
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
          <el-button type="primary" @click="openCreate">添加运费模板</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" style="width: 100%" size="small">
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column prop="name" label="模板名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="计费方式" min-width="100">
          <template #default="{ row }">{{ typeText(row.type) }}</template>
        </el-table-column>
        <el-table-column label="包邮方式" min-width="100">
          <template #default="{ row }">{{ appointText(row.appoint) }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" min-width="100" />
        <el-table-column label="添加时间" min-width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column fixed="right" width="120" label="操作">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button link type="danger" @click="removeTemplate(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="block-pagination">
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

    <el-drawer
      v-model="drawerVisible"
      title="运费模板"
      size="64%"
      direction="rtl"
      :close-on-click-modal="false"
      append-to-body
      @closed="resetForm"
    >
      <div class="drawer-content">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="small">
          <el-form-item label="模板名称：" prop="name">
            <el-input v-model="form.name" placeholder="请输入模板名称" />
          </el-form-item>
          <el-form-item label="包邮方式：" prop="appoint">
            <el-radio-group v-model="form.appoint" @change="changeAppoint">
              <el-radio :value="0">全国包邮</el-radio>
              <el-radio :value="1">部分包邮</el-radio>
              <el-radio :value="2">自定义</el-radio>
            </el-radio-group>
          </el-form-item>

          <template v-if="Number(form.appoint) > 0">
            <el-form-item label="计费方式：" prop="type">
              <el-radio-group v-model="form.type" @change="changeType">
                <el-radio :value="1">按件数</el-radio>
                <el-radio :value="2">按重量</el-radio>
                <el-radio :value="3">按体积</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="运费：" prop="region">
              <el-table :data="form.region" border fit highlight-current-row style="width: 100%" size="small" class="tempBox">
                <el-table-column label="送达到" min-width="260">
                  <template #default="{ row, $index }">
                    <span v-if="$index === 0 && form.appoint === 2">默认运费</span>
                    <el-cascader
                      v-else
                      v-model="row.city_ids"
                      style="width: 98%"
                      :options="cityList"
                      :props="cityProps"
                      collapse-tags
                      filterable
                    />
                  </template>
                </el-table-column>
                <el-table-column min-width="120" :label="columns.title">
                  <template #default="{ row }">
                    <el-input-number v-model="row.first" controls-position="right" :step-strictly="form.type === 1" :min="form.type === 1 ? 1 : 0.1" />
                  </template>
                </el-table-column>
                <el-table-column min-width="120" label="运费（元）">
                  <template #default="{ row }">
                    <el-input-number v-model="row.firstPrice" controls-position="right" :min="0" />
                  </template>
                </el-table-column>
                <el-table-column min-width="120" :label="columns.title2">
                  <template #default="{ row }">
                    <el-input-number v-model="row.renewal" controls-position="right" :step-strictly="form.type === 1" :min="form.type === 1 ? 1 : 0.1" />
                  </template>
                </el-table-column>
                <el-table-column label="续费（元）" min-width="120">
                  <template #default="{ row }">
                    <el-input-number v-model="row.renewalPrice" controls-position="right" :min="0" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80">
                  <template #default="{ $index }">
                    <el-button v-if="form.appoint === 1 || (form.appoint !== 1 && $index > 0)" link type="danger" @click="removeRow(form.region, $index)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-form-item>
            <el-form-item>
              <el-button class="mt20 mb20" type="primary" @click="addRegion">添加区域</el-button>
            </el-form-item>

            <el-form-item v-if="form.appoint === 2" label="包邮区域：">
              <el-table :data="form.free" border fit highlight-current-row style="width: 100%" size="small">
                <el-table-column label="选择区域" min-width="220">
                  <template #default="{ row }">
                    <el-cascader
                      v-model="row.city_ids"
                      style="width: 95%"
                      :options="cityList"
                      :props="cityProps"
                      collapse-tags
                      clearable
                    />
                  </template>
                </el-table-column>
                <el-table-column min-width="180" :label="columns.title3">
                  <template #default="{ row }">
                    <el-input-number v-model="row.number" controls-position="right" :step-strictly="form.type === 1" :min="form.type === 1 ? 1 : 0.1" />
                  </template>
                </el-table-column>
                <el-table-column min-width="120" label="包邮金额（元）">
                  <template #default="{ row }">
                    <el-input-number v-model="row.price" controls-position="right" :min="0" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120">
                  <template #default="{ $index }">
                    <el-button link type="danger" @click="removeRow(form.free, $index)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-form-item>
            <el-form-item v-if="form.appoint === 2">
              <el-button class="mt20 mb20" type="primary" @click="addFree">添加指定包邮区域</el-button>
            </el-form-item>
          </template>

          <el-form-item label="排序：">
            <el-input-number v-model="form.sort" controls-position="right" :min="0" :max="9999" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="drawerVisible = false">取 消</el-button>
          <el-button type="primary" :loading="saving" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  cityListTree,
  shippingTemplateDelete,
  shippingTemplateInfo,
  shippingTemplateSave,
  shippingTemplateUpdate,
  shippingTemplatesList
} from '../api';

const loading = ref(false);
const saving = ref(false);
const tableData = ref([]);
const total = ref(0);
const drawerVisible = ref(false);
const editId = ref();
const formRef = ref();
const cityList = ref([]);

const query = reactive({
  page: 1,
  limit: 20,
  keywords: ''
});

const form = reactive(defaultForm());

const rules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  appoint: [{ required: true, message: '请选择包邮方式', trigger: 'change' }],
  type: [{ required: true, message: '请选择计费方式', trigger: 'change' }]
};

const cityProps = {
  children: 'child',
  label: 'name',
  value: 'cityId',
  multiple: true
};

const statusMap = [
  { title: '首件', title2: '续件', title3: '包邮件数' },
  { title: '首件重量（kg）', title2: '续件重量（kg）', title3: '包邮重量（kg）' },
  { title: '首件体积（m³）', title2: '续件体积（m³）', title3: '包邮体积（m³）' }
];

const columns = computed(() => statusMap[(Number(form.type) || 1) - 1] || statusMap[0]);

onMounted(() => {
  loadList();
  loadCityTree();
});

function defaultForm() {
  return {
    name: '',
    type: 1,
    appoint: 0,
    sort: 0,
    region: [defaultRegion()],
    free: []
  };
}

function defaultRegion() {
  return {
    first: 1,
    firstPrice: 0,
    renewal: 1,
    renewalPrice: 0,
    city_ids: [],
    cityId: '0'
  };
}

function defaultFree() {
  return {
    number: 1,
    price: 0,
    city_ids: [],
    cityId: ''
  };
}

async function loadCityTree() {
  cityList.value = await cityListTree();
}

function search() {
  query.page = 1;
  loadList();
}

function reset() {
  query.keywords = '';
  search();
}

async function loadList() {
  loading.value = true;
  try {
    const data = await shippingTemplatesList({
      page: query.page,
      limit: query.limit,
      keywords: query.keywords || undefined
    });
    tableData.value = data?.list || [];
    total.value = data?.total || 0;
  } finally {
    loading.value = false;
  }
}

async function openCreate() {
  editId.value = undefined;
  resetForm();
  drawerVisible.value = true;
  if (!cityList.value.length) await loadCityTree();
}

async function openEdit(row) {
  editId.value = row.id;
  resetForm();
  if (!cityList.value.length) await loadCityTree();
  const info = await shippingTemplateInfo({ id: row.id });
  Object.assign(form, {
    name: info.name || '',
    type: info.appoint === 0 ? 1 : Number(info.type || 1),
    appoint: Number(info.appoint || 0),
    sort: Number(info.sort || 0),
    region: (info.regionList || []).map(mapRegionResponse),
    free: (info.freeList || []).map(mapFreeResponse)
  });
  if (form.appoint > 0 && form.region.length === 0) {
    form.region = [defaultRegion()];
  }
  if (form.appoint === 2 && form.region.length > 0) {
    form.region[0].city_ids = [];
    form.region[0].cityId = '0';
  }
  drawerVisible.value = true;
}

function mapRegionResponse(item) {
  return {
    first: Number(item.first || 1),
    firstPrice: Number(item.firstPrice || 0),
    renewal: Number(item.renewal || 1),
    renewalPrice: Number(item.renewalPrice || 0),
    city_ids: parseTitleToPaths(item.title),
    cityId: '',
    uniqid: item.uniqid
  };
}

function mapFreeResponse(item) {
  return {
    number: Number(item.number || 1),
    price: Number(item.price || 0),
    city_ids: parseTitleToPaths(item.title),
    cityId: '',
    uniqid: item.uniqid
  };
}

function parseTitleToPaths(title) {
  if (!title) return [];
  let clean = String(title).trim();
  if (clean.startsWith('[') && clean.endsWith(']')) {
    clean = clean.slice(1, -1);
  }
  if (!clean) return [];
  return clean
    .split('],[')
    .map((item) => item.replace(/\[|\]/g, '').split(',').map((value) => Number(value)).filter((value) => !Number.isNaN(value)))
    .filter((item) => item.length > 0 && !(item.length === 2 && item[0] === 0 && item[1] === 0));
}

function changeAppoint() {
  if (form.appoint === 0) {
    form.type = 1;
  }
  if (form.appoint > 0 && form.region.length === 0) {
    form.region.push(defaultRegion());
  }
  if (form.appoint === 2 && form.region.length === 0) {
    form.region.push(defaultRegion());
  }
}

function changeType() {}

function addRegion() {
  form.region.push(defaultRegion());
}

function addFree() {
  form.free.push(defaultFree());
}

function removeRow(rows, index) {
  rows.splice(index, 1);
}

async function submitForm() {
  await formRef.value?.validate();
  const payload = buildPayload();
  saving.value = true;
  try {
    if (editId.value) {
      await shippingTemplateUpdate(payload, editId.value);
    } else {
      await shippingTemplateSave(payload);
    }
    ElMessage.success('操作成功');
    drawerVisible.value = false;
    loadList();
  } finally {
    saving.value = false;
  }
}

function buildPayload() {
  const payload = {
    appoint: Number(form.appoint),
    name: form.name,
    sort: Number(form.sort || 0),
    type: form.appoint === 0 ? 0 : Number(form.type || 1)
  };
  if (form.appoint > 0) {
    payload.shippingTemplatesRegionRequestList = form.region.map((item, index) => normalizeRegionItem(item, index === 0 && form.appoint === 2));
  }
  if (form.appoint === 2) {
    payload.shippingTemplatesFreeRequestList = form.free.map((item) => normalizeFreeItem(item));
  }
  return payload;
}

function normalizeRegionItem(item, defaultRegionRow) {
  const paths = defaultRegionRow ? [[0, 0]] : (item.city_ids || []);
  return {
    cityId: defaultRegionRow ? '0' : paths.map((path) => path[path.length - 1]).join(','),
    title: JSON.stringify(paths),
    first: Number(item.first || 0),
    firstPrice: Number(item.firstPrice || 0),
    renewal: Number(item.renewal || 0),
    renewalPrice: Number(item.renewalPrice || 0),
    uniqid: item.uniqid
  };
}

function normalizeFreeItem(item) {
  const paths = item.city_ids || [];
  return {
    cityId: paths.map((path) => path[path.length - 1]).join(',') || '0',
    title: JSON.stringify(paths.length ? paths : [[0, 0]]),
    number: Number(item.number || 0),
    price: Number(item.price || 0),
    uniqid: item.uniqid
  };
}

async function removeTemplate(row) {
  await ElMessageBox.confirm('确定删除该运费模板吗？', '提示', { type: 'warning' });
  await shippingTemplateDelete({ id: row.id });
  ElMessage.success('删除成功');
  loadList();
}

function resetForm() {
  Object.assign(form, defaultForm());
  formRef.value?.clearValidate?.();
}

function typeText(value) {
  return {
    0: '无',
    1: '按件数',
    2: '按重量',
    3: '按体积'
  }[Number(value)] || '-';
}

function appointText(value) {
  return {
    0: '全国包邮',
    1: '部分包邮',
    2: '自定义'
  }[Number(value)] || '-';
}

function formatTime(value) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
}
</script>

<style scoped>
.selWidth {
  width: 350px;
}

.drawer-content {
  padding: 0 20px 70px;
  box-sizing: border-box;
}

.drawer-footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.tempBox :deep(.el-input-number--small) {
  width: 110px;
}
</style>
