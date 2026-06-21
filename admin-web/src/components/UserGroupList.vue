<template>
  <div class="divBox">
    <el-card class="box-card">
      <template #header>
        <div class="clearfix">
          <el-button type="primary" size="small" @click="onAdd(null)">
            {{ isGroup ? '添加用户分组' : '添加用户标签' }}
          </el-button>
        </div>
      </template>

      <el-table v-loading="listLoading" :data="tableData" style="width: 100%" size="small">
        <el-table-column label="ID" min-width="80" prop="id" />
        <el-table-column :label="isGroup ? '分组名称' : '标签名称'" min-width="180">
          <template #default="{ row }">
            <span>{{ isGroup ? row.groupName : row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onAdd(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
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
          @size-change="getList"
          @current-change="getList"
        />
      </div>
    </el-card>

    <el-dialog v-model="visible" :title="dialogTitle" width="540px" :close-on-click-modal="false">
      <el-form ref="ruleFormRef" :rules="rules" :model="labelForm" label-width="75px" class="demo-dynamic">
        <el-form-item :label="isGroup ? '分组名称：' : '标签名称：'" prop="value">
          <el-input v-model="labelForm.value" :placeholder="isGroup ? '请填写分组名称' : '请填写标签名称'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  userGroupDelete,
  userGroupSave,
  userGroupUpdate,
  userGroups,
  userTagDelete,
  userTagSave,
  userTagUpdate,
  userTags
} from '../api';

const props = defineProps({
  type: {
    type: String,
    default: 'tag'
  }
});

const isGroup = computed(() => props.type === 'group');
const dialogTitle = computed(() => {
  const action = labelForm.id ? '编辑' : '添加';
  return isGroup.value ? `${action}分组名称` : `${action}标签名称`;
});

const query = reactive({
  page: 1,
  limit: 20
});
const tableData = ref([]);
const total = ref(0);
const listLoading = ref(false);
const visible = ref(false);
const saving = ref(false);
const ruleFormRef = ref();
const labelForm = reactive({
  id: '',
  value: ''
});

const rules = {
  value: [{ required: true, message: '请输入用户标签', trigger: 'blur' }]
};

watch(() => props.type, () => {
  query.page = 1;
  visible.value = false;
  getList();
});

onMounted(getList);

async function getList() {
  listLoading.value = true;
  try {
    const data = isGroup.value ? await userGroups(query) : await userTags(query);
    tableData.value = data?.list || [];
    total.value = data?.total || 0;
  } finally {
    listLoading.value = false;
  }
}

function onAdd(row) {
  if (row) {
    labelForm.id = row.id;
    labelForm.value = isGroup.value ? row.groupName : row.name;
  } else {
    labelForm.id = '';
    labelForm.value = '';
  }
  visible.value = true;
}

async function confirm() {
  const valid = await ruleFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    if (isGroup.value) {
      if (labelForm.id) {
        await userGroupUpdate({ id: labelForm.id }, { groupName: labelForm.value });
        ElMessage.success('编辑成功');
      } else {
        await userGroupSave({ groupName: labelForm.value });
        ElMessage.success('新增成功');
      }
    } else if (labelForm.id) {
      await userTagUpdate({ id: labelForm.id }, { name: labelForm.value });
      ElMessage.success('编辑成功');
    } else {
      await userTagSave({ name: labelForm.value });
      ElMessage.success('新增成功');
    }
    visible.value = false;
    getList();
  } finally {
    saving.value = false;
  }
}

async function handleDelete(id) {
  await ElMessageBox.confirm('删除吗？所有用户已经关联的数据都会清除', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  });
  if (isGroup.value) {
    await userGroupDelete({ id });
  } else {
    await userTagDelete({ id });
  }
  ElMessage.success('删除成功');
  if (tableData.value.length === 1 && query.page > 1) query.page -= 1;
  getList();
}
</script>
