<template>
  <div class="divBox distribution-config">
    <el-card class="box-card" shadow="never">
      <el-form
        ref="formRef"
        v-loading="loading"
        :model="form"
        :rules="rules"
        label-width="200px"
        class="demo-promoterForm"
      >
        <el-form-item label="分销启用：" prop="brokerageFuncStatus">
          <el-radio-group v-model="form.brokerageFuncStatus">
            <el-radio label="1">开启</el-radio>
            <el-radio label="0">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="满额分销最低金额：" prop="storeBrokerageQuota">
          <el-input-number
            v-model="form.storeBrokerageQuota"
            controls-position="right"
            :min="-1"
            :step="1"
            class="selWidth"
          />
        </el-form-item>
        <el-form-item label="分销关系绑定：" prop="brokerageBindind">
          <el-radio-group v-model="form.brokerageBindind">
            <el-radio label="0">所有用户</el-radio>
            <el-radio label="1">新用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分销气泡：" prop="storeBrokerageIsBubble">
          <el-radio-group v-model="form.storeBrokerageIsBubble">
            <el-radio label="1">开启</el-radio>
            <el-radio label="0">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="一级返佣比例：" prop="storeBrokerageRatio">
          <el-input-number
            v-model="form.storeBrokerageRatio"
            controls-position="right"
            :min="0"
            :max="100"
            :step="1"
            step-strictly
            class="selWidth"
          />
          <span class="unit">%</span>
        </el-form-item>
        <el-form-item label="二级返佣比例：" prop="storeBrokerageTwo">
          <el-input-number
            v-model="form.storeBrokerageTwo"
            controls-position="right"
            :min="0"
            :max="100"
            :step="1"
            step-strictly
            class="selWidth"
          />
          <span class="unit">%</span>
        </el-form-item>
        <el-form-item label="提现最低金额：" prop="userExtractMinPrice">
          <el-input-number
            v-model="form.userExtractMinPrice"
            controls-position="right"
            :min="0"
            :step="1"
            class="selWidth"
          />
        </el-form-item>
        <el-form-item label="提现银行卡：" prop="userExtractBank">
          <el-input
            v-model="form.userExtractBank"
            type="textarea"
            :rows="4"
            placeholder="提现银行卡，每个银行换行"
            class="textarea-width"
          />
        </el-form-item>
        <el-form-item label="冻结时间：" prop="extractTime">
          <el-input-number
            v-model="form.extractTime"
            controls-position="right"
            :min="0"
            class="selWidth"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submit">提交</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { distributionConfigInfo, distributionConfigSave } from '../api';

const formRef = ref();
const loading = ref(false);
const saving = ref(false);
const form = reactive({
  brokerageFuncStatus: '0',
  storeBrokerageQuota: -1,
  brokerageBindind: '0',
  storeBrokerageIsBubble: '0',
  storeBrokerageRatio: 0,
  storeBrokerageTwo: 0,
  userExtractMinPrice: 0,
  userExtractBank: '',
  extractTime: 0
});

const rules = {
  brokerageFuncStatus: [{ required: true, message: '请选择是否启用分销', trigger: 'change' }],
  storeBrokerageRatio: [{ required: true, message: '请输入一级返佣比例', trigger: 'blur' }],
  storeBrokerageTwo: [{ required: true, message: '请输入二级返佣比例', trigger: 'blur' }],
  userExtractBank: [{ required: true, message: '请输入提现银行卡', trigger: 'blur' }]
};

onMounted(loadInfo);

async function loadInfo() {
  loading.value = true;
  try {
    const data = await distributionConfigInfo();
    Object.assign(form, {
      brokerageFuncStatus: String(data?.brokerageFuncStatus ?? '0'),
      storeBrokerageQuota: Number(data?.storeBrokerageQuota ?? -1),
      brokerageBindind: String(data?.brokerageBindind ?? '0'),
      storeBrokerageIsBubble: String(data?.storeBrokerageIsBubble ?? '0'),
      storeBrokerageRatio: Number(data?.storeBrokerageRatio ?? 0),
      storeBrokerageTwo: Number(data?.storeBrokerageTwo ?? 0),
      userExtractMinPrice: Number(data?.userExtractMinPrice ?? 0),
      userExtractBank: data?.userExtractBank || '',
      extractTime: Number(data?.extractTime ?? 0)
    });
  } finally {
    loading.value = false;
  }
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  if (Number(form.storeBrokerageRatio) + Number(form.storeBrokerageTwo) > 100) {
    ElMessage.warning('返佣比例相加不能超过100%');
    return;
  }
  saving.value = true;
  try {
    await distributionConfigSave({
      ...form,
      brokerageFuncStatus: Number(form.brokerageFuncStatus),
      brokerageBindind: Number(form.brokerageBindind),
      storeBrokerageIsBubble: Number(form.storeBrokerageIsBubble)
    });
    ElMessage.success('提交成功');
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.selWidth {
  width: 300px;
}

.textarea-width {
  max-width: 520px;
}

.unit {
  margin-left: 8px;
}
</style>
