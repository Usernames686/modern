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
assertIncludes(appSource, "initialParams.get(\"spread\")", "legacy spread param");
assertIncludes(appSource, "initialParams.get(\"spreadPid\")", "modern spreadPid param");
assertIncludes(appSource, "initialParams.get(\"spread_spid\")", "old login spread_spid param");
assertIncludes(appSource, "crmeb-front-spread", "spread localStorage key");
assertIncludes(appSource, "bindCachedSpread", "cached spread bind flow");
assertIncludes(appSource, "/api/front/user/bindSpread", "front bindSpread API call");

const controllerSource = fs.readFileSync(
  path.join(projectRoot, "modern/backend/crmeb-modern-front-api/src/main/java/com/jsy/crmeb/modern/front/controller/FrontUserCenterController.java"),
  "utf8"
);
assertIncludes(controllerSource, "@GetMapping(\"/api/front/user/bindSpread\")", "backend bindSpread path");
assertIncludes(controllerSource, "userCenterService.bindSpread(uid(request), spreadPid)", "backend bindSpread token uid");

const serviceSource = fs.readFileSync(
  path.join(projectRoot, "modern/backend/crmeb-modern-service/src/main/java/com/jsy/crmeb/modern/service/front/FrontUserCenterService.java"),
  "utf8"
);
assertIncludes(serviceSource, "public boolean bindSpread(Integer uid, Integer spreadPid)", "service bindSpread method");
assertIncludes(serviceSource, "brokerage_func_status", "distribution enable config");
assertIncludes(serviceSource, "store_brokerage_status", "distribution mode config");
assertIncludes(serviceSource, "wouldCreateSpreadLoop", "spread loop protection");
assertIncludes(serviceSource, "userMapper.updateSpread(uid, spreadPid", "spread relation write");
assertIncludes(serviceSource, "userMapper.updateSpreadCount(spreadPid, 1)", "spread count write");

const mapperSource = fs.readFileSync(
  path.join(projectRoot, "modern/backend/crmeb-modern-service/src/main/java/com/jsy/crmeb/modern/service/front/mapper/FrontUserCenterMapper.java"),
  "utf8"
);
assertIncludes(mapperSource, "selectUserSpreadUid", "spread parent lookup");

console.log(JSON.stringify({
  checked: [
    "legacy spread URL params",
    "H5 local cache and silent bind",
    "old API path /api/front/user/bindSpread",
    "distribution enable/mode guards",
    "spread loop protection",
    "old eb_user spread fields"
  ],
  ok: true
}, null, 2));
