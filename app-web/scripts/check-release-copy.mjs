import { readFileSync, readdirSync, statSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL("../src", import.meta.url));
const banned = [
  "本地迁移",
  "迁移环境",
  "迁移模式",
  "迁移阶段",
  "迁移版",
  "迁移中",
  "正在迁移",
  "未迁移",
  "本地交付环境",
  "本地安全模式",
  "暂不可用",
  "dry-run",
  "假数据",
  "mock 数据",
  "mock"
];
const hits = [];

function walk(dir) {
  for (const name of readdirSync(dir)) {
    const file = join(dir, name);
    const stat = statSync(file);
    if (stat.isDirectory()) {
      walk(file);
    } else if (/\.(vue|js|ts|css|scss)$/.test(name)) {
      const text = readFileSync(file, "utf8");
      for (const word of banned) {
        if (text.includes(word)) {
          hits.push({ file, word });
        }
      }
    }
  }
}

walk(root);

if (hits.length) {
  console.error(JSON.stringify({ releaseCopyOk: false, hits }, null, 2));
  process.exit(1);
}

console.log(JSON.stringify({ releaseCopyOk: true, checked: root }, null, 2));
