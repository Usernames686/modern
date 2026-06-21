import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const appRoot = path.resolve(scriptDir, "..");
const modernRoot = path.resolve(appRoot, "..");

const packageJson = JSON.parse(fs.readFileSync(path.join(appRoot, "package.json"), "utf8"));
const viteConfig = fs.readFileSync(path.join(appRoot, "vite.config.js"), "utf8");
const readme = fs.readFileSync(path.join(modernRoot, "README.md"), "utf8");
const migration = fs.readFileSync(path.join(modernRoot, "MIGRATION.md"), "utf8");

for (const scriptName of ["dev", "preview"]) {
  const script = packageJson.scripts?.[scriptName] || "";
  if (!script.includes("--port 19527")) {
    throw new Error(`${scriptName} script must use local modern admin port 19527`);
  }
}

if (!viteConfig.includes("port: 19527")) {
  throw new Error("vite.config.js must use local modern admin port 19527");
}

if (!viteConfig.includes("CRMEB_ADMIN_API_TARGET") || !viteConfig.includes("http://127.0.0.1:18080")) {
  throw new Error("vite.config.js must proxy admin API through CRMEB_ADMIN_API_TARGET with 18080 default");
}

if (!readme.includes("http://127.0.0.1:19527")) {
  throw new Error("README must document local modern admin port 19527");
}

if (!migration.includes("本地调试入口明确为 `http://127.0.0.1:19527`")) {
  throw new Error("MIGRATION must record local modern admin port 19527");
}

console.log(JSON.stringify({
  adminWebLocalPort: 19527,
  adminApiDefaultTarget: "http://127.0.0.1:18080",
  dockerPortKeptForStandalone: readme.includes("http://127.0.0.1:9527")
}, null, 2));
