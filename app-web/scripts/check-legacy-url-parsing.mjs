import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const appPath = path.resolve(scriptDir, "../src/App.vue");
const source = fs.readFileSync(appPath, "utf8");

function functionSource(name) {
  const marker = `function ${name}`;
  const start = source.indexOf(marker);
  if (start < 0) {
    throw new Error(`${name} not found`);
  }
  const open = source.indexOf("{", start);
  let depth = 0;
  for (let index = open; index < source.length; index += 1) {
    const char = source[index];
    if (char === "{") depth += 1;
    if (char === "}") depth -= 1;
    if (depth === 0) {
      return source.slice(start, index + 1);
    }
  }
  throw new Error(`${name} body not closed`);
}

const context = {
  URL,
  URLSearchParams,
  window: { location: { origin: "http://127.0.0.1:19528" } }
};

vm.createContext(context);
vm.runInContext([
  functionSource("parseLegacyUrl"),
  functionSource("normalizeLegacyUrl"),
  functionSource("normalizeExternalUrl"),
  functionSource("safeDecodeLegacyUrl"),
  functionSource("isLegacyInternalUrl"),
  functionSource("parseLegacyHashRoute")
].join("\n"), context);

function assertEqual(actual, expected, message) {
  if (actual !== expected) {
    throw new Error(`${message}: expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`);
  }
}

const webView = context.parseLegacyUrl("/pages/users/web_page/index?webUel=https%3A%2F%2Fexample.com%2Fa%3Fb%3D1%26c%3D2&title=帮助中心");
assertEqual(webView.pathname, "/pages/users/web_page/index", "web-view pathname");
assertEqual(webView.params.get("webUel"), "https://example.com/a?b=1&c=2", "web-view nested URL");
assertEqual(webView.params.get("title"), "帮助中心", "web-view title");

const encodedOrder = context.parseLegacyUrl("%2Fpages%2Forder%2Forder_details%2Findex%3ForderId%3D123%26isReturn%3D1");
assertEqual(encodedOrder.pathname, "/pages/order/order_details/index", "encoded order pathname");
assertEqual(encodedOrder.params.get("orderId"), "123", "encoded order id");
assertEqual(encodedOrder.params.get("isReturn"), "1", "encoded order return flag");

const hashProduct = context.parseLegacyUrl("http://127.0.0.1:19528/#/pages/goods/goods_details/index?id=45");
assertEqual(hashProduct.pathname, "/pages/goods/goods_details/index", "hash product pathname");
assertEqual(hashProduct.params.get("id"), "45", "hash product id");

const external = context.normalizeExternalUrl("https%3A%2F%2Fexample.com%2Fhelp%3Fx%3D1");
assertEqual(external, "https://example.com/help?x=1", "encoded external URL");

console.log(JSON.stringify({
  checked: [
    "web-view nested encoded URL",
    "encoded legacy order path",
    "hash legacy product path",
    "encoded external URL"
  ],
  ok: true
}, null, 2));
