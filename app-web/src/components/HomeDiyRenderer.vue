<template>
  <section v-if="visibleComponents.length" class="home-diy">
    <article
      v-for="component in visibleComponents"
      :key="component.uid"
      class="home-diy-component"
      :style="{ backgroundColor: component.bgColor || undefined }"
    >
      <template v-if="component.type === 'search_box'">
        <button class="home-diy-search" type="button" @click="openComponent(component, '/pages/goods/goods_search/index')">
          {{ component.hotWord || component.title || '搜索商品' }}
        </button>
      </template>

      <template v-else-if="component.type === 'home_comb'">
        <div class="home-diy-comb">
          <div class="home-diy-comb-search">
            <SafeImage v-if="component.logo" :src="assetUrl(component.logo)" alt="logo" label="聚商盈" variant="logo" />
            <button type="button" @click="openComponent(component, '/pages/goods/goods_search/index')">
              {{ component.hotWord || '搜索商品名称' }}
            </button>
          </div>
          <div v-if="component.menus.length" class="home-diy-nav">
            <button v-for="item in limitedItems(component.menus, 10)" :key="item.uid" type="button" @click="openItem(item, component)">
              {{ item.title }}
            </button>
          </div>
          <div v-if="imageItems(component.images, 3).length" class="home-diy-banner">
            <button v-for="item in imageItems(component.images, 3)" :key="item.uid" type="button" @click="openItem(item, component)">
              <SafeImage :src="assetUrl(item.image)" :alt="item.title" :label="item.title || component.title" variant="banner" show-label />
            </button>
          </div>
        </div>
      </template>

      <template v-else-if="component.type === 'banner' || component.type === 'picture_cube'">
        <div :class="component.type === 'picture_cube' ? 'home-diy-cube' : 'home-diy-banner'">
          <button
            v-for="item in imageItems(component.images, 6)"
            :key="item.uid"
            type="button"
            @click="openItem(item, component)"
          >
            <SafeImage :src="assetUrl(item.image)" :alt="item.title" :label="item.title || component.title" variant="cover" show-label />
          </button>
        </div>
      </template>

      <template v-else-if="component.type === 'home_menu'">
        <div class="home-diy-menu">
          <button
            v-for="item in limitedItems(component.menus, component.limit || 10)"
            :key="item.uid"
            type="button"
            @click="openItem(item, component)"
          >
            <SafeImage v-if="item.image" :src="assetUrl(item.image)" :alt="item.title" :label="item.title" variant="icon" />
            <span v-else>{{ String(item.title || '菜').slice(0, 1) }}</span>
            <em>{{ item.title }}</em>
          </button>
        </div>
      </template>

      <template v-else-if="component.type === 'nav_bar' || component.type === 'home_tab'">
        <div class="home-diy-nav">
          <button v-for="item in limitedItems(component.menus, 10)" :key="item.uid" type="button" @click="openItem(item, component)">
            {{ item.title }}
          </button>
        </div>
        <button v-if="component.type === 'home_tab'" class="home-diy-tab-goods" type="button" @click="openComponent(component, '/pages/goods/goods_list/index')">
          <span>{{ component.title || '商品推荐' }}</span>
          <strong>查看商品</strong>
          <em>{{ Number(component.limit || 6) }} 款商品</em>
        </button>
      </template>

      <template v-else-if="component.type === 'home_news_roll'">
        <button class="home-diy-news" type="button" @click="openItem(firstItem(component.notices), component)">
          <SafeImage v-if="component.logo" :src="assetUrl(component.logo)" alt="news" label="讯" variant="icon" />
          <strong>快讯</strong>
          <span>{{ firstItem(component.notices).title || component.title || '暂无公告' }}</span>
        </button>
      </template>

      <template v-else-if="component.type === 'home_title'">
        <button
          class="home-diy-title"
          type="button"
          :style="component.bgImage ? { backgroundImage: `url(${assetUrl(component.bgImage)})` } : undefined"
          @click="openComponent(component)"
        >
          <span class="home-diy-title-text">
            <strong>{{ component.title || component.cname || '标题' }}</strong>
            <small v-if="component.subTitle">{{ component.subTitle }}</small>
          </span>
          <span v-if="component.showMore && component.link" class="home-diy-title-more">{{ component.rightText || '更多' }}</span>
        </button>
      </template>

      <template v-else-if="component.type === 'z_ueditor' && component.text">
        <div class="home-diy-rich html-content" v-html="component.text"></div>
      </template>

      <template v-else-if="component.type === 'z_auxiliary_line'">
        <div class="home-diy-line"></div>
      </template>

      <template v-else-if="component.type === 'z_auxiliary_box'">
        <div class="home-diy-space" :style="{ height: `${Math.max(8, Number(component.height || component.limit || 18))}px` }"></div>
      </template>

      <template v-else-if="component.type === 'home_video'">
        <video
          v-if="component.videoUrl || component.link"
          class="home-diy-video"
          :src="assetUrl(component.videoUrl || component.link)"
          :poster="component.coverImage ? assetUrl(component.coverImage) : undefined"
          controls
          playsinline
          preload="metadata"
        ></video>
        <button v-else class="home-diy-video home-diy-video-empty" type="button" @click="openComponent(component)">
          <span>视频</span>
          <strong>{{ component.title || '视频内容' }}</strong>
        </button>
      </template>

      <template v-else-if="component.type === 'home_hotspot'">
        <div class="home-diy-hotspot">
          <SafeImage
            v-if="hotspotImage(component)"
            :src="assetUrl(hotspotImage(component))"
            :alt="component.title || '热区'"
            :label="component.title || '活动'"
            variant="banner"
            show-label
          />
          <button
            v-if="!component.hotspots?.length"
            type="button"
            class="home-diy-hotspot-full"
            @click="openComponent(component)"
          ></button>
          <button
            v-for="area in component.hotspots"
            :key="area.uid"
            type="button"
            class="home-diy-hotspot-area"
            :style="hotspotStyle(area)"
            @click="openItem(area, component)"
          >
            <span>{{ area.title }}</span>
          </button>
        </div>
      </template>

      <template v-else-if="component.type === 'home_footer'">
        <div class="home-diy-footer">
          <button v-for="item in limitedItems(component.footerItems, 5)" :key="item.uid" type="button" @click="openItem(item, component)">
            <SafeImage v-if="item.inactiveImage" :src="assetUrl(item.inactiveImage)" :alt="item.title" :label="item.title" variant="icon" />
            <span>{{ item.title }}</span>
          </button>
        </div>
      </template>

      <template v-else-if="component.type === 'home_merchant'">
        <div class="home-diy-merchant">
          <button class="home-diy-merchant-head" type="button" @click="openComponent(component, '/pages/goods/goods_list/index')">
            <span>
              <SafeImage v-if="component.logo" :src="assetUrl(component.logo)" alt="merchant" label="店" variant="icon" />
              <strong>{{ component.title || '推荐商户' }}</strong>
            </span>
            <em>更多</em>
          </button>
          <div class="home-diy-merchant-list">
            <button
              v-for="item in limitedItems(component.merchants, component.limit || 3)"
              :key="item.uid"
              type="button"
              @click="openItem(item, component)"
            >
              <SafeImage :src="assetUrl(item.image)" :alt="item.title" :label="item.title || '店铺'" variant="cover" />
              <strong>{{ item.title || '推荐店铺' }}</strong>
              <em>{{ item.subTitle || item.label || '进店看看' }}</em>
            </button>
          </div>
        </div>
      </template>

      <template v-else-if="component.type === 'home_coupon' && component.couponItems?.length">
        <div class="home-diy-coupons">
          <button class="home-diy-section-head" type="button" @click="openComponent(component, '/pages/activity/couponList/index')">
            <strong>{{ component.title || '优惠券' }}</strong>
            <span>更多</span>
          </button>
          <div class="home-diy-coupon-list">
            <button v-for="item in limitedItems(component.couponItems, component.limit || 3)" :key="item.uid" type="button" @click="openItem(item, component)">
              <strong>￥{{ item.money || '0' }}</strong>
              <span>{{ item.limitText || '领取后可用' }}</span>
              <em>领取</em>
            </button>
          </div>
        </div>
      </template>

      <template v-else-if="component.type === 'home_article' && component.articleItems?.length">
        <div class="home-diy-articles">
          <button class="home-diy-section-head" type="button" @click="openComponent(component, '/pages/news/news_list/index')">
            <strong>{{ component.title || '文章资讯' }}</strong>
            <span>更多</span>
          </button>
          <button v-for="item in limitedItems(component.articleItems, component.limit || 4)" :key="item.uid" class="home-diy-article" type="button" @click="openItem(item, component)">
            <SafeImage :src="assetUrl(item.image)" :alt="item.title" :label="item.title || '资讯'" variant="cover" />
            <strong>{{ item.title || '文章标题' }}</strong>
            <em>{{ item.time || '查看详情' }}</em>
          </button>
        </div>
      </template>

      <template v-else-if="productTypes.includes(component.type) && component.productItems?.length">
        <div class="home-diy-products" :class="`style-${Number(component.itemStyle || 0)}`">
          <button class="home-diy-section-head" type="button" @click="openComponent(component)">
            <strong>{{ marketTitle(component) }}</strong>
            <span>更多</span>
          </button>
          <div class="home-diy-product-list">
            <button v-for="item in limitedItems(component.productItems, component.limit || 4)" :key="item.uid" type="button" @click="openItem(item, component)">
              <SafeImage :src="assetUrl(item.image)" :alt="item.title" :label="item.title || '商品'" variant="product" />
              <strong>{{ item.title || '商品名称' }}</strong>
              <em>{{ productPricePrefix(component) }}￥{{ item.price || '0.00' }}</em>
              <small v-if="item.sales">已售 {{ item.sales }} {{ item.unitName || '件' }}</small>
            </button>
          </div>
        </div>
      </template>

      <template v-else-if="marketingTypes.includes(component.type)">
        <button class="home-diy-market" type="button" @click="openComponent(component)">
          <strong>{{ marketTitle(component) }}</strong>
          <span>{{ marketSubTitle(component) }}</span>
        </button>
      </template>
    </article>
  </section>
