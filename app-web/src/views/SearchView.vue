<template>
  <section class="plain-view search-view">
    <div class="search-view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <label class="search-view-box">
        <span class="search-view-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" focusable="false">
            <circle cx="11" cy="11" r="7" />
            <path d="m16.5 16.5 4 4" />
          </svg>
        </span>
        <input
          :value="keyword"
          type="search"
          placeholder="搜索商品名称 / 关键词"
          autofocus
          @input="$emit('update-keyword', $event.target.value.trim())"
          @keyup.enter="$emit('search')"
        />
      </label>
      <button type="button" class="search-view-submit" @click="$emit('search')">搜索</button>
    </div>

    <section v-if="hotKeywords.length" class="search-hot-panel">
      <div class="search-section-head">
        <h2>热门搜索</h2>
        <span>{{ hotKeywords.length }} 个</span>
      </div>
      <div class="search-hot-list">
        <button
          v-for="item in hotKeywords"
          :key="keywordText(item)"
          type="button"
          @click="$emit('hot-search', keywordText(item))"
        >
          {{ keywordText(item) }}
        </button>
      </div>
    </section>

    <ProductList
      :loading="loading"
      :products="products"
      :product-total="productTotal"
      :title="resultTitle"
      :asset-url="assetUrl"
      @open="$emit('open-product', $event)"
    />
  </section>
</template>

<script setup>
import { computed } from "vue";
import ProductList from "../components/ProductList.vue";

const props = defineProps({
  keyword: { type: String, default: "" },
  hotKeywords: { type: Array, default: () => [] },
  loading: Boolean,
  products: { type: Array, default: () => [] },
  productTotal: { type: Number, default: 0 },
  assetUrl: { type: Function, required: true }
});

const resultTitle = computed(() => (props.keyword ? `搜索「${props.keyword}」` : "搜索商品"));

function keywordText(item) {
  return String(item?.keyword || item?.title || item?.name || item || "").trim();
}

defineEmits(["back", "update-keyword", "search", "hot-search", "open-product"]);
</script>
