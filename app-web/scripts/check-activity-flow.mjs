import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const appSource = fs.readFileSync(path.resolve(scriptDir, "../src/App.vue"), "utf8");
const checkoutSource = fs.readFileSync(path.resolve(scriptDir, "../src/views/CheckoutView.vue"), "utf8");
const payStatusSource = fs.readFileSync(path.resolve(scriptDir, "../src/views/PayStatusView.vue"), "utf8");
const activitySource = fs.readFileSync(path.resolve(scriptDir, "../src/utils/activity.js"), "utf8");

function assertIncludes(source, needle, label) {
  if (!source.includes(needle)) {
    throw new Error(`${label}: expected ${needle}`);
  }
}

for (const name of ["activityOrderTypeText", "activityOrderHint", "activityOrderActionText"]) {
  assertIncludes(activitySource, `export function ${name}`, "activity helper export");
  assertIncludes(appSource, name, "activity helper wired in App");
}

assertIncludes(checkoutSource, "checkout-activity-banner", "checkout activity banner");
assertIncludes(checkoutSource, "activityTypeText(item)", "checkout activity item tag");
assertIncludes(payStatusSource, "pay-status-activity", "pay status activity banner");
assertIncludes(payStatusSource, "open-activity", "pay status activity action");
assertIncludes(appSource, "function openActivityFromPayStatus", "pay status activity routing");
assertIncludes(appSource, "openSeckillDetail({ id: info.seckillId })", "seckill pay status return");
assertIncludes(appSource, "openBargainDetail({ id: info.bargainId", "bargain pay status return");
assertIncludes(appSource, "openCombinationDetail({ id: info.combinationId", "combination pay status return");

console.log(JSON.stringify({
  checkoutActivityVisible: true,
  payStatusActivityVisible: true,
  payStatusActivityRoutes: ["seckill", "bargain", "combination"]
}, null, 2));
