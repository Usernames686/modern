<template>
  <aside v-if="open" class="detail-mask" @click.self="$emit('close')">
    <article class="detail-panel">
      <button class="close-button" type="button" @click="$emit('close')">×</button>
      <div v-if="loading" class="state detail-state">加载中...</div>
      <template v-else-if="detail">
        <div class="detail-gallery">
          <SafeImage :src="assetUrl(detailImage)" :alt="detail.productInfo.storeName" :label="detail.productInfo.storeName" variant="product" />
        </div>
        <div class="detail-body">
          <div class="price-row detail-price">
            <strong>￥{{ selectedSku?.price || detail.productInfo.price }}</strong>
            <span v-if="selectedSku?.otPrice || detail.productInfo.otPrice">￥{{ selectedSku?.otPrice || detail.productInfo.otPrice }}</span>
            <em v-if="vipPrice > 0">VIP ￥{{ vipPrice }}</em>
          </div>
          <h2>{{ detail.productInfo.storeName }}</h2>
          <div class="detail-meta-row">
            <span>原价:￥{{ selectedSku?.otPrice || detail.productInfo.otPrice || 0 }}</span>
            <span>库存:{{ (selectedSku?.stock ?? detail.productInfo.stock) || 0 }}{{ detail.productInfo.unitName || "" }}</span>
            <span>销量:{{ salesCount }}{{ detail.productInfo.unitName || "" }}</span>
          </div>
          <div v-if="purchaseDisabledReason" class="detail-stock-alert">
            {{ purchaseDisabledReason }}
          </div>
          <button v-if="firstCoupon" class="detail-coupon-entry" type="button" @click="couponOpen = true">
            <span>优惠券</span>
            <strong>满{{ firstCoupon.minPrice }}减{{ firstCoupon.money }}</strong>
            <em>领取</em>
          </button>
          <div v-if="detail.productAttr && detail.productAttr.length" class="sku-box">
            <div v-for="attr in detail.productAttr" :key="attr.id" class="sku-line">
              <span>{{ attr.attrName }}</span>
              <div class="sku-options">
                <button
                  v-for="value in parseAttrValues(attr.attrValues)"
                  :key="value"
                  type="button"
                  :class="{ active: selectedAttrs[attr.attrName] === value }"
                  @click="$emit('select-attr', { attrName: attr.attrName, value })"
                >
                  {{ value }}
                </button>
              </div>
            </div>
          </div>
          <div class="sku-line quantity-line">
            <span>数量</span>
            <div class="quantity-stepper">
              <button type="button" :disabled="Boolean(purchaseDisabledReason)" @click="$emit('change-cart-num', cartNum - 1)">-</button>
              <input
                :value="cartNum"
                inputmode="numeric"
                pattern="[0-9]*"
                :disabled="Boolean(purchaseDisabledReason)"
                @input="$emit('change-cart-num', $event.target.value)"
              />
              <button type="button" :disabled="Boolean(purchaseDisabledReason)" @click="$emit('change-cart-num', cartNum + 1)">+</button>
            </div>
          </div>
          <button class="detail-reply-entry" type="button" @click="$emit('open-replies', detail.productInfo.id)">
            <span>用户评价({{ detail.productInfo.replyNum || 0 }})</span>
            <strong>好评 {{ detail.productInfo.positiveRatio || 0 }}%</strong>
          </button>
          <article v-if="firstReply" class="detail-reply-card">
            <div class="detail-reply-user">
              <img :src="assetUrl(firstReply.avatar || defaultAvatar)" alt="" />
              <span>
                <strong>{{ firstReply.nickname || `用户${firstReply.uid || ""}` }}</strong>
                <em>{{ firstReply.createTime || "" }}</em>
              </span>
              <b>{{ firstReply.productScore || 5 }}分</b>
            </div>
            <p v-if="firstReply.sku">{{ firstReply.sku }}</p>
            <div class="detail-reply-text">{{ firstReply.comment }}</div>
            <div v-if="firstReply.pics && firstReply.pics.length" class="detail-reply-pics">
              <img v-for="pic in firstReply.pics.slice(0, 3)" :key="pic" :src="assetUrl(pic)" alt="" />
            </div>
          </article>
          <section v-if="recommendProducts.length" class="detail-recommend">
            <h3><span></span>优品推荐<span></span></h3>
            <div class="detail-recommend-list">
              <button
                v-for="item in recommendProducts"
                :key="item.id"
                type="button"
                @click="$emit('open-product', item)"
              >
                <SafeImage :src="assetUrl(item.image)" :alt="item.storeName || item.name" :label="item.storeName || item.name" variant="product" />
                <strong>{{ item.storeName || item.name }}</strong>
                <em>￥{{ item.price }}</em>
              </button>
            </div>
          </section>
          <div class="html-content" v-html="detail.productInfo.content"></div>
        </div>
        <div class="detail-actions">
          <button type="button" class="mini service" @click="$emit('service')"><span>客服</span></button>
          <button
            type="button"
            class="plain"
            :class="{ collected: detail.userCollect }"
            :disabled="collecting"
            @click="$emit('collect')"
          >
            {{ collecting ? "处理中" : detail.userCollect ? "已收藏" : "收藏" }}
          </button>
          <button type="button" class="mini cart" @click="$emit('cart')">
            <i v-if="cartBadgeCount > 0">{{ cartBadgeCount > 99 ? "99+" : cartBadgeCount }}</i>
            <span>购物车</span>
          </button>
          <button type="button" class="ghost" :disabled="Boolean(purchaseDisabledReason)" @click="$emit('add-cart')">
            {{ purchaseDisabledReason || "加入购物车" }}
          </button>
          <button type="button" class="solid" :disabled="Boolean(purchaseDisabledReason)" @click="$emit('buy-now')">
            {{ purchaseDisabledReason || "立即购买" }}
          </button>
        </div>
        <aside v-if="couponOpen" class="product-coupon-mask" @click.self="couponOpen = false">
          <section class="product-coupon-panel">
            <header class="product-coupon-head">
              <strong>优惠券</strong>
              <button type="button" @click="couponOpen = false">×</button>
            </header>
            <div v-if="coupons.length" class="product-coupon-list">
              <article
                v-for="coupon in coupons"
                :key="coupon.id"
                class="product-coupon-card"
                :class="{ disabled: coupon.isUse }"
              >
                <div class="product-coupon-money">
                  <span>￥</span>
                  <strong>{{ coupon.money }}</strong>
                  <em>满{{ coupon.minPrice }}元可用</em>
                </div>
                <div class="product-coupon-info">
                  <h3><span>{{ couponTypeText(coupon.useType) }}</span>{{ coupon.name }}</h3>
                  <p v-if="Number(coupon.day) > 0">领取后{{ coupon.day }}天内可用</p>
                  <p v-else>{{ coupon.useStartTimeStr && coupon.useEndTimeStr ? `${coupon.useStartTimeStr} - ${coupon.useEndTimeStr}` : "领取后可用" }}</p>
                  <button
                    type="button"
                    :disabled="coupon.isUse || receivingCouponId === Number(coupon.id)"
                    @click="$emit('receive-coupon', coupon)"
                  >
                    {{ coupon.isUse ? (coupon.use_title || "已领取") : (receivingCouponId === Number(coupon.id) ? "领取中" : "立即领取") }}
                  </button>
                </div>
              </article>
            </div>
            <div v-else class="product-coupon-empty">暂无可领取优惠券</div>
          </section>
        </aside>
      </template>
    </article>
  </aside>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import SafeImage from "../components/SafeImage.vue";

