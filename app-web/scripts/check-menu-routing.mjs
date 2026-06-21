import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const appPath = path.resolve(scriptDir, "../src/App.vue");
const source = fs.readFileSync(appPath, "utf8");

function functionBody(name) {
  const marker = `function ${name}`;
  const start = source.indexOf(marker);
  if (start < 0) {
    throw new Error(`${name} not found`);
  }
  const open = source.indexOf("{", start);
  let depth = 0;
  for (let index = open; index < source.length; index += 1) {
    const char = source[index];
    if (char === "{") depth += 1;
    if (char === "}") depth -= 1;
    if (depth === 0) {
      return source.slice(open + 1, index);
    }
  }
  throw new Error(`${name} body not closed`);
}

function assertBefore(body, earlier, later, name) {
  const earlierIndex = body.indexOf(earlier);
  const laterIndex = body.indexOf(later);
  if (laterIndex < 0) {
    return;
  }
  if (earlierIndex < 0 || earlierIndex > laterIndex) {
    throw new Error(`${name}: expected ${earlier} before ${later}`);
  }
}

for (const name of ["handleMenu", "handleProfileMenu"]) {
  const body = functionBody(name);
  assertBefore(body, "openLegacyOrderUrl(url)", "goods_seckill", name);
  assertBefore(body, "openLegacyCoveredUrl(url", "goods_combination", name);
  assertBefore(body, "routeKnownMenu(name, url", "showToast(`${name || \"该服务\"}暂不可用`)", name);
  assertBefore(body, "openLegacyCoveredUrl(url", "/pages/news/", name);
}

const legacyTabBody = functionBody("openLegacyTab");
assertBefore(legacyTabBody, "isLegacyProductDetailPath(parsed.pathname)", "switchTab(tab)", "openLegacyTab");
assertBefore(legacyTabBody, "legacyOrderStatus(parsed.pathname, parsed.params)", "switchTab(tab)", "openLegacyTab");

const productDetailPathBody = functionBody("isLegacyProductDetailPath");
for (const path of [
  "/pages/goods/goods_details/index",
  "/pages/goods_details/index"
]) {
  if (!productDetailPathBody.includes(path)) {
    throw new Error(`isLegacyProductDetailPath: expected ${path} support`);
  }
}
if (productDetailPathBody.includes("goods_details_store")) {
  throw new Error("goods_details_store must route to storeList, not product detail");
}
if (!source.includes('tab === "storeList"') || !source.includes("function openLegacyCombinationStatus") || !source.includes("function openLegacyActivityPoster")) {
  throw new Error("P0 legacy activity/store routes must have explicit semantic handlers");
}
const legacyCombinationStatusBody = functionBody("openLegacyCombinationStatus");
if (!source.includes("function openCombinationPinkDetail") || !source.includes("/api/front/combination/pink/")) {
  throw new Error("old goods_combination_status links must fetch pink detail directly by pinkId");
}
if (!legacyCombinationStatusBody.includes("openCombinationPinkDetail(pinkId)") || legacyCombinationStatusBody.includes("loadCombination()")) {
  throw new Error("openLegacyCombinationStatus must not depend on the first page combination list to resolve pinkId");
}
if (!source.includes("function mergeCombinationPinkDetail") || !source.includes("pinkMemberList") || !source.includes("combinationShareUrl")) {
  throw new Error("pink detail must merge members and keep share link context");
}

const orderStatusBody = functionBody("legacyOrderStatus");
if (!orderStatusBody.includes("/pages/users/order_list/index")) {
  throw new Error("legacyOrderStatus: expected old order_list path support");
}
if (!orderStatusBody.includes('params.get("status")') || !orderStatusBody.includes('params.get("type")')) {
  throw new Error("legacyOrderStatus: expected status/type query support");
}
if (!orderStatusBody.includes("[0, 1, 2, 3, -3]")) {
  throw new Error("legacyOrderStatus: expected old order tab status whitelist");
}
if (!source.includes("initialLegacyOrderType")) {
  throw new Error("initial legacy order list URL must set orderType before loading orders");
}
if (!source.includes("OrderListView") || !source.includes("function openOrders") || !legacyTabBody.includes('tab === "orders"')) {
  throw new Error("old order_list links must open the dedicated order list view");
}
if (!source.includes("openLegacyCheckoutFromParsed(parsed)")) {
  throw new Error("openLegacyTab: old order_confirm links must preserve checkout params");
}
if (!source.includes("openLegacyAddressBookFromParsed(parsed")) {
  throw new Error("openLegacyTab: old address edit/add links must preserve address mode");
}
if (!source.includes('parsed.pathname.includes("/pages/users/user_payment/index")')) {
  throw new Error("openLegacyTab: old user_payment links must open recharge panel");
}
if (!source.includes("legacyGoodsTitle") || !source.includes('legacyUrlValue(parsed, "title", "name")')) {
  throw new Error("old goods list/search links must preserve title/name in modern product list");
}
if (!source.includes("currentTab === 'search'") || !source.includes("SearchView") || !source.includes("/api/front/search/keyword")) {
  throw new Error("old goods_search links must open a dedicated search view with hot keywords");
}
if (!source.includes("function openSearch") || !source.includes("function searchByHotKeyword") || !source.includes("function submitSearch")) {
  throw new Error("search view must support hot keyword and submit search flows");
}
const mountedBody = source.slice(source.indexOf("onMounted(async () => {"));
assertBefore(mountedBody, "isLegacyProductDetailPath(window.location.pathname)", "if (authToken.value)", "onMounted");
assertBefore(mountedBody, "await openDetail({ id: initialProductId })", "if (currentTab.value === \"seckillDetail\"", "onMounted");
if (!source.includes("function openAdminMobileRoute") || !source.includes("VITE_ADMIN_WEB_BASE") || !source.includes("http://127.0.0.1:19527")) {
  throw new Error("old H5 admin mobile links must bridge to configurable modern admin web");
}
if (!legacyTabBody.includes('tab === "adminMobileOrder"') || !legacyTabBody.includes("/javaMobile/orderList")) {
  throw new Error("openLegacyTab: old /pages/admin/order/index must open modern mobile order list");
}
if (!legacyTabBody.includes('tab === "adminMobileCancellation"') || !legacyTabBody.includes("/javaMobile/orderCancellation")) {
  throw new Error("openLegacyTab: old /pages/admin/order_cancellation/index must open modern mobile cancellation page");
}
for (const fallbackName of ["我的余额", "账单明细", "每日签到", "会员中心", "推广中心", "佣金记录", "提现记录", "联系客服"]) {
  if (!source.includes(fallbackName)) {
    throw new Error(`fallback profile menu missing ${fallbackName}`);
  }
}
if (!source.includes("mergedProfileMenus") || !source.includes("fallbackProfileMenus")) {
  throw new Error("profile must merge backend menus with fallback service entries");
}