</template>

<script setup>
import { computed } from "vue";
import SafeImage from "./SafeImage.vue";

const props = defineProps({
  components: { type: Array, default: () => [] },
  compact: Boolean,
  assetUrl: { type: Function, required: true }
});

const emit = defineEmits(["open-link"]);

const marketingTypes = ["home_goods_list", "home_coupon", "home_seckill", "home_group", "home_bargain", "home_article", "home_comb"];
const productTypes = ["home_goods_list", "home_seckill", "home_group", "home_bargain"];
const compactHomeTypes = ["home_coupon", "home_seckill", "home_group", "home_bargain"];

const visibleComponents = computed(() =>
  props.components
    .filter((component) => component && !component.isHide && supportedTypes.includes(component.type) && hasRenderableContent(component))
    .filter((component) => !props.compact || compactHomeTypes.includes(component.type))
    .sort((left, right) => componentWeight(left) - componentWeight(right))
    .slice(0, props.compact ? 2 : 40)
);

const supportedTypes = [
  "search_box",
  "banner",
  "home_menu",
  "menus",
  "nav_bar",
  "home_tab",
  "home_news_roll",
  "picture_cube",
  "home_title",
  "z_ueditor",
  "z_auxiliary_line",
  "z_auxiliary_box",
  "home_video",
  "home_hotspot",
  "home_footer",
  "home_merchant",
  ...marketingTypes
];

