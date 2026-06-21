<template>
  <div class="divBox integral-manager">
    <el-card v-if="view === 'config'" class="box-card" shadow="never">
      <el-form ref="configFormRef" :model="configForm" :rules="configRules" label-width="300px" class="integral-config-form">
        <el-form-item label="积分抵用比例(1积分抵多少金额)：" prop="integral_ratio">
          <el-input-number
            v-model="configForm.integral_ratio"
            :min="0"
            :max="99"
            :precision="1"
            :step="1"
            controls-position="right"
            class="from-ipt-width"
          />
        </el-form-item>
        <el-form-item label="下单赠送积分比例（实际支付1元赠送多少积分）：" prop="order_give_integral">
          <el-input-number
            v-model="configForm.order_give_integral"
            :min="0"
            :precision="0"
            :step="1"
            step-strictly
            controls-position="right"
            class="from-ipt-width"
          />
        </el-form-item>
        <el-form-item label="积分冻结时间(天)：" prop="freeze_integral_day">
          <el-input-number
            v-model="configForm.freeze_integral_day"
            :min="0"
            :max="99"
            :precision="0"
            :step="1"
            step-strictly
            controls-position="right"
            class="from-ipt-width"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="saveConfig">提交</el-button>
          <el-button @click="loadConfig">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <template v-else>
      <el-card :bordered="false" shadow="never" class="ivu-mt" :body-style="{ padding: 0 }">
        <div class="padding-add">
          <el-form inline size="small" label-width="75px" :model="query" @submit.prevent>
            <el-form-item label="时间选择：">
              <el-date-picker
                v-model="timeRange"
                value-format="YYYY-MM-DD"
                format="YYYY-MM-DD"
                type="daterange"
                placement="bottom-end"
                placeholder="自定义时间"
                style="width: 260px"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                @change="onChangeTime"
              />
            </el-form-item>
            <el-form-item label="微信昵称：">
              <el-input
                v-model="query.keywords"
                placeholder="请输入用户昵称"
                class="selWidth"
                clearable
                @keyup.enter="search"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
              <el-button :icon="Refresh" @click="resetLog">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-card>

      <el-card class="box-card mt14" shadow="never">
        <el-table v-loading="loading" :data="rows" size="small" class="table" highlight-current-row>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="title" label="标题" min-width="130" show-overflow-tooltip />
          <el-table-column prop="balance" label="积分余量" min-width="120" sortable />
          <el-table-column prop="integral" label="明细数字" min-width="120" sortable />
          <el-table-column prop="mark" label="备注" min-width="180" show-overflow-tooltip />
          <el-table-column prop="nickName" label="用户昵称" min-width="130" show-overflow-tooltip />
          <el-table-column label="添加时间" width="180">
            <template #default="{ row }">{{ dateText(row.updateTime) }}</template>
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
            @size-change="loadLogs"
            @current-change="loadLogs"
          />
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Refresh, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { integralRecordList, systemConfigInfo, systemConfigSaveForm } from '../api';

const props = defineProps({
  path: {
    type: String,
    default: ''
  }
});

const FORM_ID = 109;
const loading = ref(false);
const saving = ref(false);
const total = ref(0);
const rows = ref([]);
const timeRange = ref([]);
const configFormRef = ref();

const view = computed(() => (props.path === '/marketing/integral/integrallog' ? 'log' : 'config'));

const configForm = reactive({
  integral_ratio: 0,
  order_give_integral: 0,
  freeze_integral_day: 0
});

const configRules = {
  integral_ratio: [{ required: true, message: '请输入积分抵用比例', trigger: 'blur' }],
  order_give_integral: [{ required: true, message: '请输入下单赠送积分比例', trigger: 'blur' }],
  freeze_integral_day: [{ required: true, message: '请输入积分冻结时间', trigger: 'blur' }]
};

const query = reactive({
  page: 1,
  limit: 20,
  dateLimit: '',
  keywords: ''
});

onMounted(loadCurrent);
watch(() => props.path, loadCurrent);

function loadCurrent() {
  if (view.value === 'config') {
    loadConfig();
  } else {
    loadLogs();
  }
}

async function loadConfig() {
  loading.value = true;
  try {
    const data = await systemConfigInfo({ formId: FORM_ID });
    configForm.integral_ratio = numberValue(data?.integral_ratio, 1);
    configForm.order_give_integral = numberValue(data?.order_give_integral, 1);
    configForm.freeze_integral_day = numberValue(data?.freeze_integral_day, 0);
  } finally {
    loading.value = false;
  }
}

async function saveConfig() {
  const valid = await configFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    await systemConfigSaveForm({
      id: FORM_ID,
      sort: 0,
      status: true,
      fields: [
        field('integral_ratio', configForm.integral_ratio),
        field('order_give_integral', configForm.order_give_integral),
        field('freeze_integral_day', configForm.freeze_integral_day)
      ]
    });
    ElMessage.success('操作成功');
    await loadConfig();
  } finally {
    saving.value = false;
  }
}

async function loadLogs() {
  loading.value = true;
  try {
    const data = await integralRecordList(
      { page: query.page, limit: query.limit },
      {
        page: query.page,
        limit: query.limit,
        dateLimit: query.dateLimit,
        keywords: query.keywords
      }
    );
    rows.value = data?.list || [];
    total.value = Number(data?.total || 0);
  } finally {
    loading.value = false;
  }
}

function search() {
  query.page = 1;
  loadLogs();
}

function resetLog() {
  query.page = 1;
  query.limit = 20;
  query.dateLimit = '';
  query.keywords = '';
  timeRange.value = [];
  loadLogs();
}

function onChangeTime(value) {
  query.dateLimit = value?.length === 2 ? value.join(',') : '';
  query.page = 1;
  loadLogs();
}

function field(name, value) {
  return {
    name,
    title: name,
    value: String(value ?? '')
  };
}

function numberValue(value, fallback) {
  const number = Number(value);
  return Number.isFinite(number) ? number : fallback;
}

function dateText(value) {
  if (!value) return '-';
  return String(value).replace('T', ' ');
}
</script>

<style scoped>
.integral-config-form {
  max-width: 760px;
}

.from-ipt-width {
  width: 220px;
}
</style>
