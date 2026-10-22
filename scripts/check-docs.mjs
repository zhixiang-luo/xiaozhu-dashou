import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const textExtensions = new Set([".md", ".yml", ".yaml", ".gitignore"]);
const skippedDirectories = new Set([".git", "node_modules", "build", "dist", "coverage"]);
const issues = [];

async function collectFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = [];

  for (const entry of entries) {
    if (skippedDirectories.has(entry.name)) {
      continue;
    }

    const absolutePath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      files.push(...await collectFiles(absolutePath));
    } else if (
      textExtensions.has(path.extname(entry.name)) ||
      entry.name === ".gitignore"
    ) {
      files.push(absolutePath);
    }
  }

  return files;
}

function relative(absolutePath) {
  return path.relative(root, absolutePath);
}

function lineNumberAt(content, offset) {
  return content.slice(0, offset).split("\n").length;
}

async function checkMarkdownLinks(file, content) {
  const markdownLink = /\[[^\]]*\]\(([^)]+)\)/g;

  for (const match of content.matchAll(markdownLink)) {
    const rawTarget = match[1].trim().replace(/^<|>$/g, "");
    if (
      rawTarget.startsWith("http://") ||
      rawTarget.startsWith("https://") ||
      rawTarget.startsWith("mailto:") ||
      rawTarget.startsWith("#") ||
      rawTarget.startsWith("codex://")
    ) {
      continue;
    }

    const targetWithoutAnchor = decodeURIComponent(
      rawTarget.split("#", 1)[0].split("?", 1)[0],
    );
    if (!targetWithoutAnchor) {
      continue;
    }

    const resolved = path.resolve(path.dirname(file), targetWithoutAnchor);
    try {
      await readFile(resolved);
    } catch {
      issues.push(
        `${relative(file)}:${lineNumberAt(content, match.index)} broken link: ${rawTarget}`,
      );
    }
  }
}

const files = (await collectFiles(root)).sort();

for (const file of files) {
  const content = await readFile(file, "utf8");
  const lines = content.split("\n");

  if (content.trim().length === 0) {
    issues.push(`${relative(file)}: file is empty`);
  }

  lines.forEach((line, index) => {
    if (/[ \t]+$/.test(line)) {
      issues.push(`${relative(file)}:${index + 1} trailing whitespace`);
    }
  });

  if (path.extname(file) === ".md") {
    const fences = lines.filter((line) => line.startsWith("```")).length;
    if (fences % 2 !== 0) {
      issues.push(`${relative(file)} unclosed fenced code block`);
    }
    await checkMarkdownLinks(file, content);
  }
}

if (issues.length > 0) {
  console.error(issues.join("\n"));
  console.error(`\nDocumentation check failed with ${issues.length} issue(s).`);
  process.exitCode = 1;
} else {
  console.log(`Documentation check passed for ${files.length} files.`);
}
