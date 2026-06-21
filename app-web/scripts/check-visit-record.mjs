import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const appRoot = path.resolve(scriptDir, "..");
const projectRoot = path.resolve(appRoot, "../..");

function assertIncludes(source, needle, label) {
  if (!source.includes(needle)) {
    throw new Error(`${label}: missing ${needle}`);
  }
}

const appSource = fs.readFileSync(path.join(appRoot, "src/App.vue"), "utf8");
assertIncludes(appSource, "function recordVisit(visitType)", "H5 visit helper");
assertIncludes(appSource, "/api/front/user/set_visit", "old visit endpoint");
assertIncludes(appSource, "recordVisit(1)", "home visit record");
assertIncludes(appSource, "recordVisit(4)", "profile visit record");

const controllerSource = fs.readFileSync(
  path.join(projectRoot, "modern/backend/crmeb-modern-front-api/src/main/java/com/jsy/crmeb/modern/front/controller/FrontUserCenterController.java"),
  "utf8"
);
assertIncludes(controllerSource, "@PostMapping(\"/api/front/user/set_visit\")", "backend old visit path");
assertIncludes(controllerSource, "frontAuthService.tokenIsExist(token)", "optional visit token");
assertIncludes(controllerSource, "userCenterService.recordVisit(currentUid, visitType)", "visit service call");

const serviceSource = fs.readFileSync(
  path.join(projectRoot, "modern/backend/crmeb-modern-service/src/main/java/com/jsy/crmeb/modern/service/front/FrontUserCenterService.java"),
  "utf8"
);
assertIncludes(serviceSource, "public boolean recordVisit(Integer uid, Integer visitType)", "visit service method");
assertIncludes(serviceSource, "safeType < 1 || safeType > 4", "visit type guard");

const userMapperSource = fs.readFileSync(
  path.join(projectRoot, "modern/backend/crmeb-modern-service/src/main/java/com/jsy/crmeb/modern/service/front/mapper/FrontUserCenterMapper.java"),
  "utf8"
);
assertIncludes(userMapperSource, "insert into eb_user_visit_record(date, uid, visit_type)", "visit table insert");

const productServiceSource = fs.readFileSync(
  path.join(projectRoot, "modern/backend/crmeb-modern-service/src/main/java/com/jsy/crmeb/modern/service/front/FrontProductService.java"),
  "utf8"
);
assertIncludes(productServiceSource, "productMapper.incrementBrowse(id)", "product browse increment");
assertIncludes(productServiceSource, "insertVisitRecord(LocalDate.now().toString(), uid, 2)", "product detail visit record");

const productMapperSource = fs.readFileSync(
  path.join(projectRoot, "modern/backend/crmeb-modern-service/src/main/java/com/jsy/crmeb/modern/service/product/mapper/StoreProductMapper.java"),
  "utf8"
);
assertIncludes(productMapperSource, "browse = coalesce(browse, 0) + 1", "product browse SQL");

console.log(JSON.stringify({
  checked: [
    "old H5 user/set_visit API",
    "home visit_type=1 report",
    "product detail visit_type=2 and browse increment",
    "profile visit_type=4 report",
    "eb_user_visit_record insert"
  ],
  ok: true
}, null, 2));
