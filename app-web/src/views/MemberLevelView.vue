<template>
  <section class="plain-view member-level-view">
    <div class="member-head">
      <div class="member-user">
        <img :src="assetUrl(user?.avatar || defaultAvatar)" alt="" />
        <div>
          <h1>{{ user?.nickname || user?.account || "聚商盈会员" }}</h1>
          <p v-if="currentLevel">{{ currentLevel.name }}</p>
          <p v-else>暂无会员等级</p>
        </div>
      </div>
    </div>
    <div v-if="!authToken" class="login-nudge">
      <h2>登录后查看会员等级</h2>
      <button type="button" @click="$emit('login')">去登录</button>
    </div>
    <div v-else-if="loading" class="state">加载中...</div>
    <template v-else>
      <section class="member-experience">
        <span>当前经验值</span>
        <strong>{{ user?.experience || 0 }}</strong>
        <div class="member-axis">
          <i :style="{ width: progress + '%' }"></i>
          <em
            v-for="item in levels"
            :key="item.id"
            :class="{ active: Number(user?.experience || 0) >= Number(item.experience || 0) }"
          ></em>
        </div>
        <div class="member-scale">
          <span v-for="item in levels" :key="item.id">{{ item.experience }}</span>
        </div>
      </section>
      <section class="member-benefits">
        <article><b>折</b><span>会员折扣</span></article>
        <article><b>徽</b><span>专属徽章</span></article>
        <article><b>升</b><span>会员升级</span></article>
        <article><b>经</b><span>经验积累</span></article>
        <article><b>权</b><span>更多特权</span></article>
      </section>
      <section class="member-gain">
        <h2>获取经验</h2>
        <button type="button" @click="$emit('sign')">
          <span><strong>签到</strong><em>每日签到可获得经验值</em></span>
          <b>去获取</b>
        </button>
        <button type="button" @click="$emit('category')">
          <span><strong>购买商品</strong><em>购买商品可获得对应经验值</em></span>
          <b>去获取</b>
        </button>
      </section>
      <section v-if="records.length" class="member-records">
        <h2>经验值明细</h2>
        <article v-for="item in records" :key="item.id">
          <span><strong>{{ item.title }}</strong><em>{{ item.createTime }}</em></span>
          <b :class="{ minus: Number(item.type) !== 1 }">{{ Number(item.type) === 1 ? "+" : "-" }}{{ item.experience }}</b>
        </article>
      </section>
    </template>
  </section>
</template>

<script setup>
defineProps({
  authToken: { type: String, default: "" },
  loading: Boolean,
  user: { type: Object, default: null },
  currentLevel: { type: Object, default: null },
  levels: { type: Array, default: () => [] },
  records: { type: Array, default: () => [] },
  progress: { type: Number, default: 0 },
  defaultAvatar: { type: String, default: "" },
  assetUrl: { type: Function, required: true }
});

defineEmits(["login", "sign", "category"]);
</script>
