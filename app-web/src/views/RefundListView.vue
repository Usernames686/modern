<template>
  <section class="plain-view refund-list-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>退货列表</h1>
      <button type="button" @click="$emit('refresh')">刷新</button>
    </div>
    <nav class="refund-status-tabs" aria-label="售后状态筛选">
      <button
        v-for="tab in tabs"
        :key="tab.value ?? 'all'"
        type="button"
        :class="{ active: activeStatus === tab.value }"
        @click="$emit('change-status', tab.value)"
      >
        {{ tab.name }}
      </button>
    </nav>
    <div v-if="loading" class="state">加载中...</div>
    <div v-else-if="orders.length === 0" class="coupon-empty-page">暂无订单~</div>
    <section v-else class="return-list">
      <article v-for="order in orders" :key="order.id || order.orderId" class="return-card" @click="$emit('open', order)">
        <div class="return-status-stamp" :class="{ powder: Number(order.refundStatus || 0) === 1 || Number(order.refundStatus || 0) === 3 }">
          {{ refundStatusText(order) }}
        </div>
        <div class="return-order-num">订单号：{{ order.orderId }}</div>
        <div v-for="goods in orderGoodsList(order)" :key="goods.id || goods.unique" class="order-goods">
          <SafeImage :src="assetUrl(goodsImage(goods))" :alt="goodsName(goods)" :label="goodsName(goods)" variant="product" />
          <div>
            <h2>{{ goodsName(goods) }}</h2>
            <p v-if="goodsSku(goods)">{{ goodsSku(goods) }}</p>
            <span>￥{{ goodsPrice(goods) }} × {{ goodsNum(goods) }}</span>
          </div>
        </div>
        <div class="return-meta" v-if="order.refundReasonWap || order.refundReasonWapExplain || order.refundReason || order.refundReasonTime">
          <p v-if="order.refundReasonWap"><span>退款原因</span><strong>{{ order.refundReasonWap }}</strong></p>
          <p v-if="order.refundReasonWapExplain"><span>备注说明</span><strong>{{ order.refundReasonWapExplain }}</strong></p>
          <p v-if="order.refundReason"><span>商家处理</span><strong>{{ order.refundReason }}</strong></p>
          <p v-if="order.refundReasonTime"><span>申请时间</span><strong>{{ order.refundReasonTime }}</strong></p>
        </div>
        <div v-if="refundImages(order).length" class="return-proof-list">
          <img v-for="image in refundImages(order)" :key="image" :src="assetUrl(image)" alt="退款凭证" />
        </div>
        <div class="return-total">
          共{{ order.totalNum || orderGoodsTotal(order) || 0 }}件商品，{{ refundAmountLabel(order) }}
          <strong>￥{{ refundAmount(order) }}</strong>
        </div>
      </article>
      <div class="return-list-bottom">我也是有底线的~</div>
    </section>
  </section>
</template>

<script setup>
import SafeImage from "../components/SafeImage.vue";

defineProps({
  loading: Boolean,
  orders: { type: Array, default: () => [] },
  tabs: { type: Array, default: () => [] },
  activeStatus: { type: [Number, null], default: null },
  assetUrl: { type: Function, required: true },
  orderGoodsList: { type: Function, required: true },
  goodsName: { type: Function, required: true },
  goodsImage: { type: Function, required: true },
  goodsSku: { type: Function, required: true },
  goodsPrice: { type: Function, required: true },
  goodsNum: { type: Function, required: true },
  orderGoodsTotal: { type: Function, required: true },
  refundStatusText: { type: Function, required: true }
});

defineEmits(["back", "refresh", "open", "change-status"]);

function refundImages(order) {
  return String(order?.refundReasonWapImg || "")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 3);
}

function refundAmount(order) {
  const refundPrice = Number(order?.refundPrice || 0);
  if (refundPrice > 0) {
    return order.refundPrice;
  }
  return order?.payPrice || "0.00";
}

function refundAmountLabel(order) {
  return Number(order?.refundStatus || 0) === 2 ? "已退金额" : "退款金额";
}
</script>
