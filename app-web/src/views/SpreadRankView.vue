<template>
  <section class="plain-view rank-view">
    <div class="rank-head">
      <button type="button" @click="$emit('back')">返回</button>
      <span>{{ title }}</span>
    </div>
    <nav class="rank-tabs">
      <button :class="{ active: type === 'week' }" type="button" @click="$emit('select-type', 'week')">周榜</button>
      <button :class="{ active: type === 'month' }" type="button" @click="$emit('select-type', 'month')">月榜</button>
    </nav>
    <div v-if="loading" class="state">加载中...</div>
    <section v-else class="rank-list" :class="{ 'brokerage-rank-list': mode === 'brokerage' }">
      <article v-for="(item, index) in items" :key="item.uid">
        <span class="rank-no">{{ index + 1 }}</span>
        <img :src="assetUrl(item.avatar || defaultAvatar)" alt="" />
        <div>
          <h2>{{ item.nickname || `用户${item.uid}` }}</h2>
          <p v-if="mode === 'brokerage'">佣金：￥{{ moneyText(item.brokeragePrice) }}</p>
          <p v-else>{{ item.spreadCount || 0 }} 人</p>
        </div>
      </article>
      <div v-if="items.length === 0" class="coupon-empty-page">暂无排行～</div>
    </section>
  </section>
</template>

<script setup>
defineProps({
  loading: Boolean,
  title: { type: String, required: true },
  mode: { type: String, default: "spread" },
  type: { type: String, default: "week" },
  items: { type: Array, default: () => [] },
  defaultAvatar: { type: String, required: true },
  assetUrl: { type: Function, required: true },
  moneyText: { type: Function, required: true }
});

defineEmits(["back", "select-type"]);
</script>
