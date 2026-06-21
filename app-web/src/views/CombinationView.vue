<template>
  <section class="combination-view">
    <div class="combination-hero">
      <button class="seckill-back" type="button" @click="$emit('back')">‹</button>
      <div class="combination-title">
        <strong>拼团活动</strong>
        <span>{{ header.title || "多人拼团更优惠" }}</span>
      </div>
      <div class="combination-total">已有 {{ header.countPeople || header.countPeopleAll || 0 }} 人参与拼团</div>
    </div>
    <div v-if="loading" class="state">加载中...</div>
    <template v-else>
      <section v-if="header.banner || header.image" class="combination-banner">
        <img :src="assetUrl(header.banner || header.image)" alt="拼团活动" />
      </section>
      <div v-if="items.length === 0" class="state">暂无拼团活动</div>
      <section v-else class="combination-product-list">
        <article v-for="item in items" :key="item.id" class="combination-product" @click="$emit('open', item)">
          <img :src="assetUrl(item.image)" :alt="item.title" />
          <div class="combination-product-main">
            <h2>{{ item.title }}</h2>
            <div class="combination-price">
              <strong>￥{{ item.price }}</strong>
              <span v-if="item.otPrice">￥{{ item.otPrice }}</span>
            </div>
            <p>
              <em>{{ item.people || 0 }}人团</em>
              <span>已拼 {{ item.countPeople || item.sales || 0 }} 件</span>
            </p>
            <div class="combination-meta">
              <span>限量 {{ item.quota || item.stock || 0 }} {{ item.unitName || "件" }}</span>
              <span>{{ item.stopTimeText || dateText(item.stopTime) }}</span>
            </div>
          </div>
          <button type="button" :class="{ ended: buttonText(item) !== '去拼团' }">
            {{ buttonText(item) }}
          </button>
        </article>
      </section>
    </template>
  </section>
</template>

<script setup>
defineProps({
  loading: Boolean,
  header: { type: Object, default: () => ({}) },
  items: { type: Array, default: () => [] },
  assetUrl: { type: Function, required: true },
  buttonText: { type: Function, required: true },
  dateText: { type: Function, required: true }
});

defineEmits(["back", "open"]);
</script>
