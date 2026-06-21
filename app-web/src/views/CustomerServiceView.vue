<template>
  <section class="plain-view customer-service-view">
    <div class="view-head">
      <button type="button" @click="$emit('back')">返回</button>
      <h1>{{ title }}</h1>
      <span></span>
    </div>
    <div v-if="loading" class="state">加载中...</div>
    <template v-else>
      <section class="customer-agent-card">
        <div class="customer-agent-avatar">客</div>
        <div class="customer-agent-meta">
          <strong>聚商盈客服</strong>
          <span>{{ agentStatus }}</span>
        </div>
        <button v-if="chatUrl" type="button" @click="openHumanService">人工客服</button>
        <a v-else-if="displayPhone" :href="`tel:${displayPhone}`">拨打电话</a>
      </section>

      <section class="customer-chat-panel" aria-label="客服对话">
        <div class="customer-chat-date">今天</div>
        <div
          v-for="message in messages"
          :key="message.id"
          class="customer-message"
          :class="{ mine: message.mine }"
        >
          <div v-if="!message.mine" class="customer-message-avatar">客</div>
          <div class="customer-bubble">{{ message.text }}</div>
        </div>
      </section>

      <section class="customer-quick-panel" aria-label="快捷咨询">
        <button
          v-for="item in quickActions"
          :key="item"
          type="button"
          @click="sendQuick(item)"
        >
          {{ item }}
        </button>
      </section>

      <form class="customer-input-bar" @submit.prevent="sendTypedMessage">
        <input v-model.trim="draft" type="text" placeholder="请输入您想咨询的问题" />
        <button type="submit" :disabled="!draft">发送</button>
      </form>
    </template>
  </section>
</template>

<script setup>
import { computed, ref } from "vue";

const props = defineProps({
  loading: Boolean,
  config: { type: Object, default: () => ({}) },
  chatUrl: { type: String, default: "" },
  title: { type: String, default: "联系客服" }
});

const emit = defineEmits(["back", "open-chat"]);

const draft = ref("");
const quickActions = ["订单问题", "物流查询", "退款售后", "优惠券", "人工客服"];
const messages = ref([
  {
    id: 1,
    mine: false,
    text: "您好，我是聚商盈客服助手。您可以直接描述问题，也可以点击下面的快捷咨询。"
  },
  {
    id: 2,
    mine: false,
    text: "订单、物流、退款、优惠券这些常见问题，我可以先帮您快速定位入口。"
  }
]);

const displayPhone = computed(() => {
  const phone = String(props.config.consumerHotline || "").trim();
  const digits = phone.replace(/\D/g, "");
  if (!digits || digits.length < 10 || /^(\d)\1+$/.test(digits) || /^1{6,}$/.test(digits)) {
    return "";
  }
  return phone;
});

const agentStatus = computed(() => {
  if (props.chatUrl) {
    return "在线客服已接入，可转人工";
  }
  if (displayPhone.value) {
    return "当前为电话客服，可直接拨打";
  }
  return "自助客服在线，常见问题可直接咨询";
});

function sendQuick(text) {
  sendMessage(text);
}

function sendTypedMessage() {
  if (!draft.value) {
    return;
  }
  sendMessage(draft.value);
  draft.value = "";
}

function sendMessage(text) {
  const nextId = Date.now();
  messages.value.push({ id: nextId, mine: true, text });
  messages.value.push({ id: nextId + 1, mine: false, text: replyFor(text) });
}

function replyFor(text) {
  const value = String(text || "");
  if (value.includes("人工") || value.includes("客服")) {
    if (props.chatUrl) {
      return "可以的，点击上方“人工客服”即可打开在线客服窗口。";
    }
    if (displayPhone.value) {
      return `在线客服开通后可转接，您也可以点击上方电话按钮拨打 ${displayPhone.value}。`;
    }
    return "人工客服开通后可转接。您也可以先在订单详情或售后页面提交问题，商家处理后会同步状态。";
  }
  if (value.includes("订单")) {
    return "订单问题可以到“我的-全部订单”查看。待付款、待发货、待收货、待评价都会按真实订单状态展示。";
  }
  if (value.includes("物流") || value.includes("快递")) {
    return "物流信息在订单详情里查看。进入“我的-待收货”或订单详情后，点击物流即可看到配送状态。";
  }
  if (value.includes("退款") || value.includes("售后") || value.includes("退货")) {
    return "售后问题可以从订单详情进入“申请售后”，也可以在“我的-售后/退款”查看处理进度。";
  }
  if (value.includes("优惠") || value.includes("券")) {
    return "优惠券可以在首页领券中心领取，已领取的券在“我的-优惠券”查看，可用券会在结算页自动匹配。";
  }
  if (value.includes("商品") || value.includes("价格") || value.includes("库存")) {
    return "商品价格、库存和规格以商品详情页为准。您可以打开商品详情选择规格后加入购物车或立即购买。";
  }
  return "收到。您可以补充订单号、商品名称或问题类型，我会继续帮您定位处理入口。";
}

function openHumanService() {
  emit("open-chat");
}
</script>
