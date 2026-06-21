<template>
  <div class="divBox">
    <el-card class="box-card">
      <template #header>
        <div class="config-category-header">
          <el-button type="primary" size="small" @click="openCreate({ id: 0, name: '顶层目录' })">添加分类</el-button>
          <el-alert
            title="温馨提示"
            type="success"
            description="添加一级分类以后，务必添加二级分类并配置表单，否则会出现渲染错误"
            :closable="false"
          />
        </div>
      </template>

      <el-table
        v-loading="loading"
        ref="treeListRef"
        :data="treeList"
        row-key="id"
        size="small"
        class="table"
        highlight-current-row
        :tree-props="{ children: 'child', hasChildren: 'hasChildren' }"
      >
        <el-table-column prop="name" label="分类昵称" min-width="300" />
        <el-table-column prop="url" label="英文名称" show-overflow-tooltip min-width="180" />
        <el-table-column prop="extra" label="已关联的表单" show-overflow-tooltip min-width="130">
          <template #default="{ row }">{{ row.extra || '-' }}</template>
        </el-table-column>
        <el-table-column label="启用状态" min-width="100">
          <template #default="{ row }">{{ row.status ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.pid > 0" @click="openCreate(row)">添加子目录</el-button>
            <el-divider direction="vertical" />
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button link type="primary" @click="openFormSelector(row)">配置列表</el-button>
            <el-divider direction="vertical" />
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="editDialog.visible"
      :title="editDialog.mode === 'create' ? '添加分类' : '编辑分类'"
      destroy-on-close
      :close-on-click-modal="false"
      width="540px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="95px">
        <el-form-item label="父级：">
          <el-cascader v-model="form.pid" :options="parentOptions" :props="categoryProps" disabled class="form-full" />
        </el-form-item>
        <el-form-item label="分类名称：" prop="name">
          <el-input v-model="form.name" placeholder="分类名称" />
        </el-form-item>
        <el-form-item label="英文名称：" prop="url">
          <el-input v-model="form.url" placeholder="URL" />
        </el-form-item>
        <el-form-item label="排序：" prop="sort">
          <el-input-number v-model="form.sort" controls-position="right" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="状态：">
          <el-switch v-model="form.status" :active-value="true" :inactive-value="false" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCategory">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="formSelector.visible" title="选择已配置的表单" width="720px" class="user-dialog">
      <div class="padding-add padding-24">
        <el-form inline :model="formQuery" @submit.prevent>
          <el-form-item label="关键字：">
            <el-input
              v-model="formQuery.keywords"
              placeholder="请输入id，名称，描述"
              clearable
              class="selWidth"
              size="small"
              @keyup.enter="searchForms"
            />
          </el-form-item>
          <el-button type="primary" size="small" @click="searchForms">搜索</el-button>
        </el-form>
      </div>
      <el-table
        v-loading="formLoading"
        :data="formRows"
        highlight-current-row
        size="small"
        class="table"
        @current-change="selectFormRow"
      >
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="名称" prop="name" min-width="180" />
        <el-table-column label="描述" prop="info" min-width="220" />
        <el-table-column label="更新时间" prop="updateTime" min-width="180" />
      </el-table>
      <div class="block-pagination">
        <el-pagination
          v-model:current-page="formQuery.page"
          v-model:page-size="formQuery.limit"
          :page-sizes="[10, 20, 40, 60]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="formTotal"
          background
          @size-change="loadForms"
          @current-change="loadForms"
        />
      </div>
      <template #footer>
        <el-button type="primary" class="form-selector-confirm" :disabled="!selectedForm?.id" @click="linkSelectedForm">关联</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { categoryDelete, categoryList, categorySave, categoryUpdate, systemFormTempList } from '../api';

const CONFIG_CATEGORY_TYPE = 6;

const loading = ref(false);
const saving = ref(false);
const formLoading = ref(false);
const formRef = ref();
const treeList = ref([]);
const formRows = ref([]);
const formTotal = ref(0);
const selectedForm = ref(null);

const editDialog = reactive({
  visible: false,
  mode: 'create',
  parent: null
});

const formSelector = reactive({
  visible: false,
  currentRow: null
});

const form = reactive(defaultCategoryForm());

const formQuery = reactive({
  page: 1,
  limit: 10,
  keywords: ''
});

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: ['blur', 'change'] }],
  url: [{ required: true, message: '英文名称不能为空', trigger: ['blur', 'change'] }],
  sort: [{ required: true, message: '请输入排序', trigger: ['blur', 'change'] }]
};

const categoryProps = {
  value: 'id',
  label: 'name',
  children: 'child',
  checkStrictly: true,
  emitPath: false
};

