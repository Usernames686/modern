<template>
  <section class="plain-view store-list-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>门店列表</h1>
      <button type="button" @click="$emit('refresh')">刷新</button>
    </div>

    <div v-if="loading" class="state">加载中...</div>
    <div v-else-if="stores.length === 0" class="coupon-empty-page">暂无门店信息</div>
    <section v-else class="store-page-list">
      <article v-for="store in stores" :key="store.id" class="store-page-item">
        <img :src="assetUrl(store.image)" :alt="store.name" />
        <div>
          <h2>{{ store.name }}</h2>
          <p>{{ store.fullAddress || `${store.address || ''}${store.detailedAddress ? ', ' + store.detailedAddress : ''}` }}</p>
          <span>{{ store.phone || "暂无电话" }}</span>
        </div>
        <nav>
          <a v-if="store.phone" :href="`tel:${store.phone}`">拨号</a>
          <button type="button" @click="$emit('map', store)">地图</button>
        </nav>
      </article>
    </section>
  </section>
</template>

<script setup>
defineProps({
  loading: Boolean,
  stores: { type: Array, default: () => [] },
  assetUrl: { type: Function, required: true }
});

defineEmits(["back", "refresh", "map"]);
</script>
