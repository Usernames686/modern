import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const appRoot = path.resolve(scriptDir, "..");
const projectRoot = path.resolve(appRoot, "../..");

function read(relativePath) {
  return fs.readFileSync(path.join(projectRoot, relativePath), "utf8");
}

function assertIncludes(source, needle, label) {
  if (!source.includes(needle)) {
    throw new Error(`${label}: missing ${needle}`);
  }
}

function assertNotIncludes(source, needle, label) {
  if (source.includes(needle)) {
    throw new Error(`${label}: unexpected ${needle}`);
  }
}

const frontApi = "modern/backend/crmeb-modern-front-api/src/main/java/com/jsy/crmeb/modern/front/controller";
const service = "modern/backend/crmeb-modern-service/src/main/java/com/jsy/crmeb/modern/service/front";

const seckillController = read(`${frontApi}/FrontSeckillController.java`);
const combinationController = read(`${frontApi}/FrontCombinationController.java`);
const bargainController = read(`${frontApi}/FrontBargainController.java`);

for (const [name, source] of [
  ["seckill detail controller", seckillController],
  ["combination detail controller", combinationController],
  ["bargain detail controller", bargainController]
]) {
  assertIncludes(source, "HttpServletRequest request", name);
  assertIncludes(source, "uidOrNull(request)", name);
  assertIncludes(source, "frontAuthService.tokenIsExist(token)", name);
}

const seckillService = read(`${service}/FrontSeckillService.java`);
const combinationService = read(`${service}/FrontCombinationService.java`);
const bargainService = read(`${service}/FrontBargainService.java`);

for (const [name, source] of [
  ["seckill detail service", seckillService],
  ["combination detail service", combinationService],
  ["bargain detail service", bargainService]
]) {
  assertIncludes(source, "detail(Integer id, Integer uid)", name);
  assertIncludes(source, "userCenterService.recordVisit(uid, 3)", name);
}

const combinationMapper = read(`${service}/mapper/FrontCombinationMapper.java`);
assertIncludes(combinationMapper, "update eb_store_combination", "combination browse table");
assertIncludes(combinationMapper, "browse = coalesce(browse, 0) + 1", "combination browse increment");

const bargainMapper = read(`${service}/mapper/FrontBargainMapper.java`);
assertIncludes(bargainMapper, "update eb_store_bargain", "bargain look table");
assertIncludes(bargainMapper, "look = coalesce(look, 0) + 1", "bargain look increment");

assertNotIncludes(seckillService, "eb_store_seckill set browse", "seckill nonexistent browse column");
assertNotIncludes(seckillService, "incrementBrowse", "seckill nonexistent browse increment");

console.log(JSON.stringify({
  checked: [
    "seckill/combination/bargain detail optional token",
    "marketing activity visit_type=3",
    "combination browse increment",
    "bargain look increment",
    "no seckill browse write"
  ],
  ok: true
}, null, 2));
