<template>
  <section class="plain-view spread-view">
    <div class="spread-head">
      <button type="button" @click="$emit('extract-records')">提现记录 ›</button>
      <span>当前佣金</span>
      <strong>{{ moneyText(info.commissionCount) }}</strong>
      <div>
        <p><em>{{ moneyText(info.lastDayCount) }}</em><span>昨日收益</span></p>
        <p><em>{{ moneyText(info.extractCount) }}</em><span>累积已提</span></p>
      </div>
    </div>
    <div v-if="!authToken" class="login-nudge">
      <h2>登录后查看推广</h2>
      <button type="button" @click="$emit('login')">去登录</button>
    </div>
    <div v-else-if="loading" class="state">加载中...</div>
    <template v-else>
      <button class="spread-cash-button" type="button" @click="$emit('extract-cash')">立即提现</button>
      <section class="spread-tools">
        <button type="button" @click="$emit('poster')"><b>码</b><span>推广名片</span></button>
        <button type="button" @click="$emit('people')"><b>人</b><span>推广人统计</span></button>
        <button type="button" @click="$emit('brokerage-records')"><b>佣</b><span>佣金明细</span></button>
        <button type="button" @click="$emit('orders')"><b>单</b><span>推广人订单</span></button>
        <button type="button" @click="$emit('spread-rank')"><b>排</b><span>推广人排行</span></button>
        <button type="button" @click="$emit('brokerage-rank')"><b>榜</b><span>佣金排行</span></button>
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

defineEmits([
  "login",
  "extract-records",
  "extract-cash",
  "poster",
  "people",
  "brokerage-records",
  "orders",
  "spread-rank",
  "brokerage-rank"
]);
</script>
