<template>
  <template v-if="isHome || isCategory">
    <SearchHeader :model-value="keyword" @update:model-value="$emit('update-keyword', $event)" @search="$emit('search')" />

    <HomeQuickMenu
      :is-home="isHome"
      :main-banner="mainBanner"
      :banners="banners"
      :menus="menus"
      :asset-url="assetUrl"
      :menu-icon-class="menuIconClass"
      @open-banner="$emit('open-banner', $event)"
      @open-menu="$emit('open-menu', $event)"
    />

    <HomeDiyRenderer
      v-if="isHome"
      :components="diyComponents"
      :asset-url="assetUrl"
      @open-link="$emit('open-diy-link', $event)"
    />

    <CategoryNav
      :is-home="isHome"
      :is-category="isCategory"
      :home-categories="homeCategories"
      :categories="categories"
      :flat-categories="flatCategories"
      :active-cid="activeCid"
      :asset-url="assetUrl"
      @select-category="$emit('select-category', $event)"
      @show-more="$emit('show-more')"
    />

    <section class="tab-row" v-if="isHome">
      <button
        v-for="tab in tabs"
        :key="tab.type"
        :class="{ active: String(activeType) === String(tab.type) }"
        type="button"
        @click="$emit('select-type', tab.type)"
      >
        {{ tab.name }}
      </button>
    </section>

    <ProductList
      :loading="loading"
      :products="products"
      :layout-products="homeIndexProducts"
      :use-layout-fallback="isHome"
      :product-total="productTotal"
      :title="title"
      :notice="productNotice"
      :asset-url="assetUrl"
      @open="$emit('open-product', $event)"
    />
  </template>
</template>

<script setup>
import CategoryNav from "../components/CategoryNav.vue";
import HomeDiyRenderer from "../components/HomeDiyRenderer.vue";
import HomeQuickMenu from "../components/HomeQuickMenu.vue";
import ProductList from "../components/ProductList.vue";
import SearchHeader from "../components/SearchHeader.vue";

defineProps({
  isHome: Boolean,
  isCategory: Boolean,
  keyword: { type: String, default: "" },
  mainBanner: { type: Object, default: null },
  banners: { type: Array, default: () => [] },
  menus: { type: Array, default: () => [] },
  diyComponents: { type: Array, default: () => [] },
  categories: { type: Array, default: () => [] },
  homeCategories: { type: Array, default: () => [] },
  flatCategories: { type: Array, default: () => [] },
  activeCid: { type: [Number, String], default: "" },
  tabs: { type: Array, default: () => [] },
  activeType: { type: [Number, String], default: 0 },
  homeIndexProducts: { type: Array, default: () => [] },
  loading: Boolean,
  products: { type: Array, default: () => [] },
  productTotal: { type: [Number, String], default: 0 },
  title: { type: String, default: "" },
  productNotice: { type: String, default: "" },
  assetUrl: { type: Function, required: true },
  menuIconClass: { type: Function, required: true }
});

defineEmits([
  "update-keyword",
  "search",
  "open-banner",
  "open-menu",
  "open-diy-link",
  "select-category",
  "show-more",
  "select-type",
  "open-product"
]);
</script>
