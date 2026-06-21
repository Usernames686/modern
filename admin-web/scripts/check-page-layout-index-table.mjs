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
const pageLayoutServiceSource = read(path.join(
  modernRoot,
  "backend/crmeb-modern-service/src/main/java/com/jsy/crmeb/modern/service/design/PageLayoutAdminService.java"
));
const frontProductSource = read(path.join(
  modernRoot,
  "backend/crmeb-modern-service/src/main/java/com/jsy/crmeb/modern/service/front/FrontProductService.java"
));

assertIncludes(apiSource, "indexTable: '/admin/page/layout/index/table/save'", "admin save path");
assertIncludes(pageSource, "indexTable", "admin page indexTable state");
assertIncludes(pageSource, "首页商品 Tab", "admin tab label");
assertIncludes(pageSource, "preview-product-tabs", "admin phone preview");

assertIncludes(controllerSource, "@PostMapping(\"/api/admin/page/layout/index/table/save\")", "backend save endpoint");
assertIncludes(pageLayoutServiceSource, "GID_INDEX_TABLE = 70", "old gid 70");
assertIncludes(pageLayoutServiceSource, "saveIndexTable", "save service");
assertIncludes(pageLayoutServiceSource, "listValue(request, \"indexTable\")", "old request key");

assertIncludes(frontProductSource, "GID_INDEX_TABLE = 70", "front old gid 70");
assertIncludes(frontProductSource, "groupDataMapper.selectByGid(GID_INDEX_TABLE)", "front reads old group data");
assertIncludes(frontProductSource, "response.put(\"indexTable\", indexTable)", "front returns indexTable");
assertIncludes(frontProductSource, "response.put(\"explosiveMoney\", indexTable.isEmpty() ? hotProducts : indexTable)", "front returns old explosiveMoney");
assertIncludes(frontProductSource, "homePageSaleListStyle", "front reads old style key");

console.log(JSON.stringify({
  indexTableAdmin: true,
  gid70FrontIndex: true,
  oldExplosiveMoneyField: true
}, null, 2));
