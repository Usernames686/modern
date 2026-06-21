<template>
  <aside class="coupon-mask" @click.self="$emit('close')">
    <section class="coupon-panel">
      <div class="coupon-head">
        <strong>优惠券</strong>
        <button type="button" @click="$emit('close')">×</button>
      </div>
      <div v-if="loading" class="state">加载中...</div>
      <div v-else-if="coupons.length === 0" class="coupon-empty">暂无可用优惠券</div>
      <div v-else class="coupon-list">
        <article
          v-for="coupon in coupons"
          :key="coupon.id"
          class="coupon-item"
          :class="{ active: selectedCoupon?.id === coupon.id }"
          @click="$emit('select', coupon)"
        >
          <div class="coupon-money">
            <span>￥</span>
            <strong>{{ coupon.money }}</strong>
            <em>满{{ coupon.minPrice }}元可用</em>
          </div>
          <div class="coupon-info">
            <h2>
              <span>{{ couponTypeText(coupon) }}</span>
              {{ coupon.name }}
            </h2>
            <p>{{ coupon.useStartTimeStr || "" }} - {{ coupon.useEndTimeStr || "" }}</p>
            <button type="button">{{ actionText(coupon) }}</button>
          </div>
        </article>
      </div>
    </section>
  </aside>
</template>

<script setup>
const props = defineProps({
  loading: Boolean,
  coupons: { type: Array, default: () => [] },
  selectedCoupon: { type: Object, default: null },
  couponTypeText: { type: Function, required: true }
});

defineEmits(["close", "select"]);

function actionText(coupon) {
  if (coupon.needReceive && !coupon.received) {
    return "领取使用";
  }
  return selectedText(coupon);
}

function selectedText(coupon) {
  return props.selectedCoupon?.id === coupon?.id ? "不使用" : "立即使用";
}
</script>
