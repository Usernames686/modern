<template>
  <section class="plain-view user-coupon-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>我的优惠券</h1>
      <span>{{ items.length }} 张</span>
    </div>
    <div v-if="!authToken" class="login-nudge">
      <h2>登录后查看优惠券</h2>
      <button type="button" @click="$emit('login')">去登录</button>
    </div>
    <template v-else>
      <nav class="user-coupon-tabs">
        <button
          v-for="item in tabs"
          :key="item.type"
          :class="{ active: type === item.type }"
          type="button"
          @click="$emit('select-type', item.type)"
        >
          {{ item.name }}
        </button>
      </nav>
      <div v-if="loading" class="state">加载中...</div>
      <div v-else-if="items.length === 0" class="coupon-empty-page">暂无优惠券</div>
      <section v-else class="user-coupon-list">
        <article
          v-for="item in items"
          :key="item.id"
          class="user-coupon-card"
          :class="{ disabled: statusText(item.validStr) !== '可用' }"
        >
          <div class="user-coupon-money">
            <p>￥<strong>{{ Number(item.money || 0) }}</strong></p>
            <span>满{{ Number(item.minPrice || 0) }}元可用</span>
          </div>
          <div class="user-coupon-info">
            <h2>
              <em>{{ couponTypeText(item) }}</em>
              <span>{{ item.name }}</span>
            </h2>
            <p>{{ item.useStartTimeStr || "-" }}~{{ item.useEndTimeStr || "-" }}</p>
            <strong>{{ statusText(item.validStr) }}</strong>
          </div>
        </article>
        <p class="coupon-bottom-line">我也是有底线的~</p>
      </section>
    </template>
  </section>
</template>

<script setup>
defineProps({
  authToken: { type: String, default: "" },
  loading: Boolean,
  tabs: { type: Array, default: () => [] },
  type: { type: String, default: "usable" },
  items: { type: Array, default: () => [] },
  statusText: { type: Function, required: true },
  couponTypeText: { type: Function, required: true }
});

defineEmits(["back", "login", "select-type"]);
</script>
