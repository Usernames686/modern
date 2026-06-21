<template>
  <div class="mobile-order-detail-shell">
    <div class="mobile-order-detail-page">
      <section class="mobile-order-detail-header acea-row row-middle">
        <div class="state">{{ title }}</div>
        <div class="data">
          <div class="order-num">订单：{{ orderInfo.orderId || orderNo }}</div>
          <div><span class="time">{{ formatTime(orderInfo.createTime) }}</span></div>
        </div>
      </section>

      <button v-if="!looksOnly" class="mobile-order-remarks acea-row row-between-wrapper" type="button" @click="openChange(1)">
        <span class="flag">⚑</span>
        <span class="line1">{{ orderInfo.remark || '订单未备注，点击添加备注信息' }}</span>
      </button>

      <div class="mobile-order-user acea-row row-middle"><span class="contact">●</span>{{ orderInfo.realName || '-' }}</div>
      <div class="mobile-order-address">
        <div class="name">{{ orderInfo.realName || '-' }}<span class="phone">{{ orderInfo.userPhone || '' }}</span></div>
        <div>{{ orderInfo.userAddress || '暂无收货地址' }}</div>
      </div>
      <div class="mobile-order-line"><img src="../assets/imgs/line.jpg" alt="" /></div>

      <div class="mobile-order-detail-goods">
        <div v-for="item in productRows" :key="item.id || item.unique" class="goods acea-row row-between-wrapper">
          <div class="picTxt acea-row row-between-wrapper">
            <div class="pictrue"><img :src="item.info?.image || fallbackImage" alt="" /></div>
            <div class="text">
              <div class="info line2">{{ item.info?.productName || '-' }}</div>
              <div class="attr overflow">{{ item.info?.sku || '' }}</div>
            </div>
          </div>
          <div class="money">
            <div class="x-money">￥{{ money(item.info?.price) }}</div>
            <div class="num">x{{ item.info?.payNum || 1 }}</div>
          </div>
        </div>
      </div>

      <div class="mobile-order-total">
        共{{ orderInfo.totalNum || 1 }}件商品，应支付 <span class="money">￥{{ money(orderInfo.payPrice) }}</span>
        ( 邮费 ¥{{ money(orderInfo.payPostage) }})
      </div>

      <section class="mobile-detail-wrapper">
        <div class="item acea-row row-between-wrapper">
          <div>订单编号：</div>
          <div class="conter acea-row row-middle row-right">{{ orderInfo.orderId }}<button class="copy" type="button" @click="copy(orderInfo.orderId)">复制</button></div>
        </div>
        <div class="item acea-row row-between-wrapper"><div>下单时间：</div><div class="conter">{{ formatTime(orderInfo.createTime) }}</div></div>
        <div class="item acea-row row-between-wrapper"><div>支付状态：</div><div class="conter">{{ title }}</div></div>
        <div class="item acea-row row-between-wrapper"><div>支付方式：</div><div class="conter">{{ orderInfo.payTypeStr || '-' }}</div></div>
        <div class="item acea-row row-between-wrapper"><div>买家留言：</div><div class="conter">{{ orderInfo.mark || '-' }}</div></div>
      </section>

      <section class="mobile-detail-wrapper">
        <div class="item acea-row row-between-wrapper"><div>支付金额：</div><div class="conter">￥{{ money(orderInfo.totalPrice) }}</div></div>
        <div class="item acea-row row-between-wrapper"><div>优惠券抵扣：</div><div class="conter">-￥{{ money(orderInfo.couponPrice) }}</div></div>
        <div class="item acea-row row-between-wrapper"><div>运费：</div><div class="conter">￥{{ money(orderInfo.payPostage) }}</div></div>
        <div class="actualPay acea-row row-right">实付款：<span class="money font-color-red">￥{{ money(orderInfo.payPrice) }}</span></div>
      </section>

      <section v-if="orderInfo.deliveryType" class="mobile-detail-wrapper">
        <div class="item acea-row row-between-wrapper"><div>配送方式：</div><div class="conter">{{ deliveryTypeText }}</div></div>
        <div v-if="orderInfo.deliveryType !== 'noNeed'" class="item acea-row row-between-wrapper">
          <div>{{ orderInfo.deliveryType === 'express' ? '快递公司：' : '送货人：' }}</div>
          <div class="conter">{{ orderInfo.deliveryName || '-' }}</div>
        </div>
        <div v-if="orderInfo.deliveryType !== 'noNeed'" class="item acea-row row-between-wrapper">
          <div>{{ orderInfo.deliveryType === 'express' ? '快递单号：' : '送货人电话：' }}</div>
          <div class="conter">{{ orderInfo.deliveryId || '-' }}<button v-if="orderInfo.deliveryId" class="copy" type="button" @click="copy(orderInfo.deliveryId)">复制</button></div>
        </div>
      </section>

      <div class="mobile-detail-footer-space"></div>
      <footer v-if="!looksOnly" class="mobile-detail-footer acea-row row-right row-middle">
        <button v-if="types === 'unPaid'" class="bnt cancel" type="button" @click="openChange(0)">一键改价</button>
        <button v-if="types === 'refunding'" class="bnt cancel" type="button" @click="openChange(2)">立即退款</button>
        <button class="bnt cancel" type="button" @click="openChange(1)">订单备注</button>
        <button v-if="canDelivery" class="bnt delivery" type="button" @click="navigate(`/javaMobile/orderDelivery/${orderInfo.orderId}/${orderInfo.id}`)">去发货</button>
        <button v-if="canWriteOff" class="bnt delivery" type="button" @click="navigate('/javaMobile/orderCancellation')">去核销</button>
      </footer>

      <div v-if="changeVisible" class="mobile-price-change on">
        <div class="priceTitle">
          {{ modalTitle }}
          <button class="close" type="button" @click="closeChange">x</button>
        </div>
        <div v-if="modalStatus === 0 || modalStatus === 2" class="listChange">
          <div v-if="orderInfo.refundStatus === 0" class="item acea-row row-between-wrapper"><div>商品总价(¥)</div><div class="money">{{ money(orderInfo.totalPrice) }}</div></div>
          <div v-if="orderInfo.refundStatus === 0" class="item acea-row row-between-wrapper"><div>原始邮费(¥)</div><div class="money">{{ money(orderInfo.payPostage) }}</div></div>
          <div v-if="orderInfo.refundStatus === 0" class="item acea-row row-between-wrapper"><div>实际支付(¥)</div><div class="money"><input v-model="price" type="number" /></div></div>
          <div v-if="orderInfo.refundStatus === 1" class="item acea-row row-between-wrapper"><div>实际支付(¥)</div><div class="money">{{ money(orderInfo.payPrice) }}</div></div>
          <div v-if="orderInfo.refundStatus === 1" class="item acea-row row-between-wrapper"><div>退款金额(¥)</div><div class="money"><input v-model="refundPrice" type="number" /></div></div>
        </div>
        <div v-else class="listChange"><textarea v-model="remark" :placeholder="orderInfo.remark || '请填写备注信息...'" maxlength="100"></textarea></div>
        <button class="modify" type="button" @click="saveChange">{{ modalStatus === 2 ? '确认退款' : '立即提交' }}</button>
      </div>
      <div v-if="changeVisible" class="maskModel" @touchmove.prevent></div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { orderInfo as fetchOrderInfo, orderMark, orderRefund, orderUpdatePrice } from '../api';

