import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { matchedLegacyTab } from "../src/legacy/routes.js";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const appRoot = path.resolve(scriptDir, "..");

function assertEqual(actual, expected, label) {
  if (actual !== expected) {
    throw new Error(`${label}: expected ${expected}, got ${actual}`);
  }
}

function assertIncludes(source, needle, label) {
  if (!source.includes(needle)) {
    throw new Error(`${label}: missing ${needle}`);
  }
}

for (const legacyPath of [
  "/pages/promoter/user_spread_user/index",
  "/pages/users/user_spread_user/index",
  "/pages/promoter/index"
]) {
  assertEqual(matchedLegacyTab(legacyPath, new URLSearchParams()), "spread", legacyPath);
}

assertEqual(matchedLegacyTab("/pages/service/index", new URLSearchParams()), "customerService", "legacy service path");

const appSource = fs.readFileSync(path.join(appRoot, "src/App.vue"), "utf8");
assertIncludes(appSource, "link.includes(\"/pages/promoter/index\")", "promoter index menu fallback");
assertIncludes(appSource, "link.includes(\"/pages/service\")", "service menu fallback");

console.log(JSON.stringify({
  spreadAliases: 3,
  serviceAlias: true,
  ok: true
}, null, 2));
