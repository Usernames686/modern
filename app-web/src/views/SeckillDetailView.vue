<template>
  <section class="plain-view seckill-detail-view">
    <div class="seckill-detail-head">
      <button class="seckill-back" type="button" @click="$emit('back')">‹</button>
      <span>秒杀详情</span>
      <button type="button" @click="$emit('back')">列表</button>
    </div>
    <div v-if="loading" class="state">加载中...</div>
    <template v-else-if="item">
      <section class="seckill-detail-card">
        <img :src="assetUrl(item.image)" :alt="item.title" />
        <div>
          <h1>{{ item.title }}</h1>
          <p class="seckill-detail-price">
            <strong>￥{{ item.price }}</strong>
            <span v-if="item.otPrice">￥{{ item.otPrice }}</span>
          </p>
          <p>限量：{{ item.quota }}{{ item.unitName || "件" }}</p>
          <p>库存：{{ item.stock }}{{ item.unitName || "件" }}</p>
        </div>
      </section>

      <section class="seckill-detail-progress">
        <div>
          <span>活动状态</span>
          <strong>{{ status }}</strong>
        </div>
        <div>
          <span>已抢</span>
          <strong>{{ item.percent || 0 }}%</strong>
        </div>
        <div>
          <span>时段</span>
          <strong>{{ currentHeader?.time || "-" }}</strong>
        </div>
        <i><em :style="{ width: `${Math.max(0, Math.min(100, Number(item.percent || 0)))}%` }"></em></i>
      </section>

      <section class="seckill-detail-info">
        <h2>活动信息</h2>
        <p><span>活动时间</span><strong>{{ dateText(item.startTime) }} 至 {{ dateText(item.stopTime) }}</strong></p>
        <p><span>活动库存</span><strong>{{ item.stock || item.quota || 0 }} {{ item.unitName || "件" }}</strong></p>
        <p><span>秒杀价</span><strong>￥{{ item.price }}</strong></p>
        <p><span>原价</span><strong>￥{{ item.otPrice || "0.00" }}</strong></p>
        <p><span>原商品</span><strong>{{ item.productId ? `商品ID ${item.productId}` : "未关联" }}</strong></p>
      </section>

      <section class="seckill-detail-actions">
        <button type="button" class="ghost" @click="$emit('open-master')">查看原商品</button>
        <button type="button" class="solid" :disabled="status !== '抢购中'" @click="$emit('checkout')">
          {{ status === "抢购中" ? "马上抢" : status }}
        </button>
      </section>
    </template>
    <div v-else class="state">暂无秒杀详情</div>
  </section>
</template>

<script setup>
defineProps({
  loading: Boolean,
  item: { type: Object, default: null },
  currentHeader: { type: Object, default: null },
  status: { type: String, default: "" },
  assetUrl: { type: Function, required: true },
  dateText: { type: Function, required: true }
});

defineEmits(["back", "open-master", "checkout"]);
</script>