function limitedItems(items, limit) {
  return (items || []).filter((item) => item?.image || item?.title).slice(0, limit);
}

function componentWeight(component) {
  return component.type === "home_menu" || component.type === "menus" ? -1 : 0;
}

function imageItems(items, limit) {
  return (items || []).filter((item) => hasImage(item?.image)).slice(0, limit);
}

function hasRenderableContent(component) {
  if (component.type === "home_menu" || component.type === "menus") {
    return false;
  }
  if (component.type === "home_comb" || component.type === "home_news_roll") {
    return false;
  }
  if (component.type === "home_title" || component.type === "home_footer") {
    return false;
  }
  if (component.type === "banner" || component.type === "picture_cube") {
    return imageItems(component.images, 1).length > 0;
  }
  if (component.type === "home_comb") {
    return Boolean(component.logo || component.hotWord || component.menus?.length || imageItems(component.images, 1).length);
  }
  if (component.type === "home_hotspot") {
    return hasImage(hotspotImage(component));
  }
  if (component.type === "home_merchant") {
    return Boolean(component.title || component.logo || imageItems(component.merchants, 1).length);
  }
  if (component.type === "home_article") {
    return imageItems(component.articleItems, 1).length > 0;
  }
  if (component.type === "home_coupon") {
    return component.couponItems?.length > 0;
  }
  if (component.type === "home_goods_list") {
    return false;
  }
  if (productTypes.includes(component.type)) {
    return imageItems(component.productItems, 1).length > 0;
  }
  if (marketingTypes.includes(component.type)) {
    return false;
  }
  if (component.type === "z_ueditor") {
    return typeof component.text === "string" && component.text.trim() && component.text.trim() !== "[object Object]";
  }
  return true;
}

