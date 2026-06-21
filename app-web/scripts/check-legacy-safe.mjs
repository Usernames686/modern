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

const safePaths = [
  "/pages/users/wechat_login/index",
  "/pages/users/alipay_invoke/index",
  "/pages/users/app_update/app_update",
  "/pages/auth/index"
];

for (const legacyPath of safePaths) {
  assertEqual(matchedLegacyTab(legacyPath, new URLSearchParams()), "legacySafe", legacyPath);
}

assertEqual(matchedLegacyTab("/pages/users/login/index", new URLSearchParams()), "profile", "account login path");
assertEqual(matchedLegacyTab("/pages/users/app_login/index", new URLSearchParams()), "profile", "app login path");

const appSource = fs.readFileSync(path.join(appRoot, "src/App.vue"), "utf8");
assertIncludes(appSource, "currentTab === 'legacySafe'", "App legacySafe view");
assertIncludes(appSource, "legacySafeContext", "App legacySafe context");
assertIncludes(appSource, "@pay-status=\"loadPayStatus\"", "safe pay status action");
assertIncludes(appSource, "@payment=\"openPaymentPage\"", "safe payment action");

const viewSource = fs.readFileSync(path.join(appRoot, "src/views/LegacySafeView.vue"), "utf8");
assertIncludes(viewSource, "不主动调用微信授权接口", "wechat safe copy");
assertIncludes(viewSource, "不会自动发起第三方支付", "alipay safe copy");
assertIncludes(viewSource, "不主动下载或跳转应用市场", "app update safe copy");

console.log("legacy safe route checks passed");
