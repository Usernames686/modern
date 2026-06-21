<template>
  <div class="divBox auth-crmeb-info">
    <el-card class="box-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>申请授权</span>
          <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadInfo">刷新</el-button>
        </div>
      </template>
      <el-alert
        class="mb14"
        type="info"
        :closable="false"
        show-icon
        title="当前展示系统版权与备案配置；外部授权检测需配置授权服务后启用。"
      />
      <el-descriptions :column="2" border>
        <el-descriptions-item label="商城名称">{{ info.siteName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="版本号">{{ info.appVersion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="授权状态">
          <el-tag type="success">{{ info.authStatus === 'local' ? '未连接授权服务' : info.authStatus || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="检测时间">{{ dateText(info.checkedAt) }}</el-descriptions-item>
        <el-descriptions-item label="版权公司">{{ info.companyName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备案号">
          <a v-if="info.icpNumberUrl" :href="info.icpNumberUrl" target="_blank" rel="noreferrer">{{ info.icpNumber || info.icpNumberUrl }}</a>
          <span v-else>{{ info.icpNumber || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="网安备案">
          <a v-if="info.internetRecordUrl" :href="info.internetRecordUrl" target="_blank" rel="noreferrer">{{ info.internetRecord || info.internetRecordUrl }}</a>
          <span v-else>{{ info.internetRecord || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="版权图片">
          <el-image v-if="info.companyImage" class="company-image" :src="info.companyImage" :preview-src-list="[info.companyImage]" preview-teleported />
          <span v-else>无</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="box-card mt14" shadow="never">
      <template #header>服务说明</template>
      <el-table :data="rows" border size="small">
        <el-table-column prop="name" label="项目" width="180" />
        <el-table-column prop="value" label="说明" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { Refresh } from '@element-plus/icons-vue';
import { copyrightInfo } from '../api';

const loading = ref(false);
const info = ref({});
const rows = [
  { name: '页面路径', value: '/maintain/authCRMEB' },
  { name: '接口路径', value: '/api/admin/copyright/get/info' },
  { name: '数据来源', value: '系统配置中的站点、版权公司、备案与版本信息' },
  { name: '授权检测', value: '未配置授权服务时仅展示本地配置，不发起外部检测' }
];

onMounted(loadInfo);

async function loadInfo() {
  loading.value = true;
  try {
    info.value = await copyrightInfo();
  } finally {
    loading.value = false;
  }
}

function dateText(value) {
  return value ? String(value).replace('T', ' ') : '-';
}
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.company-image {
  width: 120px;
  max-height: 56px;
  border-radius: 4px;
}
</style>
