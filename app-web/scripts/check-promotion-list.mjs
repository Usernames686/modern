import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { matchedLegacyTab } from "../src/legacy/routes.js";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const appSource = fs.readFileSync(path.resolve(scriptDir, "../src/App.vue"), "utf8");
const routesSource = fs.readFileSync(path.resolve(scriptDir, "../src/legacy/routes.js"), "utf8");

function assertIncludes(source, needle, label) {
  if (!source.includes(needle)) {
    throw new Error(`${label}: expected ${needle}`);
  }
}

const params = new URLSearchParams("type=4&name=%E4%BF%83%E9%94%80");
if (matchedLegacyTab("/pages/activity/promotionList/index", params) !== "home") {
  throw new Error("promotionList must enter the modern H5 product list");
}

assertIncludes(routesSource, "/pages/activity/promotionList/index", "legacy promotion list route");
assertIncludes(appSource, "function legacyPromotionType", "legacy promotion type parser");
assertIncludes(appSource, "initialPromotionType", "initial promotion state");
assertIncludes(appSource, "activeType.value = initialPromotionType", "promotion type applied before first load");
assertIncludes(appSource, "legacyGoodsTitle.value = initialParams.get(\"name\")", "promotion title preserved");
assertIncludes(appSource, "params.get(\"typeId\")", "legacy typeId query support");
assertIncludes(appSource, "function legacyPromotionRank", "legacy product rank parser");
assertIncludes(appSource, "legacyProductRank", "legacy product rank state");
assertIncludes(appSource, "salesOrder: legacyProductRank.value ? \"desc\" : undefined", "rank list must use sales desc sorting");
assertIncludes(appSource, "applyLegacyPromotionFilters(parsed)", "clicked promotion links must preserve rank semantics");
assertIncludes(appSource, "apiGet(`/api/front/index/product/${activeType.value}`", "real product tab API");
assertIncludes(appSource, "apiGet(\"/api/front/products\", params)", "rank list must use real product API");

console.log(JSON.stringify({
  legacyPromotionListCovered: true,
  initialTypeAppliedBeforeLoad: true,
  realProductTabApi: "/api/front/index/product/{type}",
  rankUsesSalesOrder: true
}, null, 2));
