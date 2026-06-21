import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const appPath = path.resolve(scriptDir, "../src/App.vue");
const source = fs.readFileSync(appPath, "utf8");

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function objectLiteralBody(name) {
  const start = source.indexOf(`const ${name} = {`);
  if (start < 0) {
    throw new Error(`${name} not found`);
  }
  const open = source.indexOf("{", start);
  let depth = 0;
  for (let index = open; index < source.length; index += 1) {
    if (source[index] === "{") depth += 1;
    if (source[index] === "}") depth -= 1;
    if (depth === 0) {
      return source.slice(open + 1, index);
    }
  }
  throw new Error(`${name} body not closed`);
}

function literalMap(name) {
  const body = objectLiteralBody(name);
  return Object.fromEntries(
    [...body.matchAll(/'([^']+)'\s*:\s*(?:'([^']+)'|\{[\s\S]*?\})/g)]
      .map((match) => [match[1], match[2] || true])
  );
}

function renderedRules() {
  const exact = new Set();
  const prefixes = new Set();
  for (const match of source.matchAll(/currentPath\s*={2,3}\s*'([^']+)'/g)) {
    exact.add(match[1]);
  }
  for (const match of source.matchAll(/currentMenu\?\.component\s*={2,3}\s*'([^']+)'/g)) {
    exact.add(match[1]);
  }
  for (const match of source.matchAll(/currentPath\.startsWith\('([^']+)'\)/g)) {
    prefixes.add(match[1]);
  }
  return { exact, prefixes };
}

const routeAliases = literalMap("routeAliases");
const parentRouteFallbacks = literalMap("parentRouteFallbacks");
const migratedRouteMeta = literalMap("migratedRouteMeta");
const { exact, prefixes } = renderedRules();

const requiredLegacyAliases = {
  "/systemSetting/setting/index": "/operation/setting",
  "/systemSetting/administratorAuthority/adminList/index": "/operation/roleManager/adminList",
  "/systemSetting/administratorAuthority/adminList/edit": "/operation/roleManager/adminList",
  "/systemSetting/administratorAuthority/identityManager/edit": "/operation/roleManager/identityManager",
  "/systemSetting/deliverGoods/takeGoods/deliveryAddress/index": "/operation/deliverGoods/takeGoods/deliveryAddress",
  "/systemSetting/deliverGoods/takeGoods/deliveryAddress/addPoint": "/operation/deliverGoods/takeGoods/deliveryAddress",
  "/systemSetting/deliverGoods/takeGoods/collateOrder/index": "/operation/deliverGoods/takeGoods/collateOrder",
  "/systemSetting/deliverGoods/takeGoods/collateUser/addClerk": "/operation/deliverGoods/takeGoods/collateUser",
  "/systemSetting/deliverGoods/freightSet/creatTemplates": "/operation/deliverGoods/freightSet",
  "/sms/smsTemplate/index": "/operation/systemSms/template",
  "/sms/smsRecord/index": "/operation/systemSms/record",
  "/appSetting/wxAccount/wxMenus": "/appSetting/publicAccount/wxMenus",
  "/appSetting/wxAccount/reply/follow/index": "/appSetting/publicAccount/wxReply/follow",
  "/appSetting/wxAccount/reply/keyword/index": "/appSetting/publicAccount/wxReply/keyword",
  "/maintain/devconfig/combinedData": "/maintain/devconfiguration/combineddata",
  "/maintain/devconfig/configList": "/maintain/devconfiguration/configCategory",
  "/financial/commission/index": "/financial/commission/template",
  "/financial/commission/withdrawal": "/financial/commission/template",
  "/design/devise/index": "/design/devise",
  "/design/devise/creatDevise": "/design/devise",
  "/marketing/seckill/seckillConfig/index": "/marketing/seckill/config",
  "/marketing/seckill/seckillList/creatSeckill": "/marketing/seckill/creatSeckill",
  "/marketing/bargain/bargainGoods/index": "/marketing/bargain/bargainGoods",
  "/marketing/bargain/bargainGoods/creatBargain": "/marketing/bargain/creatBargain",
  "/marketing/groupBuy/groupGoods/index": "/marketing/groupBuy/groupGoods",
  "/marketing/groupBuy/groupGoods/creatGroup": "/marketing/groupBuy/creatGroup",
  "/store/creatStore": "/store/list/creatProduct",
  "/store/creatStore/index": "/store/list/creatProduct",
  "/user/list/index": "/user/index",
  "/user/list/edit": "/user/index",
  "/user/list/userDetails": "/user/index",
  "/user/grade/creatGrade": "/user/grade",
  "/content/article/list": "/content/articleManager"
};

for (const [from, to] of Object.entries(requiredLegacyAliases)) {
  if (routeAliases[from] !== to) {
    throw new Error(`routeAliases: expected ${from} -> ${to}`);
  }
}

