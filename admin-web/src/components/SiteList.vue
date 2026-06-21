<template>
  <div class="divBox site-list">
    <el-card class="box-card" shadow="never">
      <div class="filter-bar">
        <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadInfo">刷新</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" style="width: 100%" size="small" class="table" highlight-current-row>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="站点信息" min-width="220">
          <template #default="{ row }">
            <div class="site-cell">
              <el-image v-if="row.logo" class="site-logo" :src="row.logo" :preview-src-list="[row.logo]" preview-teleported />
              <div>
                <div>{{ row.siteName || '-' }}</div>
                <div class="muted">{{ row.seoTitle || row.siteUrl || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="siteUrl" label="网站地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="apiUrl" label="支付回调 API" min-width="180" show-overflow-tooltip />
        <el-table-column prop="frontApiUrl" label="移动商城 API" min-width="180" show-overflow-tooltip />
        <el-table-column prop="localUploadUrl" label="图片域名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="appVersion" label="版本号" width="100" />
        <el-table-column label="状态" width="110">
          <template #default>
            <el-tag type="success">本地站点</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="box-card mt14" shadow="never">
      <template #header>站点配置说明</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="后台左上角 Logo">
          <el-image v-if="info.siteLogoLeftTop" class="desc-logo" :src="info.siteLogoLeftTop" :preview-src-list="[info.siteLogoLeftTop]" preview-teleported />
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="登录页 Logo">
          <el-image v-if="info.siteLogoLogin" class="desc-logo" :src="info.siteLogoLogin" :preview-src-list="[info.siteLogoLogin]" preview-teleported />
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="备案号">{{ info.icpNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="版权公司">{{ info.companyName || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { Refresh } from '@element-plus/icons-vue';
import { copyrightInfo } from '../api';

const loading = ref(false);
const info = ref({});
const rows = computed(() => [
  {
    id: 1,
    siteName: info.value.siteName,
    seoTitle: info.value.seoTitle,
    siteUrl: info.value.siteUrl,
    apiUrl: info.value.apiUrl,
    frontApiUrl: info.value.frontApiUrl,
    localUploadUrl: info.value.localUploadUrl,
    appVersion: info.value.appVersion,
    logo: info.value.siteLogoLeftTop || info.value.siteLogoSquare || info.value.siteLogoLogin
  }
]);

onMounted(loadInfo);

async function loadInfo() {
  loading.value = true;
  try {
    info.value = await copyrightInfo();
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.filter-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 14px;
}

.site-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.site-logo {
  width: 42px;
  height: 42px;
  border-radius: 4px;
}

.desc-logo {
  width: 120px;
  max-height: 56px;
  border-radius: 4px;
}

.muted {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}
</style>
