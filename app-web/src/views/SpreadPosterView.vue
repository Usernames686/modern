<template>
  <section class="plain-view spread-poster-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>推广名片</h1>
      <span>{{ posters.length || 0 }} 张</span>
    </div>
    <div v-if="loading" class="state">加载中...</div>
    <template v-else>
      <section class="poster-carousel" v-if="currentPoster">
        <button type="button" :disabled="posterIndex === 0" @click="$emit('select', posterIndex - 1)">‹</button>
        <article class="poster-card">
          <img :src="assetUrl(currentPoster.pic)" :alt="currentPoster.title || '推广海报'" />
          <div class="poster-user">
            <img :src="assetUrl(user?.avatar || defaultAvatar)" alt="" />
            <span>
              <strong>{{ user?.nickname || "用户" }}</strong>
              <em>邀请您加入</em>
            </span>
          </div>
        </article>
        <button
          type="button"
          :disabled="posterIndex >= posters.length - 1"
          @click="$emit('select', posterIndex + 1)"
        >›</button>
      </section>
      <nav class="poster-dots" v-if="posters.length > 1">
        <button
          v-for="(item, index) in posters"
          :key="item.id || index"
          type="button"
          :class="{ active: posterIndex === index }"
          @click="$emit('select', index)"
        />
      </nav>
      <section class="poster-link-box">
        <span>推广链接</span>
        <p>{{ link }}</p>
        <button type="button" @click="$emit('copy')">复制链接</button>
        <div v-if="manualCopyLink" class="poster-manual-copy">
          <strong>手动复制</strong>
          <textarea :value="manualCopyLink" readonly rows="3" @focus="$event.target.select()" />
          <button type="button" @click="$emit('close-manual-copy')">收起</button>
        </div>
      </section>
      <div v-if="posters.length === 0" class="coupon-empty-page">暂无推广海报～</div>
    </template>
  </section>
</template>

<script setup>
defineProps({
  loading: Boolean,
  posters: { type: Array, default: () => [] },
  currentPoster: { type: Object, default: null },
  posterIndex: { type: Number, default: 0 },
  user: { type: Object, default: null },
  link: { type: String, default: "" },
  manualCopyLink: { type: String, default: "" },
  defaultAvatar: { type: String, required: true },
  assetUrl: { type: Function, required: true }
});

defineEmits(["back", "select", "copy", "close-manual-copy"]);
</script>