const props = defineProps({
  open: Boolean,
  loading: Boolean,
  detail: { type: Object, default: null },
  detailImage: { type: String, default: "" },
  selectedSku: { type: Object, default: null },
  selectedAttrs: { type: Object, default: () => ({}) },
  cartNum: { type: Number, default: 1 },
  cartBadgeCount: { type: Number, default: 0 },
  receivingCouponId: { type: Number, default: 0 },
  collecting: Boolean,
  purchaseDisabledReason: { type: String, default: "" },
  defaultAvatar: { type: String, default: "" },
  assetUrl: { type: Function, required: true }
});

defineEmits([
  "close",
  "collect",
  "service",
  "cart",
  "select-attr",
  "change-cart-num",
  "add-cart",
  "buy-now",
  "open-replies",
  "open-product",
  "receive-coupon"
]);

const couponOpen = ref(false);
const coupons = computed(() => props.detail?.defaultCoupon || props.detail?.couponList || props.detail?.coupons || []);
const firstCoupon = computed(() => coupons.value[0] || null);
const recommendProducts = computed(() => props.detail?.goodList || props.detail?.good_list || props.detail?.recommend || []);
const firstReply = computed(() => (props.detail?.reply || props.detail?.replyList || [])[0] || null);
const vipPrice = computed(() => Number(props.selectedSku?.vipPrice || props.detail?.productInfo?.vipPrice || 0));
const salesCount = computed(() =>
  Math.floor(Number(props.detail?.productInfo?.sales || 0)) + Math.floor(Number(props.detail?.productInfo?.ficti || 0))
);

watch(() => props.open, (open) => {
  if (!open) {
    couponOpen.value = false;
  }
});

watch(() => props.detail?.productInfo?.id, () => {
  couponOpen.value = false;
});

function parseAttrValues(raw) {
  if (Array.isArray(raw)) {
    return raw.map((item) => String(item)).filter(Boolean);
  }
  if (raw == null) {
    return [];
  }
  const text = String(raw).trim();
  if (!text) {
    return [];
  }
  try {
    const parsed = JSON.parse(text);
    if (Array.isArray(parsed)) {
      return parsed.map((item) => String(item)).filter(Boolean);
    }
  } catch {
    // Old CRMEB data can be stored as a comma separated string.
  }
  return text
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function couponTypeText(type) {
  if (Number(type) === 1) {
    return "通用";
  }
  if (Number(type) === 3) {
    return "品类";
  }
  return "商品";
}
</script>
