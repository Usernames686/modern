<template>
  <section class="plain-view integral-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>积分中心</h1>
      <span></span>
    </div>
    <div v-if="!authToken" class="login-nudge">
      <h2>登录后查看积分</h2>
      <button type="button" @click="$emit('login')">去登录</button>
    </div>
    <div v-else-if="loading" class="state">加载中...</div>
    <template v-else>
      <section class="integral-head">
        <span>当前积分</span>
        <strong>{{ info.integral || 0 }}</strong>
        <div>
          <p><em>{{ info.sumIntegral || 0 }}</em><span>累计积分</span></p>
          <p><em>{{ info.deductionIntegral || 0 }}</em><span>累计消费</span></p>
          <p><em>{{ info.frozenIntegral || 0 }}</em><span>冻结积分</span></p>
        </div>
      </section>
      <nav class="integral-tabs">
        <button :class="{ active: panel === 'detail' }" type="button" @click="$emit('select-panel', 'detail')">分值明细</button>
        <button :class="{ active: panel === 'raise' }" type="button" @click="$emit('select-panel', 'raise')">分值提升</button>
      </nav>
      <section v-if="panel === 'detail'" class="integral-list">
        <div class="integral-tip">提示：积分数值的高低会直接影响您的会员等级</div>
        <div v-if="records.length === 0" class="coupon-empty-page">暂无积分记录哦～</div>
        <article v-for="item in records" v-else :key="item.id">
          <span><strong>{{ item.title }}</strong><em>{{ item.updateTime }}</em></span>
          <b :class="{ income: Number(item.type) === 1 }">{{ Number(item.type) === 1 ? "+" : "-" }}{{ item.integral }}</b>
        </article>
      </section>
      <section v-else class="integral-raise">
        <button type="button" @click="$emit('home')"><strong>购买商品可获得积分奖励</strong><span>赚积分</span></button>
        <button type="button" @click="$emit('sign')"><strong>每日签到可获得积分奖励</strong><span>赚积分</span></button>
      </section>
    </template>
  </section>
</template>

<script setup>
defineProps({
  authToken: { type: String, default: "" },
  loading: Boolean,
  info: { type: Object, default: () => ({}) },
  panel: { type: String, default: "detail" },
  records: { type: Array, default: () => [] }
});

defineEmits(["back", "login", "select-panel", "home", "sign"]);
</script>
