import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const orderListPath = path.resolve(scriptDir, "../src/components/OrderList.vue");
const source = fs.readFileSync(orderListPath, "utf8");

function assertIncludes(needle, label) {
  if (!source.includes(needle)) {
    throw new Error(`${label}: expected ${needle}`);
  }
}

function assertNotIncludes(needle, label) {
  if (source.includes(needle)) {
    throw new Error(`${label}: unexpected ${needle}`);
  }
}

assertIncludes("function assetUrl(value)", "refund voucher asset normalization helper");
assertIncludes(':src="assetUrl(item)"', "refund voucher images use normalized src");
assertIncludes(':preview-src-list="[assetUrl(item)]"', "refund voucher preview uses normalized src");
assertIncludes("refundStatusText(currentOrder)", "detail drawer refund status");
assertIncludes("Number(currentOrder.refundStatus || 0) === 1", "detail drawer refund actions");
assertIncludes("微信/支付宝退款需配置对应支付服务后启用，未配置时仅更新售后状态和日志。", "refund safe-mode prompt");
assertIncludes("{ name: 'refundRefused', label: '已拒绝' }", "refused refund tab");
assertIncludes("row.refundReason", "refused refund display");
assertIncludes("openLogistics(currentOrder)", "delivery tab logistics entry");
assertIncludes("logisticsDialogVisible", "local logistics dialog");
assertIncludes("expressRecordTypeText(currentOrder.expressRecordType)", "express record type display");
assertIncludes("第三方快递轨迹需配置物流服务后启用。", "logistics safe-mode copy");
assertNotIncludes('<el-image :src="item" :preview-src-list="[item]" fit="cover" />', "raw refund voucher image path");

console.log(JSON.stringify({
  orderRefundDetail: true,
  refundVoucherAssetUrl: true,
  refundSafeModePrompt: true,
  refundRefusedVisible: true,
  localLogisticsDialog: true
}, null, 2));
