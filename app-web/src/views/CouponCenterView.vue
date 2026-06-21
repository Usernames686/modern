<template>
  <section class="plain-view coupon-center-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>领取优惠券</h1>
      <span></span>
    </div>
    <div v-if="!authToken" class="login-nudge">
      <h2>登录后领取优惠券</h2>
      <button type="button" @click="$emit('login')">去登录</button>
    </div>
    <template v-else>
      <nav class="user-coupon-tabs coupon-center-tabs">
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
      <div v-else-if="items.length === 0" class="coupon-empty-page">暂无可领取优惠券</div>
      <section v-else class="user-coupon-list">
        <article
          v-for="(item, index) in items"
          :key="item.id"
          class="user-coupon-card coupon-receive-card"
          :class="{ disabled: item.isUse }"
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
            <p v-if="Number(item.day || 0) > 0">领取后{{ item.day }}天内可用</p>
            <p v-else>{{ item.useStartTimeStr || "-" }}~{{ item.useEndTimeStr || "-" }}</p>
            <button type="button" :disabled="item.isUse || receivingId === item.id" @click="$emit('receive', item, index)">
              {{ item.isUse ? "已领取" : receivingId === item.id ? "领取中" : "立即领取" }}
            </button>
          </div>
        </article>
      </section>
    </template>
  </section>
</template>

<script setup>
defineProps({
  authToken: { type: String, default: "" },
  loading: Boolean,
  tabs: { type: Array, default: () => [] },
  type: { type: Number, default: 1 },
  items: { type: Array, default: () => [] },
  receivingId: { type: Number, default: 0 },
  couponTypeText: { type: Function, required: true }
});

defineEmits(["back", "login", "select-type", "receive"]);
</script>
