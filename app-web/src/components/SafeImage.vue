<template>
  <span class="safe-image" :class="[`safe-image-${variant}`, { failed: !displaySrc }]" :title="alt">
    <img v-if="displaySrc" :src="displaySrc" :alt="alt" @error="failed = true" />
    <span v-else class="safe-image-fallback">
      <b>{{ fallbackText }}</b>
      <em v-if="showLabel">{{ alt || label || "聚商盈" }}</em>
    </span>
  </span>
</template>

<script setup>
import { computed, ref, watch } from "vue";

const props = defineProps({
  src: { type: String, default: "" },
  alt: { type: String, default: "" },
  label: { type: String, default: "" },
  variant: { type: String, default: "cover" },
  showLabel: Boolean
});

const failed = ref(false);

watch(
  () => props.src,
  () => {
    failed.value = false;
  }
);

const displaySrc = computed(() => (props.src && !failed.value ? props.src : ""));
const fallbackText = computed(() => {
  if (props.variant === "product") {
    return "品";
  }
  return String(props.label || props.alt || "聚").trim().slice(0, 1) || "聚";
});
</script>
