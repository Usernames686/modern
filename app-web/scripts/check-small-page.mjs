import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { matchedLegacyTab } from "../src/legacy/routes.js";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const appRoot = path.resolve(scriptDir, "..");
const modernRoot = path.resolve(appRoot, "..");
const appSource = fs.readFileSync(path.join(appRoot, "src/App.vue"), "utf8");
const routesSource = fs.readFileSync(path.join(appRoot, "src/legacy/routes.js"), "utf8");
const controllerSource = fs.readFileSync(path.join(modernRoot, "backend/crmeb-modern-front-api/src/main/java/com/jsy/crmeb/modern/front/controller/FrontProductController.java"), "utf8");

function assertIncludes(source, needle, label) {
  if (!source.includes(needle)) {
    throw new Error(`${label}: expected ${needle}`);
  }
}

if (matchedLegacyTab("/pages/activity/small_page/index", new URLSearchParams("id=12")) !== "smallPage") {
  throw new Error("legacy small_page must map to smallPage");
}

assertIncludes(routesSource, 'tab: "smallPage"', "small page route");
assertIncludes(controllerSource, "@GetMapping(\"/api/front/page/diy/{id}\")", "front page diy info endpoint");
assertIncludes(appSource, "currentTab === 'smallPage'", "small page view branch");
assertIncludes(appSource, "HomeDiyRenderer", "small page renderer reuse");
assertIncludes(appSource, "function loadSmallPage", "small page loader");
assertIncludes(appSource, "apiGet(`/api/front/page/diy/${Number(id || 0)}`", "small page real API");
assertIncludes(appSource, "initialSmallPageId", "legacy small page id");
assertIncludes(appSource, "@open-link=\"openHomeDiyLink\"", "small page link routing");

console.log(JSON.stringify({
  legacySmallPageCovered: true,
  frontDiyByIdEndpoint: true,
  rendererReused: "HomeDiyRenderer"
}, null, 2));
