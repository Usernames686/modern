<template>
  <section class="plain-view profile-view">
    <div class="profile-card" v-if="user">
      <SafeImage class="profile-avatar" :src="assetUrl(user.avatar || defaultAvatar)" :alt="user.nickname || user.account || '用户'" :label="user.nickname || user.account || '用户'" variant="icon" />
      <div>
        <h1>{{ user.nickname || user.account }}</h1>
        <p>{{ user.phone || "未绑定手机号" }}</p>
      </div>
      <button type="button" @click="$emit('logout')">退出</button>
    </div>

    <form v-else class="login-card" @submit.prevent="$emit('login')">
      <h1>账号登录</h1>
      <input :value="loginForm.account" type="text" placeholder="手机号/账号" @input="$emit('update-login-field', 'account', $event.target.value.trim())" />
      <input :value="loginForm.password" type="password" placeholder="密码" @input="$emit('update-login-field', 'password', $event.target.value)" />
      <button type="submit" :disabled="loginLoading">{{ loginLoading ? "登录中..." : "登录" }}</button>
      <p>{{ authMessage || "使用老 CRMEB 用户账号登录" }}</p>
    </form>

    <template v-if="authToken">
      <section class="profile-stats" v-if="user">
        <div @click="$emit('balance')">
          <strong>{{ user.nowMoney || 0 }}</strong>
          <span>余额</span>
        </div>
        <div @click="$emit('integral')">
          <strong>{{ user.integral || 0 }}</strong>
          <span>积分</span>
        </div>
        <div>
          <strong>{{ orderCountSummary }}</strong>
          <span>订单</span>
        </div>
      </section>

      <section class="profile-links">
        <button type="button" @click="$emit('user-info')">
          <span>个人资料</span>
          <strong>›</strong>
        </button>
        <button type="button" @click="$emit('collection')">
          <span>我的收藏</span>
          <strong>›</strong>
        </button>
        <button type="button" @click="$emit('coupons')">
          <span>我的优惠券</span>
          <strong>{{ userCouponUsableCount || "" }}</strong>
        </button>
        <button type="button" @click="$emit('address')">
          <span>收货地址</span>
          <strong>{{ addresses.length || "" }}</strong>
        </button>
        <button type="button" @click="$emit('refund-list')">
          <span>售后/退款</span>
          <strong>›</strong>
        </button>
      </section>

      <section class="profile-activity-panel">
        <div class="service-title">
          <h1>活动记录</h1>
          <span>砍价 / 拼团 / 秒杀</span>
        </div>
        <div class="profile-activity-grid">
          <button type="button" @click="$emit('bargain-records')">
            <b>砍</b>
            <span>砍价记录</span>
          </button>
          <button type="button" @click="$emit('activity-orders', 'combination')">
            <b>拼</b>
            <span>拼团记录</span>
          </button>
          <button type="button" @click="$emit('activity-orders', 'seckill')">
            <b>秒</b>
            <span>秒杀订单</span>
          </button>
        </div>
      </section>

      <section v-if="profileBanners.length" class="profile-banner">
        <button
          v-for="item in profileBanners"
          :key="item.id"
          type="button"
          @click="$emit('profile-menu', item)"
        >
          <SafeImage :src="assetUrl(item.pic)" :alt="item.name || '活动'" :label="item.name || '活动'" variant="banner" />
        </button>
      </section>

      <section class="profile-service-panel">
        <div class="service-title">
          <h1>我的服务</h1>
          <span>{{ profileMenus.length }} 项</span>
        </div>
        <div v-if="profileMenuLoading" class="state">加载中...</div>
        <div v-else-if="profileMenus.length === 0" class="state">暂无服务菜单</div>
        <div v-else class="profile-service-grid">
          <button
            v-for="item in profileMenus"
            :key="item.id"
            type="button"
            @click="$emit('profile-menu', item)"
          >
            <SafeImage v-if="item.pic" :src="assetUrl(item.pic)" :alt="item.name" :label="item.name" variant="icon" />
            <b v-else>{{ item.icon || String(item.name || "服").slice(0, 1) }}</b>
            <span>{{ item.name }}</span>
          </button>
        </div>
      </section>

      <section class="order-panel">
        <div class="order-summary">
          <div>
            <strong>订单信息</strong>
            <span>消费订单：{{ orderData.orderCount || orderCountSummary || 0 }}</span>
          </div>
          <img src="/static/images/orderTime.png" alt="" />
        </div>
        <div class="view-head compact">
          <h1>订单中心</h1>
          <button type="button" @click="$emit('refresh-orders')">刷新</button>
        </div>
        <div class="order-tabs">
          <button
            v-for="item in orderTabs"
            :key="item.type"
            :class="{ active: orderType === item.type }"
            type="button"
            @click="$emit('select-order-type', item.type)"
          >
            {{ item.name }}
          </button>
        </div>
        <div v-if="orderLoading" class="state">加载中...</div>
        <div v-else-if="orders.length === 0" class="state">暂无订单</div>
        <article v-for="order in orders" v-else :key="order.id || order.orderId" class="order-card" @click="$emit('open-order', order, orderType === -3)">
          <div class="order-card-head">
            <span>{{ order.orderId }}</span>
            <strong>{{ orderType === -3 ? refundStatusText(order) : order.statusText }}</strong>
          </div>
          <div v-for="goods in orderGoodsList(order)" :key="goods.id || goods.unique" class="order-goods">
            <SafeImage :src="assetUrl(goodsImage(goods))" :alt="goodsName(goods)" :label="goodsName(goods)" variant="product" />
            <div>
              <h2>{{ goodsName(goods) }}</h2>
              <p>{{ goodsSku(goods) || "默认规格" }}</p>
              <span>￥{{ goodsPrice(goods) }} × {{ goodsNum(goods) }}</span>
            </div>
          </div>
          <div class="order-total">实付 ￥{{ order.payPrice }}</div>
          <div class="order-actions">
            <button type="button" @click.stop="$emit('open-order', order, orderType === -3)">查看详情</button>
            <button v-if="isUnpaid(order)" type="button" :disabled="payingOrder === order.orderId" @click.stop="$emit('pay-order', order)">
              {{ payingOrder === order.orderId ? "支付中..." : "去支付" }}
            </button>
            <button v-if="isUnpaid(order)" type="button" @click.stop="$emit('cancel-order', order)">取消订单</button>
            <button v-if="canApplyRefund(order)" type="button" @click.stop="$emit('refund-order', order)">申请售后</button>
            <button v-if="canTakeOrder(order)" type="button" @click.stop="$emit('take-order', order)">确认收货</button>
            <button v-if="canCommentOrder(order)" type="button" @click.stop="$emit('comment-order', order)">去评价</button>
            <button v-if="canDeleteOrder(order)" type="button" @click.stop="$emit('delete-order', order)">删除订单</button>
            <button v-if="canOrderAgain(order)" type="button" @click.stop="$emit('again-order', order)">再次购买</button>
          </div>
        </article>
      </section>
    </template>
  </section>
