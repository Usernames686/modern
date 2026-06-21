<template>
  <section class="seckill-view">
    <div class="seckill-hero">
      <button class="seckill-back" type="button" @click="$emit('back')">‹</button>
      <div class="seckill-title">
        <strong>限时秒杀</strong>
        <span>限量好物限时抢购</span>
      </div>
    </div>
    <div v-if="loading" class="state">加载中...</div>
    <template v-else>
      <section v-if="currentHeader && currentSlides.length" class="seckill-banner">
        <img :src="assetUrl(currentSlides[0].sattDir || currentSlides[0].attDir || currentSlides[0].url)" alt="秒杀轮播" />
      </section>
      <section v-if="headers.length" class="seckill-time-list">
        <button
          v-for="item in headers"
          :key="item.id"
          :class="{ active: Number(item.id) === Number(activeTimeId) }"
          type="button"
          @click="$emit('select-time', item)"
        >
          <strong>{{ String(item.time || "").split(",")[0] }}</strong>
          <span>{{ item.statusName }}</span>
        </button>
      </section>
      <div v-if="!headers.length" class="state">暂无秒杀活动</div>
      <div v-else-if="items.length === 0" class="state">当前时段暂无秒杀商品</div>
      <section v-else class="seckill-product-list">
        <article v-for="item in items" :key="item.id" class="seckill-product" @click="$emit('open', item)">
          <img :src="assetUrl(item.image)" :alt="item.title" />
          <div class="seckill-product-main">
            <h2>{{ item.title }}</h2>
            <div class="seckill-price">
              <strong>￥{{ item.price }}</strong>
              <span>￥{{ item.otPrice }}</span>
            </div>
            <p>限量 <em>{{ item.quota }} {{ item.unitName || "件" }}</em></p>
            <div class="seckill-progress">
              <i :style="{ width: `${Math.max(0, Math.min(100, Number(item.percent || 0)))}%` }"></i>
              <span>已抢{{ item.percent || 0 }}%</span>
            </div>
          </div>
          <button type="button" :class="{ ended: buttonText(item) !== '马上抢' }">
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
  currentHeader: { type: Object, default: null },
  currentSlides: { type: Array, default: () => [] },
  headers: { type: Array, default: () => [] },
  activeTimeId: { type: [Number, String], default: "" },
  items: { type: Array, default: () => [] },
  assetUrl: { type: Function, required: true },
  buttonText: { type: Function, required: true }
});

defineEmits(["back", "select-time", "open"]);
</script>
