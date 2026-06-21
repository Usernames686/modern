<template>
  <section class="plain-view order-list-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>我的订单</h1>
      <button type="button" @click="$emit('refresh-orders')">刷新</button>
    </div>

    <section v-if="!authToken" class="login-nudge">
      <div>
        <h2>请先登录</h2>
        <p>登录后查看全部订单、售后进度和支付状态。</p>
        <button type="button" @click="$emit('login')">去登录</button>
      </div>
    </section>

    <template v-else>
      <section class="order-list-summary">
        <strong>{{ orderData.orderCount || orderCountSummary || 0 }}</strong>
        <span>消费订单</span>
        <em>{{ orderTypeText }}</em>
      </section>

      <div class="order-tabs">
        <button
          v-for="item in orderTabs"
          :key="item.type"
          :class="{ active: orderType === item.type }"
          type="button"
          @click="$emit('select-order-type', item.type)"
        >
          {{ item.name }}
        </button>
      </div>

      <div v-if="orderLoading" class="state">加载中...</div>
      <div v-else-if="orders.length === 0" class="state">暂无订单</div>
      <article v-for="order in orders" v-else :key="order.id || order.orderId" class="order-card standalone-order-card" @click="$emit('open-order', order, orderType === -3)">
        <div class="order-card-head">
          <span>{{ order.orderId }}</span>
          <strong>{{ orderType === -3 ? refundStatusText(order) : order.statusText }}</strong>
        </div>
        <div v-for="goods in orderGoodsList(order)" :key="goods.id || goods.unique" class="order-goods">
          <SafeImage :src="assetUrl(goodsImage(goods))" :alt="goodsName(goods)" :label="goodsName(goods)" variant="product" />
          <div>
            <h2>{{ goodsName(goods) }}</h2>
            <p>{{ goodsSku(goods) || "默认规格" }}</p>
            <span>￥{{ goodsPrice(goods) }} × {{ goodsNum(goods) }}</span>
          </div>
        </div>
        <div class="order-total">实付 ￥{{ order.payPrice }}</div>
        <div class="order-actions">
          <button type="button" @click.stop="$emit('open-order', order, orderType === -3)">查看详情</button>
          <button v-if="isUnpaid(order)" type="button" :disabled="payingOrder === order.orderId" @click.stop="$emit('pay-order', order)">
            {{ payingOrder === order.orderId ? "支付中..." : "去支付" }}
          </button>
          <button v-if="isUnpaid(order)" type="button" @click.stop="$emit('cancel-order', order)">取消订单</button>
          <button v-if="canApplyRefund(order)" type="button" @click.stop="$emit('refund-order', order)">申请售后</button>
          <button v-if="canTakeOrder(order)" type="button" @click.stop="$emit('take-order', order)">确认收货</button>
          <button v-if="canCommentOrder(order)" type="button" @click.stop="$emit('comment-order', order)">去评价</button>
          <button v-if="canDeleteOrder(order)" type="button" @click.stop="$emit('delete-order', order)">删除订单</button>
          <button v-if="canOrderAgain(order)" type="button" @click.stop="$emit('again-order', order)">再次购买</button>
        </div>
      </article>
    </template>
  </section>
</template>

<script setup>
import { computed } from "vue";
import SafeImage from "../components/SafeImage.vue";

const props = defineProps({
  authToken: { type: String, default: "" },
  orderData: { type: Object, default: () => ({}) },
  orderCountSummary: { type: Number, default: 0 },
  orderTabs: { type: Array, default: () => [] },
  orderType: { type: [Number, String, null], default: null },
  orderLoading: Boolean,
  orders: { type: Array, default: () => [] },
  payingOrder: { type: String, default: "" },
  assetUrl: { type: Function, required: true },
  orderGoodsList: { type: Function, required: true },
  goodsName: { type: Function, required: true },
  goodsImage: { type: Function, required: true },
  goodsSku: { type: Function, required: true },
  goodsPrice: { type: Function, required: true },
  goodsNum: { type: Function, required: true },
  refundStatusText: { type: Function, required: true },
  isUnpaid: { type: Function, required: true },
  canApplyRefund: { type: Function, required: true },
  canTakeOrder: { type: Function, required: true },
  canCommentOrder: { type: Function, required: true },
  canDeleteOrder: { type: Function, required: true },
  canOrderAgain: { type: Function, required: true }
});

const orderTypeText = computed(() => {
  const matched = props.orderTabs.find((item) => item.type === props.orderType);
  return matched?.name || "全部订单";
});

defineEmits([
  "back",
  "login",
  "refresh-orders",
  "select-order-type",
  "open-order",
  "pay-order",
  "cancel-order",
  "refund-order",
  "take-order",
  "comment-order",
  "delete-order",
  "again-order"
]);
</script>