</template>

<script setup>
import SafeImage from "../components/SafeImage.vue";

defineProps({
  authToken: { type: String, default: "" },
  user: { type: Object, default: null },
  loginForm: { type: Object, required: true },
  loginLoading: Boolean,
  authMessage: { type: String, default: "" },
  defaultAvatar: { type: String, required: true },
  userCouponUsableCount: { type: [Number, String], default: 0 },
  addresses: { type: Array, default: () => [] },
  profileBanners: { type: Array, default: () => [] },
  profileMenus: { type: Array, default: () => [] },
  profileMenuLoading: Boolean,
  orderData: { type: Object, default: () => ({}) },
  orderCountSummary: { type: Number, default: 0 },
  orderTabs: { type: Array, default: () => [] },
  orderType: { type: [Number, String, null], default: null },
  orderLoading: Boolean,
  orders: { type: Array, default: () => [] },
  payingOrder: { type: String, default: "" },
  assetUrl: { type: Function, required: true },
  orderGoodsList: { type: Function, required: true },
  goodsName: { type: Function, required: true },
  goodsImage: { type: Function, required: true },
  goodsSku: { type: Function, required: true },
  goodsPrice: { type: Function, required: true },
  goodsNum: { type: Function, required: true },
  refundStatusText: { type: Function, required: true },
  isUnpaid: { type: Function, required: true },
  canApplyRefund: { type: Function, required: true },
  canTakeOrder: { type: Function, required: true },
  canCommentOrder: { type: Function, required: true },
  canDeleteOrder: { type: Function, required: true },
  canOrderAgain: { type: Function, required: true }
});

defineEmits([
  "logout",
  "login",
  "update-login-field",
  "balance",
  "integral",
  "user-info",
  "collection",
  "coupons",
  "address",
  "refund-list",
  "bargain-records",
  "activity-orders",
  "profile-menu",
  "refresh-orders",
  "select-order-type",
  "open-order",
  "pay-order",
  "cancel-order",
  "refund-order",
  "take-order",
  "comment-order",
  "delete-order",
  "again-order"
]);
</script>
