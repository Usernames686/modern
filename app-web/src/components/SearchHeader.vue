<template>
  <section class="top-safe">
    <div class="search-row">
      <div class="location">聚商盈</div>
      <label class="search-box">
        <span class="search-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" focusable="false">
            <circle cx="11" cy="11" r="7" />
            <path d="m16.5 16.5 4 4" />
          </svg>
        </span>
        <input
          :value="modelValue"
          type="search"
          placeholder="搜索商品"
          @input="$emit('update:modelValue', $event.target.value.trim())"
          @keyup.enter="$emit('search')"
        />
      </label>
      <button class="scan-button" type="button" @click="$emit('search')">搜索</button>
    </div>
  </section>
</template>

<script setup>
defineProps({
  modelValue: { type: String, default: "" }
});

defineEmits(["update:modelValue", "search"]);
</script>
