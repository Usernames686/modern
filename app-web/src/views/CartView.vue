<template>
  <section class="plain-view cart-view">
    <div class="view-head">
      <h1>购物车</h1>
      <span>{{ cartTotal }} 件</span>
    </div>
    <div v-if="!authToken" class="login-nudge">
      <h2>登录后查看购物车</h2>
      <button type="button" @click="$emit('login')">去登录</button>
    </div>
    <div v-else-if="loading" class="state">加载中...</div>
    <div v-else-if="items.length === 0" class="state">购物车暂无商品</div>
    <div v-else>
      <section class="cart-assurance">
        <span>100%正品保证</span>
        <span>所有商品精挑细选</span>
        <span>售后无忧</span>
      </section>
      <section class="cart-box">
        <div class="cart-nav">
          <span>购物数量 <em>{{ cartTotal }}</em></span>
          <button type="button" @click="$emit('toggle-manage')">{{ manage ? "取消" : "管理" }}</button>
        </div>
        <div class="cart-list">
          <article v-for="item in items" :key="item.id" class="cart-item">
            <label class="cart-check">
              <input type="checkbox" :checked="selectedIds.includes(item.id)" @change="$emit('toggle-item', item.id)" />
              <span></span>
            </label>
            <SafeImage
              class="cart-goods-image"
              :src="assetUrl(item.image)"
              :alt="item.storeName"
              :label="item.storeName"
              variant="product"
              @click="$emit('open-product', { id: item.productId })"
            />
            <div class="cart-info">
              <h2 :class="{ invalid: item.attrStatus === false }" @click="$emit('open-product', { id: item.productId })">
                {{ item.storeName }}
              </h2>
              <p v-if="item.suk">属性：{{ item.suk }}</p>
              <p v-else>默认规格</p>
              <div class="cart-bottom">
                <strong>￥{{ item.vipPrice || item.price }}</strong>
                <div class="stepper">
                  <button type="button" @click="$emit('change-num', item, -1)">-</button>
                  <span>{{ item.cartNum }}</span>
                  <button type="button" @click="$emit('change-num', item, 1)">+</button>
                </div>
              </div>
            </div>
          </article>
        </div>
      </section>
      <section class="cart-submit">
        <label class="cart-all">
          <input type="checkbox" :checked="isAllSelected" @change="$emit('toggle-all')" />
          <span></span>
          全选({{ selectedIds.length }})
        </label>
        <div v-if="!manage" class="cart-submit-main">
          <strong>￥{{ selectedTotal }}</strong>
          <button type="button" @click="$emit('checkout')">立即下单</button>
        </div>
        <div v-else class="cart-submit-main manage">
          <button type="button" @click="$emit('collect-selected')">收藏</button>
          <button type="button" @click="$emit('delete-selected')">删除</button>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import SafeImage from "../components/SafeImage.vue";

defineProps({
  authToken: { type: String, default: "" },
  loading: Boolean,
  items: { type: Array, default: () => [] },
  selectedIds: { type: Array, default: () => [] },
  cartTotal: { type: Number, default: 0 },
  manage: Boolean,
  selectedTotal: { type: String, default: "0.00" },
  isAllSelected: Boolean,
  assetUrl: { type: Function, required: true }
});

defineEmits([
  "login",
  "toggle-manage",
  "toggle-item",
  "open-product",
  "change-num",
  "toggle-all",
  "checkout",
  "collect-selected",
  "delete-selected"
]);
</script>
