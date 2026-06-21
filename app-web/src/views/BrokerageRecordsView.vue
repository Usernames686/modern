<template>
  <section class="plain-view commission-record-view">
    <div class="commission-head">
      <button type="button" @click="$emit('back')">返回</button>
      <span>佣金明细</span>
      <strong>￥{{ moneyText(total) }}</strong>
    </div>
    <div v-if="loading" class="state">加载中...</div>
    <section v-else class="commission-record-list">
      <article v-for="group in groups" :key="group.date" class="commission-record-group">
        <h2>{{ group.date }}</h2>
        <div>
          <p v-for="item in group.list" :key="item.id">
            <span><strong>{{ item.title }}</strong><em>{{ item.updateTime }}</em></span>
            <b :class="{ minus: Number(item.type) !== 1 }">{{ Number(item.type) === 1 ? "+" : "-" }}{{ moneyText(item.price) }}</b>
          </p>
        </div>
      </article>
      <div v-if="groups.length === 0" class="coupon-empty-page">暂无佣金记录~</div>
    </section>
  </section>
</template>

<script setup>
defineProps({
  loading: Boolean,
  groups: { type: Array, default: () => [] },
  total: { type: [String, Number], default: 0 },
  moneyText: { type: Function, required: true }
});

defineEmits(["back"]);
</script>
