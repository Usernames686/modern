<template>
  <div class="divBox">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form inline :model="query" size="small" @submit.prevent>
          <el-form-item label="提货点名称：">
            <el-select v-model="query.storeId" placeholder="请选择" class="selWidth" clearable @change="search">
              <el-option v-for="item in storeSelectList" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <template #header>
        <div class="clearfix">
          <el-button type="primary" size="small" @click="openCreate">添加核销员</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" size="small" border>
        <el-table-column prop="id" label="ID" sortable width="80" />
        <el-table-column prop="staffName" label="核销员名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="avatar" label="账号" min-width="150">
          <template #default="{ row }">{{ row.avatar || row.user?.account || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号码" min-width="120">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column prop="systemStore.detailedAddress" label="所属提货点" min-width="200">
          <template #default="{ row }">{{ row.systemStore?.detailedAddress || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="添加时间" min-width="180">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row.id)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="block">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.limit"
          :page-sizes="[20, 40, 60, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改核销员' : '添加核销员'" width="540px" @close="resetForm">
      <el-form
        ref="formRef"
        v-loading="formLoading"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="demo-ruleForm"
        @submit.prevent
      >
        <el-form-item label="管理员：" prop="uid">
          <span>{{ form.avatar }}</span>
          <el-button class="ml10" type="primary" size="small" @click="openAdminDialog">选择管理员</el-button>
        </el-form-item>
        <el-form-item label="所属提货点：" prop="storeId">
          <el-select v-model="form.storeId" placeholder="请选择" class="dialogWidth" clearable>
            <el-option v-for="item in storeSelectList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="核销员名称：" prop="staffName">
          <el-input v-model="form.staffName" placeholder="请输入核销员名称" class="dialogWidth" />
        </el-form-item>
        <el-form-item label="手机号码：" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号码" class="dialogWidth" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="formLoading" @click="submitForm">
          {{ form.id ? '修改' : '提交' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="adminDialogVisible" title="请选择管理员" width="900px" append-to-body @close="resetAdminDialog">
      <el-form inline :model="adminQuery">
        <el-form-item label="姓名：">
          <el-input v-model="adminQuery.realName" size="small" placeholder="请输入姓名或者账号" class="selWidth" @keyup.enter="searchAdmins" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" @click="searchAdmins">搜索</el-button>
          <el-button size="small" @click="resetAdminSearch">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="adminLoading" :data="adminRows" max-height="400px" size="small" border>
        <el-table-column label="" width="55">
          <template #default="{ row }">
            <el-radio :model-value="form.uid" :label="row.id" @change="selectAdmin(row)">&nbsp;</el-radio>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" sortable width="80" />
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="account" label="账号" min-width="120" />
        <el-table-column label="身份" min-width="230">
          <template #default="{ row }">
            <el-tag
              v-for="item in roleNameList(row.roleNames)"
              :key="item"
              size="small"
              type="info"
              class="mr5"
            >
              {{ item }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后登录时间" prop="lastTime" min-width="180">
          <template #default="{ row }">{{ formatTime(row.lastTime) || '-' }}</template>
        </el-table-column>
        <el-table-column label="最后登录IP" prop="lastIp" min-width="150">
          <template #default="{ row }">{{ row.lastIp || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" prop="status" min-width="100">
          <template #default="{ row }">{{ row.status ? '显示' : '隐藏' }}</template>
        </el-table-column>
        <el-table-column label="删除标记" prop="isDel" min-width="100">
          <template #default="{ row }">{{ row.isDel ? '是' : '否' }}</template>
        </el-table-column>
      </el-table>
      <div class="block">
        <el-pagination
          v-model:current-page="adminQuery.page"
          v-model:page-size="adminQuery.limit"
          :page-sizes="[20, 40, 60, 100]"
          layout="sizes, prev, pager, next, jumper"
          :total="adminTotal"
          @size-change="loadAdmins"
          @current-change="loadAdmins"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  storePointList,
  storeStaffDelete,
  storeStaffInfo,
  storeStaffList,
  storeStaffSave,
  storeStaffUpdate,
  systemAdminList
} from '../api';

const loading = ref(false);
const formLoading = ref(false);
const adminLoading = ref(false);
const dialogVisible = ref(false);
const adminDialogVisible = ref(false);
const tableData = ref([]);
const total = ref(0);
const storeSelectList = ref([]);
const adminRows = ref([]);
const adminTotal = ref(0);
const formRef = ref();

const query = reactive({
  page: 1,
  limit: 20,
  storeId: ''
});

const form = reactive(defaultForm());

const adminQuery = reactive({
  page: 1,
  limit: 20,
  status: true,
  realName: '',
  roles: ''
});

const rules = {
  uid: [{ required: true, message: '请选择管理员', trigger: 'change' }],
  storeId: [{ required: true, message: '请选择提货点地址', trigger: 'change' }],
  staffName: [{ required: true, message: '核销员名称不能为空', trigger: 'blur' }],
  phone: [{ pattern: /^1[3456789]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
};

function defaultForm() {
  return {
    id: 0,
    phone: '',
    storeId: '',
    uid: '',
    avatar: '',
    staffName: '',
    verifyStatus: 0,
    status: 1
  };
}

async function loadStores() {
  const data = await storePointList({ page: 1, limit: 9999, status: 1, keywords: '' });
  storeSelectList.value = data?.list || [];
}

async function loadList() {
  loading.value = true;
  try {
    const data = await storeStaffList({ ...query });
    tableData.value = data?.list || [];
    total.value = data?.total || 0;
  } finally {
    loading.value = false;
  }
}

function search() {
  query.page = 1;
  loadList();
}

async function openCreate() {
  Object.assign(form, defaultForm());
  dialogVisible.value = true;
}

async function openEdit(id) {
  dialogVisible.value = true;
  formLoading.value = true;
  try {
    const data = await storeStaffInfo({ id });
    Object.assign(form, defaultForm(), data || {});
  } finally {
    formLoading.value = false;
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该核销员吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch {
    return;
  }
  await storeStaffDelete({ id: row.id });
  ElMessage.success('删除成功');
  await loadList();
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  formLoading.value = true;
  try {
    if (form.id) {
      await storeStaffUpdate({ ...form });
      ElMessage.success('编辑成功');
    } else {
      const payload = { ...form };
      delete payload.id;
      await storeStaffSave(payload);
      ElMessage.success('提交成功');
    }
    dialogVisible.value = false;
    await loadList();
  } finally {
    formLoading.value = false;
  }
}

function resetForm() {
  formRef.value?.clearValidate();
  Object.assign(form, defaultForm());
}

function openAdminDialog() {
  adminDialogVisible.value = true;
  loadAdmins();
}

async function loadAdmins() {
  adminLoading.value = true;
  try {
    const data = await systemAdminList({ ...adminQuery });
    adminRows.value = data?.list || [];
    adminTotal.value = data?.total || 0;
  } finally {
    adminLoading.value = false;
  }
}

function searchAdmins() {
  adminQuery.page = 1;
  loadAdmins();
}

function resetAdminSearch() {
  adminQuery.realName = '';
  adminQuery.roles = '';
  searchAdmins();
}

function resetAdminDialog() {
  adminQuery.page = 1;
  adminQuery.realName = '';
  adminQuery.roles = '';
}

function selectAdmin(row) {
  form.avatar = row.account;
  form.uid = row.id;
  if (!form.staffName) {
    form.staffName = row.realName || row.account;
  }
  if (!form.phone && row.phone) {
    form.phone = row.phone;
  }
  adminDialogVisible.value = false;
}

function roleNameList(value) {
  return String(value || '').split(',').map((item) => item.trim()).filter(Boolean);
}

function formatTime(value) {
  if (!value) return '';
  return String(value).replace('T', ' ').slice(0, 19);
}

onMounted(async () => {
  await Promise.all([loadStores(), loadList()]);
});
</script>
