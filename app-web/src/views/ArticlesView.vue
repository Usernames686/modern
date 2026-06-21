<template>
  <section v-if="mode === 'list'" class="plain-view article-list-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>消息资讯</h1>
      <button type="button" @click="$emit('refresh')">刷新</button>
    </div>
    <div v-if="loading" class="state">加载中...</div>
    <template v-else>
      <section v-if="banners.length" class="article-banner">
        <img :src="assetUrl(banners[0].imageInput)" alt="资讯轮播" @click="$emit('open-detail', banners[0])" />
      </section>
      <nav class="article-tabs">
        <button :class="{ active: activeCid === '0' }" type="button" @click="$emit('select-category', '0')">热门</button>
        <button
          v-for="category in categories"
          :key="category.id"
          :class="{ active: String(category.id) === activeCid }"
          type="button"
          @click="$emit('select-category', String(category.id))"
        >
          {{ category.name }}
        </button>
      </nav>
      <section class="article-list">
        <article v-for="article in articles" :key="article.id" class="article-card" @click="$emit('open-detail', article)">
          <div>
            <h2>{{ article.title }}</h2>
            <p>{{ article.synopsis || article.author || "" }}</p>
            <span>{{ article.createTime || "-" }}</span>
          </div>
          <img v-if="article.imageInput" :src="assetUrl(article.imageInput)" :alt="article.title" />
        </article>
        <div v-if="articles.length === 0" class="coupon-empty-page">暂无消息资讯</div>
      </section>
    </template>
  </section>

  <section v-else class="plain-view article-detail-view">
    <div class="view-head">
      <button type="button" @click="$emit('back-detail')">返回</button>
      <h1>资讯详情</h1>
      <span></span>
    </div>
    <div v-if="detailLoading" class="state">加载中...</div>
    <template v-else-if="selectedArticle">
      <article class="article-detail">
        <h1>{{ selectedArticle.title }}</h1>
        <div class="article-meta">
          <span>{{ selectedArticle.author || "平台" }}</span>
          <span>{{ selectedArticle.createTime || "-" }}</span>
          <span>浏览 {{ selectedArticle.visit || 0 }}</span>
        </div>
        <div class="article-content html-content" v-html="selectedArticle.content || selectedArticle.synopsis"></div>
        <section v-if="selectedArticle.productId" class="article-product-link">
          <span>关联商品</span>
          <button type="button" @click="$emit('open-product', { id: selectedArticle.productId })">查看商品</button>
        </section>
      </article>
    </template>
    <div v-else class="state">文章不存在</div>
  </section>
</template>

<script setup>
defineProps({
  mode: { type: String, default: "list" },
  loading: Boolean,
  detailLoading: Boolean,
  categories: { type: Array, default: () => [] },
  banners: { type: Array, default: () => [] },
  articles: { type: Array, default: () => [] },
  activeCid: { type: String, default: "0" },
  selectedArticle: { type: Object, default: null },
  assetUrl: { type: Function, required: true }
});

defineEmits(["back", "refresh", "select-category", "open-detail", "back-detail", "open-product"]);
</script>
