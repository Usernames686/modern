<template>
  <section class="product-section">
    <div class="section-title">
      <h1>{{ title }}</h1>
      <span>{{ visibleTotal }} 件</span>
    </div>
    <p v-if="notice" class="section-notice">{{ notice }}</p>
    <div v-if="loading" class="state">加载中...</div>
    <div v-else-if="visibleProducts.length === 0" class="state">暂无商品</div>
    <div v-else class="product-grid">
      <article v-for="product in visibleProducts" :key="product.id" class="product-card" @click="$emit('open', product)">
        <div class="product-image-wrap">
          <SafeImage
            class="product-card-fallback"
            :src="assetUrl(product.image)"
            :alt="product.storeName"
            :label="product.storeName || '商品'"
            variant="product"
          />
          <span v-if="salesCount(product) > 0" class="product-sales-badge">已售 {{ salesCount(product) }}</span>
        </div>
        <div class="product-info">
          <div class="product-tag-row">
            <span v-if="product.layoutItem" class="product-tag">推荐</span>
            <span v-else-if="salesCount(product) > 20" class="product-tag">热卖</span>
            <span v-if="Number(product.stock || 0) > 0" class="product-tag muted">现货</span>
          </div>
          <h2>{{ product.storeName }}</h2>
          <div v-if="product.price !== '' && product.price !== undefined" class="price-row">
            <strong>￥{{ product.price }}</strong>
            <span v-if="product.otPrice">￥{{ product.otPrice }}</span>
          </div>
          <p>
            <span>{{ product.unitName || "件" }}</span>
            <em>去看看</em>
          </p>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed } from "vue";
import SafeImage from "./SafeImage.vue";

const props = defineProps({
  loading: Boolean,
  products: { type: Array, default: () => [] },
  layoutProducts: { type: Array, default: () => [] },
  useLayoutFallback: Boolean,
  productTotal: { type: Number, default: 0 },
  title: { type: String, default: "推荐商品" },
  notice: { type: String, default: "" },
  assetUrl: { type: Function, required: true }
});

const displayProducts = computed(() => props.products.length ? props.products : props.useLayoutFallback ? props.layoutProducts : []);
const visibleProducts = computed(() => displayProducts.value.filter((product) => hasProductCardContent(product)));
const visibleTotal = computed(() => props.productTotal || visibleProducts.value.length);

function hasProductCardContent(product) {
  return Boolean(
    product
      && isText(product.image)
      && isText(product.storeName)
      && product.price !== ""
      && product.price !== undefined
      && product.price !== null
  );
}

function isText(value) {
  return typeof value === "string" && value.trim() && value.trim() !== "[object Object]";
}

function salesCount(product) {
  return Number(product.sales || 0) + Number(product.ficti || 0);
}

defineEmits(["open"]);
</script>
