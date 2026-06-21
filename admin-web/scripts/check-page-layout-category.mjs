import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const appRoot = path.resolve(scriptDir, "..");
const modernRoot = path.resolve(appRoot, "..");

function read(file) {
  return fs.readFileSync(file, "utf8");
}

function assertIncludes(source, needle, label) {
  if (!source.includes(needle)) {
    throw new Error(`${label}: expected ${needle}`);
  }
}

const apiSource = read(path.join(appRoot, "src/api.js"));
const pageSource = read(path.join(appRoot, "src/components/PageLayoutManager.vue"));
const controllerSource = read(path.join(
  modernRoot,
  "backend/crmeb-modern-admin-api/src/main/java/com/jsy/crmeb/modern/admin/controller/AdminPageLayoutController.java"
));
const serviceSource = read(path.join(
  modernRoot,
  "backend/crmeb-modern-service/src/main/java/com/jsy/crmeb/modern/service/design/PageLayoutAdminService.java"
));
const frontProductSource = read(path.join(
  modernRoot,
  "backend/crmeb-modern-service/src/main/java/com/jsy/crmeb/modern/service/front/FrontProductService.java"
));

assertIncludes(apiSource, "pageLayoutCategoryConfig", "admin api method");
assertIncludes(apiSource, "/admin/page/layout/category/config", "old category config read path");
assertIncludes(apiSource, "/admin/page/layout/category/config/save", "category config save path");

assertIncludes(pageSource, "categoryConfig", "category config state");
assertIncludes(pageSource, "分类页配置", "category config tab");
assertIncludes(pageSource, "categoryPageConfig", "category page style field");
assertIncludes(pageSource, "isShowCategory", "category visibility field");

assertIncludes(controllerSource, "@GetMapping(\"/api/admin/page/layout/category/config\")", "backend read endpoint");
assertIncludes(controllerSource, "@PostMapping(\"/api/admin/page/layout/category/config/save\")", "backend save endpoint");
assertIncludes(serviceSource, "CATEGORY_PAGE_CONFIG = \"category_page_config\"", "old category_page_config key");
assertIncludes(serviceSource, "IS_SHOW_CATEGORY = \"is_show_category\"", "old is_show_category key");
assertIncludes(serviceSource, "saveCategoryConfig", "save category config service");
assertIncludes(frontProductSource, "category_page_config", "front index reads same config key");
assertIncludes(frontProductSource, "is_show_category", "front index reads same visibility key");

console.log(JSON.stringify({
  categoryConfigEndpoint: true,
  categoryConfigUi: true,
  oldConfigKeysSharedWithFront: true
}, null, 2));
