<template>
  <section class="plain-view balance-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>我的余额</h1>
      <span></span>
    </div>
    <div v-if="!authToken" class="login-nudge">
      <h2>登录后查看余额</h2>
      <button type="button" @click="$emit('login')">去登录</button>
    </div>
    <div v-else-if="loading" class="state">加载中...</div>
    <template v-else>
      <section class="balance-card">
        <div>
          <span>总资产(元)</span>
          <strong>{{ moneyText(info.nowMoney) }}</strong>
        </div>
        <button type="button" @click="$emit('recharge')">充值</button>
        <footer>
          <p><em>{{ moneyText(info.recharge) }}</em><span>累计充值(元)</span></p>
          <p><em>{{ moneyText(info.orderStatusSum) }}</em><span>累计消费(元)</span></p>
        </footer>
      </section>
      <section class="balance-nav">
        <button type="button" @click="$emit('open-bill', 'all')"><strong>账</strong><span>账单记录</span></button>
        <button type="button" @click="$emit('open-bill', 'expenditure')"><strong>消</strong><span>消费记录</span></button>
        <button type="button" @click="$emit('open-bill', 'income')"><strong>充</strong><span>充值记录</span></button>
        <button type="button" @click="$emit('integral')"><strong>积</strong><span>积分中心</span></button>
      </section>
      <section class="balance-adverts">
        <button type="button" @click="$emit('sign')">
          <span><strong>签到领积分</strong><em>赚积分抵现金</em></span>
        </button>
        <button type="button" @click="$emit('coupons')">
          <span><strong>领取优惠券</strong><em>满减享优惠</em></span>
        </button>
      </section>
    </template>
  </section>
</template>

<script setup>
defineProps({
  authToken: { type: String, default: "" },
  loading: Boolean,
  info: { type: Object, default: () => ({}) },
  moneyText: { type: Function, required: true }
});

defineEmits(["back", "login", "recharge", "open-bill", "integral", "sign", "coupons"]);
</script>
