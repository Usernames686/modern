<template>
  <div class="divBox">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form :model="queryParams" inline>
          <el-form-item label="菜单名称：">
            <el-input v-model="queryParams.name" class="selWidth" placeholder="请输入菜单名称" clearable size="small" />
          </el-form-item>
          <el-form-item label="状态：">
            <el-select v-model="queryParams.menuType" class="selWidth" placeholder="菜单状态" clearable size="small">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery">搜索</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <el-row :gutter="10" class="mb20">
        <el-col :span="1.5">
          <el-button type="primary" plain @click="handleAdd(null)">新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="info" plain @click="toggleExpandAll">展开/折叠</el-button>
        </el-col>
      </el-row>

      <el-table
        v-if="refreshTable"
        v-loading="listLoading"
        :data="menuList"
        row-key="id"
        :default-expand-all="isExpandAll"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      >
        <el-table-column prop="name" label="菜单名称" show-overflow-tooltip width="160" />
        <el-table-column prop="icon" label="图标" width="100">
          <template #default="{ row }">
            <el-icon v-if="row.icon"><component :is="iconComponent(row.icon)" /></el-icon>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column prop="perms" label="权限标识" show-overflow-tooltip />
        <el-table-column prop="component" label="组件路径" show-overflow-tooltip />
        <el-table-column prop="isShow" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="boolValue(row.isShow) ? 'success' : 'danger'">{{ boolValue(row.isShow) ? '显示' : '隐藏' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" min-width="165">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="menuType" label="类型" width="80">
          <template #default="{ row }">
            <span v-if="row.menuType === 'M'" class="type_tag one">目录</span>
            <span v-else-if="row.menuType === 'C'" class="type_tag two">菜单</span>
            <span v-else class="type_tag three">按钮</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" class-name="small-padding fixed-width">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleUpdate(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button link type="primary" @click="handleAdd(row)">新增</el-button>
            <el-divider direction="vertical" />
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="open" :title="title" width="680px" append-to-body :close-on-click-modal="false" @closed="reset">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
          <el-row>
            <el-col :span="24">
              <el-form-item label="上级菜单：">
                <el-tree-select
                  v-model="form.pid"
                  :data="menuOptions"
                  :props="{ label: 'name', children: 'children', value: 'id' }"
                  node-key="id"
                  check-strictly
                  render-after-expand
                  placeholder="选择上级菜单"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="菜单类型：" prop="menuType">
                <el-radio-group v-model="form.menuType">
                  <el-radio value="M">目录</el-radio>
                  <el-radio value="C">菜单</el-radio>
                  <el-radio value="A">按钮</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col v-if="form.menuType !== 'A'" :span="24">
              <el-form-item label="菜单图标：">
                <div class="icon-input-row">
                  <el-input v-model="form.icon" placeholder="请选择菜单图标或输入图标名称">
                    <template #prefix>
                      <el-icon v-if="form.icon"><component :is="iconComponent(form.icon)" /></el-icon>
                    </template>
                  </el-input>
                  <el-button @click="iconDialogVisible = true">选择</el-button>
                  <el-button v-if="form.icon" @click="form.icon = ''">清除</el-button>
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="菜单名称：" prop="name">
                <el-input v-model="form.name" placeholder="请输入菜单名称" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="显示排序：" prop="sort">
                <el-input-number v-model="form.sort" controls-position="right" :min="0" />
              </el-form-item>
            </el-col>
            <el-col v-if="form.menuType !== 'A'" :span="24">
              <el-form-item label="组件路径：" prop="component">
                <el-input v-model="form.component" placeholder="请输入组件路径" />
              </el-form-item>
            </el-col>
            <el-col v-if="form.menuType !== 'M'" :span="24">
              <el-form-item label="权限字符：" prop="perms">
                <el-input v-model="form.perms" placeholder="请输入权限标识" maxlength="100" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="显示状态：" prop="isShow">
                <el-radio-group v-model="form.isShow">
                  <el-radio v-for="item in showStatus" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <template #footer>
          <el-button @click="cancel">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitForm">确定</el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="iconDialogVisible"
        title="选择图标"
        append-to-body
        width="620px"
        class="icon-picker-dialog"
      >
        <div class="icon-picker-grid">
          <button
            v-for="item in iconOptions"
            :key="item"
            type="button"
            class="icon-picker-item"
            :class="{ active: form.icon === item }"
            @click="selectIcon(item)"
          >
            <el-icon><component :is="iconComponent(item)" /></el-icon>
            <span>{{ item }}</span>
          </button>
        </div>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import * as Icons from '@element-plus/icons-vue';
import {
  systemMenuAdd,
  systemMenuDelete,
  systemMenuInfo,
  systemMenuList,
  systemMenuUpdate
} from '../api';

const listLoading = ref(false);
const saving = ref(false);
const menuList = ref([]);
const menuOptions = ref([]);
const menuDataList = ref([]);
const open = ref(false);
const iconDialogVisible = ref(false);
const title = ref('');
const isExpandAll = ref(false);
const refreshTable = ref(true);
const formRef = ref();

const queryParams = reactive({
  name: '',
  menuType: ''
});

const form = reactive(defaultForm());

const rules = {
  name: [{ required: true, message: '菜单名称不能为空', trigger: 'blur' }],
  sort: [{ required: true, message: '菜单顺序不能为空', trigger: 'blur' }],
  menuType: [{ required: true, message: '菜单类型不能为空', trigger: 'change' }],
  component: [{ validator: validateComponent, trigger: 'blur' }],
  perms: [{ validator: validatePerms, trigger: 'blur' }]
};

const statusOptions = [
  { value: 'M', label: '目录' },
  { value: 'C', label: '菜单' },
  { value: 'A', label: '按钮' }
];

const showStatus = [
  { label: '显示', value: true },
  { label: '隐藏', value: false }
];

const iconOptions = [
  'Menu',
  'Setting',
  'User',
  'Goods',
  'List',
  'Tickets',
  'HomeFilled',
  'DataAnalysis',
  'Money',
  'Bell',
  'ChatLineRound',
  'Picture',
  'ShoppingCart',
  'Shop',
  'Document',
  'Operation',
  'Tools',
  'Calendar',
  'Folder',
  'Grid',
  'Collection',
  'Promotion',
  'Wallet',
  'Van',
  'Lock',
  'Key',
  'Link',
  'Monitor',
  'UserFilled',
  'Memo'
];

onMounted(getList);

function defaultForm() {
  return {
    id: undefined,
    pid: 0,
    name: '',
    icon: '',
    menuType: 'M',
    sort: 0,
    isShow: true,
    component: '',
    perms: ''
  };
}

async function getList() {
  listLoading.value = true;
  try {
    const rows = await systemMenuList(queryParams);
    menuDataList.value = (rows || []).map((item) => ({
      ...item,
      isShow: boolValue(item.isShow),
      parentId: item.pid,
      children: []
    }));
    menuList.value = handleTree(menuDataList.value);
    getTreeselect();
  } finally {
    listLoading.value = false;
  }
}

function getTreeselect() {
  menuOptions.value = [
    {
      id: 0,
      name: '主类目',
      children: handleTree(menuDataList.value)
    }
  ];
}

function handleTree(data) {
  const map = new Map();
  const roots = [];
  for (const item of data || []) {
    map.set(item.id, { ...item, children: [] });
  }
  for (const item of map.values()) {
    if (item.pid && map.has(item.pid)) {
      map.get(item.pid).children.push(item);
    } else {
      roots.push(item);
    }
  }
  return sortTree(roots);
}

function sortTree(items) {
  return [...items]
    .sort((a, b) => (Number(b.sort || 0) - Number(a.sort || 0)) || (Number(a.id || 0) - Number(b.id || 0)))
    .map((item) => ({
      ...item,
      children: sortTree(item.children || [])
    }));
}

function cancel() {
  open.value = false;
  reset();
}

function reset() {
  Object.assign(form, defaultForm());
  iconDialogVisible.value = false;
  formRef.value?.clearValidate();
}

function handleQuery() {
  getList();
}

function resetQuery() {
  queryParams.name = '';
  queryParams.menuType = '';
  handleQuery();
}

function handleAdd(row) {
  reset();
  form.pid = row?.id || 0;
  open.value = true;
  title.value = '添加菜单';
}

function toggleExpandAll() {
  refreshTable.value = false;
  isExpandAll.value = !isExpandAll.value;
  nextTick(() => {
    refreshTable.value = true;
  });
}

async function handleUpdate(row) {
  reset();
  getTreeselect();
  const data = await systemMenuInfo(row.id);
  Object.assign(form, {
    ...defaultForm(),
    ...data,
    isShow: boolValue(data.isShow)
  });
  open.value = true;
  title.value = '修改菜单';
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    const payload = {
      id: form.id,
      pid: form.pid || 0,
      name: form.name,
      icon: form.icon,
      menuType: form.menuType,
      sort: form.sort,
      isShow: form.isShow,
      component: form.component,
      perms: form.perms
    };
    if (form.id !== undefined && form.id !== null) {
      await systemMenuUpdate(payload);
      ElMessage.success('修改成功');
    } else {
      delete payload.id;
      await systemMenuAdd(payload);
      ElMessage.success('新增成功');
    }
    open.value = false;
    await getList();
  } finally {
    saving.value = false;
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`删除名称为"${row.name}"的数据项？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch {
    return;
  }
  await systemMenuDelete(row.id);
  ElMessage.success('删除成功');
  await getList();
}

function validateComponent(_rule, value, callback) {
  if (form.menuType === 'C' && !String(value || '').trim()) {
    callback(new Error('菜单类型的组件路径不能为空'));
    return;
  }
  callback();
}

function validatePerms(_rule, value, callback) {
  if (form.menuType === 'A' && !String(value || '').trim()) {
    callback(new Error('按钮类型的权限标识不能为空'));
    return;
  }
  callback();
}

function selectIcon(name) {
  form.icon = name;
  iconDialogVisible.value = false;
}

function boolValue(value) {
  return value === true || value === 1;
}

function iconComponent(name) {
  if (!name) return Icons.Menu;
  const pascal = String(name)
    .split(/[-_]/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join('');
  return Icons[pascal] || Icons.Menu;
}

function formatTime(value) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
}
</script>

<style scoped>
.type_tag {
  display: inline-block;
  line-height: 22px;
  padding: 0 8px;
  border-radius: 3px;
  font-size: 12px;
}

.type_tag.one {
  color: #409eff;
  background: #ecf5ff;
}

.type_tag.two {
  color: #67c23a;
  background: #f0f9eb;
}

.type_tag.three {
  color: #909399;
  background: #f4f4f5;
}

.icon-input-row {
  display: flex;
  gap: 8px;
}

.icon-input-row :deep(.el-input) {
  flex: 1;
}

.icon-picker-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(116px, 1fr));
  gap: 10px;
  max-height: 420px;
  overflow: auto;
}

.icon-picker-item {
  min-width: 0;
  height: 68px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
  color: #606266;
  cursor: pointer;
  display: grid;
  place-items: center;
  gap: 4px;
}

.icon-picker-item:hover,
.icon-picker-item.active {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}

.icon-picker-item .el-icon {
  font-size: 22px;
}

.icon-picker-item span {
  max-width: 100%;
  padding: 0 6px;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
