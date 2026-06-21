<template>
  <section class="plain-view order-payment-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>订单付款</h1>
      <span></span>
    </div>
    <div v-if="loading" class="state">加载中...</div>
    <template v-else-if="order">
      <section class="payment-amount-card">
        <span>￥</span><strong>{{ moneyText(order.payPrice) }}</strong>
        <p>{{ order.orderId }}</p>
      </section>
      <section class="payment-method-card">
        <button
          v-for="method in methods"
          :key="method.value"
          class="pay-method"
          :class="{ disabled: !paymentMethodEnabled(method) }"
          type="button"
          :disabled="paymentLoading || !paymentMethodEnabled(method)"
          @click="$emit('choose', order, method.value)"
        >
          <i class="pay-method-icon" :class="paymentIconClass(method.value)">{{ paymentIconText(method.value) }}</i>
          <span><strong>{{ method.name }}</strong><em>{{ paymentMethodSubText(method) }}</em></span>
          <b>{{ paymentMethodEnabled(method) ? "选择" : "不可用" }}</b>
        </button>
      </section>
      <button class="payment-submit-button" type="button" :disabled="payingOrder === order.orderId" @click="$emit('choose', order, 'yue')">
        {{ payingOrder === order.orderId ? "支付中..." : "立即支付" }}
      </button>
    </template>
    <div v-else class="state">缺少订单信息</div>
  </section>
</template>

<script setup>
defineProps({
  loading: Boolean,
  paymentLoading: Boolean,
  order: { type: Object, default: null },
  methods: { type: Array, default: () => [] },
  payingOrder: { type: [String, Number], default: "" },
  moneyText: { type: Function, required: true },
  paymentMethodEnabled: { type: Function, required: true },
  paymentMethodSubText: { type: Function, required: true },
  paymentIconClass: { type: Function, required: true },
  paymentIconText: { type: Function, required: true }
});

defineEmits(["back", "choose"]);
</script>
