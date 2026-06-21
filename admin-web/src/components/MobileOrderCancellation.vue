<template>
  <div class="mobile-cancel-shell">
    <div class="mobile-cancel-page">
      <div class="OrderCancellation">
        <div class="header"></div>
        <div class="whiteBg">
          <div class="input">
            <input v-model="verifyCode" type="number" placeholder="请输入核销码" @keyup.enter="storeCancellation" />
          </div>
          <div class="bnt" @click="storeCancellation">立即核销</div>
        </div>
        <div class="scan">
          <img src="../assets/imgs/scan.gif" alt="" @click="openQRCode" />
        </div>
      </div>

      <div v-show="dialogVisible">
        <div class="WriteOff">
          <div class="pictrue">
            <img :src="firstProductImage" alt="" />
          </div>
          <div class="num acea-row row-center-wrapper">
            {{ orderInfo?.orderId || '' }}
            <div class="views" @click="showDetail">查看<span class="views-jian">›</span></div>
          </div>
          <div class="tip">确定要核销此订单吗？</div>
          <div class="sure" @click="confirm">确定核销</div>
          <div class="sure cancel" @click="cancel">取消</div>
        </div>
        <div class="maskModel" @touchmove.prevent></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { orderWriteConfirm, orderWriteUpdate } from '../api';

const verifyCode = ref('');
const orderInfo = ref(null);
const dialogVisible = ref(false);

const productRows = computed(() => orderInfo.value?.storeOrderInfoVos || orderInfo.value?.productList || []);
const firstProductImage = computed(() => productRows.value?.[0]?.info?.image || '');

async function storeCancellation() {
  const code = String(verifyCode.value || '').trim();
  const ref = /^[0-9]{10}$/;
  if (!code) {
    ElMessage.error('请输入核销码');
    return;
  }
  if (!ref.test(code)) {
    ElMessage.error('请输入正确的核销码');
    return;
  }
  try {
    orderInfo.value = await orderWriteConfirm(code);
    dialogVisible.value = true;
  } catch (error) {
    verifyCode.value = '';
    ElMessage.error(error.message || '查询失败');
  }
}

async function confirm() {
  try {
    await orderWriteUpdate(String(verifyCode.value || '').trim());
    dialogVisible.value = false;
    orderInfo.value = null;
    verifyCode.value = '';
    ElMessage.success('核销成功');
  } catch (error) {
    ElMessage.error(error.message || '核销失败');
  }
}

function cancel() {
  dialogVisible.value = false;
}

function showDetail() {
  if (!orderInfo.value?.orderId) return;
  ElMessage.info(`订单号：${orderInfo.value.orderId}`);
}

function openQRCode() {
  ElMessage.warning('微信扫码能力会按老项目继续接入，当前请手动输入核销码。');
}
</script>
