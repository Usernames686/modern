<template>
  <div class="divBox">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form inline :model="query" size="small" @submit.prevent>
          <el-form-item label="身份搜索：">
            <el-select v-model="query.roles" placeholder="身份" clearable class="selWidth">
              <el-option v-for="item in searchRoleOptions" :key="item.id" :label="item.roleName" :value="String(item.id)" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态搜索：">
            <el-select v-model="query.status" placeholder="状态" clearable class="selWidth">
              <el-option label="开启" :value="true" />
              <el-option label="关闭" :value="false" />
            </el-select>
          </el-form-item>
          <el-form-item label="姓名账号：">
            <el-input v-model="query.realName" placeholder="姓名或者账号" clearable class="selWidth" @keyup.enter="search" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="search">搜索</el-button>
            <el-button size="small" @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <el-form inline @submit.prevent>
        <el-form-item>
          <el-button type="primary" @click="openEdit(null)">添加管理员</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="tableData" size="small">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="姓名" prop="realName" min-width="120" />
        <el-table-column label="账号" prop="account" min-width="120" />
        <el-table-column label="手机号" prop="phone" min-width="120">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="身份" min-width="230">
          <template #default="{ row }">
            <el-tag v-for="item in roleNameList(row.roleNames)" :key="item" size="small" type="info" class="mr5">
              {{ item }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后登录时间" prop="lastTime" min-width="180">
          <template #default="{ row }">{{ formatTime(row.lastTime) }}</template>
        </el-table-column>
        <el-table-column label="最后登录IP" prop="lastIp" min-width="150">
          <template #default="{ row }">{{ row.lastIp || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="110">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="true"
              :inactive-value="false"
              active-text="开启"
              inactive-text="关闭"
              @change="onchangeIsShow(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="是否接收短信" min-width="120">
          <template #default="{ row }">
            <el-switch
              v-model="row.isSms"
              :active-value="true"
              :inactive-value="false"
              active-text="开启"
              inactive-text="关闭"
              :disabled="!row.phone"
              @change="onchangeIsSms(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="删除标记" min-width="100">
          <template #default="{ row }">{{ row.isDel ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <template v-if="row.isDel">
              <span>-</span>
            </template>
            <template v-else>
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-divider direction="vertical" />
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="block">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.limit"
          :page-sizes="[20, 40, 60, 80, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑身份' : '创建身份'"
      destroy-on-close
      :close-on-click-modal="false"
      width="540px"
      @closed="resetDialog"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" @submit.prevent>
        <el-form-item label="管理员账号：" prop="account">
          <el-input v-model="form.account" placeholder="管理员账号" />
        </el-form-item>
        <el-form-item label="管理员密码：" prop="pwd">
          <el-input v-model="form.pwd" placeholder="管理员密码" clearable @input="handlerPwdInput" @clear="handlerPwdInput" />
        </el-form-item>
        <el-form-item v-if="form.pwd" label="确认密码" prop="repwd">
          <el-input v-model="form.repwd" placeholder="确认密码" clearable />
        </el-form-item>
        <el-form-item label="管理员姓名：" prop="realName">
          <el-input v-model="form.realName" placeholder="管理员姓名" />
        </el-form-item>
        <el-form-item label="管理员身份：" prop="roles">
          <el-select v-model="form.roles" placeholder="身份" clearable multiple style="width: 100%">
            <el-option v-for="item in roleOptions" :key="item.id" :label="item.roleName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号：" prop="phone">
          <el-input v-model="form.phone" type="text" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="状态：">
          <el-switch v-model="form.status" :active-value="true" :inactive-value="false" />
        </el-form-item>
        <el-form-item class="dialog-footer-inner">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submit">{{ form.id ? '更新' : '确定' }}</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  systemAdminDelete,
  systemAdminInfo,
  systemAdminList,
  systemAdminSave,
  systemAdminUpdate,
  systemAdminUpdateIsSms,
  systemAdminUpdateStatus,
  systemRoleList
} from '../api';

const query = reactive({
  page: 1,
  limit: 20,
  roles: '',
  status: '',
  realName: ''
});

const tableData = ref([]);
const total = ref(0);
const roleOptions = ref([]);
const searchRoleOptions = ref([]);
const loading = ref(false);
const dialogVisible = ref(false);
const saving = ref(false);
const formRef = ref();
const requirePassword = ref(true);

