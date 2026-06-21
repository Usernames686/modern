<template>
  <section class="plain-view activity-order-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>{{ title }}</h1>
      <button type="button" @click="$emit('refresh')">刷新</button>
    </div>
    <div v-if="!authToken" class="login-nudge">
      <h2>登录后查看活动订单</h2>
      <button type="button" @click="$emit('login')">去登录</button>
    </div>
    <div v-else-if="loading" class="state">加载中...</div>
    <section v-else class="activity-order-list">
      <article v-for="order in orders" :key="order.orderId || order.id" class="order-card activity-order-card" @click="$emit('open', order)">
        <div class="order-card-head">
          <span>{{ order.orderId }}</span>
          <strong>{{ typeText(order) }} · {{ order.statusText }}</strong>
        </div>
        <div v-for="goods in order.cartInfo" :key="goods.id" class="order-goods">
          <img :src="assetUrl(goods.image)" :alt="goods.productName" />
          <div>
            <h2>{{ goods.productName }}</h2>
            <p>{{ goods.sku || "默认规格" }}</p>
            <span>￥{{ goods.price }} × {{ goods.payNum }}</span>
          </div>
        </div>
        <div class="activity-order-meta">
          <span>{{ order.createTime || "-" }}</span>
          <strong>实付 ￥{{ order.payPrice }}</strong>
        </div>
        <div class="order-actions">
          <button type="button" @click.stop="$emit('open', order)">查看详情</button>
          <button v-if="isUnpaid(order)" type="button" :disabled="payingOrder === order.orderId" @click.stop="$emit('pay', order)">
            {{ payingOrder === order.orderId ? "支付中..." : "去支付" }}
          </button>
          <button v-if="isUnpaid(order)" type="button" @click.stop="$emit('cancel', order)">取消订单</button>
          <button v-if="canApplyRefund(order)" type="button" @click.stop="$emit('refund', order)">申请售后</button>
          <button v-if="canViewLogistics(order)" type="button" @click.stop="$emit('logistics', order)">查看物流</button>
        </div>
      </article>
      <div v-if="orders.length === 0" class="coupon-empty-page">暂无{{ title }}</div>
    </section>
  </section>
</template>

<script setup>
defineProps({
  authToken: { type: String, default: "" },
  loading: Boolean,
  title: { type: String, required: true },
  orders: { type: Array, default: () => [] },
  payingOrder: { type: String, default: "" },
  assetUrl: { type: Function, required: true },
  typeText: { type: Function, required: true },
  isUnpaid: { type: Function, required: true },
  canApplyRefund: { type: Function, required: true },
  canViewLogistics: { type: Function, required: true }
});

defineEmits(["back", "refresh", "login", "open", "pay", "cancel", "refund", "logistics"]);
</script>
