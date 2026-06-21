<template>
  <div class="divBox">
    <el-card class="box-card">
      <el-table
        v-loading="loading"
        :data="tableData"
        size="small"
        class="table"
        row-key="cityId"
        highlight-current-row
        border
        lazy
        :load="loadChildren"
        :tree-props="{ children: 'child', hasChildren: 'hasChildren' }"
      >
        <el-table-column prop="cityId" label="编号" min-width="100" />
        <el-table-column prop="parentName" label="上级名称" min-width="100" />
        <el-table-column prop="name" min-width="250" label="地区名称" />
        <el-table-column fixed="right" width="80" label="操作">
          <template #default="{ row }">
            <el-button link type="primary" @click="editCity(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="编辑城市" width="540px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="95px">
        <el-form-item label="上级名称：">
          <el-input v-model="form.parentName" disabled />
        </el-form-item>
        <el-form-item label="地区名称：" prop="name">
          <el-input v-model="form.name" placeholder="请输入地区名称" maxlength="30" />
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
import { ElMessage, ElMessageBox } from 'element-plus';
import { cityInfo, cityList, cityUpdate } from '../api';

const loading = ref(false);
const saving = ref(false);
const tableData = ref([]);
const dialogVisible = ref(false);
const formRef = ref();
const editingRow = ref(null);

const form = reactive(defaultForm());

const rules = {
  name: [{ required: true, message: '请输入地区名称', trigger: 'blur' }]
};

onMounted(loadRoot);

function defaultForm() {
  return {
    id: undefined,
    parentId: 0,
    parentName: '中国',
    name: ''
  };
}

async function loadRoot() {
  loading.value = true;
  try {
    tableData.value = await fetchCities(0, '中国');
  } finally {
    loading.value = false;
  }
}

async function loadChildren(row, treeNode, resolve) {
  const rows = await fetchCities(row.cityId, row.name);
  resolve(rows);
}

async function fetchCities(parentId, parentName) {
  const rows = await cityList({ parentId });
  return (rows || []).map((item) => normalizeCity(item, parentName));
}

function normalizeCity(item, parentName) {
  return {
    ...item,
    parentName,
    hasChildren: Number(item.level) < 2,
    child: undefined
  };
}

async function editCity(row) {
  try {
    await ElMessageBox.confirm('修改此配置项？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch (error) {
    ElMessage.info('已取消');
    return;
  }
  const info = await cityInfo({ id: row.id });
  editingRow.value = row;
  Object.assign(form, {
    id: info.id,
    parentId: info.parentId,
    parentName: row.parentName || '中国',
    name: info.name || ''
  });
  dialogVisible.value = true;
}

async function submit() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    await cityUpdate({
      id: form.id,
      parentId: form.parentId,
      name: form.name
    });
    if (editingRow.value) {
      editingRow.value.name = form.name;
    }
    ElMessage.success('修改成功');
    dialogVisible.value = false;
  } finally {
    saving.value = false;
  }
}

function resetForm() {
  Object.assign(form, defaultForm());
  editingRow.value = null;
  formRef.value?.clearValidate?.();
}
</script>
