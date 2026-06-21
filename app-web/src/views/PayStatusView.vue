<template>
  <section class="plain-view pay-status-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>{{ title }}</h1>
      <span></span>
    </div>
    <div v-if="loading" class="state">正在加载中...</div>
    <template v-else-if="info">
      <section class="payment-status-card">
        <div class="pay-status-icon" :class="{ success: Number(info.paid || 0) === 1 }">
          {{ Number(info.paid || 0) === 1 ? "✓" : "×" }}
        </div>
        <strong>{{ title }}</strong>
        <section v-if="activityTypeText(info)" class="pay-status-activity">
          <b>{{ activityTypeText(info) }}订单</b>
          <span>{{ activityHint(info) }}</span>
        </section>
        <div class="pay-status-wrapper">
          <p><span>订单编号</span><em>{{ info.orderId || info.orderNo }}</em></p>
          <p><span>下单时间</span><em>{{ info.createTime || "-" }}</em></p>
          <p><span>支付方式</span><em>{{ paymentMethodText(info.payType) }}</em></p>
          <p><span>支付金额</span><em>￥{{ moneyText(info.payPrice) }}</em></p>
          <p v-if="Number(info.paid || 0) !== 1 && info.message">
            <span>失败原因</span><em>{{ info.message }}</em>
          </p>
        </div>
        <div class="pay-status-actions">
          <button class="pay-status-primary" type="button" @click="$emit('open-order')">查看订单</button>
          <button v-if="isUnpaid(info)" class="pay-status-primary" type="button" @click="$emit('pay', info)">继续支付</button>
          <button v-if="canViewLogistics(info)" class="pay-status-secondary" type="button" @click="$emit('logistics', info)">查看物流</button>
          <button v-if="canTakeOrder(info)" class="pay-status-secondary" type="button" @click="$emit('take', info)">确认收货</button>
          <button v-if="canApplyRefund(info)" class="pay-status-secondary" type="button" @click="$emit('refund', info)">申请售后</button>
          <button v-if="canCommentOrder(info)" class="pay-status-secondary" type="button" @click="$emit('comment-first', info)">去评价</button>
          <button v-if="canOrderAgain(info)" class="pay-status-secondary" type="button" @click="$emit('again', info)">再次购买</button>
          <button v-if="activityTypeText(info)" class="pay-status-secondary" type="button" @click="$emit('open-activity', info)">
            {{ activityActionText(info) }}
          </button>
        </div>
        <template v-if="Number(info.pinkId || 0) > 0 && Number(info.paid || 0) === 1">
          <div class="pay-status-pink-actions">
            <button class="pay-status-secondary" type="button" @click="$emit('open-pink')">邀请好友参团</button>
            <button v-if="info.combinationId" class="pay-status-secondary" type="button" @click="$emit('open-combination')">
              查看拼团详情
            </button>
          </div>
          <section v-if="pinkShareLink" class="pay-status-share-box">
            <strong>参团邀请链接</strong>
            <textarea :value="pinkShareLink" readonly rows="3" @focus="$event.target.select()" />
            <button type="button" @click="$emit('close-pink-share')">收起</button>
          </section>
        </template>
        <button v-else class="pay-status-secondary" type="button" @click="$emit('home')">返回首页</button>
      </section>
    </template>
    <div v-else class="state">缺少参数无法查看订单支付状态</div>
  </section>
</template>

<script setup>
defineProps({
  loading: Boolean,
  info: { type: Object, default: null },
  title: { type: String, default: "" },
  moneyText: { type: Function, required: true },
  paymentMethodText: { type: Function, required: true },
  isUnpaid: { type: Function, default: () => false },
  canApplyRefund: { type: Function, default: () => false },
  canViewLogistics: { type: Function, default: () => false },
  canTakeOrder: { type: Function, default: () => false },
  canCommentOrder: { type: Function, default: () => false },
  canOrderAgain: { type: Function, default: () => false },
  activityTypeText: { type: Function, default: () => "" },
  activityHint: { type: Function, default: () => "" },
  activityActionText: { type: Function, default: () => "" },
  pinkShareLink: { type: String, default: "" }
});

defineEmits([
  "back",
  "open-order",
  "open-pink",
  "open-combination",
  "open-activity",
  "close-pink-share",
  "home",
  "pay",
  "logistics",
  "take",
  "refund",
  "comment-first",
  "again"
]);
</script>