const parentOptions = computed(() => [
  { id: 0, name: '顶层目录', child: [] },
  ...addLabels(structuredCloneSafe(treeList.value))
]);

onMounted(loadCategories);

function defaultCategoryForm() {
  return {
    id: 0,
    extra: '',
    name: '',
    pid: 0,
    sort: 1,
    status: true,
    type: CONFIG_CATEGORY_TYPE,
    url: ''
  };
}

async function loadCategories() {
  loading.value = true;
  try {
    const data = await categoryList({ type: CONFIG_CATEGORY_TYPE, status: -1 });
    treeList.value = addLabels(buildTree(Array.isArray(data) ? data : data?.list || []));
  } catch (error) {
    ElMessage.error(error.message || '配置分类加载失败');
    treeList.value = [];
  } finally {
    loading.value = false;
  }
}

function openCreate(parent) {
  editDialog.mode = 'create';
  editDialog.parent = parent;
  Object.assign(form, defaultCategoryForm(), {
    pid: parent?.id || 0
  });
  editDialog.visible = true;
}

function openEdit(row) {
  editDialog.mode = 'edit';
  editDialog.parent = row;
  Object.assign(form, {
    id: row.id,
    extra: row.extra || '',
    name: row.name || '',
    pid: row.pid ?? 0,
    sort: row.sort ?? 1,
    status: Boolean(row.status),
    type: CONFIG_CATEGORY_TYPE,
    url: row.url || ''
  });
  editDialog.visible = true;
}

async function submitCategory() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    const payload = buildCategoryPayload();
    if (editDialog.mode === 'create') {
      await categorySave(payload);
      ElMessage.success('创建分类成功');
    } else {
      await categoryUpdate(payload);
      ElMessage.success('更新分类成功');
    }
    editDialog.visible = false;
    await loadCategories();
  } finally {
    saving.value = false;
  }
}

async function remove(row) {
  await ElMessageBox.confirm('删除当前数据?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  });
  await categoryDelete({ id: row.id });
  ElMessage.success('删除成功');
  await loadCategories();
}

async function openFormSelector(row) {
  formSelector.currentRow = row;
  selectedForm.value = null;
  formQuery.page = 1;
  formQuery.keywords = '';
  formSelector.visible = true;
  await loadForms();
}

async function loadForms() {
  formLoading.value = true;
  try {
    const data = await systemFormTempList({
      page: formQuery.page,
      limit: formQuery.limit,
      keywords: formQuery.keywords || undefined
    });
    formRows.value = data?.list || [];
    formTotal.value = data?.total || 0;
  } finally {
    formLoading.value = false;
  }
}

function searchForms() {
  formQuery.page = 1;
  loadForms();
}

function selectFormRow(row) {
  selectedForm.value = row || null;
}

async function linkSelectedForm() {
  if (!formSelector.currentRow || !selectedForm.value?.id) return;
  const row = formSelector.currentRow;
  await categoryUpdate({
    id: row.id,
    extra: String(selectedForm.value.id),
    name: row.name,
    pid: row.pid,
    sort: row.sort ?? 1,
    status: Boolean(row.status),
    type: CONFIG_CATEGORY_TYPE,
    url: row.url || ''
  });
  ElMessage.success('关联表单成功');
  formSelector.visible = false;
  await loadCategories();
}

function buildCategoryPayload() {
  return {
    id: form.id || undefined,
    extra: form.extra || '',
    name: form.name,
    pid: Array.isArray(form.pid) ? form.pid[form.pid.length - 1] : form.pid,
    sort: form.sort,
    status: form.status,
    type: CONFIG_CATEGORY_TYPE,
    url: form.url || ''
  };
}

function addLabels(list) {
  return (list || []).map((item) => ({
    ...item,
    label: item.name,
    child: addLabels(item.child || [])
  }));
}

function buildTree(list) {
  const map = new Map();
  for (const item of list || []) {
    map.set(item.id, { ...item, child: [] });
  }
  const roots = [];
  for (const item of map.values()) {
    const parent = map.get(item.pid);
    if (parent) {
      parent.child.push(item);
    } else {
      roots.push(item);
    }
  }
  return sortTree(roots);
}

function sortTree(list) {
  return (list || [])
    .map((item) => ({ ...item, child: sortTree(item.child) }))
    .sort((left, right) => (Number(right.sort || 0) - Number(left.sort || 0)) || (Number(left.id || 0) - Number(right.id || 0)));
}

function structuredCloneSafe(value) {
  return JSON.parse(JSON.stringify(value || []));
}
</script>

<style scoped>
.config-category-header {
  display: grid;
  gap: 12px;
}

.form-full {
  width: 100%;
}

.padding-24 {
  padding: 0 0 12px;
}

.form-selector-confirm {
  width: 100%;
}
</style>
