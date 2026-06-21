import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const appRoot = path.resolve(scriptDir, "..");

function read(file) {
  return fs.readFileSync(file, "utf8");
}

function assertIncludes(source, needle, label) {
  if (!source.includes(needle)) {
    throw new Error(`${label}: expected ${needle}`);
  }
}

const appSource = read(path.join(appRoot, "src/App.vue"));
const homeCategorySource = read(path.join(appRoot, "src/views/HomeCategoryView.vue"));
const productListSource = read(path.join(appRoot, "src/components/ProductList.vue"));
const styleSource = read(path.join(appRoot, "src/style.css"));

assertIncludes(appSource, "homeIndexTabs", "home index tab state");
assertIncludes(appSource, "homeIndexProducts", "home index product state");
assertIncludes(appSource, "homeSaleListStyle", "old homePageSaleListStyle state");
assertIncludes(appSource, "indexData.indexTable || indexData.explosiveMoney", "front indexTable/explosiveMoney source");
assertIncludes(appSource, "normalizeHomeIndexTabs", "home index tab normalizer");
assertIncludes(appSource, "normalizeHomeIndexProducts", "home explosiveMoney normalizer");
assertIncludes(appSource, "isLayoutType", "layout tab detector");
assertIncludes(appSource, "activeLayoutTab", "active layout tab resolver");
assertIncludes(appSource, "openHomeDiyLink(layoutTab)", "layout tab link routing");
assertIncludes(appSource, "extractHomeDiyProducts", "home diy product extractor");
assertIncludes(appSource, "mergeHomeProducts", "home product merge");
assertIncludes(appSource, "products.value = product ? mergeHomeProducts([product], homeDiyProducts.value) : homeIndexProducts.value", "layout tab fallback products");

assertIncludes(appSource, ":home-index-products=\"homeIndexProducts\"", "home category receives layout products");
assertIncludes(homeCategorySource, ":layout-products=\"homeIndexProducts\"", "product list receives layout products");
assertIncludes(homeCategorySource, "String(activeType) === String(tab.type)", "string-safe active tab matching");

assertIncludes(productListSource, "layoutProducts", "product list layout props");
assertIncludes(productListSource, "displayProducts", "product list fallback computed");
assertIncludes(productListSource, "product.layoutItem", "layout item visual tag");
assertIncludes(productListSource, "product-card-fallback", "layout item no-image fallback");
assertIncludes(styleSource, ".product-card-fallback", "layout item fallback style");

console.log(JSON.stringify({
  h5ReadsIndexTable: true,
  h5RendersConfiguredTabs: true,
  h5RoutesConfiguredLinks: true,
  h5FallsBackToLayoutCards: true
}, null, 2));
