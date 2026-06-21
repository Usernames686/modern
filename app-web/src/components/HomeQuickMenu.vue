<template>
  <template v-if="isHome">
    <section
      v-if="bannerItems.length"
      class="banner"
      @touchstart.passive="handleTouchStart"
      @touchend.passive="handleTouchEnd"
    >
      <div class="banner-track" :style="{ transform: `translateX(-${activeIndex * 100}%)` }">
        <button
          v-for="item in bannerItems"
          :key="item.id || item.tempid || item.pic || item.image"
          type="button"
          class="banner-slide"
          @click="openCurrent(item)"
        >
          <SafeImage
            :src="assetUrl(item.pic || item.image)"
            :alt="item.name || '聚商盈'"
            :label="item.name || '聚商盈'"
            variant="banner"
            show-label
          />
        </button>
      </div>
      <div v-if="bannerItems.length > 1" class="banner-dots">
        <button
          v-for="(_, index) in bannerItems"
          :key="index"
          type="button"
          :class="{ active: index === activeIndex }"
          :aria-label="`切换到第${index + 1}张轮播图`"
          @click.stop="goTo(index)"
        />
      </div>
    </section>

    <section v-if="menus.length" class="quick-menu">
      <button v-for="item in menus" :key="item.id" type="button" @click="$emit('open-menu', item)">
        <span :class="menuIconClass(item)">{{ item.name.slice(0, 1) }}</span>
        <em>{{ item.name }}</em>
      </button>
    </section>
  </template>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import SafeImage from "./SafeImage.vue";

const props = defineProps({
  isHome: Boolean,
  mainBanner: { type: Object, default: null },
  banners: { type: Array, default: () => [] },
  menus: { type: Array, default: () => [] },
  assetUrl: { type: Function, required: true },
  menuIconClass: { type: Function, required: true }
});

const emit = defineEmits(["open-banner", "open-menu"]);

const activeIndex = ref(0);
const touchStartX = ref(0);
const didSwipe = ref(false);
let timer = null;

const bannerItems = computed(() => {
  const list = Array.isArray(props.banners) && props.banners.length ? props.banners : (props.mainBanner ? [props.mainBanner] : []);
  return list.filter((item) => item && (item.pic || item.image));
});

function stopAutoPlay() {
  if (timer) {
    window.clearInterval(timer);
    timer = null;
  }
}

function startAutoPlay() {
  stopAutoPlay();
  if (bannerItems.value.length <= 1) return;
  timer = window.setInterval(() => {
    activeIndex.value = (activeIndex.value + 1) % bannerItems.value.length;
  }, 3000);
}

function goTo(index) {
  activeIndex.value = index;
  startAutoPlay();
}

function move(delta) {
  const total = bannerItems.value.length;
  if (total <= 1) return;
  activeIndex.value = (activeIndex.value + delta + total) % total;
  startAutoPlay();
}

function handleTouchStart(event) {
  touchStartX.value = event.changedTouches?.[0]?.clientX || 0;
  didSwipe.value = false;
}

function handleTouchEnd(event) {
  const endX = event.changedTouches?.[0]?.clientX || 0;
  const distance = endX - touchStartX.value;
  if (Math.abs(distance) < 40) return;
  didSwipe.value = true;
  move(distance > 0 ? -1 : 1);
  window.setTimeout(() => {
    didSwipe.value = false;
  }, 0);
}

function openCurrent(item) {
  if (didSwipe.value) return;
  emit("open-banner", item);
}

watch(() => bannerItems.value.length, (length) => {
  if (activeIndex.value >= length) {
    activeIndex.value = 0;
  }
  startAutoPlay();
});

onMounted(startAutoPlay);
onBeforeUnmount(stopAutoPlay);
</script>
