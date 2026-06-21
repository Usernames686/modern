export const legacyRouteRules = [
  { paths: ["/pages/index/index"], tab: "home" },
  { paths: ["/pages/order_addcart/order_addcart"], tab: "cart" },
  { paths: ["/pages/user/index", "/pages/user/user"], tab: "profile" },
  { paths: ["/pages/goods_cate/goods_cate"], tab: "category" },
  { paths: ["/pages/goods/goods_comment_list/index", "/pages/goods_comment_list/index"], tab: "productReplies" },
  { paths: ["/pages/goods/goods_comment_con/index", "/pages/goods_comment_con/index"], tab: "comment" },
  { paths: ["/pages/goods/goods_logistics/index", "/pages/goods_logistics/index"], tab: "logistics" },
  { paths: ["/pages/goods/goods_details_store/index", "/pages/goods_details_store/index"], tab: "storeList" },
  { paths: ["/pages/goods/goods_details/index", "/pages/goods_details/index"], tab: "home" },
  {
    paths: ["/pages/goods/goods_list/index", "/pages/goods_list/index"],
    tab: ({ params }) => (params.get("cid") ? "category" : "home")
  },
  { paths: ["/pages/goods/goods_search/index", "/pages/goods_search/index"], tab: "search" },
  { paths: ["/pages/activity/goods_seckill_details/index"], tab: "seckillDetail" },
  { paths: ["/pages/activity/goods_seckill/index"], tab: "seckill" },
  { paths: ["/pages/activity/goods_bargain_details/index"], tab: "bargainDetail" },
  { paths: ["/pages/activity/bargain/index"], tokens: ["bargain_record"], tab: "bargainRecords" },
  { paths: ["/pages/activity/goods_bargain/index"], tab: "bargain" },
  { paths: ["/pages/activity/goods_combination_details/index"], tab: "combinationDetail" },
  { paths: ["/pages/activity/goods_combination_status/index"], tab: "combinationStatus" },
  { paths: ["/pages/activity/goods_combination/index"], tab: "combination" },
  { paths: ["/pages/activity/promotionList/index"], tab: "home" },
  { paths: ["/pages/activity/couponList/index"], tab: "couponCenter" },
  { paths: ["/pages/activity/poster-poster/index"], tab: "activityPoster" },
  { paths: ["/pages/activity/small_page/index", "/pages/small_page/index"], tab: "smallPage" },
  { tokens: ["combination_record", "seckill_order", "bargain_order"], tab: "activityOrders" },
  { paths: ["/pages/order/order_confirm/index"], tab: "checkout" },
  { paths: ["/pages/users/user_return_list/index"], tab: "refundList" },
  { paths: ["/pages/goods/goods_return/index"], tab: "refund" },
  { paths: ["/pages/order/order_payment/index"], tab: "paymentPage" },
  { paths: ["/pages/order/order_pay_status/index"], tab: "payStatus" },
  { paths: ["/pages/order/order_details/index"], tab: "orderDetail" },
  { paths: ["/pages/news/news_details/index", "/pages/news_details/index"], tab: "articleDetail" },
  { paths: ["/pages/news/news_list/index", "/pages/goods/news_list/index", "/pages/news_list/index", "/pages/news/"], tab: "articles" },
  { paths: ["/pages/users/kefu/index", "/pages/users/kefu", "/pages/users/web_page/index", "/pages/users/web_page", "/pages/service/index", "/pages/service"], tab: "customerService" },
  {
    paths: [
      "/pages/users/login/index",
      "/pages/users/app_login/index",
      "/pages/users/wechat_login/index",
      "/pages/users/alipay_invoke/index",
      "/pages/users/app_update/app_update",
      "/pages/auth/index"
    ],
    tab: ({ pathname }) => (
      pathname.includes("/pages/users/login/index") || pathname.includes("/pages/users/app_login/index")
        ? "profile"
        : "legacySafe"
    )
  },
  { paths: ["/pages/users/order_list/index"], tab: "orders" },
  { paths: ["/pages/admin/order/index"], tab: "adminMobileOrder" },
  { paths: ["/pages/admin/order_cancellation/index"], tab: "adminMobileCancellation" },
  { paths: ["/pages/users/user_address_list/index", "/pages/users/user_address/index"], tab: "address" },
  { paths: ["/pages/infos/user_info/index", "/pages/users/user_info/index", "/pages/user_info/index"], tab: "userInfo" },
  { paths: ["/pages/infos/user_phone/index", "/pages/users/user_phone/index", "/pages/user_phone/index"], tab: "userPhone" },
  { paths: ["/pages/infos/user_pwd_edit/index", "/pages/users/user_pwd_edit/index", "/pages/users/user_pwd/index", "/pages/user_pwd_edit/index", "/pages/user_pwd/index"], tab: "userPassword" },
  { paths: ["/pages/infos/user_vip/index", "/pages/users/user_vip/index"], tab: "memberLevel" },
  { paths: ["/pages/users/user_get_coupon/index"], tab: "couponCenter" },
  { paths: ["/pages/users/user_coupon/index"], tab: "userCoupons" },
  { paths: ["/pages/users/user_goods_collection/index"], tab: "collection" },
  { paths: ["/pages/users/user_integral/index"], tab: "integral" },
  { paths: ["/pages/users/user_bill/index"], tab: "bill" },
  { paths: ["/pages/users/user_payment/index", "/pages/users/user_money/index"], tab: "balance" },
  { paths: ["/pages/users/user_sgin_list/index"], tab: "signRecords" },
  { paths: ["/pages/users/user_sgin/index"], tab: "sign" },
  { paths: ["/pages/users/user_cash/index", "/pages/user_cash/index"], tab: "extractCash" },
  {
    paths: ["/pages/promoter/user_spread_money/index", "/pages/user_spread_money/index"],
    tab: ({ params }) => (params.get("type") === "1" ? "extractRecords" : "brokerageRecords")
  },
  { paths: ["/pages/promoter/promoter-list/index", "/pages/promoter-list/index"], tab: "spreadPeople" },
  { paths: ["/pages/promoter/promoter-order/index", "/pages/promoter-order/index"], tab: "spreadOrders" },
  { paths: ["/pages/promoter/promoter_rank/index", "/pages/promoter_rank/index"], tab: "spreadRank" },
  { paths: ["/pages/promoter/commission_rank/index", "/pages/commission_rank/index"], tab: "brokerageRank" },
  { paths: ["/pages/promoter/user_spread_code/index", "/pages/user_spread_code/index", "/pages/promotion-card/promotion-card"], tab: "spreadPoster" },
  { paths: ["/pages/promoter/user_spread_user/index", "/pages/users/user_spread_user/index", "/pages/user_spread_user/index", "/pages/promoter/index"], tab: "spread" }
];

export function matchedLegacyTab(pathname, params) {
  const rule = legacyRouteRules.find((item) =>
    (item.paths || []).some((path) => pathname.includes(path))
    || (item.tokens || []).some((token) => pathname.includes(token))
  );
  if (!rule) {
    return "";
  }
  return typeof rule.tab === "function" ? rule.tab({ pathname, params }) : rule.tab;
}

export function legacyTabFromLocation(pathname, params) {
  const tab = params.get("tab");
  if (tab) {
    return tab;
  }
  return matchedLegacyTab(pathname, params) || "home";
}

export function isLegacyPathCovered(pathname) {
  return legacyRouteRules.some((item) =>
    (item.paths || []).some((path) => pathname.includes(path))
    || (item.tokens || []).some((token) => pathname.includes(token))
  );
}

export function legacyActivityOrderType(pathname, params) {
  const type = params.get("activityOrderType");
  if (type === "seckill" || type === "bargain" || type === "combination") {
    return type;
  }
  if (pathname.includes("seckill_order")) {
    return "seckill";
  }
  if (pathname.includes("bargain_order")) {
    return "bargain";
  }
  return "combination";
}
