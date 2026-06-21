<template>
  <div class="divBox">
    <el-card shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
      <div class="padding-add">
        <el-form inline size="small" :model="query" @submit.prevent>
          <el-form-item label="规格搜索：">
            <el-input
              v-model="query.keywords"
              placeholder="请输入商品规格"
              class="selWidth"
              clearable
              @keyup.enter="searchList"
            />
          </el-form-item>
          <el-button type="primary" @click="searchList">搜索</el-button>
        </el-form>
      </div>
    </el-card>

    <el-card class="box-card mt14">
      <template #header>
        <div class="clearfix">
          <el-button type="primary" size="small" @click="openCreate">添加商品规格</el-button>
          <el-button size="small" :disabled="!selectedRows.length" @click="removeSelected">批量删除</el-button>
        </div>
      </template>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="tableData"
        style="width: 100%"
        size="small"
        highlight-current-row
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column prop="ruleName" label="规格名称" min-width="150" />
        <el-table-column label="商品规格" min-width="150">
          <template #default="{ row }">
            <span v-for="(item, index) in row.ruleValueParsed" :key="index" class="mr10">{{ item.value }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商品属性" min-width="300">
          <template #default="{ row }">
            <div v-for="(item, index) in row.ruleValueParsed" :key="index">{{ (item.detail || []).join(',') }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button link type="danger" @click="remove(row.id)">删除</el-button>
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
      v-model="dialog.visible"
      :title="dialog.mode === 'create' ? '添加商品规格' : '编辑商品规格'"
      width="760px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" v-loading="saving" :model="form" :rules="rules" class="attrFrom mb20" label-width="90px">
        <el-row :gutter="24">
          <el-col :span="10">
            <el-form-item label="规格名称：" prop="ruleName">
              <el-input class="selWidth" maxlength="20" v-model="form.ruleName" placeholder="请输入标题名称" />
            </el-form-item>
          </el-col>
          <el-col v-for="(item, index) in form.ruleValue" :key="index" :span="24" class="noForm">
            <el-form-item>
              <div class="acea-row row-middle">
                <span class="mr5">{{ item.value }}</span>
                <el-icon class="rule-remove-icon" @click="removeRuleName(index)"><CircleClose /></el-icon>
              </div>
              <div class="rulesBox">
                <el-tag
                  v-for="(detail, detailIndex) in item.detail"
                  :key="detailIndex"
                  closable
                  size="large"
                  :disable-transitions="false"
                  class="mb5 mr10"
                  @close="removeRuleValue(item.detail, detailIndex)"
                >
                  {{ detail }}
                </el-tag>
                <el-input
                  v-if="item.inputVisible"
                  v-model="item.inputValue"
                  class="input-new-tag"
                  size="small"
                  @keyup.enter="createRuleValue(item)"
                  @blur="createRuleValue(item)"
                />
                <el-button v-else class="button-new-tag" size="small" @click="showInput(item)">+ 添加</el-button>
              </div>
            </el-form-item>
          </el-col>
          <el-col v-if="addingRule" :span="24" class="rule-create-row">
            <el-col :span="9">
              <el-form-item label="规格：">
                <el-input class="selWidth" v-model="newRuleName" placeholder="请输入规格" />
              </el-form-item>
            </el-col>
            <el-col :span="9">
              <el-form-item label="规格值：">
                <el-input class="selWidth" v-model="newRuleValue" placeholder="请输入规格值" />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-button @click="cancelAddRule">取消</el-button>
              <el-button type="primary" class="mr10" @click="createRuleName">确定</el-button>
            </el-col>
          </el-col>
        </el-row>
        <el-button v-if="!addingRule" type="primary" class="ml75" @click="addingRule = true">添加新规格</el-button>
      </el-form>

      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue';
import { CircleClose } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { productRuleDelete, productRuleList, productRuleSave, productRuleUpdate } from '../api';

const tableRef = ref();
const formRef = ref();
const loading = ref(false);
const saving = ref(false);
const addingRule = ref(false);
const selectedRows = ref([]);
const selectedAll = ref([]);
const tableData = ref([]);
const total = ref(0);
const newRuleName = ref('');
const newRuleValue = ref('');

const query = reactive({
  page: 1,
  limit: 20,
  keywords: ''
});

const dialog = reactive({
  visible: false,
  mode: 'create'
});

const form = reactive({
  id: 0,
  ruleName: '',
  ruleValue: []
});

const rules = {
  ruleName: [{ required: true, message: '请输入规格名称', trigger: 'blur' }]
};

function searchList() {
  query.page = 1;
  loadList();
}

async function loadList() {
  loading.value = true;
  try {
    const data = await productRuleList({
      page: query.page,
      limit: query.limit,
      keywords: query.keywords || undefined
    });
    tableData.value = (data?.list || []).map((item) => ({
      ...item,
      ruleValueParsed: parseRuleValue(item.ruleValue)
    }));
    total.value = data?.total || 0;
    await nextTick();
    restoreSelection();
  } finally {
    loading.value = false;
  }
}

function parseRuleValue(value) {
  if (Array.isArray(value)) {
    return normalizeRules(value);
  }
  if (!value) {
    return [];
  }
  try {
    return normalizeRules(JSON.parse(value));
  } catch {
    return [];
  }
}

function normalizeRules(list) {
  return (list || []).map((item) => ({
    value: item.value || '',
    detail: Array.isArray(item.detail) ? [...item.detail] : [],
    inputVisible: false,
    inputValue: ''
  }));
}

function handleSelectionChange(rows) {
  selectedRows.value = rows;
  const selectedIds = new Set(rows.map((row) => row.id));
  const currentIds = tableData.value.map((row) => row.id);
  selectedAll.value = selectedAll.value.filter((row) => !currentIds.includes(row.id) || selectedIds.has(row.id));
  rows.forEach((row) => {
    if (!selectedAll.value.some((item) => item.id === row.id)) {
      selectedAll.value.push(row);
    }
  });
}

function restoreSelection() {
  if (!selectedAll.value.length || !tableRef.value) return;
  const selectedIds = selectedAll.value.map((row) => row.id);
  tableRef.value.clearSelection();
  tableData.value.forEach((row) => {
    if (selectedIds.includes(row.id)) {
      tableRef.value.toggleRowSelection(row, true);
    }
  });
}

function openCreate() {
  resetForm();
  dialog.mode = 'create';
  dialog.visible = true;
}

function openEdit(row) {
  resetForm();
  dialog.mode = 'edit';
  Object.assign(form, {
    id: row.id,
    ruleName: row.ruleName,
    ruleValue: normalizeRules(row.ruleValueParsed)
  });
  dialog.visible = true;
}

function resetForm() {
  Object.assign(form, {
    id: 0,
    ruleName: '',
    ruleValue: []
  });
  addingRule.value = false;
  newRuleName.value = '';
  newRuleValue.value = '';
}

function closeDialog() {
  dialog.visible = false;
  resetForm();
}

function removeRuleName(index) {
  form.ruleValue.splice(index, 1);
}

function removeRuleValue(list, index) {
  list.splice(index, 1);
}

function showInput(item) {
  item.inputVisible = true;
  item.inputValue = '';
}

function createRuleValue(item) {
  const value = (item.inputValue || '').trim();
  if (!value) {
    item.inputVisible = false;
    return;
  }
  if (!item.detail.includes(value)) {
    item.detail.push(value);
  }
  item.inputVisible = false;
  item.inputValue = '';
}

function cancelAddRule() {
  addingRule.value = false;
  newRuleName.value = '';
  newRuleValue.value = '';
}

function createRuleName() {
  const name = newRuleName.value.trim();
  const value = newRuleValue.value.trim();
  if (!name || !value) {
    ElMessage.warning('请添加规格名称');
    return;
  }
  const existing = form.ruleValue.find((item) => item.value === name);
  if (existing) {
    if (!existing.detail.includes(value)) {
      existing.detail.push(value);
    }
  } else {
    form.ruleValue.push({
      value: name,
      detail: [value],
      inputVisible: false,
      inputValue: ''
    });
  }
  cancelAddRule();
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  if (!form.ruleValue.length) {
    ElMessage.warning('请至少添加一条属性规格！');
    return;
  }
  saving.value = true;
  try {
    const payload = {
      id: form.id || undefined,
      ruleName: form.ruleName,
      ruleValue: JSON.stringify(form.ruleValue.map((item) => ({
        value: item.value,
        detail: item.detail,
        inputVisible: false
      })))
    };
    if (dialog.mode === 'create') {
      await productRuleSave(payload);
    } else {
      await productRuleUpdate(payload);
    }
    ElMessage.success('提交成功');
    closeDialog();
    await loadList();
  } finally {
    saving.value = false;
  }
}

async function remove(ids) {
  await ElMessageBox.confirm('确定删除当前数据?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  });
  await productRuleDelete(ids);
  ElMessage.success('删除成功');
  if (tableData.value.length === 1 && query.page > 1) {
    query.page -= 1;
  }
  await loadList();
}

async function removeSelected() {
  if (!selectedAll.value.length) {
    ElMessage.warning('请选择商品规格');
    return;
  }
  const ids = selectedAll.value.map((item) => item.id).join(',');
  await remove(ids);
  selectedAll.value = [];
  selectedRows.value = [];
}

onMounted(loadList);
</script>
