<template>
  <div class="mobile-delivery-shell">
    <div class="mobile-delivery-page">
      <header>
        <div class="order-num acea-row row-between-wrapper">
          <div class="num line1">订单号：{{ orderNo }}</div>
          <div class="name line1"><span class="contact">●</span>{{ delivery.realName || '-' }}</div>
        </div>
        <div class="address">
          <div class="name">{{ delivery.realName || '-' }}<span class="phone">{{ delivery.userPhone || '' }}</span></div>
          <div>{{ delivery.userAddress || '暂无收货地址' }}</div>
        </div>
        <div class="line"><img src="../assets/imgs/line.jpg" alt="" /></div>
      </header>

      <div class="wrapper">
        <div class="item acea-row row-between-wrapper">
          <div>发货方式</div>
          <div class="mode acea-row row-middle row-right">
            <button v-for="(item, index) in types" :key="item.type" class="goods" :class="{ on: active === index }" type="button" @click="changeType(item, index)">
              {{ item.title }}
            </button>
          </div>
        </div>
        <div v-show="active === 0" class="list">
          <div class="item acea-row row-between-wrapper">
            <div>发货方式</div>
            <select v-model="expressCode" class="mode">
              <option value="">选择快递公司</option>
              <option v-for="item in express" :key="item.code" :value="item.code">{{ item.name }}</option>
            </select>
          </div>
          <div class="item acea-row row-between-wrapper">
            <div>快递单号</div>
            <input v-model="expressNumber" type="text" placeholder="填写快递单号" class="mode" />
          </div>
        </div>
        <div v-show="active === 1" class="list">
          <div class="item acea-row row-between-wrapper">
            <div>送货人</div>
            <input v-model="deliveryName" type="text" placeholder="填写送货人" class="mode" />
          </div>
          <div class="item acea-row row-between-wrapper">
            <div>送货电话</div>
            <input v-model="deliveryTel" type="text" placeholder="填写送货电话" class="mode" />
          </div>
        </div>
      </div>

      <div class="mobile-detail-footer-space"></div>
      <button class="confirm" type="button" :disabled="saving" @click="saveInfo">确认提交</button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { expressList, orderInfo, orderSend } from '../api';

const types = [
  { type: '1', title: '发货' },
  { type: '2', title: '送货' },
  { type: '3', title: '无需发货' }
];

const delivery = reactive({});
const express = ref([]);
const active = ref(0);
const type = ref('1');
const deliveryName = ref('');
const deliveryTel = ref('');
const expressCode = ref('');
const expressNumber = ref('');
const saving = ref(false);
const orderNo = computed(() => window.location.pathname.split('/').filter(Boolean)[2] || '');

function changeType(item, index) {
  active.value = index;
  type.value = item.type;
  deliveryName.value = '';
  deliveryTel.value = '';
  expressCode.value = '';
  expressNumber.value = '';
}

async function loadOrder() {
  const data = await orderInfo({ orderNo: orderNo.value });
  Object.assign(delivery, data || {});
}

async function loadExpress() {
  const data = await expressList({ page: 1, limit: 999, isShow: 1 });
  express.value = Array.isArray(data?.list) ? data.list : [];
}

async function saveInfo() {
  const payload = { type: type.value, orderNo: orderNo.value };
  if (type.value === '1') {
    if (!expressCode.value) {
      ElMessage.error('请输入快递公司');
      return;
    }
    if (!expressNumber.value) {
      ElMessage.error('请输入快递单号');
      return;
    }
    const company = express.value.find((item) => item.code === expressCode.value);
    Object.assign(payload, {
      expressRecordType: 1,
      expressCode: expressCode.value,
      expressName: company?.name || '',
      expressNumber: expressNumber.value
    });
  } else if (type.value === '2') {
    if (!deliveryName.value.trim()) {
      ElMessage.error('请填写送货人');
      return;
    }
    if (!deliveryTel.value.trim()) {
      ElMessage.error('请填写送货电话');
      return;
    }
    Object.assign(payload, { deliveryName: deliveryName.value.trim(), deliveryTel: deliveryTel.value.trim() });
  }
  saving.value = true;
  try {
    await orderSend(payload);
    ElMessage.success('发送货成功');
    navigate(`/javaMobile/orderDetail/${orderNo.value}`);
  } finally {
    saving.value = false;
  }
}

function navigate(path) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path } }));
}

onMounted(async () => {
  await Promise.all([loadOrder(), loadExpress()]);
});
</script>
