<template>
  <div class="divBox">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form inline :model="query" size="small" @submit.prevent>
          <el-form-item label="昵称搜索：">
            <el-input v-model="query.roleName" placeholder="请输入角色昵称" clearable class="selWidth" @keyup.enter="search" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="search">搜索</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <el-form inline @submit.prevent>
        <el-form-item>
          <el-button type="primary" @click="openEdit(null)">添加角色</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="tableData"
        size="small"
        :header-cell-style="{ background: '#f8f8f9', color: '#515a6e' }"
      >
        <el-table-column label="角色编号" prop="id" width="120" />
        <el-table-column label="角色昵称" prop="roleName" min-width="130" />
        <el-table-column label="状态" prop="status" min-width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="true"
              :inactive-value="false"
              style="width: 40px"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" min-width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" prop="updateTime" min-width="170">
          <template #default="{ row }">{{ formatTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑身份' : '创建身份'"
      destroy-on-close
      :close-on-click-modal="false"
      width="500px"
      @closed="resetDialog"
    >
      <el-form ref="formRef" v-loading="dialogLoading" :model="form" :rules="rules" label-width="100px" @submit.prevent>
        <el-form-item label="角色名称：" prop="roleName">
          <el-input v-model="form.roleName" placeholder="身份名称" />
        </el-form-item>
        <el-form-item label="状态：">
          <el-switch v-model="form.status" :active-value="true" :inactive-value="false" />
        </el-form-item>
        <el-form-item label="菜单权限：" prop="rules">
          <div class="tree-tools">
            <el-checkbox v-model="menuExpand" @change="handleCheckedTreeExpand">展开/折叠</el-checkbox>
            <el-checkbox v-model="menuCheckStrictly">父子联动</el-checkbox>
          </div>
          <el-tree
            ref="menuTreeRef"
            class="tree-border"
            :data="menuOptions"
            show-checkbox
            node-key="id"
            :check-strictly="!menuCheckStrictly"
            empty-text="加载中，请稍候"
            :props="defaultProps"
          />
        </el-form-item>
        <div class="dialog-footer-inner pd-bt-20">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submit">
            {{ form.id ? '更新' : '确定' }}
          </el-button>
        </div>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  systemMenuCacheTree,
  systemRoleDelete,
  systemRoleInfo,
  systemRoleList,
  systemRoleSave,
  systemRoleUpdate,
  systemRoleUpdateStatus
} from '../api';

const query = reactive({
  page: 1,
  limit: 20,
  roleName: ''
});

const tableData = ref([]);
const total = ref(0);
const loading = ref(false);
const dialogVisible = ref(false);
const dialogLoading = ref(false);
const saving = ref(false);
const formRef = ref();
const menuTreeRef = ref();
const menuOptions = ref([]);
const menuExpand = ref(false);
const menuCheckStrictly = ref(true);

const form = reactive(defaultForm());
const defaultProps = {
  children: 'childList',
  label: 'name',
  disabled: 'disabled'
};

const rules = {
  roleName: [{ required: true, message: '请填写角色名称', trigger: ['blur', 'change'] }],
  rules: [{ validator: validateRules, trigger: 'change' }]
};

onMounted(loadList);

function defaultForm() {
  return {
    id: 0,
    roleName: '',
    rules: '',
    status: true
  };
}

async function loadList() {
  loading.value = true;
  try {
    const data = await systemRoleList(query);
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

async function openEdit(row) {
  Object.assign(form, defaultForm(), row ? { id: row.id, roleName: row.roleName, status: row.status } : {});
  dialogVisible.value = true;
  dialogLoading.value = true;
  try {
    if (row?.id) {
      const data = await systemRoleInfo(row.id);
      Object.assign(form, {
        id: data.id,
        roleName: data.roleName,
        status: data.status
      });
      menuOptions.value = markFixedMenus(data.menuList || []);
      await nextTick();
      setCheckedLeafKeys(menuOptions.value);
    } else {
      const data = await systemMenuCacheTree();
      menuOptions.value = markFixedMenus(data || []);
      await nextTick();
      setCheckedLeafKeys(menuOptions.value);
    }
  } finally {
    dialogLoading.value = false;
  }
}

async function handleStatusChange(row) {
  try {
    await systemRoleUpdateStatus({ id: row.id, status: row.status });
    ElMessage.success('更新状态成功');
  } catch (error) {
    row.status = !row.status;
    throw error;
  } finally {
    loadList();
  }
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
  await systemRoleDelete({ id: row.id });
  ElMessage.success('删除数据成功');
  if (tableData.value.length === 1 && query.page > 1) query.page -= 1;
  await loadList();
}

async function submit() {
  syncRulesFromTree();
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    const payload = {
      id: form.id || undefined,
      roleName: form.roleName,
      status: form.status,
      rules: form.rules
    };
    if (form.id) {
      await systemRoleUpdate(payload);
      ElMessage.success('更新身份成功');
    } else {
      await systemRoleSave(payload);
      ElMessage.success('创建身份成功');
    }
    dialogVisible.value = false;
    await loadList();
  } finally {
    saving.value = false;
  }
}

function syncRulesFromTree() {
  const checkedKeys = menuTreeRef.value?.getCheckedKeys?.() || [];
  const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys?.() || [];
  form.rules = [...new Set([...checkedKeys, ...halfCheckedKeys])].join(',');
}

function validateRules(_rule, _value, callback) {
  syncRulesFromTree();
  if (!form.rules) {
    callback(new Error('权限不能为空'));
    return;
  }
  callback();
}

function handleCheckedTreeExpand(value) {
  setExpand(menuOptions.value, value);
}

function setExpand(items, expanded) {
  for (const item of items || []) {
    const node = menuTreeRef.value?.getNode?.(item.id);
    if (node) node.expanded = expanded;
    setExpand(item.childList, expanded);
  }
}

function setCheckedLeafKeys(items) {
  const checkedLeafKeys = [];
  collectCheckedLeafKeys(items, checkedLeafKeys);
  menuTreeRef.value?.setCheckedKeys?.(checkedLeafKeys);
}

function collectCheckedLeafKeys(items, output) {
  for (const item of items || []) {
    if (item.checked && !item.childList?.length) output.push(item.id);
    collectCheckedLeafKeys(item.childList, output);
  }
}

function markFixedMenus(items) {
  return (items || []).map((item) => {
    const fixed = [1, 280, 294, 344].includes(Number(item.id));
    return {
      ...item,
      disabled: fixed || item.disabled,
      childList: markFixedMenus(item.childList).map((child) => ({
        ...child,
        disabled: fixed || child.disabled,
        checked: fixed ? true : child.checked
      })),
      checked: fixed ? true : item.checked
    };
  });
}

function resetDialog() {
  Object.assign(form, defaultForm());
  menuOptions.value = [];
  menuExpand.value = false;
  menuCheckStrictly.value = true;
  formRef.value?.clearValidate();
}

function formatTime(value) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
}
</script>

<style scoped>
.tree-tools {
  margin-bottom: 8px;
}

.tree-border {
  width: 100%;
  max-height: 360px;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px;
}

.pd-bt-20 {
  padding-bottom: 20px;
}

.dialog-footer-inner {
  text-align: right;
}
</style>
