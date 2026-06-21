<template>
  <section class="plain-view comment-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>商品评价</h1>
      <span></span>
    </div>
    <div v-if="loading" class="state">加载中...</div>
    <form v-else-if="goods" class="comment-form" @submit.prevent="$emit('submit')">
      <section class="comment-goods-card">
        <img :src="assetUrl(goods.image)" :alt="goods.productName" />
        <div>
          <h2>{{ goods.productName || goods.storeName }}</h2>
          <p v-if="goods.sku">{{ goods.sku }}</p>
          <span>￥{{ goods.price || goods.truePrice }} × {{ goods.payNum || goods.cartNum }}</span>
        </div>
      </section>

      <section class="comment-score-card">
        <div class="comment-score-row">
          <span>商品质量</span>
          <div class="comment-stars">
            <button
              v-for="star in 5"
              :key="'product-' + star"
              :class="{ active: productScore >= star }"
              type="button"
              @click="$emit('update:productScore', star)"
            >
              ★
            </button>
          </div>
          <em>{{ productScore ? productScore + "分" : "" }}</em>
        </div>
        <div class="comment-score-row">
          <span>服务态度</span>
          <div class="comment-stars">
            <button
              v-for="star in 5"
              :key="'service-' + star"
              :class="{ active: serviceScore >= star }"
              type="button"
              @click="$emit('update:serviceScore', star)"
            >
              ★
            </button>
          </div>
          <em>{{ serviceScore ? serviceScore + "分" : "" }}</em>
        </div>
        <textarea
          :value="comment"
          maxlength="512"
          rows="5"
          placeholder="商品满足你的期待么？说说你的想法，分享给想买的他们吧~"
          @input="$emit('update:comment', $event.target.value.trim())"
        ></textarea>
        <div class="comment-pic-list">
          <figure v-for="(pic, index) in pics" :key="pic.localPath || pic.url">
            <img :src="assetUrl(pic.localPath || pic.url)" alt="" />
            <button type="button" @click="$emit('remove-pic', index)">×</button>
          </figure>
          <label v-if="pics.length < 8" class="comment-upload">
            <input type="file" accept="image/*" :disabled="uploading" @change="$emit('upload', $event)" />
            <strong>＋</strong>
            <span>{{ uploading ? "上传中" : "上传图片" }}</span>
          </label>
        </div>
        <button class="comment-submit" type="submit" :disabled="submitting">
          {{ submitting ? "正在发布评论..." : "立即评价" }}
        </button>
      </section>
    </form>
  </section>
</template>

<script setup>
defineProps({
  loading: Boolean,
  goods: { type: Object, default: null },
  productScore: { type: Number, default: 0 },
  serviceScore: { type: Number, default: 0 },
  comment: { type: String, default: "" },
  pics: { type: Array, default: () => [] },
  uploading: Boolean,
  submitting: Boolean,
  assetUrl: { type: Function, required: true }
});

defineEmits([
  "back",
  "submit",
  "update:productScore",
  "update:serviceScore",
  "update:comment",
  "upload",
  "remove-pic"
]);
</script>