function hasImage(value) {
  const text = typeof value === "string" ? value.trim() : "";
  return Boolean(text) && text !== "[object Object]" && !/^data:image\//i.test(text);
}

function firstItem(items) {
  return (items || [])[0] || {};
}

function hotspotImage(component) {
  return component.image || component.images?.[0]?.image || "";
}

function openItem(item, component) {
  emit("open-link", {
    ...item,
    componentType: component.type,
    componentTitle: component.title || component.cname || component.label,
    fallbackLink: component.link
  });
}

function openComponent(component, fallbackLink = "") {
  emit("open-link", {
    title: component.title || component.cname || component.label,
    link: component.link || fallbackLink,
    componentType: component.type,
    componentTitle: component.title || component.cname || component.label
  });
}

function marketTitle(component) {
  return component.title || component.cname || {
    home_goods_list: "精选商品",
    home_coupon: "优惠券",
    home_merchant: "推荐商户",
    home_seckill: "限时秒杀",
    home_group: "拼团专区",
    home_bargain: "砍价活动",
    home_article: "文章资讯"
  }[component.type] || "活动";
}

function marketSubTitle(component) {
  return {
    home_goods_list: "进入商品列表",
    home_coupon: "领取优惠券",
    home_merchant: "发现好店",
    home_seckill: "马上抢购",
    home_group: "好友拼团",
    home_bargain: "参与砍价",
    home_article: "查看资讯"
  }[component.type] || (component.link ? "查看详情" : "");
}

function productPricePrefix(component) {
  return {
    home_seckill: "秒杀 ",
    home_group: "拼团 ",
    home_bargain: "低至 "
  }[component.type] || "";
}

function hotspotStyle(area) {
  return {
    left: `${clampPercent(area.x)}%`,
    top: `${clampPercent(area.y)}%`,
    width: `${clampPercent(area.width, 1)}%`,
    height: `${clampPercent(area.height, 1)}%`
  };
}

function clampPercent(value, min = 0) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return min;
  }
  return Math.max(min, Math.min(100, Number(number.toFixed(1))));
}
</script>
