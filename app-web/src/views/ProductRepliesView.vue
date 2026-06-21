<template>
  <section class="plain-view product-reply-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>用户评价</h1>
      <span>{{ config.sumCount || 0 }} 条</span>
    </div>
    <section class="reply-summary-card">
      <div>
        <strong>{{ Math.round(Number(config.replyChance || 0) * 100) }}%</strong>
        <span>好评率</span>
      </div>
      <div>
        <strong>{{ config.replyStar || 5 }}</strong>
        <span>综合评分</span>
      </div>
    </section>
    <div class="reply-tabs">
      <button :class="{ active: type === 0 }" type="button" @click="$emit('change-type', 0)">全部({{ config.sumCount || 0 }})</button>
      <button :class="{ active: type === 1 }" type="button" @click="$emit('change-type', 1)">好评({{ config.goodCount || 0 }})</button>
      <button :class="{ active: type === 2 }" type="button" @click="$emit('change-type', 2)">中评({{ config.inCount || 0 }})</button>
      <button :class="{ active: type === 3 }" type="button" @click="$emit('change-type', 3)">差评({{ config.poorCount || 0 }})</button>
    </div>
    <div v-if="loading" class="state">加载中...</div>
    <div v-else-if="replies.length === 0" class="state">暂无评价</div>
    <section v-else class="reply-list">
      <article v-for="item in replies" :key="item.id" class="reply-card">
        <div class="reply-user">
          <img :src="assetUrl(item.avatar || defaultAvatar)" alt="" />
          <div>
            <strong>{{ item.nickname || "用户" + item.uid }}</strong>
            <span>{{ item.createTime || "" }}</span>
          </div>
          <em>{{ item.productScore || 5 }}分</em>
        </div>
        <p v-if="item.sku">{{ item.sku }}</p>
        <div class="reply-content">{{ item.comment }}</div>
        <div v-if="item.pics && item.pics.length" class="reply-pics">
          <img v-for="pic in item.pics" :key="pic" :src="assetUrl(pic)" alt="" />
        </div>
      </article>
    </section>
  </section>
</template>

<script setup>
defineProps({
  loading: Boolean,
  replies: { type: Array, default: () => [] },
  config: { type: Object, default: () => ({}) },
  type: { type: Number, default: 0 },
  defaultAvatar: { type: String, required: true },
  assetUrl: { type: Function, required: true }
});

defineEmits(["back", "change-type"]);
</script>
