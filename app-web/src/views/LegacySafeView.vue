<template>
  <section class="plain-view legacy-safe-view">
    <div class="view-head">
      <button type="button" @click="$emit('back', backTab)">返回</button>
      <h1>{{ page.title }}</h1>
      <span></span>
    </div>

    <section class="legacy-safe-hero">
      <div class="legacy-safe-mark">{{ page.mark }}</div>
      <h2>{{ page.heading }}</h2>
      <p>{{ page.description }}</p>
    </section>

    <section class="legacy-safe-card">
      <div v-for="item in page.items" :key="item.label">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </section>

    <section class="legacy-safe-actions">
      <button type="button" class="primary" @click="$emit(page.primaryEvent, page.primaryPayload)">
        {{ page.primaryText }}
      </button>
      <button
        v-if="page.secondaryText"
        type="button"
        @click="$emit(page.secondaryEvent, page.secondaryPayload)"
      >
        {{ page.secondaryText }}
      </button>
    </section>

    <p class="legacy-safe-note">{{ page.note }}</p>
  </section>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  context: {
    type: Object,
    default: () => ({ pathname: "", params: {}, returnTab: "profile" })
  }
});

defineEmits(["back", "profile", "pay-status", "payment", "home"]);

const params = computed(() => props.context?.params || {});
const pathname = computed(() => String(props.context?.pathname || ""));
const backTab = computed(() => props.context?.returnTab || "profile");

const page = computed(() => {
  if (pathname.value.includes("/pages/users/app_update/app_update")) {
    return {
      title: "版本更新",
      mark: "UP",
      heading: "当前为 H5 访问环境",
      description: "App 原生更新能力需要 plus.runtime 环境。H5 版本保留检查入口，但不主动下载或跳转应用市场。",
      items: [
        { label: "旧路径", value: "/pages/users/app_update/app_update" },
        { label: "当前环境", value: "Web H5" },
        { label: "安全策略", value: "不触发原生更新" }
      ],
      primaryText: "回到个人中心",
      primaryEvent: "profile",
      secondaryText: "回首页",
      secondaryEvent: "home",
      note: "生产 App 包启用前，需要单独确认 Android/iOS 下载地址和强制更新策略。"
    };
  }

  if (pathname.value.includes("/pages/users/alipay_invoke/index")) {
    const orderNo = params.value.orderNo || params.value.order_id || params.value.orderId || params.value.id || "";
    return {
      title: "支付宝支付",
      mark: "PAY",
      heading: "支付宝唤起已安全承接",
      description: "当前仅承接订单状态和支付页，未配置支付参数前不会自动发起第三方支付。",
      items: [
        { label: "订单号", value: orderNo || "未提供" },
        { label: "支付类型", value: params.value.type || "order" },
        { label: "外部调用", value: "已关闭" }
      ],
      primaryText: orderNo ? "查看支付状态" : "回到订单中心",
      primaryEvent: orderNo ? "pay-status" : "profile",
      primaryPayload: orderNo,
      secondaryText: orderNo ? "选择支付方式" : "",
      secondaryEvent: "payment",
      secondaryPayload: orderNo,
      note: "微信、支付宝、真实扣款和退款需完成生产参数确认后启用。"
    };
  }

  if (pathname.value.includes("/pages/auth/index")) {
    const backUrl = params.value.back_url || params.value.backUrl || "";
    return {
      title: "授权回调",
      mark: "AUTH",
      heading: "授权回调已被安全承接",
      description: "老页面会用 code/state 换取微信登录态并跳回来源页。公众号授权服务未配置前不主动调用授权接口。",
      items: [
        { label: "code", value: params.value.code ? "已携带" : "未携带" },
        { label: "state", value: params.value.state || "无" },
        { label: "回跳地址", value: backUrl ? "已携带" : "无" }
      ],
      primaryText: "账号密码登录",
      primaryEvent: "profile",
      secondaryText: "回首页",
      secondaryEvent: "home",
      note: "该页保留旧授权入口语义，避免本地误调用微信网页授权。"
    };
  }

  return {
    title: "微信登录",
    mark: "WX",
    heading: "微信授权登录已安全承接",
    description: "老 H5 会根据微信 code 直接授权登录或绑定手机号。当前使用账号密码登录承接，不主动调用微信授权接口。",
    items: [
      { label: "code", value: params.value.code ? "已携带" : "未携带" },
      { label: "scope", value: params.value.scope || "未指定" },
      { label: "外部授权", value: "已关闭" }
    ],
    primaryText: "账号密码登录",
    primaryEvent: "profile",
    secondaryText: "回首页",
    secondaryEvent: "home",
    note: "生产公众号、小程序登录启用前，需要确认 AppID、回调域名、绑定手机号策略。"
  };
});
</script>
