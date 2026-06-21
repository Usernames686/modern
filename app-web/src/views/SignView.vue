<template>
  <section class="plain-view sign-view">
    <div class="sign-head">
      <div class="sign-user">
        <img :src="assetUrl(user?.avatar || defaultAvatar)" alt="" />
        <div>
          <h1>{{ user?.nickname || user?.account || "聚商盈会员" }}</h1>
          <p>积分：{{ info.integral ?? user?.integral ?? 0 }}</p>
        </div>
      </div>
      <button type="button" @click="$emit('records')">明细</button>
    </div>
    <div v-if="!authToken" class="login-nudge">
      <h2>登录后参与签到</h2>
      <button type="button" @click="$emit('login')">去登录</button>
    </div>
    <div v-else-if="loading" class="state">加载中...</div>
    <template v-else>
      <section class="sign-cycle">
        <div class="sign-reward-list">
          <article
            v-for="(item, index) in config"
            :key="item.id || item.day"
            :class="{ active: Number(info.signNum || 0) >= index + 1, final: index + 1 === config.length }"
          >
            <strong>{{ item.title || `第${item.day}天` }}</strong>
            <i></i>
            <span>+{{ item.integral }}</span>
          </article>
        </div>
        <button class="sign-button" type="button" :disabled="signing || info.isDaySign" @click="$emit('sign')">
          {{ info.isDaySign ? "已签到" : signing ? "签到中..." : "立即签到" }}
        </button>
      </section>
      <section class="sign-total">
        <span>已累计签到</span>
        <div>
          <b v-for="(digit, index) in countDigits" :key="index">{{ digit }}</b>
          <em>天</em>
        </div>
        <p>连续签到第 {{ config.length || 0 }} 天可获得超额积分，一定要坚持签到哦~~~</p>
      </section>
      <section class="sign-recent">
        <article v-for="item in list" :key="item.id">
          <span><strong>{{ item.title }}</strong><em>{{ item.createDay }}</em></span>
          <b>+{{ item.number }}</b>
        </article>
        <button v-if="list.length >= 3" type="button" @click="$emit('records')">点击查看更多 ›</button>
        <div v-if="list.length === 0" class="coupon-empty-page">暂无签到记录</div>
      </section>
    </template>
  </section>
</template>

<script setup>
defineProps({
  authToken: { type: String, default: "" },
  loading: Boolean,
  user: { type: Object, default: null },
  defaultAvatar: { type: String, default: "" },
  info: { type: Object, default: () => ({}) },
  config: { type: Array, default: () => [] },
  list: { type: Array, default: () => [] },
  countDigits: { type: Array, default: () => [] },
  signing: Boolean,
  assetUrl: { type: Function, required: true }
});

defineEmits(["login", "records", "sign"]);
</script>
