<template>
  <section class="plain-view refund-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>申请退货</h1>
      <span></span>
    </div>
    <div v-if="loading" class="state">加载中...</div>
    <form v-else-if="order" class="refund-form" @submit.prevent="$emit('submit')">
      <section class="checkout-goods">
        <article v-for="goods in orderGoodsList(order)" :key="goods.id || goods.unique" class="order-goods">
          <SafeImage :src="assetUrl(goodsImage(goods))" :alt="goodsName(goods)" :label="goodsName(goods)" variant="product" />
          <div>
            <h2>{{ goodsName(goods) }}</h2>
            <p>{{ goodsSku(goods) || "默认规格" }}</p>
            <span>￥{{ goodsPrice(goods) }} × {{ goodsNum(goods) }}</span>
          </div>
        </article>
      </section>
      <section class="checkout-price">
        <div><span>订单编号</span><strong>{{ order.orderId }}</strong></div>
        <div><span>退货件数</span><strong>{{ order.totalNum || orderGoodsTotal(order) }}</strong></div>
        <div class="pay-line"><span>退款金额</span><strong>￥{{ order.payPrice }}</strong></div>
      </section>
      <section class="refund-fields">
        <label>
          <span>退款原因</span>
          <select :value="refundText" @change="$emit('update:refundText', $event.target.value)">
            <option value="">请选择退款原因</option>
            <option v-for="reason in reasons" :key="reason" :value="reason">{{ reason }}</option>
          </select>
        </label>
        <label>
          <span>备注说明</span>
          <textarea
            :value="refundExplain"
            rows="4"
            maxlength="100"
            placeholder="填写备注信息，100字以内"
            @input="$emit('update:refundExplain', $event.target.value.trim())"
          ></textarea>
        </label>
        <div class="refund-upload-block">
          <div class="refund-upload-title">
            <span>上传凭证</span>
            <em>( 最多可上传3张 )</em>
          </div>
          <div class="refund-pic-list">
            <figure v-for="(pic, index) in pics" :key="pic.url">
              <img :src="assetUrl(pic.localPath || pic.url)" alt="" />
              <button type="button" @click="$emit('remove-pic', index)">×</button>
            </figure>
            <label v-if="pics.length < 3" class="refund-upload">
              <input type="file" accept="image/*" :disabled="uploading" @change="$emit('upload', $event)" />
              <strong>＋</strong>
              <span>{{ uploading ? "上传中" : "上传凭证" }}</span>
            </label>
          </div>
        </div>
      </section>
      <button class="create-order-button" type="submit" :disabled="submitting">
        {{ submitting ? "提交中..." : "申请退款" }}
      </button>
    </form>
  </section>
</template>

<script setup>
import SafeImage from "../components/SafeImage.vue";

defineProps({
  loading: Boolean,
  order: { type: Object, default: null },
  refundText: { type: String, default: "" },
  refundExplain: { type: String, default: "" },
  reasons: { type: Array, default: () => [] },
  pics: { type: Array, default: () => [] },
  uploading: Boolean,
  submitting: Boolean,
  assetUrl: { type: Function, required: true },
  orderGoodsList: { type: Function, required: true },
  goodsName: { type: Function, required: true },
  goodsImage: { type: Function, required: true },
  goodsSku: { type: Function, required: true },
  goodsPrice: { type: Function, required: true },
  goodsNum: { type: Function, required: true },
  orderGoodsTotal: { type: Function, required: true }
});

defineEmits(["back", "submit", "update:refundText", "update:refundExplain", "upload", "remove-pic"]);
</script>
