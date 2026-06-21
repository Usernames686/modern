<template>
  <div class="mobile-order-list-shell">
    <div class="mobile-order-list-page" ref="containerRef">
      <nav class="mobile-order-tabs acea-row row-middle">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          class="mobile-order-tab"
          :class="{ on: query.status === tab.value }"
          type="button"
          @click="changeStatus(tab.value)"
        >
          {{ tab.label }}
        </button>
      </nav>

      <div class="mobile-order-list">
        <article v-for="item in rows" :key="item.orderId" class="mobile-order-card">
          <button class="mobile-order-num acea-row row-middle" type="button" @click="toDetail(item)">
            <span>订单号：{{ item.orderId }}</span>
            <span class="time">下单时间：{{ formatTime(item.createTime) }}</span>
          </button>

          <div v-for="product in item.productList || []" :key="product.id || product.unique" class="mobile-order-goods">
            <button class="goods acea-row row-between-wrapper" type="button" @click="toDetail(item)">
              <div class="picTxt acea-row row-between-wrapper">
                <div class="pictrue">
                  <img :src="product.info?.image || fallbackImage" alt="" />
                </div>
                <div class="text">
                  <div class="info line2">{{ product.info?.productName || '-' }}</div>
                  <div v-if="product.info?.sku" class="attr">{{ product.info.sku }}</div>
                </div>
              </div>
              <div class="money">
                <div class="x-money">￥{{ money(product.info?.price) }}</div>
                <div class="num">x{{ product.info?.payNum || 1 }}</div>
              </div>
            </button>
          </div>

          <div class="mobile-order-total">
            共{{ item.totalNum || 1 }}件商品，应支付 <span class="money">￥{{ money(item.payPrice) }}</span>
            (邮费 ¥{{ money(item.totalPostage) }})
          </div>

          <div class="mobile-order-operation acea-row row-between-wrapper">
            <div></div>
            <div class="acea-row row-middle mobile-order-actions">
              <button v-if="!item.isAlterPrice && item.paid === false" class="bnt" type="button" @click="openChange(item, 0)">一键改价</button>
              <button class="bnt" type="button" @click="openChange(item, 1)">订单备注</button>
              <button v-if="query.status === 'refunding' && item.refundStatus === 1" class="bnt" type="button" @click="openChange(item, 2)">立即退款</button>
              <button v-if="query.status === 'refunding' && item.refundStatus === 1" class="bnt" type="button" @click="openChange(item, 3)">拒绝退款</button>
              <button v-if="query.status === 'notShipped' && item.shippingType !== 2 && item.refundStatus !== 2" class="bnt" type="button" @click="toDelivery(item)">去发货</button>
              <button v-if="canWriteOff(item)" class="bnt" type="button" @click="navigate('/javaMobile/orderCancellation')">去核销</button>
            </div>
          </div>
        </article>

        <div v-if="!loading && rows.length === 0" class="mobile-order-empty">暂无数据</div>
      </div>

      <div class="mobile-order-loading">
        <span v-if="loading">正在加载...</span>
        <span v-else-if="loaded">没有更多了</span>
        <button v-else class="load-more" type="button" @click="loadList">加载更多</button>
      </div>

      <div v-if="changeVisible" class="mobile-price-change on">
        <div class="priceTitle">
          {{ modalTitle }}
          <button class="close" type="button" @click="closeChange">x</button>
        </div>
        <div v-if="modalStatus === 0 || modalStatus === 2" class="listChange">
          <div v-if="orderInfo?.refundStatus === 0" class="item acea-row row-between-wrapper">
            <div>商品总价(¥)</div>
            <div class="money">{{ money(orderInfo?.totalPrice) }}</div>
          </div>
          <div v-if="orderInfo?.refundStatus === 0" class="item acea-row row-between-wrapper">
            <div>原始邮费(¥)</div>
            <div class="money">{{ money(orderInfo?.payPostage) }}</div>
          </div>
          <div v-if="orderInfo?.refundStatus === 0" class="item acea-row row-between-wrapper">
            <div>实际支付(¥)</div>
            <div class="money"><input v-model="price" type="number" /></div>
          </div>
          <div v-if="orderInfo?.refundStatus === 1" class="item acea-row row-between-wrapper">
            <div>实际支付(¥)</div>
            <div class="money">{{ money(orderInfo?.payPrice) }}</div>
          </div>
          <div v-if="orderInfo?.refundStatus === 1" class="item acea-row row-between-wrapper">
            <div>退款金额(¥)</div>
            <div class="money"><input v-model="refundPrice" type="number" /></div>
          </div>
        </div>
        <div v-else-if="modalStatus === 3" class="listChange">
          <textarea v-model="reason" placeholder="请填写退款原因" maxlength="100"></textarea>
        </div>
        <div v-else class="listChange">
          <textarea v-model="remark" :placeholder="orderInfo?.remark || '请填写备注信息...'" maxlength="100"></textarea>
        </div>
        <button class="modify" type="button" @click="saveChange">
          {{ orderInfo?.refundStatus === 0 || modalStatus === 1 || modalStatus === 3 ? '立即提交' : '确认退款' }}
        </button>
      </div>
      <div v-if="changeVisible" class="maskModel" @touchmove.prevent></div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { orderList, orderMark, orderRefund, orderRefundRefuse, orderUpdatePrice } from '../api';

