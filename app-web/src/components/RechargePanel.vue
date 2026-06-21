<template>
  <aside class="coupon-mask" @click.self="$emit('close')">
    <section class="coupon-panel recharge-panel">
      <div class="coupon-head">
        <strong>余额充值</strong>
        <button type="button" @click="$emit('close')">×</button>
      </div>
      <div v-if="loading" class="state">加载中...</div>
      <template v-else>
        <div v-if="config.rechargeSwitch === false" class="coupon-empty">充值功能已关闭</div>
        <template v-else>
          <div class="recharge-quota">
            <button
              v-for="item in config.rechargeQuota || []"
              :key="item.id"
              type="button"
              :class="{ active: selectedId === item.id }"
              @click="$emit('select-quota', item)"
            >
              <strong>￥{{ moneyText(item.price) }}</strong>
              <span v-if="Number(item.giveMoney || 0) > 0">赠送 ￥{{ moneyText(item.giveMoney) }}</span>
              <span v-else>固定充值</span>
            </button>
          </div>
          <label class="recharge-custom">
            <span>自定义金额</span>
            <input
              :value="customPrice"
              inputmode="decimal"
              placeholder="请输入充值金额"
              @input="$emit('update-custom-price', $event.target.value)"
              @focus="$emit('custom-focus')"
            />
          </label>
          <p class="recharge-min">最低充值 ￥{{ moneyText(config.minRecharge || 1) }}</p>
          <div v-if="(config.rechargeAttention || []).length" class="recharge-attention">
            <strong>充值说明</strong>
            <p v-for="line in config.rechargeAttention" :key="line">{{ line }}</p>
          </div>
          <button class="recharge-submit" type="button" :disabled="submitting" @click="$emit('submit')">
            {{ submitting ? "提交中..." : "立即充值" }}
          </button>
        </template>
      </template>
    </section>
  </aside>
</template>

<script setup>
defineProps({
  loading: Boolean,
  submitting: Boolean,
  config: { type: Object, default: () => ({}) },
  selectedId: { type: [Number, String], default: null },
  customPrice: { type: String, default: "" },
  moneyText: { type: Function, required: true }
});

defineEmits(["close", "select-quota", "update-custom-price", "custom-focus", "submit"]);
</script>
