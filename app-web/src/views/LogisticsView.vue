<template>
  <section class="plain-view logistics-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>物流信息</h1>
      <span></span>
    </div>
    <div v-if="loading" class="state">加载中...</div>
    <template v-else-if="info">
      <section class="logistics-product" v-if="product">
        <img :src="assetUrl(product.productImg)" :alt="product.productName" />
        <div>
          <h2>{{ product.productName }}</h2>
          <span>￥{{ product.price }} × {{ product.payNum }}</span>
        </div>
      </section>
      <section class="logistics-card">
        <div class="logistics-company">
          <span>物流</span>
          <div>
            <p><strong>物流公司：</strong>{{ info.order?.deliveryName || "-" }}</p>
            <p><strong>快递单号：</strong>{{ info.order?.deliveryId || "-" }}</p>
          </div>
          <button type="button" @click="$emit('copy')">复制单号</button>
        </div>
        <div v-if="manualCopyId" class="logistics-manual-copy">
          <strong>手动复制单号</strong>
          <input :value="manualCopyId" readonly @focus="$event.target.select()" />
          <button type="button" @click="$emit('close-manual-copy')">收起</button>
        </div>
        <div v-if="tracks.length === 0" class="state">暂无物流轨迹</div>
        <div v-for="(item, index) in tracks" :key="index" class="logistics-track">
          <i :class="{ active: index === 0 }"></i>
          <div :class="{ active: index === 0 }">
            <p>{{ item.status || item.context || item.text }}</p>
            <span>{{ item.time || item.ftime || "" }}</span>
          </div>
        </div>
      </section>
    </template>
  </section>
</template>

<script setup>
defineProps({
  loading: Boolean,
  info: { type: Object, default: null },
  product: { type: Object, default: null },
  tracks: { type: Array, default: () => [] },
  manualCopyId: { type: String, default: "" },
  assetUrl: { type: Function, required: true }
});

defineEmits(["back", "copy", "close-manual-copy"]);
</script>