const statusTabs = [
  { label: '待付款', value: 'unPaid' },
  { label: '待发货', value: 'notShipped' },
  { label: '待收货', value: 'spike' },
  { label: '待核销', value: 'toBeWrittenOff' },
  { label: '已完成', value: 'complete' },
  { label: '退款', value: 'refunding' }
];

const containerRef = ref();
const loading = ref(false);
const loaded = ref(false);
const rows = ref([]);
const changeVisible = ref(false);
const orderInfo = ref(null);
const modalStatus = ref(0);
const price = ref('0.00');
const refundPrice = ref('0.00');
const remark = ref('');
const reason = ref('');

const query = reactive({
  page: 1,
  limit: 10,
  status: statusFromPath()
});

const fallbackImage = 'data:image/svg+xml;charset=utf-8,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%2280%22 height=%2280%22%3E%3Crect width=%2280%22 height=%2280%22 fill=%22%23f1f3f5%22/%3E%3Ctext x=%2240%22 y=%2245%22 font-size=%2212%22 fill=%22%23999%22 text-anchor=%22middle%22%3E暂无图片%3C/text%3E%3C/svg%3E';

const modalTitle = computed(() => {
  if (modalStatus.value === 3) return '拒绝原因';
  if (modalStatus.value === 1) return '订单备注';
  if (orderInfo.value?.refundStatus === 1) return '立即退款';
  return '一键改价';
});

function statusFromPath() {
  const parts = window.location.pathname.split('/').filter(Boolean);
  return parts[2] || 'unPaid';
}

function normalizeStatus(value) {
  return statusTabs.some((item) => item.value === value) ? value : 'unPaid';
}

async function loadList() {
  if (loading.value || loaded.value) return;
  loading.value = true;
  try {
    const data = await orderList({ page: query.page, limit: query.limit, status: query.status });
    const list = Array.isArray(data?.list) ? data.list : [];
    rows.value.push(...list);
    loaded.value = list.length < query.limit;
    query.page += 1;
  } finally {
    loading.value = false;
  }
}

function init() {
  rows.value = [];
  query.page = 1;
  loaded.value = false;
  loading.value = false;
  loadList();
}

function changeStatus(status) {
  if (query.status === status) return;
  query.status = status;
  navigate(`/javaMobile/orderList/${status}`);
  init();
}

function navigate(path) {
  if (!path) return;
  if (window.location.pathname !== path) {
    window.history.pushState({}, '', path);
  }
  window.dispatchEvent(new CustomEvent('crmeb:navigate', { detail: { path } }));
}

function openChange(item, status) {
  orderInfo.value = item;
  modalStatus.value = status;
  price.value = money(item.payPrice);
  refundPrice.value = money(item.payPrice);
  remark.value = item.remark || '';
  reason.value = '';
  changeVisible.value = true;
}

function closeChange() {
  changeVisible.value = false;
  orderInfo.value = null;
}

async function saveChange() {
  const orderNo = orderInfo.value?.orderId;
  if (!orderNo) return;
  try {
    if (modalStatus.value === 0 && orderInfo.value.refundStatus === 0) {
      await orderUpdatePrice({ orderNo, payPrice: price.value });
      ElMessage.success('改价成功');
    } else if (modalStatus.value === 2 && orderInfo.value.refundStatus === 1) {
      await orderRefund({ orderNo, amount: refundPrice.value });
      ElMessage.success('退款成功');
    } else if (modalStatus.value === 3) {
      if (!reason.value.trim()) {
        ElMessage.error('请填写拒绝退款原因');
        return;
      }
      await orderRefundRefuse({ orderNo, reason: reason.value.trim() });
      ElMessage.success('已拒绝退款');
    } else {
      if (!remark.value.trim()) {
        ElMessage.error('请填写备注信息');
        return;
      }
      await orderMark({ orderNo, mark: remark.value.trim() });
      ElMessage.success('提交成功');
    }
    closeChange();
    init();
  } catch (error) {
    ElMessage.error(error.message || '提交失败');
  }
}

function toDetail(item) {
  navigate(`/javaMobile/orderDetail/${item.orderId}`);
}

function toDelivery(item) {
  navigate(`/javaMobile/orderDelivery/${item.orderId}/${item.id || ''}`);
}

function canWriteOff(item) {
  return query.status === 'toBeWrittenOff' && item.shippingType === 2 && item.refundStatus === 0 && item.paid === true;
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '';
}

function money(value) {
  return Number(value || 0).toFixed(2);
}

function syncStatusFromPath() {
  const next = normalizeStatus(statusFromPath());
  if (query.status !== next) {
    query.status = next;
    init();
  }
}

function handleNavigate(event) {
  if (event?.detail?.path?.startsWith('/javaMobile/orderList')) syncStatusFromPath();
}

function handleScroll() {
  const node = containerRef.value;
  if (!node || loading.value || loaded.value) return;
  if (node.scrollTop + node.clientHeight >= node.scrollHeight - 60) loadList();
}

onMounted(() => {
  query.status = normalizeStatus(query.status);
  init();
  containerRef.value?.addEventListener('scroll', handleScroll);
  window.addEventListener('popstate', syncStatusFromPath);
  window.addEventListener('crmeb:navigate', handleNavigate);
});

onBeforeUnmount(() => {
  containerRef.value?.removeEventListener('scroll', handleScroll);
  window.removeEventListener('popstate', syncStatusFromPath);
  window.removeEventListener('crmeb:navigate', handleNavigate);
});
</script>
