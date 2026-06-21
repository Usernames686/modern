<template>
  <section class="plain-view order-detail-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>订单详情</h1>
      <span></span>
    </div>
    <div v-if="loading" class="state">加载中...</div>
    <template v-else-if="order">
      <section class="refund-status-head" v-if="isRefundDetail">
        <strong>{{ order.orderStatusMsg || order.statusText || refundStatusText(order) }}</strong>
        <span>{{ order.refundReasonTime || order.payTime || "" }}</span>
      </section>
      <section class="order-detail-card">
        <div class="order-card-head">
          <span>{{ order.orderId }}</span>
          <strong>{{ isRefundDetail ? refundStatusText(order) : order.statusText }}</strong>
        </div>
        <div class="refund-reject" v-if="!isRefundDetail && order.refundReason">
          <strong>商家拒绝退款</strong>
          <p>拒绝原因：{{ order.refundReason }}</p>
        </div>
        <template v-if="Number(order.shippingType || 1) === 2">
          <p>核销信息：{{ order.verifyCode || "-" }}</p>
          <p>联系人：{{ order.realName }} {{ order.userPhone }}</p>
          <p>自提门店：{{ order.systemStore?.name || order.userAddress }}</p>
          <p>自提地址：{{ order.systemStore?.fullAddress || "-" }}</p>
          <p>门店电话：{{ order.systemStore?.phone || "-" }}</p>
        </template>
        <template v-else>
          <p>收货人：{{ order.realName }} {{ order.userPhone }}</p>
          <p>收货地址：{{ order.userAddress }}</p>
        </template>
        <p>下单时间：{{ order.createTime || "-" }}</p>
        <p>支付状态：{{ order.paid ? "已支付" : "未支付" }}</p>
        <p>支付方式：{{ order.payTypeStr || order.payType || "-" }}</p>
        <p v-if="order.mark">买家留言：{{ order.mark }}</p>
        <p v-if="order.deliveryType === 'express'">物流公司：{{ order.deliveryName || "-" }}</p>
        <p v-if="order.deliveryType === 'express'">快递单号：{{ order.deliveryId || "-" }}</p>
      </section>
      <section class="order-detail-card refund-detail-card" v-if="hasRefundInfo(order)">
        <div class="refund-detail-title">
          <strong>售后进度</strong>
          <span>{{ refundStatusText(order) }}</span>
        </div>
        <div class="refund-progress-grid">
          <p><span>退款金额</span><strong>￥{{ refundAmount(order) }}</strong></p>
          <p v-if="Number(order.refundPrice || 0) > 0"><span>已退金额</span><strong>￥{{ order.refundPrice }}</strong></p>
          <p v-if="order.refundReasonTime"><span>申请时间</span><strong>{{ order.refundReasonTime }}</strong></p>
          <p v-if="order.refundReasonWap"><span>退款原因</span><strong>{{ order.refundReasonWap }}</strong></p>
          <p v-if="order.refundReasonWapExplain"><span>备注说明</span><strong>{{ order.refundReasonWapExplain }}</strong></p>
          <p v-if="order.refundReason"><span>商家处理</span><strong>{{ order.refundReason }}</strong></p>
        </div>
        <div v-if="refundImages(order).length" class="refund-detail-proofs">
          <span>退款凭证：</span>
          <img v-for="image in refundImages(order)" :key="image" :src="assetUrl(image)" alt="退款凭证" />
        </div>
      </section>
      <section class="checkout-goods">
        <article v-for="goods in orderGoodsList(order)" :key="goods.id || goods.unique" class="order-goods">
          <SafeImage :src="assetUrl(goodsImage(goods))" :alt="goodsName(goods)" :label="goodsName(goods)" variant="product" />
          <div>
            <h2>{{ goodsName(goods) }}</h2>
            <p>{{ goodsSku(goods) || "默认规格" }}</p>
            <span>￥{{ goodsPrice(goods) }} × {{ goodsNum(goods) }}</span>
            <button v-if="canCommentGoods(order, goods)" class="goods-comment-button" type="button" @click="$emit('comment-goods', order, goods)">
              评价
            </button>
            <em v-else-if="Number(goods.isReply || 0) === 1" class="goods-comment-done">已评价</em>
          </div>
        </article>
      </section>
      <section class="checkout-price">
        <div><span>订单编号</span><strong>{{ order.orderId }}</strong></div>
        <div><span>商品金额</span><strong>￥{{ order.totalPrice || order.proTotalPrice || order.payPrice }}</strong></div>
        <div v-if="Number(order.payPostage || 0) > 0"><span>运费</span><strong>￥{{ order.payPostage }}</strong></div>
        <div v-if="Number(order.couponPrice || 0) > 0"><span>优惠券抵扣</span><strong>-￥{{ order.couponPrice }}</strong></div>
        <div v-if="Number(order.deductionPrice || 0) > 0"><span>积分抵扣</span><strong>-￥{{ order.deductionPrice }}</strong></div>
        <div class="pay-line"><span>实付</span><strong>￥{{ order.payPrice }}</strong></div>
      </section>
      <div class="order-actions detail-actions-row">
        <button v-if="isUnpaid(order)" type="button" :disabled="payingOrder === order.orderId" @click="$emit('pay', order)">
          {{ payingOrder === order.orderId ? "支付中..." : "去支付" }}
        </button>
        <button v-if="isUnpaid(order)" type="button" @click="$emit('cancel', order)">取消订单</button>
        <button v-if="canApplyRefund(order)" type="button" @click="$emit('refund', order)">申请售后</button>
        <button v-if="canViewLogistics(order)" type="button" @click="$emit('logistics', order)">查看物流</button>
        <button v-if="canTakeOrder(order)" type="button" @click="$emit('take', order)">确认收货</button>
        <button v-if="canCommentOrder(order)" type="button" @click="$emit('comment-first', order)">去评价</button>
        <button v-if="canDeleteOrder(order)" type="button" @click="$emit('delete', order)">删除订单</button>
        <button v-if="canOrderAgain(order)" type="button" @click="$emit('again', order)">再次购买</button>
      </div>
    </template>
  </section>
