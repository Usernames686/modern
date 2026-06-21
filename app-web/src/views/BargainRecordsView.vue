<template>
  <section class="plain-view bargain-record-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>砍价记录</h1>
      <button type="button" @click="$emit('refresh')">刷新</button>
    </div>
    <div v-if="!authToken" class="login-nudge">
      <h2>登录后查看砍价记录</h2>
      <button type="button" @click="$emit('login')">去登录</button>
    </div>
    <div v-else-if="loading" class="state">加载中...</div>
    <section v-else class="bargain-record-list">
      <article v-for="item in items" :key="item.bargainUserId" class="bargain-record-card">
        <div class="bargain-record-main">
          <img :src="assetUrl(item.image)" alt="" />
          <div>
            <h2>{{ item.title }}</h2>
            <p v-if="Number(item.status) === 1">倒计时 {{ item.stopTimeText || "-" }}</p>
            <p v-else :class="mainStatusClass(item)">{{ mainStatusText(item) }}</p>
            <em>已砍至 <span>￥</span><strong>{{ moneyText(item.surplusPrice) }}</strong></em>
          </div>
        </div>
        <div class="bargain-record-bottom">
          <strong :class="mainStatusClass(item)">
            {{ item.statusText || statusText(item) }}
          </strong>
          <div>
            <button v-if="canPay(item)" type="button" @click="$emit('pay', item)">立即付款</button>
            <button v-else-if="canCreateOrder(item)" type="button" @click="$emit('open', item)">去付款</button>
            <button v-else-if="Number(item.status) === 1" type="button" @click="$emit('open', item)">继续砍价</button>
            <button v-else-if="Number(item.status) === 2" type="button" @click="$emit('restart')">重开一个</button>
          </div>
        </div>
      </article>
      <div v-if="items.length === 0" class="coupon-empty-page">暂无砍价记录～</div>
    </section>
  </section>
</template>

<script setup>
defineProps({
  authToken: { type: String, default: "" },
  loading: Boolean,
  items: { type: Array, default: () => [] },
  assetUrl: { type: Function, required: true },
  moneyText: { type: Function, required: true },
  statusText: { type: Function, required: true },
  mainStatusText: { type: Function, required: true },
  mainStatusClass: { type: Function, required: true },
  canPay: { type: Function, required: true },
  canCreateOrder: { type: Function, required: true }
});

defineEmits(["back", "refresh", "login", "pay", "open", "restart"]);
</script>
