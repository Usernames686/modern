<template>
  <section class="plain-view checkout-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>确认订单</h1>
      <span></span>
    </div>

    <div v-if="loading" class="state">加载中...</div>
    <template v-else>
      <section v-if="activityTypeText(items[0])" class="checkout-activity-banner">
        <strong>{{ activityTypeText(items[0]) }}订单</strong>
        <span>{{ activityHint(items[0]) }}</span>
      </section>

      <section class="shipping-tabs">
        <button :class="{ active: shippingType === 0 }" type="button" @click="$emit('select-shipping', 0)">快递配送</button>
        <button
          :class="{ active: shippingType === 1 }"
          type="button"
          :disabled="!storeSelfMention"
          @click="$emit('select-shipping', 1)"
        >
          到店自提
        </button>
      </section>
      <section v-if="shippingType === 0" class="checkout-address" @click="$emit('open-address')">
        <strong>{{ address.realName || "暂无收货地址" }}</strong>
        <span>{{ address.phone || "" }}</span>
        <p>{{ address.fullAddress || "请先维护默认收货地址" }}</p>
      </section>
      <section v-else class="checkout-address pickup-address" @click="$emit('open-store')">
        <strong>{{ selectedStore?.name || "暂无门店信息" }}</strong>
        <span>{{ selectedStore?.phone || "" }}</span>
        <p>{{ selectedStore?.fullAddress || "请选择提货点" }}</p>
      </section>

      <section class="checkout-goods">
        <article v-for="item in items" :key="item.id" class="order-goods">
          <img :src="assetUrl(item.image)" :alt="item.storeName" />
          <div>
            <h2>{{ item.storeName }}</h2>
            <p>{{ item.suk || "默认规格" }}</p>
            <em v-if="activityTypeText(item)">{{ activityTypeText(item) }}</em>
            <span>￥{{ item.price }} × {{ item.cartNum }}</span>
          </div>
        </article>
      </section>

      <section class="checkout-options">
        <button type="button" @click="$emit('open-coupon')">
          <span>优惠券</span>
          <strong>{{ selectedCoupon?.name || "请选择" }} ›</strong>
        </button>
        <label>
          <span>积分抵扣</span>
          <strong>当前积分 {{ userInfo?.integral || 0 }}</strong>
          <input
            :checked="useIntegral"
            type="checkbox"
            @change="$emit('update:useIntegral', $event.target.checked); $emit('recompute')"
          />
        </label>
        <template v-if="shippingType === 1">
          <label class="pickup-field">
            <span>联系人</span>
            <input
              :value="pickupContact"
              type="text"
              maxlength="20"
              placeholder="请填写您的联系姓名"
              @input="$emit('update:pickupContact', $event.target.value.trim())"
            />
          </label>
          <label class="pickup-field">
            <span>联系电话</span>
            <input
              :value="pickupPhone"
              type="tel"
              maxlength="11"
              placeholder="请填写您的联系电话"
              @input="$emit('update:pickupPhone', $event.target.value.trim())"
            />
          </label>
        </template>
        <label class="mark-field">
          <span>备注信息</span>
          <textarea
            :value="mark"
            maxlength="150"
            rows="3"
            placeholder="请添加备注（150字以内）"
            @input="$emit('update:mark', $event.target.value.trim())"
          ></textarea>
        </label>
      </section>

      <section class="checkout-price">
        <div><span>商品金额</span><strong>￥{{ price.proTotalFee || 0 }}</strong></div>
        <div><span>运费</span><strong>￥{{ price.freightFee || 0 }}</strong></div>
        <div><span>优惠抵扣</span><strong>-￥{{ price.couponFee || 0 }}</strong></div>
        <div><span>积分抵扣</span><strong>-￥{{ price.deductionPrice || 0 }}</strong></div>
        <div class="pay-line"><span>应付</span><strong>￥{{ price.payFee || 0 }}</strong></div>
      </section>

      <section class="checkout-submit">
        <span>合计：<strong>￥{{ price.payFee || 0 }}</strong></span>
        <button type="button" :disabled="creating" @click="$emit('create')">
          {{ creating ? "提交中..." : "提交订单" }}
        </button>
      </section>
    </template>
  </section>
</template>

<script setup>
defineProps({
  loading: Boolean,
  items: { type: Array, default: () => [] },
  address: { type: Object, default: () => ({}) },
  price: { type: Object, default: () => ({}) },
  shippingType: { type: Number, default: 0 },
  storeSelfMention: Boolean,
  selectedStore: { type: Object, default: null },
  selectedCoupon: { type: Object, default: null },
  activityTypeText: { type: Function, default: () => "" },
  activityHint: { type: Function, default: () => "" },
  userInfo: { type: Object, default: null },
  useIntegral: Boolean,
  pickupContact: { type: String, default: "" },
  pickupPhone: { type: String, default: "" },
  mark: { type: String, default: "" },
  creating: Boolean,
  assetUrl: { type: Function, required: true }
});

defineEmits([
  "back",
  "select-shipping",
  "open-address",
  "open-store",
  "open-coupon",
  "update:useIntegral",
  "update:pickupContact",
  "update:pickupPhone",
  "update:mark",
  "recompute",
  "create"
]);
</script>