const prefixAliases = [...source.matchAll(/\{ from: '([^']+)', to: '([^']+)' \}/g)]
  .map((match) => ({ from: match[1], to: match[2] }));

const legacyDynamicPrefixes = [
  "/content/articleCreat",
  "/store/list/creatProduct",
  "/marketing/coupon/list/save",
  "/appSetting/publicAccount/wxReply/keyword/save",
  "/marketing/seckill/list/",
  "/marketing/seckill/creatSeckill",
  "/marketing/bargain/creatBargain",
  "/marketing/groupBuy/creatGroup",
  "/marketing/border/add",
  "/marketing/atmosphere/add"
];

const legacyRouteSamples = [
  "/page/design/creatDevise/12/1",
  "/design/devise/creatDevise/12/1",
  "/store/creatStore/index/39/1",
  "/store/creatStore/39",
  "/marketing/coupon/list/creatCoupon/7",
  "/marketing/seckill/seckillList/creatSeckill/add/2",
  "/marketing/bargain/bargainGoods/creatBargain/39",
  "/marketing/groupBuy/groupGoods/creatGroup/39/1",
  "/marketing/atmosphere/atmosphereList/addAtmosphere/4",
  "/systemSetting/deliverGoods/takeGoods/deliveryAddress/index",
  "/systemSetting/deliverGoods/takeGoods/deliveryAddress/addPoint",
  "/systemSetting/deliverGoods/takeGoods/collateOrder/index",
  "/systemSetting/deliverGoods/freightSet/creatTemplates",
  "/appSetting/wxAccount/reply/follow/index",
  "/appSetting/wxAccount/reply/keyword/index",
  "/financial/commission/withdrawal",
  "/sms/smsRecord/index",
  "/user/list/userDetails"
];

function normalizeRoutePath(pathname) {
  if (!pathname) return "";
  if (routeAliases[pathname]) return routeAliases[pathname];
  const prefixAlias = prefixAliases.find((item) => pathname.startsWith(item.from));
  if (prefixAlias) return prefixAlias.to + pathname.slice(prefixAlias.from.length);
  return parentRouteFallbacks[pathname] || pathname;
}

function isRendered(pathname) {
  if (!pathname) return false;
  if (exact.has(pathname)) return true;
  for (const prefix of prefixes) {
    if (pathname.startsWith(prefix)) return true;
  }
  return legacyDynamicPrefixes.some((prefix) => pathname.startsWith(prefix));
}

function flattenMenus(items, parents = [], out = []) {
  for (const item of items || []) {
    const chain = [...parents, item.name].filter(Boolean);
    if (item.childList?.length) {
      flattenMenus(item.childList, chain, out);
    } else if (item.component) {
      out.push({
        name: item.name,
        component: item.component,
        chain: chain.join(" / ")
      });
    }
  }
  return out;
}

const metaMissing = Object.keys(migratedRouteMeta)
  .map((pathname) => ({ component: pathname, normalized: normalizeRoutePath(pathname) }))
  .filter((row) => !isRendered(row.normalized));

if (metaMissing.length) {
  throw new Error(`migratedRouteMeta has unrendered paths: ${JSON.stringify(metaMissing, null, 2)}`);
}

const legacySampleMissing = legacyRouteSamples
  .map((pathname) => ({ component: pathname, normalized: normalizeRoutePath(pathname) }))
  .filter((row) => !isRendered(row.normalized));

if (legacySampleMissing.length) {
  throw new Error(`legacy route samples have unrendered paths: ${JSON.stringify(legacySampleMissing, null, 2)}`);
}

let menuCount = 0;
let menuMissing = [];
const menuJsonPath = process.argv[2];
if (menuJsonPath) {
  const body = JSON.parse(fs.readFileSync(menuJsonPath, "utf8"));
  const menuData = Array.isArray(body) ? body : body.data || [];
  const leaves = flattenMenus(menuData);
  menuCount = leaves.length;
  menuMissing = leaves
    .map((row) => ({ ...row, normalized: normalizeRoutePath(row.component) }))
    .filter((row) => !isRendered(row.normalized));
  if (menuMissing.length) {
    throw new Error(`admin menu has unrendered paths: ${JSON.stringify(menuMissing, null, 2)}`);
  }
}

if (!source.includes("module-placeholder")) {
  throw new Error("module-placeholder fallback not found");
}
if (!source.match(new RegExp(`${escapeRegExp("currentPath === '/design/devise'")}|${escapeRegExp("currentPath.startsWith('/design/devise/')")}`))) {
  throw new Error("design/devise route support not found");
}

console.log(JSON.stringify({
  migratedRoutes: Object.keys(migratedRouteMeta).length,
  legacySamples: legacyRouteSamples.length,
  menuCount,
  missing: 0
}, null, 2));
