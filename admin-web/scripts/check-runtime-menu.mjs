import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const adminBase = process.env.CRMEB_ADMIN_WEB || "http://127.0.0.1:19527";
const account = process.env.CRMEB_ADMIN_ACCOUNT || "admin";
const pwd = process.env.CRMEB_ADMIN_PWD || "123456";
const timeoutMs = Number(process.env.CRMEB_RUNTIME_CHECK_TIMEOUT || 10000);
const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const adminWebDir = path.resolve(scriptDir, "..");

async function requestJson(url, options = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, { ...options, signal: controller.signal });
    const text = await response.text();
    let body;
    try {
      body = JSON.parse(text);
    } catch {
      body = { raw: text.slice(0, 200) };
    }
    return { response, body };
  } finally {
    clearTimeout(timer);
  }
}

function flattenMenus(items, out = []) {
  for (const item of items || []) {
    if (item.childList?.length) {
      flattenMenus(item.childList, out);
    } else if (item.component) {
      out.push(item);
    }
  }
  return out;
}

const login = await requestJson(`${adminBase}/api/admin/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account, pwd })
});

if (login.body?.code !== 200 || !login.body?.data?.token) {
  throw new Error(`admin login failed: ${JSON.stringify(login.body)}`);
}

const token = login.body.data.token;
const menuResult = await requestJson(`${adminBase}/api/admin/getMenus`, {
  headers: { "Authori-zation": token }
});

if (menuResult.body?.code !== 200) {
  throw new Error(`admin menus failed: ${JSON.stringify(menuResult.body)}`);
}

const leaves = flattenMenus(menuResult.body.data || []);
if (leaves.length < 50) {
  throw new Error(`admin menu leaf count too low: ${leaves.length}`);
}

const tmpFile = path.join(os.tmpdir(), `crmeb-admin-menu-${Date.now()}-${Math.random().toString(16).slice(2)}.json`);
fs.writeFileSync(tmpFile, JSON.stringify(menuResult.body.data || []));

try {
  const result = spawnSync(process.execPath, [
    path.join(scriptDir, "check-menu-coverage.mjs"),
    tmpFile
  ], {
    cwd: adminWebDir,
    encoding: "utf8"
  });
  if (result.stdout) process.stdout.write(result.stdout);
  if (result.stderr) process.stderr.write(result.stderr);
  if (result.status !== 0) {
    throw new Error(`runtime menu coverage failed with status ${result.status}`);
  }
} finally {
  fs.rmSync(tmpFile, { force: true });
}

console.log(JSON.stringify({
  runtimeMenuCoverageOk: true,
  adminBase,
  menuLeaves: leaves.length
}, null, 2));
