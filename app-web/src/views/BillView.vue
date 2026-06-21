<template>
  <section class="plain-view bill-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>账单记录</h1>
      <span></span>
    </div>
    <div v-if="!authToken" class="login-nudge">
      <h2>登录后查看账单</h2>
      <button type="button" @click="$emit('login')">去登录</button>
    </div>
    <template v-else>
      <nav class="bill-tabs">
        <button
          v-for="item in tabs"
          :key="item.type"
          :class="{ active: activeType === item.type }"
          type="button"
          @click="$emit('select', item.type)"
        >
          {{ item.name }}
        </button>
      </nav>
      <div v-if="loading" class="state">加载中...</div>
      <div v-else-if="groups.length === 0" class="coupon-empty-page">暂无账单的记录哦～</div>
      <section v-else class="bill-list">
        <article v-for="group in groups" :key="group.date" class="bill-group">
          <h2>{{ group.date }}</h2>
          <div>
            <p v-for="item in group.list" :key="item.id">
              <span><strong>{{ item.title }}</strong><em>{{ item.add_time || item.createTime }}</em></span>
              <b :class="{ income: Number(item.pm) === 1 }">{{ Number(item.pm) === 1 ? "+" : "-" }}{{ moneyText(item.number) }}</b>
            </p>
          </div>
        </article>
      </section>
    </template>
  </section>
</template>

<script setup>
defineProps({
  authToken: { type: String, default: "" },
  loading: Boolean,
  tabs: { type: Array, default: () => [] },
  activeType: { type: String, default: "all" },
  groups: { type: Array, default: () => [] },
  moneyText: { type: Function, required: true }
});

defineEmits(["back", "login", "select"]);
</script>
