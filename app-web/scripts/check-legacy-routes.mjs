import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { isLegacyPathCovered, legacyTabFromLocation } from "../src/legacy/routes.js";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(scriptDir, "../../..");
const pagesJsonPath = path.join(repoRoot, "app/pages.json");

function stripJsonComments(input) {
  let output = "";
  let inString = false;
  let stringQuote = "";
  let escaped = false;

  for (let index = 0; index < input.length; index += 1) {
    const char = input[index];
    const next = input[index + 1];

    if (inString) {
      output += char;
      if (escaped) {
        escaped = false;
      } else if (char === "\\") {
        escaped = true;
      } else if (char === stringQuote) {
        inString = false;
      }
      continue;
    }

    if (char === "\"" || char === "'") {
      inString = true;
      stringQuote = char;
      output += char;
      continue;
    }

    if (char === "/" && next === "/") {
      while (index < input.length && input[index] !== "\n") {
        index += 1;
      }
      output += "\n";
      continue;
    }

    if (char === "/" && next === "*") {
      index += 2;
      while (index < input.length && !(input[index] === "*" && input[index + 1] === "/")) {
        index += 1;
      }
      index += 1;
      continue;
    }

    output += char;
  }

  return output;
}

function loadLegacyPages() {
  const pagesConfig = JSON.parse(stripJsonComments(fs.readFileSync(pagesJsonPath, "utf8")));
  const legacyPages = [];

  for (const item of pagesConfig.pages || []) {
    legacyPages.push(`/${item.path}`);
  }

  for (const subPackage of pagesConfig.subPackages || []) {
    for (const item of subPackage.pages || []) {
      legacyPages.push(`/${path.posix.join(subPackage.root, item.path)}`);
    }
  }

  return legacyPages;
}

const pages = loadLegacyPages();
const missing = [];
const covered = [];

for (const legacyPath of pages) {
  if (!isLegacyPathCovered(legacyPath)) {
    missing.push(legacyPath);
    continue;
  }
  covered.push({
    path: legacyPath,
    tab: legacyTabFromLocation(legacyPath, new URLSearchParams())
  });
}

console.log(JSON.stringify({
  total: pages.length,
  covered: covered.length,
  missing
}, null, 2));

if (missing.length > 0) {
  process.exit(1);
}
