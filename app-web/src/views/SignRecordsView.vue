<template>
  <section class="plain-view sign-record-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>签到明细</h1>
      <span></span>
    </div>
    <div v-if="loading" class="state">加载中...</div>
    <section v-else class="sign-record-list">
      <article v-for="group in groups" :key="group.month" class="sign-record-group">
        <h2>{{ group.month }}</h2>
        <div>
          <p v-for="item in group.list" :key="item.id">
            <span><strong>{{ item.title }}</strong><em>{{ item.createDay }}</em></span>
            <b>+{{ item.number }}</b>
          </p>
        </div>
      </article>
      <div v-if="groups.length === 0" class="coupon-empty-page">暂无签到记录</div>
    </section>
  </section>
</template>

<script setup>
defineProps({
  loading: Boolean,
  groups: { type: Array, default: () => [] }
});

defineEmits(["back"]);
</script>
