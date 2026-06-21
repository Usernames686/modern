<template>
  <section class="plain-view spread-people-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>推广人统计</h1>
      <span>{{ summary.count || 0 }} 人</span>
    </div>
    <section class="spread-people-total">
      <p><em>{{ summary.total || 0 }}</em><span>一级推广人</span></p>
      <p><em>{{ summary.totalLevel || 0 }}</em><span>二级推广人</span></p>
    </section>
    <nav class="spread-people-tabs">
      <button :class="{ active: grade === 0 }" type="button" @click="$emit('select-grade', 0)">一级</button>
      <button :class="{ active: grade === 1 }" type="button" @click="$emit('select-grade', 1)">二级</button>
    </nav>
    <nav class="spread-sort-tabs">
      <button :class="{ active: sortKey === '' }" type="button" @click="$emit('select-sort', '')">加入时间</button>
      <button :class="{ active: sortKey === 'childCount' }" type="button" @click="$emit('select-sort', 'childCount')">团队</button>
      <button :class="{ active: sortKey === 'orderCount' }" type="button" @click="$emit('select-sort', 'orderCount')">订单</button>
      <button :class="{ active: sortKey === 'numberCount' }" type="button" @click="$emit('select-sort', 'numberCount')">金额</button>
    </nav>
    <div v-if="loading" class="state">加载中...</div>
    <section v-else class="spread-people-list">
      <article v-for="item in items" :key="item.uid">
        <img :src="assetUrl(item.avatar || defaultAvatar)" alt="" />
        <div>
          <h2>{{ item.nickname || `用户${item.uid}` }}</h2>
          <p>{{ item.time || "-" }}</p>
        </div>
        <aside>
          <span>{{ item.childCount || 0 }}人</span>
          <span>{{ item.orderCount || 0 }}单</span>
          <strong>￥{{ moneyText(item.numberCount) }}</strong>
        </aside>
      </article>
      <div v-if="items.length === 0" class="coupon-empty-page">暂无推广人~</div>
    </section>
  </section>
</template>

<script setup>
defineProps({
  loading: Boolean,
  summary: { type: Object, default: () => ({}) },
  items: { type: Array, default: () => [] },
  grade: { type: Number, default: 0 },
  sortKey: { type: String, default: "" },
  defaultAvatar: { type: String, required: true },
  assetUrl: { type: Function, required: true },
  moneyText: { type: Function, required: true }
});

defineEmits(["back", "select-grade", "select-sort"]);
</script>
