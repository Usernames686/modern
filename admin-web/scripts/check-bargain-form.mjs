import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const appRoot = path.resolve(scriptDir, "..");
const modernRoot = path.resolve(appRoot, "..");

const formSource = fs.readFileSync(path.join(appRoot, "src/components/BargainProductForm.vue"), "utf8");
const serviceSource = fs.readFileSync(path.join(modernRoot, "backend/crmeb-modern-service/src/main/java/com/jsy/crmeb/modern/service/marketing/BargainAdminService.java"), "utf8");
const mapperSource = fs.readFileSync(path.join(modernRoot, "backend/crmeb-modern-service/src/main/java/com/jsy/crmeb/modern/service/marketing/mapper/BargainAdminMapper.java"), "utf8");

function assertIncludes(source, needle, label) {
  if (!source.includes(needle)) {
    throw new Error(`${label}: expected ${needle}`);
  }
}

assertIncludes(formSource, "v-model=\"selectedSkuKey\"", "bargain SKU single-select");
assertIncludes(formSource, "attrValue: [row]", "bargain payload keeps old single SKU rule");
assertIncludes(formSource, "砍价起始金额不能小于", "bargain minimum price guard");
assertIncludes(formSource, "限量不能大于库存", "bargain stock quota guard");
assertIncludes(formSource, "v-model=\"form.sort\"", "bargain sort field");
assertIncludes(formSource, "sort: form.sort", "bargain sort payload");
assertIncludes(serviceSource, "PRODUCT_TYPE_BARGAIN = 2", "bargain product type");
assertIncludes(serviceSource, "selectByProductIdAndType(id, PRODUCT_TYPE_BARGAIN)", "bargain attr value read");
assertIncludes(serviceSource, "row.put(\"minPrice\", result.get(\"minPrice\"))", "bargain min price edit echo");
assertIncludes(mapperSource, "type, quota,", "bargain attr value insert columns");
assertIncludes(mapperSource, "0, 0, 2, #{quota}", "bargain attr value type=2 insert");

console.log(JSON.stringify({
  bargainSingleSkuCompatible: true,
  typeTwoAttrValueSaved: true,
  sortFieldCovered: true,
  minPriceEchoCovered: true
}, null, 2));