const form = reactive(defaultForm());

const rules = computed(() => ({
  account: [{ required: true, message: '请填管理员账号', trigger: ['blur', 'change'] }],
  pwd: requirePassword.value ? [{ required: true, message: '请填管理员密码', trigger: ['blur', 'change'] }] : [],
  repwd: form.pwd ? [{ validator: validateRepeatPassword, trigger: ['blur', 'change'] }] : [],
  realName: [{ required: true, message: '管理员姓名', trigger: ['blur', 'change'] }],
  roles: [{ required: true, message: '管理员身份', trigger: ['blur', 'change'] }],
  phone: [
    { required: true, message: '请输入手机号', trigger: ['blur', 'change'] },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确!', trigger: ['blur', 'change'] }
  ]
}));

onMounted(async () => {
  await Promise.all([loadRoles(), loadList()]);
});

function defaultForm() {
  return {
    id: 0,
    account: '',
    pwd: '',
    repwd: '',
    realName: '',
    roles: [],
    status: true,
    phone: '',
    isDel: false
  };
}

async function loadRoles() {
  const [allRoles, activeRoles] = await Promise.all([
    systemRoleList({ page: 1, limit: 100 }),
    systemRoleList({ page: 1, limit: 100, status: true })
  ]);
  searchRoleOptions.value = allRoles?.list || [];
  roleOptions.value = activeRoles?.list || [];
}

async function loadList() {
  loading.value = true;
  try {
    const params = { ...query };
    if (params.status === '') delete params.status;
    const data = await systemAdminList(params);
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

function resetSearch() {
  query.roles = '';
  query.status = '';
  query.realName = '';
  query.page = 1;
  loadList();
}

async function onchangeIsShow(row) {
  try {
    await systemAdminUpdateStatus({ id: row.id, status: row.status });
    ElMessage.success('修改成功');
  } catch (error) {
    row.status = !row.status;
    throw error;
  } finally {
    loadList();
  }
}

async function onchangeIsSms(row) {
  if (!row.phone) {
    ElMessage.warning('请先为管理员添加手机号!');
    row.isSms = false;
    return;
  }
  try {
    await systemAdminUpdateIsSms({ id: row.id });
    ElMessage.success('修改成功');
  } catch (error) {
    row.isSms = !row.isSms;
    throw error;
  } finally {
    loadList();
  }
}

async function openEdit(row) {
  Object.assign(form, defaultForm());
  requirePassword.value = !row;
  if (row?.id) {
    const data = await systemAdminInfo({ id: row.id });
    Object.assign(form, {
      id: data.id,
      account: data.account || '',
      realName: data.realName || '',
      roles: parseRoles(data.roles),
      status: data.status,
      phone: data.phone || '',
      isDel: data.isDel
    });
  }
  dialogVisible.value = true;
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('删除当前数据', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch {
    return;
  }
  await systemAdminDelete({ id: row.id });
  ElMessage.success('删除数据成功');
  if (tableData.value.length === 1 && query.page > 1) query.page -= 1;
  await loadList();
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    const payload = {
      id: form.id || undefined,
      account: form.account,
      pwd: form.pwd || '',
      realName: form.realName,
      roles: form.roles.join(','),
      status: form.status,
      phone: form.phone,
      isDel: form.isDel
    };
    if (form.id) {
      await systemAdminUpdate(payload);
      ElMessage.success('更新管理员成功');
    } else {
      await systemAdminSave(payload);
      ElMessage.success('创建管理员成功');
    }
    dialogVisible.value = false;
    await loadList();
  } finally {
    saving.value = false;
  }
}

function handlerPwdInput() {
  formRef.value?.clearValidate?.(['pwd', 'repwd']);
}

function validateRepeatPassword(_rule, value, callback) {
  if (form.pwd && !value) {
    callback(new Error('请再次输入密码'));
    return;
  }
  if (value !== form.pwd) {
    callback(new Error('两次输入密码不一致!'));
    return;
  }
  callback();
}

function resetDialog() {
  Object.assign(form, defaultForm());
  requirePassword.value = true;
  formRef.value?.clearValidate();
}

function parseRoles(roles) {
  if (!roles) return [];
  return String(roles)
    .split(',')
    .map((item) => Number.parseInt(item, 10))
    .filter((item) => Number.isFinite(item));
}

function roleNameList(names) {
  return String(names || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function formatTime(value) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
}
</script>
