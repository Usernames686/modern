<template>
  <div class="divBox">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form inline :model="query" @submit.prevent>
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
          <el-form-item label="显示状态：">
            <el-select v-model="query.isShow" placeholder="请选择" class="selWidth" clearable @change="search">
              <el-option label="显示" :value="1" />
              <el-option label="隐藏" :value="0" />
            </el-select>
          </el-form-item>
          <el-button type="primary" size="small" @click="search">搜索</el-button>
          <el-button size="small" @click="reset">重置</el-button>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <template #header>
        <div class="clearfix">
          <el-button type="primary" size="small" :loading="syncing" @click="syncExpressList">同步物流公司</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" size="small">
        <el-table-column prop="id" label="ID" min-width="90" />
        <el-table-column prop="name" label="物流公司名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="code" label="编码" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" min-width="100" sortable />
        <el-table-column label="是否显示" min-width="120">
          <template #default="{ row }">
            <el-switch
              v-model="row.isShow"
              class="demo"
              :active-value="true"
              :inactive-value="false"
              active-text="开启"
              inactive-text="关闭"
              @change="changeShow(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="是否可用" min-width="110">
          <template #default="{ row }">
            <el-tag :type="row.status ? 'success' : 'warning'">{{ row.status ? '可用' : '未启用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="电子面单配置" min-width="180">
          <template #default="{ row }">
            <span v-if="row.net">网点：{{ row.netName || '-' }}</span>
            <span v-else-if="row.partnerId">月结账号：{{ row.account || '-' }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column fixed="right" width="150" label="操作">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">{{ editText(row) }}</el-button>
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

    <el-dialog v-model="dialogVisible" title="编辑物流公司" width="540px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="95px" class="demo-ruleForm">
        <el-form-item v-if="formData.partnerId" label="月结账号：" prop="account">
          <el-input v-model="formData.account" placeholder="请输入月结账号" />
        </el-form-item>
        <el-form-item v-if="formData.partnerKey" label="月结密码：" prop="password">
          <el-input v-model="formData.password" placeholder="请输入月结密码" />
        </el-form-item>
        <el-form-item v-if="formData.net" label="网点名称：" prop="netName">
          <el-input v-model="formData.netName" placeholder="请输入网点名称" />
        </el-form-item>
        <el-form-item label="排序：" prop="sort">
          <el-input-number v-model="formData.sort" controls-position="right" :min="0" :max="9999" label="排序" />
        </el-form-item>
        <el-form-item label="是否启用：" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="false">关闭</el-radio>
            <el-radio :value="true">开启</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { expressInfo, expressList, expressSync, expressUpdate, expressUpdateShow } from '../api';

const loading = ref(false);
const saving = ref(false);
const syncing = ref(false);
const tableData = ref([]);
const total = ref(0);
const dialogVisible = ref(false);
const formRef = ref();

const query = reactive({
  page: 1,
  limit: 20,
  keywords: '',
  isShow: ''
});

const formData = reactive(defaultForm());

const rules = {
  sort: [{ required: true, message: '请输入排序', trigger: 'blur' }],
  account: [{ required: true, message: '请输入月结账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入月结密码', trigger: 'blur' }],
  netName: [{ required: true, message: '请输入网点名称', trigger: 'blur' }]
};

onMounted(loadList);

function defaultForm() {
  return {
    id: undefined,
    account: '',
    password: '',
    netName: '',
    sort: 0,
    status: false,
    partnerId: false,
    partnerKey: false,
    net: false
  };
}

function search() {
  query.page = 1;
  loadList();
}

function reset() {
  query.keywords = '';
  query.isShow = '';
  search();
}

async function loadList() {
  loading.value = true;
  try {
    const data = await expressList({
      page: query.page,
      limit: query.limit,
      keywords: query.keywords || undefined,
      isShow: query.isShow === '' ? undefined : query.isShow
    });
    tableData.value = (data?.list || []).map((item) => ({
      ...item,
      isShow: boolValue(item.isShow),
      status: boolValue(item.status),
      partnerId: boolValue(item.partnerId),
      partnerKey: boolValue(item.partnerKey),
      net: boolValue(item.net)
    }));
    total.value = data?.total || 0;
  } finally {
    loading.value = false;
  }
}

async function syncExpressList() {
  syncing.value = true;
  try {
    const data = await expressSync();
    ElMessage({
      message: data?.message || '同步成功',
      type: data?.localMode ? 'warning' : 'success'
    });
    query.page = 1;
    loadList();
  } finally {
    syncing.value = false;
  }
}

async function changeShow(row) {
  try {
    await expressUpdateShow({ id: row.id, isShow: row.isShow });
    ElMessage.success('操作成功');
  } catch (error) {
    row.isShow = !row.isShow;
  }
}

async function openEdit(row) {
  resetForm();
  const info = await expressInfo({ id: row.id });
  Object.assign(formData, {
    ...info,
    isShow: boolValue(info.isShow),
    status: boolValue(info.status),
    partnerId: boolValue(info.partnerId),
    partnerKey: boolValue(info.partnerKey),
    net: boolValue(info.net),
    sort: Number(info.sort || 0)
  });
  dialogVisible.value = true;
}

async function submit() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    await expressUpdate({
      id: formData.id,
      account: formData.account,
      password: formData.password,
      netName: formData.netName,
      sort: formData.sort,
      status: formData.status
    });
    ElMessage.success('操作成功');
    dialogVisible.value = false;
    loadList();
  } finally {
    saving.value = false;
  }
}

function resetForm() {
  Object.assign(formData, defaultForm());
  formRef.value?.clearValidate?.();
}

function editText(row) {
  if (row.net) return '收件网点名称编辑';
  if (row.partnerId) return '月结账号编辑';
  return '编辑';
}

function boolValue(value) {
  if (typeof value === 'boolean') return value;
  if (typeof value === 'number') return value !== 0;
  return value === 'true' || value === '1';
}
</script>

<style scoped>
.selWidth {
  width: 350px;
}

.demo :deep(.el-switch__label) {
  color: var(--el-text-color-regular);
}
</style>
