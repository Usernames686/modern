import { matchedLegacyTab } from "../src/legacy/routes.js";

function assertTab(pathname, expected, query = "") {
  const actual = matchedLegacyTab(pathname, new URLSearchParams(query));
  if (actual !== expected) {
    throw new Error(`${pathname}: expected ${expected}, got ${actual || "(empty)"}`);
  }
}

assertTab("/pages/goods_details/index", "home", "id=9");
assertTab("/pages/goods_details_store/index", "storeList", "id=9");
assertTab("/pages/goods_list/index", "category", "cid=245&title=热门推荐");
assertTab("/pages/goods_search/index", "search", "keyword=香水");
assertTab("/pages/user/user", "profile");
assertTab("/pages/activity/couponList/index", "couponCenter");
assertTab("/pages/activity/goods_combination_status/index", "combinationStatus", "id=39");
assertTab("/pages/activity/poster-poster/index", "activityPoster", "type=2&id=39");
assertTab("/pages/small_page/index", "smallPage", "id=217");
assertTab("/pages/news_details/index", "articleDetail", "id=17");
assertTab("/pages/news_list/index", "articles");
assertTab("/pages/news/", "articles");
assertTab("/pages/users/kefu", "customerService");
assertTab("/pages/users/web_page", "customerService");
assertTab("/pages/service", "customerService");
assertTab("/pages/users/order_list/index", "orders", "status=1");
assertTab("/pages/admin/order/index", "adminMobileOrder");
assertTab("/pages/admin/order_cancellation/index", "adminMobileCancellation");
assertTab("/pages/promoter-list/index", "spreadPeople");
assertTab("/pages/promoter-order/index", "spreadOrders");
assertTab("/pages/promoter_rank/index", "spreadRank");
assertTab("/pages/commission_rank/index", "brokerageRank");
assertTab("/pages/promotion-card/promotion-card", "spreadPoster");
assertTab("/pages/user_spread_code/index", "spreadPoster");
assertTab("/pages/user_cash/index", "extractCash");
assertTab("/pages/user_spread_money/index", "brokerageRecords", "type=2");
assertTab("/pages/user_spread_money/index", "extractRecords", "type=1");
assertTab("/pages/user_spread_user/index", "spread");
assertTab("/pages/users/user_vip/index", "memberLevel");
assertTab("/pages/user_info/index", "userInfo");
assertTab("/pages/user_phone/index", "userPhone");
assertTab("/pages/user_pwd/index", "userPassword");
assertTab("/pages/users/user_phone/index", "userPhone");
assertTab("/pages/users/user_pwd/index", "userPassword");

console.log(JSON.stringify({
  shortGoodsLinks: true,
  oldSqlActivityLinks: true,
  shortArticleLinks: true,
  shortServiceLinks: true,
  shortPromoterLinks: true,
  shortProfileLinks: true
}, null, 2));
