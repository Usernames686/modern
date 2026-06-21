<template>
  <section class="plain-view bargain-detail-view">
    <div class="bargain-detail-head">
      <button class="seckill-back" type="button" @click="$emit('back')">‹</button>
      <span>砍价详情</span>
      <button type="button" @click="$emit('back')">列表</button>
    </div>
    <div v-if="loading" class="state">加载中...</div>
    <template v-else-if="item">
      <section class="bargain-detail-card">
        <img :src="assetUrl(item.image)" :alt="item.title" />
        <div>
          <h1>{{ item.title }}</h1>
          <p>最低价：<strong>￥{{ item.minPrice }}</strong></p>
          <p>当前价：￥{{ item.price }}</p>
          <p>剩余：{{ item.quota }}{{ item.unitName || "件" }}</p>
        </div>
      </section>

      <section class="bargain-detail-progress">
        <div>
          <span>已砍金额</span>
          <strong>￥{{ userInfo ? moneyText(userInfo.alreadyPrice) : "0.00" }}</strong>
        </div>
        <div>
          <span>还剩金额</span>
          <strong>￥{{ userInfo ? moneyText(userInfo.surplusPrice) : moneyText(item.price) }}</strong>
        </div>
        <div>
          <span>砍价状态</span>
          <strong>{{ userStatusText }}</strong>
        </div>
        <i><em :style="{ width: `${Math.max(0, Math.min(100, Number(userInfo?.bargainPercent || 0)))}%` }"></em></i>
      </section>

      <section v-if="shareLink" class="bargain-share-box">
        <strong>分享给好友帮砍</strong>
        <textarea :value="shareLink" readonly rows="3" @focus="$event.target.select()" />
        <button type="button" @click="$emit('close-share')">收起</button>
      </section>

      <section v-if="isSuccess" class="bargain-pay-success">
        <strong>{{ userStatusText === "已支付" ? "恭喜您砍价成功，去看看别的商品吧~" : "恭喜您砍价成功，快去支付吧~" }}</strong>
        <div>
          <button type="button" @click="$emit('continue')">继续选购</button>
          <button v-if="userStatusText !== '已支付'" type="button" @click="$emit('action')">
            {{ actionText }}
          </button>
        </div>
      </section>

      <section v-if="userInfo?.storeBargainUserName" class="bargain-user-owner">
        <img :src="assetUrl(userInfo.storeBargainUserAvatar || defaultAvatar)" alt="" />
        <span>{{ userInfo.storeBargainUserName }} 邀请你帮忙砍价</span>
      </section>

      <section class="bargain-detail-info">
        <h2>活动信息</h2>
        <p><span>活动时间</span><strong>{{ item.startTimeText || "-" }} 至 {{ item.stopTimeText || "-" }}</strong></p>
        <p><span>购买数量</span><strong>每人限购 {{ item.num || 0 }} {{ item.unitName || "件" }}</strong></p>
        <p><span>商品规格</span><strong>{{ item.sku || "默认规格" }}</strong></p>
        <p><span>原商品状态</span><strong>{{ masterStatusText(item.masterStatus) }}</strong></p>
      </section>

      <section class="bargain-help-list">
        <h2>砍价记录</h2>
        <article v-for="help in helpList" :key="help.id">
          <img :src="assetUrl(help.avatar || defaultAvatar)" alt="" />
          <span>
            <strong>{{ help.nickname || "用户" }}</strong>
            <em>{{ help.addTimeStr || help.addTime }}</em>
          </span>
          <b>帮砍 ￥{{ moneyText(help.price) }}</b>
        </article>
        <div v-if="helpList.length === 0" class="state compact-state">暂无好友帮砍记录</div>
      </section>

      <section class="bargain-detail-content">
        <h2>商品详情</h2>
        <div v-if="item.content" class="html-content" v-html="item.content"></div>
        <div v-else class="state compact-state">暂无商品详情</div>
      </section>

      <section class="bargain-detail-actions">
        <button type="button" class="ghost" @click="$emit('open-master')">查看原商品</button>
        <button type="button" class="solid" :disabled="actionDisabled" @click="$emit('action')">
          {{ actionText }}
        </button>
      </section>
    </template>
    <div v-else class="state">暂无砍价详情</div>
  </section>
</template>

<script setup>
defineProps({
  loading: Boolean,
  item: { type: Object, default: null },
  userInfo: { type: Object, default: null },
  helpList: { type: Array, default: () => [] },
  userStatusText: { type: String, default: "" },
  isSuccess: Boolean,
  actionText: { type: String, default: "" },
  actionDisabled: Boolean,
  shareLink: { type: String, default: "" },
  defaultAvatar: { type: String, required: true },
  assetUrl: { type: Function, required: true },
  moneyText: { type: Function, required: true },
  masterStatusText: { type: Function, required: true }
});

defineEmits(["back", "continue", "action", "close-share", "open-master"]);
</script>
