<template>
  <el-dialog v-model="visible" class="list-Dialog" title="优惠劵" width="896px" @close="handleClose">
    <div class="divBox">
      <div class="header clearfix">
        <div class="container">
          <el-form inline size="small">
            <el-form-item label="优惠券名称：">
              <el-input v-model="query.keywords" placeholder="请输入优惠券名称" class="selWidth" size="small" />
              <el-button class="ml30" type="primary" size="small" @click="searchList">搜索</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
      <el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%"
        size="small"
        max-height="400"
        tooltip-effect="dark"
        highlight-current-row
        border
      >
        <el-table-column prop="id" label="ID" min-width="50" />
        <el-table-column prop="name" label="优惠券名称" min-width="90" show-overflow-tooltip />
        <el-table-column prop="money" label="优惠券面值" min-width="90" />
        <el-table-column label="最低消费额" min-width="90">
          <template #default="{ row }">
            <span>{{ Number(row.minPrice) === 0 ? '不限制' : row.minPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column label="有效期限" min-width="190">
          <template #default="{ row }">
            <span>{{ row.isFixedTime ? `${formatTime(row.useStartTime)} 一 ${formatTime(row.useEndTime)}` : '不限制' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="剩余数量" min-width="90">
          <template #default="{ row }">
            <span>{{ !row.isLimited ? '不限量' : row.lastTotal }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :loading="sendingId === row.id" @click="sendGrant(row)">发送</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="block mb20 acea-row fr">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.limit"
          :page-sizes="[10, 20, 30, 40]"
          layout="sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { couponReceive, couponSendList } from '../api';

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  userIds: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['update:modelValue', 'sent']);

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const sendingId = ref(null);
const query = reactive({
  page: 1,
  limit: 10,
  keywords: '',
  type: 1
});

watch(
  () => props.modelValue,
  (value) => {
    if (value) {
      query.page = 1;
      loadList();
    }
  }
);

function searchList() {
  query.page = 1;
  loadList();
}

async function loadList() {
  loading.value = true;
  try {
    const data = await couponSendList(query);
    tableData.value = data?.list || [];
    total.value = data?.total || 0;
  } finally {
    loading.value = false;
  }
}

async function sendGrant(row) {
  if (!props.userIds) {
    ElMessage.warning('请选择要设置的用户');
    return;
  }
  try {
    await ElMessageBox.confirm('发送优惠劵吗', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch {
    return;
  }
  sendingId.value = row.id;
  try {
    await couponReceive({ couponId: row.id, uid: props.userIds });
    ElMessage.success('发送成功');
    emit('sent');
    await loadList();
  } finally {
    sendingId.value = null;
  }
}

function handleClose() {
  visible.value = false;
}

function formatTime(value) {
  if (!value) return '';
  return String(value).replace('T', ' ').slice(0, 19);
}
</script>

<style scoped>
.list-Dialog :deep(.el-dialog__body) {
  padding: 20px 24px 0 24px !important;
}

.selWidth {
  width: 219px !important;
}

.fr {
  float: right;
}

.mb20 {
  margin-bottom: 20px;
}
</style>
