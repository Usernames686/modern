<template>
  <section class="plain-view combination-detail-view">
    <div class="seckill-detail-head combination-detail-head">
      <button class="seckill-back" type="button" @click="$emit('back')">‹</button>
      <span>拼团详情</span>
      <button type="button" @click="$emit('back')">列表</button>
    </div>
    <div v-if="loading" class="state">加载中...</div>
    <template v-else-if="item">
      <section class="combination-detail-card">
        <img :src="assetUrl(item.image)" :alt="item.title" />
        <div>
          <h1>{{ item.title }}</h1>
          <p><strong>￥{{ item.price }}</strong><span v-if="item.otPrice">￥{{ item.otPrice }}</span></p>
          <p>{{ item.people || 0 }}人团 · 已拼 {{ item.countPeople || item.sales || 0 }} 件</p>
        </div>
      </section>

      <section class="combination-detail-info">
        <h2>活动信息</h2>
        <p><span>活动状态</span><strong>{{ status }}</strong></p>
        <p><span>活动时间</span><strong>{{ dateText(item.startTime) }} 至 {{ dateText(item.stopTime) }}</strong></p>
        <p><span>库存限量</span><strong>{{ item.quota || item.stock || 0 }} {{ item.unitName || "件" }}</strong></p>
        <p><span>单次限购</span><strong>{{ item.onceNum || item.num || 1 }} {{ item.unitName || "件" }}</strong></p>
      </section>

      <section v-if="invitePinkId" class="combination-invite-box">
        <strong>好友邀请你参团</strong>
        <span>团号：{{ invitePinkId }}</span>
        <button type="button" :disabled="status !== '拼团中'" @click="$emit('join', invitePinkId)">
          {{ status === "拼团中" ? "立即参团" : status }}
        </button>
      </section>

      <section v-if="pinkList.length" class="combination-pink-list">
        <h2>正在拼团</h2>
        <article v-for="pink in pinkList" :key="pink.id">
          <img :src="assetUrl(pink.avatar || defaultAvatar)" alt="" />
          <span>
            <strong>{{ pink.nickname || "用户" }}<i v-if="pink.isLeader || pink.kId === 0">团长</i></strong>
            <em>{{ pinkText(pink) }}</em>
          </span>
          <button type="button" :disabled="!canJoinPink(pink)" @click="$emit('join', pink.id)">
            {{ canJoinPink(pink) ? "去参团" : "已结束" }}
          </button>
        </article>
      </section>

      <section v-if="memberList.length" class="combination-member-list">
        <h2>团长/团员</h2>
        <article v-for="member in memberList" :key="member.id || member.uid || member.nickname">
          <img :src="assetUrl(member.avatar || defaultAvatar)" alt="" />
          <span>
            <strong>{{ member.nickname || member.realName || "用户" }}</strong>
            <em>{{ member.isLeader || member.kId === 0 ? "团长" : "团员" }}</em>
          </span>
        </article>
      </section>

      <section class="bargain-detail-content">
        <h2>商品详情</h2>
        <div v-if="item.content" class="html-content" v-html="item.content"></div>
        <div v-else class="state compact-state">暂无商品详情</div>
      </section>

      <section class="seckill-detail-actions">
        <button type="button" class="ghost" @click="$emit('open-master')">单独购买</button>
        <button type="button" class="solid" :disabled="status !== '拼团中'" @click="$emit('checkout', 0)">
          {{ status === "拼团中" ? "立即开团" : status }}
        </button>
      </section>
    </template>
    <div v-else class="state">暂无拼团详情</div>
  </section>
</template>

<script setup>
defineProps({
  loading: Boolean,
  item: { type: Object, default: null },
  status: { type: String, default: "" },
  pinkList: { type: Array, default: () => [] },
  memberList: { type: Array, default: () => [] },
  invitePinkId: { type: [String, Number], default: "" },
  defaultAvatar: { type: String, required: true },
  assetUrl: { type: Function, required: true },
  dateText: { type: Function, required: true },
  pinkText: { type: Function, required: true },
  canJoinPink: { type: Function, required: true }
});

defineEmits(["back", "open-master", "checkout", "join"]);
</script>