const knownMenuBody = functionBody("routeKnownMenu");
for (const keyword of [
  "待付款",
  "待发货",
  "待收货",
  "待评价",
  "全部订单",
  "订单详情",
  "去付款",
  "支付状态",
  "查看物流",
  "商品评价",
  "发表评价",
  "申请售后",
  "menuOrderIdFromLink",
  "menuProductIdFromLink",
  "商城首页",
  "购物车",
  "个人中心",
  "/pages/user/user",
  "优惠券中心",
  "商品搜索",
  "productTypeFromMenuLabel",
  "全部商品",
  "商品中心",
  "分类导航",
  "openPhoneChange",
  "openPasswordChange",
  "账号管理",
  "帐号管理",
  "账户管理",
  "权限设置",
  "openRechargePanel",
  "我的钱包",
  "钱包",
  "消费记录",
  "充值记录",
  "余额记录",
  "资金记录",
  "recharge_record",
  "consume_record",
  "我的卡券",
  "卡券",
  "成长值",
  "退货",
  "帮助",
  "服务中心",
  "常见问题",
  "售后政策",
  "关于我们",
  "平台介绍",
  "我的团队",
  "分享海报",
  "邀请好友",
  "extract_record",
  "extract_cash",
  "brokerage_rank",
  "spread_rank",
  "commission_record",
  "promoter_order",
  "spread_order",
  "promoter_list",
  "spread_people",
  "spread_poster",
  "promotion-card",
  "/pages/spread/index",
  "openBill",
  "openSignRecords",
  "openActivityOrders",
  "openSpreadOrders",
  "openBrokerageRecords"
]) {
  if (!knownMenuBody.includes(keyword)) {
    throw new Error(`routeKnownMenu: expected ${keyword} support`);
  }
}

const productTypeMenuBody = functionBody("productTypeFromMenuLabel");
for (const keyword of [
  "精品",
  "推荐",
  "热门",
  "热销",
  "排行",
  "新品",
  "上新",
  "促销",
  "特价",
  "折扣"
]) {
  if (!productTypeMenuBody.includes(keyword)) {
    throw new Error(`productTypeFromMenuLabel: expected ${keyword} support`);
  }
}

for (const name of ["openBanner", "handleMenu", "handleProfileMenu"]) {
  const body = functionBody(name);
  if (!body.includes("legacyMenuUrl(item)")) {
    throw new Error(`${name}: expected legacyMenuUrl(item)`);
  }
  if (!body.includes("openExternalUrl(url)")) {
    throw new Error(`${name}: expected openExternalUrl(url)`);
  }
}

if (source.includes("item?.url || item?.wap_url")) {
  throw new Error("menu URL extraction must use legacyMenuUrl(item)");
}

if (source.includes("window.confirm(")) {
  throw new Error("H5 order/address confirmations must use the in-app confirm dialog, not window.confirm");
}
if (!source.includes("function openConfirm") || !source.includes("confirmDialog")) {
  throw new Error("H5 must keep the in-app confirm dialog for order/address critical actions");
}

const profileSource = fs.readFileSync(path.resolve(scriptDir, "../src/views/ProfileView.vue"), "utf8");
if (!profileSource.includes('v-if="item.pic"') || !profileSource.includes("item.icon")) {
  throw new Error("ProfileView fallback menus without pic must render a text icon instead of a broken image");
}

console.log(JSON.stringify({
  checked: ["openBanner", "handleMenu", "handleProfileMenu"],
  legacyUrlFirst: true,
  productDetailUrlOpensDetail: true,
  orderListStatusOpensTab: true,
  legacyCheckoutPreservesParams: true,
  legacyAddressPreservesMode: true,
  legacyRechargeOpensPanel: true,
  knownMenuFallbacks: true,
  legacyMenuUrlFields: true,
  externalUrlHandled: true,
  inAppConfirmDialog: true
}, null, 2));