const orderInfo = reactive({});
const changeVisible = ref(false);
const modalStatus = ref(0);
const price = ref('0.00');
const refundPrice = ref('0.00');
const remark = ref('');
const fallbackImage = 'data:image/svg+xml;charset=utf-8,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%2280%22 height=%2280%22%3E%3Crect width=%2280%22 height=%2280%22 fill=%22%23f1f3f5%22/%3E%3Ctext x=%2240%22 y=%2245%22 font-size=%2212%22 fill=%22%23999%22 text-anchor=%22middle%22%3E暂无图片%3C/text%3E%3C/svg%3E';

const orderNo = computed(() => window.location.pathname.split('/').filter(Boolean)[2] || '');
const looksOnly = computed(() => window.location.pathname.split('/').filter(Boolean)[3] === 'looks');
const productRows = computed(() => orderInfo.productList || orderInfo.storeOrderInfoVos || orderInfo.orderInfo || []);
const title = computed(() => orderInfo.statusStr?.value || '-');
const types = computed(() => orderInfo.statusStr?.key || '');
const canDelivery = computed(() => types.value === 'notShipped' && orderInfo.shippingType !== 2 && orderInfo.refundStatus !== 2);
const canWriteOff = computed(() => types.value === 'toBeWrittenOff' && orderInfo.shippingType === 2 && orderInfo.refundStatus === 0 && orderInfo.paid === true);
const deliveryTypeText = computed(() => ({ express: '快递', send: '送货', noNeed: '无需发货', fictitious: '无需发货' }[orderInfo.deliveryType] || orderInfo.deliveryType || '-'));
const modalTitle = computed(() => (modalStatus.value === 1 ? '订单备注' : orderInfo.refundStatus === 1 ? '立即退款' : '一键改价'));

async function loadDetail() {
  const data = await fetchOrderInfo({ orderNo: orderNo.value });
  Object.assign(orderInfo, data || {});
}

function openChange(status) {
  modalStatus.value = status;
  price.value = money(orderInfo.payPrice);
  refundPrice.value = money(orderInfo.payPrice);
  remark.value = orderInfo.remark || '';
  changeVisible.value = true;
}

function closeChange() {
  changeVisible.value = false;
}

async function saveChange() {
  if (!orderInfo.orderId) return;
  if (modalStatus.value === 0 && orderInfo.refundStatus === 0) {
    await orderUpdatePrice({ orderNo: orderInfo.orderId, payPrice: price.value });
    ElMessage.success('改价成功');
  } else if (modalStatus.value === 2 && orderInfo.refundStatus === 1) {
    await orderRefund({ orderNo: orderInfo.orderId, amount: refundPrice.value });
    ElMessage.success('退款成功');
  } else {
    if (!remark.value.trim()) {
      ElMessage.error('请填写备注信息');
      return;
    }
    await orderMark({ orderNo: orderInfo.orderId, mark: remark.value.trim() });
    ElMessage.success('提交成功');
  }
  closeChange();
  await loadDetail();
}

function navigate(path) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path } }));
}

async function copy(value) {
  if (!value) return;
  await navigator.clipboard?.writeText(String(value));
  ElMessage.success('复制成功');
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '';
}

function money(value) {
  return Number(value || 0).toFixed(2);
}

onMounted(loadDetail);
</script>
