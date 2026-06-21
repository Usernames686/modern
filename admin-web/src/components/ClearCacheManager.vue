<template>
  <div class="divBox clear-cache-manager">
    <el-card class="box-card" shadow="never">
      <el-button type="primary" :icon="Delete" :loading="clearing" @click="handleClear">清除缓存</el-button>
      <span v-if="lastResult" class="clear-cache-result">{{ lastResult }}</span>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Delete } from '@element-plus/icons-vue';
import { clearSystemConfigCache } from '../api';

const clearing = ref(false);
const lastResult = ref('');

async function handleClear() {
  clearing.value = true;
  try {
    const result = await clearSystemConfigCache();
    lastResult.value = result || '清除成功';
    ElMessage.success(lastResult.value);
  } finally {
    clearing.value = false;
  }
}
</script>

<style scoped>
.clear-cache-result {
  margin-left: 12px;
  color: #606266;
  font-size: 13px;
}
</style>
