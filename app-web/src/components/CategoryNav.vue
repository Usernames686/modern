<template>
  <template v-if="isHome">
    <section v-if="homeCategories.length" class="category-grid">
      <button v-for="category in homeCategories" :key="category.id" type="button" @click="$emit('select-category', category.id)">
        <span class="category-icon">
          <SafeImage v-if="category.extra" :src="assetUrl(category.extra)" :alt="category.name" :label="category.name" variant="icon" />
          <b v-else>{{ category.name.slice(0, 1) }}</b>
        </span>
        <em>{{ category.name }}</em>
      </button>
      <button v-if="flatCategories.length > homeCategories.length" type="button" @click="$emit('show-more')">
        <span class="category-icon more-icon">•••</span>
        <em>更多</em>
      </button>
    </section>
  </template>

  <section v-if="isCategory && categories.length" class="category-panel">
    <aside class="category-side">
      <button
        v-for="category in categories"
        :key="category.id"
        :class="{ active: isActiveParent(category) }"
        type="button"
        @click="$emit('select-category', category.id)"
      >
        {{ category.name }}
      </button>
    </aside>
    <div class="category-main">
      <div v-if="activeParent" class="category-main-head">
        <strong>{{ activeParent.name }}</strong>
        <button type="button" @click="$emit('select-category', activeParent.id)">全部</button>
      </div>
      <div class="category-child-grid">
        <button
          v-for="category in activeChildren"
          :key="category.id"
          :class="{ active: String(activeCid) === String(category.id) }"
          type="button"
          @click="$emit('select-category', category.id)"
        >
          <span class="category-icon">
            <SafeImage v-if="category.extra" :src="assetUrl(category.extra)" :alt="category.name" :label="category.name" variant="icon" />
            <b v-else>{{ category.name.slice(0, 1) }}</b>
          </span>
          <em>{{ category.name }}</em>
        </button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from "vue";
import SafeImage from "./SafeImage.vue";

const props = defineProps({
  isHome: Boolean,
  isCategory: Boolean,
  categories: { type: Array, default: () => [] },
  homeCategories: { type: Array, default: () => [] },
  flatCategories: { type: Array, default: () => [] },
  activeCid: { type: String, default: "" },
  assetUrl: { type: Function, required: true }
});

defineEmits(["select-category", "show-more"]);

const activeParent = computed(() => {
  if (!props.categories.length) {
    return null;
  }
  return props.categories.find(isActiveParent) || props.categories[0];
});

const activeChildren = computed(() => {
  const children = activeParent.value?.child || [];
  return children.length ? children : [activeParent.value].filter(Boolean);
});

function isActiveParent(category) {
  const ids = [category.id, ...(category.child || []).map((item) => item.id)].map(String);
  return ids.includes(String(props.activeCid));
}
</script>
