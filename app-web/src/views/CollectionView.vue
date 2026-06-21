<template>
  <section class="plain-view collection-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>收藏商品</h1>
      <button v-if="items.length" type="button" @click="$emit('toggle-manage')">{{ manage ? "取消" : "管理" }}</button>
    </div>
    <div v-if="!authToken" class="login-nudge">
      <h2>登录后查看收藏</h2>
      <button type="button" @click="$emit('login')">去登录</button>
    </div>
    <div v-else-if="loading" class="state">加载中...</div>
    <template v-else>
      <section v-if="items.length" class="collection-head">
        <span>当前共 <em>{{ total }}</em> 件商品</span>
        <button type="button" @click="$emit('toggle-manage')">{{ manage ? "取消" : "管理" }}</button>
      </section>
      <div v-if="items.length === 0" class="state">暂无收藏商品</div>
      <section v-else class="collection-list">
        <article
          v-for="item in items"
          :key="item.id"
          class="collection-item"
          :class="{ managing: manage }"
          @click="manage ? $emit('toggle-item', item.id) : $emit('open-product', { id: item.productId })"
        >
          <label v-if="manage" class="cart-check" @click.stop>
            <input type="checkbox" :checked="selectedIds.includes(Number(item.id))" @change="$emit('toggle-item', item.id)" />
            <span></span>
          </label>
          <img :src="assetUrl(item.image)" :alt="item.storeName" />
          <div>
            <h2>{{ item.storeName }}</h2>
            <strong>￥{{ item.price }}</strong>
          </div>
        </article>
      </section>
      <section v-if="manage && items.length" class="collection-submit">
        <label class="cart-all">
          <input type="checkbox" :checked="isAllSelected" @change="$emit('toggle-all')" />
          <span></span>
          {{ isAllSelected ? "取消" : "全选" }}
        </label>
        <button type="button" @click="$emit('delete-selected')">取消收藏</button>
      </section>
    </template>
  </section>
</template>

<script setup>
defineProps({
  authToken: { type: String, default: "" },
  loading: Boolean,
  items: { type: Array, default: () => [] },
  total: { type: [Number, String], default: 0 },
  manage: Boolean,
  selectedIds: { type: Array, default: () => [] },
  isAllSelected: Boolean,
  assetUrl: { type: Function, required: true }
});

defineEmits(["back", "login", "toggle-manage", "toggle-item", "toggle-all", "delete-selected", "open-product"]);
</script>
