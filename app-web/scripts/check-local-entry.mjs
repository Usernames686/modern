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
  if (!script.includes("--port 19528")) {
    throw new Error(`${scriptName} script must use local modern H5 port 19528`);
  }
}

if (!viteConfig.includes("port: 19528")) {
  throw new Error("vite.config.js must use local modern H5 port 19528");
}

if (!readme.includes("http://127.0.0.1:19528")) {
  throw new Error("README must document local modern H5 port 19528");
}

if (!migration.includes("本地开发和预览端口已统一为 `19528`")) {
  throw new Error("MIGRATION must record local modern H5 port 19528");
}

console.log(JSON.stringify({
  appWebLocalPort: 19528,
  dockerPortKeptForStandalone: readme.includes("http://127.0.0.1:9528")
}, null, 2));
