import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const pagePath = path.resolve(scriptDir, "../src/components/PageDiyManager.vue");
const source = fs.readFileSync(pagePath, "utf8");

function assertIncludes(needle, label) {
  if (!source.includes(needle)) {
    throw new Error(`${label}: expected ${needle}`);
  }
}

for (const needle of [
  "{ type: 'search_box', label: '搜索框'",
  "supportsSearchBox(activeComponent)",
  "selectSearchLogo(activeComponent)",
  "activeComponent.hotWords",
  "payload.name = 'headerSerch'",
  "payload.logoConfig",
  "payload.logoFixConfig",
  "payload.placeWords",
  "payload.hotWords",
  "payload.searConfig",
  "headerSerch: 'search_box'"
]) {
  assertIncludes(needle, "page DIY search box support");
}

for (const needle of [
  "{ type: 'home_hotspot', label: '热区'",
  "supportsHotspots(activeComponent)",
  "selectHotspotImage(activeComponent)",
  "addHotspot(activeComponent)",
  "removeHotspot(activeComponent, index)",
  "function normalizeHotspots",
  "item.checkoutConfig?.hotspot",
  "payload.name = 'homeHotspot'",
  "payload.checkoutConfig",
  "hotspot: normalizeHotspots(component.hotspots)",
  "picStyle",
  "isHotspot: 1",
  "function hotspotStyle"
]) {
  assertIncludes(needle, "page DIY hotspot support");
}

for (const needle of [
  "{ type: 'home_comb', label: '头部组合'",
  "supportsHomeComb(activeComponent)",
  "selectHomeCombLogo(activeComponent)",
  "component.type === 'home_comb'",
  "payload.name = 'homeComb'",
  "payload.logoConfig",
  "payload.placeWords",
  "payload.hotWords",
  "payload.listConfig",
  "payload.swiperConfig",
  "homeComb: 'home_comb'",
  "swiperConfig?.list",
  "listConfig?.list",
  "info[1]?.value"
]) {
  assertIncludes(needle, "page DIY homeComb support");
}

for (const needle of [
  "{ type: 'home_news_roll', label: '新闻播报'",
  "supportsNewsRoll(activeComponent)",
  "selectNewsLogo(activeComponent)",
  "addNotice(activeComponent)",
  "removeNotice(activeComponent, index)",
  "component.type === 'home_news_roll'",
  "item.newsConfig?.list",
  "payload.name = 'news'",
  "payload.logoConfig",
  "payload.listConfig",
  "chiild",
  "news: 'home_news_roll'",
  "preview-news-roll"
]) {
  assertIncludes(needle, "page DIY news roll support");
}

for (const needle of [
  "{ type: 'home_footer', label: '底部导航'",
  "supportsFooter(activeComponent)",
  "selectFooterImage(activeComponent, index, 'activeImage')",
  "selectFooterImage(activeComponent, index, 'inactiveImage')",
  "component.type === 'home_footer'",
  "payload.name = 'footer'",
  "payload.menuList",
  "checked: item.activeImage",
  "unchecked: item.inactiveImage",
  "footer: 'home_footer'",
  "preview-footer-nav"
]) {
  assertIncludes(needle, "page DIY footer support");
}

for (const needle of [
  "{ type: 'nav_bar', label: '商品分类'",
  "component.type === 'nav_bar'",
  "payload.name = 'tabNav'",
  "payload.listConfig",
  "tabNav: 'nav_bar'",
  "preview-nav-bar"
]) {
  assertIncludes(needle, "page DIY nav bar support");
}

for (const needle of [
  "{ type: 'home_tab', label: '商品选项卡'",
  "supportsHomeTab(activeComponent)",
  "payload.name = 'homeTab'",
  "payload.tabItemConfig",
  "payload.tabConfig",
  "payload.itemStyle",
  "payload.numConfig",
  "homeTab: 'home_tab'",
  "preview-home-tab"
]) {
  assertIncludes(needle, "page DIY home tab support");
}

for (const needle of [
  "supportsTitle(activeComponent)",
  "selectTitleBg(activeComponent)",
  "activeComponent.subTitle",
  "activeComponent.rightText",
  "activeComponent.showMore",
  "activeComponent.bgImage",
  "payload.name = 'titles'",
  "payload.titleConfig",
  "payload.titleFuConfig",
  "payload.titleRightConfig",
  "payload.linkConfig",
  "payload.bgImg",
  "payload.selectShow",
  "titles: 'home_title'"
]) {
  assertIncludes(needle, "page DIY title support");
}

for (const needle of [
  "{ type: 'home_video', label: '视频'",
  "supportsVideo(activeComponent)",
  "selectVideoFile(activeComponent)",
  "selectVideoCover(activeComponent)",
  "component.type === 'home_video'",
  "payload.name = 'video'",
  "payload.uploadVideo",
  "payload.cover",
  "payload.videoUrl",
  "videoUrl: item.videoUrl"
]) {
  assertIncludes(needle, "page DIY video support");
}

console.log(JSON.stringify({
  adminPageDiyHotspot: true,
  adminPageDiySearchBox: true,
  adminPageDiyHomeComb: true,
  adminPageDiyNewsRoll: true,
  adminPageDiyFooter: true,
  adminPageDiyNavBar: true,
  adminPageDiyHomeTab: true,
  adminPageDiyTitle: true,
  adminPageDiyVideo: true,
  oldHomeHotspotSaveShape: true,
  oldSearchBoxSaveShape: true,
  oldHomeCombSaveShape: true,
  oldNewsRollSaveShape: true,
  oldFooterSaveShape: true,
  oldNavBarSaveShape: true,
  oldHomeTabSaveShape: true,
  oldTitleSaveShape: true,
  oldVideoSaveShape: true,
  percentHotspotEditor: true
}, null, 2));
