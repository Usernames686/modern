<template>
  <aside class="store-mask" @click.self="$emit('close')">
    <section class="store-panel">
      <div class="coupon-head">
        <strong>选择提货点</strong>
        <button type="button" @click="$emit('close')">×</button>
      </div>
      <div v-if="loading" class="state">加载中...</div>
      <div v-else-if="stores.length === 0" class="coupon-empty">暂无门店信息</div>
      <div v-else class="store-list">
        <article
          v-for="store in stores"
          :key="store.id"
          class="store-item"
          :class="{ active: selectedStore?.id === store.id }"
          @click="$emit('choose', store)"
        >
          <img :src="assetUrl(store.image)" :alt="store.name" />
          <div>
            <h2>{{ store.name }}</h2>
            <p>{{ store.fullAddress || store.address + ', ' + store.detailedAddress }}</p>
            <span>{{ store.phone }}</span>
          </div>
          <em>{{ selectedStore?.id === store.id ? "已选择" : "选择" }}</em>
        </article>
      </div>
    </section>
  </aside>
</template>

<script setup>
defineProps({
  loading: Boolean,
  stores: { type: Array, default: () => [] },
  selectedStore: { type: Object, default: null },
  assetUrl: { type: Function, required: true }
});

defineEmits(["close", "choose"]);
</script>
