import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const appPath = path.resolve(scriptDir, "../src/App.vue");
const homeCategoryPath = path.resolve(scriptDir, "../src/views/HomeCategoryView.vue");
const rendererPath = path.resolve(scriptDir, "../src/components/HomeDiyRenderer.vue");
const backendControllerPath = path.resolve(
  scriptDir,
  "../../backend/crmeb-modern-front-api/src/main/java/com/jsy/crmeb/modern/front/controller/FrontProductController.java"
);

const appSource = fs.readFileSync(appPath, "utf8");
const homeCategorySource = fs.readFileSync(homeCategoryPath, "utf8");
const rendererSource = fs.readFileSync(rendererPath, "utf8");
const backendControllerSource = fs.readFileSync(backendControllerPath, "utf8");

function assertIncludes(source, needle, label) {
  if (!source.includes(needle)) {
    throw new Error(`${label}: expected ${needle}`);
  }
}

assertIncludes(backendControllerSource, '@GetMapping("/api/front/page/diy/default")', "front DIY endpoint");
assertIncludes(backendControllerSource, "pageDiyService.info(0)", "front DIY default data source");

assertIncludes(appSource, 'apiGet("/api/front/page/diy/default").catch(() => null)', "home loader");
assertIncludes(appSource, "homeDiyComponents.value = parseHomeDiyComponents", "home DIY parser");
assertIncludes(appSource, "function normalizeHomeDiyHotspots", "old hotspot parser");
assertIncludes(appSource, "item.checkoutConfig?.hotspot", "old hotspot data shape");
assertIncludes(appSource, "function openHomeDiyLink", "home DIY link handler");
assertIncludes(appSource, "const url = legacyMenuUrl(item)", "home DIY unified URL extraction");
assertIncludes(appSource, "openLegacyCoveredUrl(url", "home DIY legacy URL routing");
assertIncludes(appSource, "routeKnownMenu(title || homeDiyTypeTitle", "home DIY title fallback routing");
assertIncludes(appSource, "openHomeDiyLooseLink(item, url)", "home DIY loose URL fallback routing");
assertIncludes(appSource, "function openHomeDiyFallback", "home DIY empty/broken link safe fallback function");
assertIncludes(appSource, 'openHomeDiyFallback(item, title)', "home DIY empty link routes to real module instead of unavailable toast");
assertIncludes(appSource, "function openHomeDiyLooseLink", "home DIY loose URL fallback function");
assertIncludes(appSource, "function openHomeDiyPlaceholderLink", "home DIY placeholder URL fallback");
assertIncludes(appSource, "function numericUrlValue", "home DIY numeric URL product fallback");
assertIncludes(appSource, "function isPlaceholderDiyUrl", "home DIY placeholder URL detector");
assertIncludes(appSource, "const id = url.match(/(?:id|productId)=(\\d+)/)?.[1] || numericUrlValue(url)", "home DIY pure numeric URL opens product detail");
assertIncludes(appSource, "numericIdFromDiyItem(item)", "home DIY item ID fallback");
assertIncludes(appSource, "openArticleDetail({ id })", "home DIY article detail fallback");
assertIncludes(appSource, "openSeckillDetail({ id })", "home DIY seckill detail fallback");
assertIncludes(appSource, "openCombinationDetail({ id })", "home DIY combination detail fallback");
assertIncludes(appSource, "openBargainDetail({ id })", "home DIY bargain detail fallback");
assertIncludes(appSource, "loadSmallPage(id)", "home DIY small page fallback");
assertIncludes(appSource, "switchTab(\"category\")", "home DIY category fallback");
assertIncludes(appSource, 'type === "home_coupon"', "home DIY coupon item fallback");
assertIncludes(appSource, 'switchTab("couponCenter")', "home DIY coupon routes to coupon center");
assertIncludes(appSource, "/pages/activity/couponList/index?id=", "home DIY coupon ID link");
assertIncludes(appSource, "function homeDiyTypeTitle", "home DIY component type fallback routing");

assertIncludes(homeCategorySource, 'import HomeDiyRenderer from "../components/HomeDiyRenderer.vue"', "home category renderer import");
assertIncludes(homeCategorySource, ':components="diyComponents"', "home category renderer props");
assertIncludes(homeCategorySource, '@open-link="$emit(\'open-diy-link\', $event)"', "home category renderer event");

for (const type of [
  "search_box",
  "banner",
  "home_menu",
  "picture_cube",
  "nav_bar",
  "home_tab",
  "home_news_roll",
  "home_comb",
  "home_hotspot",
  "home_footer",
  "home_merchant",
  "home_goods_list",
  "home_coupon",
  "home_seckill",
  "home_group",
  "home_bargain",
  "home_article"
]) {
  assertIncludes(rendererSource, type, "home DIY renderer component support");
}

