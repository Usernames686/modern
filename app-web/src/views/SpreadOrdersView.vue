<template>
  <section class="plain-view spread-order-view">
    <div class="spread-order-head">
      <button type="button" @click="$emit('back')">返回</button>
      <span>累积推广订单</span>
      <strong>{{ count || 0 }} 单</strong>
    </div>
    <div v-if="loading" class="state">加载中...</div>
    <section v-else class="spread-order-list">
      <article v-for="group in groups" :key="group.time" class="spread-order-group">
        <header>
          <strong>{{ group.time }}</strong>
          <span>本月累计推广订单：{{ group.count || 0 }}单</span>
        </header>
        <div>
          <p v-for="item in group.child" :key="item.orderId + item.time">
            <span>
              <img :src="assetUrl(item.avatar || defaultAvatar)" alt="" />
              <em>{{ item.nickname || "用户" }}</em>
            </span>
            <b>返佣：￥{{ moneyText(item.number) }}</b>
            <small>订单编号：{{ item.orderId }}</small>
            <small>下单时间：{{ item.time }}</small>
          </p>
        </div>
      </article>
      <div v-if="groups.length === 0" class="coupon-empty-page">暂无推广订单～</div>
    </section>
  </section>
</template>

<script setup>
defineProps({
  loading: Boolean,
  groups: { type: Array, default: () => [] },
  count: { type: [Number, String], default: 0 },
  defaultAvatar: { type: String, required: true },
  assetUrl: { type: Function, required: true },
  moneyText: { type: Function, required: true }
});

defineEmits(["back"]);
</script>
