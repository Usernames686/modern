<template>
  <section class="bargain-view">
    <div class="bargain-hero">
      <button class="seckill-back" type="button" @click="$emit('back')">‹</button>
      <div class="bargain-title">
        <strong>砍价活动</strong>
        <span>邀请好友一起砍到底价</span>
      </div>
      <div class="bargain-total">已有 {{ header.bargainTotal || 0 }} 人帮砍成功</div>
    </div>
    <div v-if="loading" class="state">加载中...</div>
    <template v-else>
      <section v-if="successList.length" class="bargain-success">
        <div v-for="item in successList" :key="`${item.nickName}-${item.title}`">
          <img :src="assetUrl(item.avatar || defaultAvatar)" alt="" />
          <span>{{ item.nickName || "用户" }} 砍到 ￥{{ item.price }}，拿下 {{ item.title }}</span>
        </div>
      </section>
      <div v-if="items.length === 0" class="state">暂无砍价活动</div>
      <section v-else class="bargain-product-list">
        <article v-for="item in items" :key="item.id" class="bargain-product" @click="$emit('open', item)">
          <img :src="assetUrl(item.image)" :alt="item.title" />
          <div class="bargain-product-main">
            <h2>{{ item.title }}</h2>
            <div class="bargain-price">
              <span>最低可砍至</span>
              <strong>￥{{ item.minPrice }}</strong>
            </div>
            <p>
              原价 ￥{{ item.price }}
              <em>限量 {{ item.quota }} {{ item.unitName || "件" }}</em>
            </p>
            <div class="bargain-meta">
              <span>{{ item.peopleNum || 0 }} 人助力</span>
              <span>{{ item.stopTimeText }}</span>
            </div>
          </div>
          <button type="button" :class="{ ended: item.isSoldOut }">{{ item.isSoldOut ? "已售罄" : "参与砍价" }}</button>
        </article>
      </section>
    </template>
  </section>
</template>

<script setup>
defineProps({
  loading: Boolean,
  header: { type: Object, default: () => ({}) },
  successList: { type: Array, default: () => [] },
  items: { type: Array, default: () => [] },
  defaultAvatar: { type: String, required: true },
  assetUrl: { type: Function, required: true }
});

defineEmits(["back", "open"]);
</script>