assertIncludes(rendererSource, "function openItem", "home DIY item payload routing");
assertIncludes(rendererSource, "function openComponent", "home DIY component payload routing");
assertIncludes(rendererSource, "componentType", "home DIY component type payload");
assertIncludes(rendererSource, "home-diy-tab-goods", "home DIY tab product entry render");
assertIncludes(rendererSource, "home-diy-hotspot-area", "home DIY hotspot clickable areas");
assertIncludes(rendererSource, "hotspotStyle(area)", "home DIY hotspot percent positioning");
assertIncludes(rendererSource, "<video", "home DIY video render");
assertIncludes(rendererSource, "component.videoUrl", "home DIY video source");
assertIncludes(rendererSource, "component.coverImage", "home DIY video poster");
assertIncludes(rendererSource, "component.subTitle", "home DIY title subtitle render");
assertIncludes(rendererSource, "component.rightText", "home DIY title right text render");
assertIncludes(rendererSource, "component.showMore", "home DIY title right button visibility");
assertIncludes(rendererSource, "component.bgImage", "home DIY title background render");
assertIncludes(rendererSource, "home-diy-merchant", "home DIY merchant render");
assertIncludes(rendererSource, "component.merchants", "home DIY merchant items render");
assertIncludes(rendererSource, "home-diy-products", "home DIY product list render");
assertIncludes(rendererSource, "component.productItems", "home DIY product items render");
assertIncludes(rendererSource, "home-diy-coupon-list", "home DIY coupon list render");
assertIncludes(rendererSource, "component.couponItems", "home DIY coupon items render");
assertIncludes(rendererSource, "home-diy-articles", "home DIY article list render");
assertIncludes(rendererSource, "component.articleItems", "home DIY article items render");
assertIncludes(appSource, "uploadVideo?.url", "old video upload data shape");
assertIncludes(appSource, "cover?.url", "old video cover data shape");
assertIncludes(appSource, "titleConfig?.val", "old title main data shape");
assertIncludes(appSource, "titleFuConfig?.val", "old title subtitle data shape");
assertIncludes(appSource, "titleRightConfig?.val", "old title right text data shape");
assertIncludes(appSource, "linkConfig?.val", "old title link data shape");
assertIncludes(appSource, "bgImg?.url", "old title background data shape");
assertIncludes(appSource, "selectShow?.tabVal", "old title right button data shape");
assertIncludes(appSource, "tabItemConfig?.list", "old home tab item data shape");
assertIncludes(appSource, "homeTab", "old home tab alias support");
assertIncludes(appSource, "homeMerchant", "old merchant alias support");
assertIncludes(appSource, "activeValueMer", "old merchant data shape");
assertIncludes(appSource, "function normalizeHomeDiyMerchants", "old merchant parser");
assertIncludes(appSource, "function normalizeHomeDiyProducts", "old product parser");
assertIncludes(appSource, "function normalizeHomeDiyCoupons", "old coupon parser");
assertIncludes(appSource, "function normalizeHomeDiyArticles", "old article parser");
assertIncludes(appSource, "goodsList?.list", "old goods list data shape");
assertIncludes(appSource, "selectConfig?.goodsList", "old selected goods data shape");
assertIncludes(appSource, "selectConfig?.articleList", "old selected article data shape");

for (const alias of [
  "headerSerch",
  "swiperBg",
  "pictureCube",
  "blankPage",
  "guide",
  "titles",
  "richTextEditor",
  "goodList",
  "homeCoupons",
  "homeMerchant",
  "homeArticle",
  "homeComb",
  "homeHotspot",
  "footer",
  "news",
  "tabNav",
  "homeTab",
  "menus",
  "seckill",
  "group",
  "bargain"
]) {
  assertIncludes(appSource, alias, "old Page DIY component alias support");
}

for (const shape of [
  "menuConfig?.list",
  "listConfig?.list",
  "swiperConfig?.list",
  "picStyle?.picList",
  "pictureCure?.list",
  "tpmf?.list",
  "menuList?.list",
  "tabConfig?.list",
  "newsConfig?.list"
]) {
  assertIncludes(appSource, shape, "old Page DIY nested data shape support");
}

console.log(JSON.stringify({
  frontDiyEndpoint: true,
  homeLoadsDiy: true,
  homeRendersDiy: true,
  legacyLinksHandled: true,
  looseDiyLinksHandled: true
}, null, 2));