</template>

<script setup>
import SafeImage from "../components/SafeImage.vue";

defineProps({
  loading: Boolean,
  order: { type: Object, default: null },
  isRefundDetail: Boolean,
  payingOrder: { type: [String, Number], default: "" },
  assetUrl: { type: Function, required: true },
  orderGoodsList: { type: Function, default: () => [] },
  goodsName: { type: Function, default: () => "" },
  goodsImage: { type: Function, default: () => "" },
  goodsSku: { type: Function, default: () => "" },
  goodsPrice: { type: Function, default: () => "" },
  goodsNum: { type: Function, default: () => 1 },
  refundStatusText: { type: Function, default: () => "" },
  isUnpaid: { type: Function, default: () => false },
  canApplyRefund: { type: Function, default: () => false },
  canViewLogistics: { type: Function, default: () => false },
  canTakeOrder: { type: Function, default: () => false },
  canCommentOrder: { type: Function, default: () => false },
  canCommentGoods: { type: Function, default: () => false },
  canDeleteOrder: { type: Function, default: () => false },
  canOrderAgain: { type: Function, default: () => false }
});

defineEmits([
  "back",
  "pay",
  "cancel",
  "refund",
  "logistics",
  "take",
  "comment-first",
  "comment-goods",
  "delete",
  "again"
]);

function refundImages(order) {
  return String(order?.refundReasonWapImg || "")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 3);
}

function refundAmount(order) {
  const refundPrice = Number(order?.refundPrice || 0);
  if (refundPrice > 0) {
    return order.refundPrice;
  }
  return order?.payPrice || "0.00";
}

function hasRefundInfo(order) {
  return Number(order?.refundStatus || 0) > 0
    || Boolean(order?.refundReason)
    || Boolean(order?.refundReasonWap)
    || Boolean(order?.refundReasonWapExplain)
    || Boolean(order?.refundReasonWapImg)
    || Boolean(order?.refundReasonTime);
}
</script>
