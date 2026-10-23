import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import {
  parseSkillYaml,
  type SkillManifest
} from "@personal-jarvis/skills";
import type { SkillPromptEntry } from "../model/intent.js";

type IntentArgument = string | number | boolean;

export class SkillValidationError extends Error {}

export class SkillCatalog {
  private readonly manifests: Map<string, SkillManifest>;

  constructor(manifests: SkillManifest[]) {
    this.manifests = new Map(
      manifests.map((manifest) => [manifest.id, manifest])
    );
  }

  static fromDirectory(root: string): SkillCatalog {
    return new SkillCatalog(
      listManifestFiles(root).map((path) =>
        parseSkillYaml(readFileSync(path, "utf8"))
      )
    );
  }

  get(skillId: string): SkillManifest | undefined {
    return this.manifests.get(skillId);
  }

  promptEntries(): SkillPromptEntry[] {
    return [...this.manifests.values()].map((manifest) => ({
      id: manifest.id,
      displayName: manifest.displayName ?? manifest.id,
      appPackages: manifest.appPackages,
      inputs: Object.fromEntries(
        Object.entries(manifest.inputs).map(([key, input]) => [
          key,
          {
            type: input.type,
            required: input.required,
            ...(input.description
              ? { description: input.description }
              : {})
          }
        ])
      )
    }));
  }

  validateArguments(
    skillId: string,
    input: Record<string, IntentArgument>
  ): Record<string, IntentArgument> {
    const manifest = this.manifests.get(skillId);
    if (!manifest) {
      throw new SkillValidationError("unknown skill");
    }
    for (const key of Object.keys(input)) {
      if (!(key in manifest.inputs)) {
        throw new SkillValidationError(`unknown argument: ${key}`);
      }
    }
    for (const [key, definition] of Object.entries(manifest.inputs)) {
      const value = input[key];
      if (value === undefined) {
        if (definition.required) {
          throw new SkillValidationError(`missing argument: ${key}`);
        }
        continue;
      }
      if (definition.type === "text" && typeof value !== "string") {
        throw new SkillValidationError(`invalid text argument: ${key}`);
      }
      if (definition.type === "number" && typeof value !== "number") {
        throw new SkillValidationError(`invalid number argument: ${key}`);
      }
      if (definition.type === "boolean" && typeof value !== "boolean") {
        throw new SkillValidationError(`invalid boolean argument: ${key}`);
      }
    }
    return input;
  }
}

export function loadDefaultSkillCatalog(): SkillCatalog {
  return SkillCatalog.fromDirectory(
    fileURLToPath(new URL("../../../../skills", import.meta.url))
  );
}

function listManifestFiles(root: string): string[] {
  const files: string[] = [];
  for (const entry of readdirSync(root, { withFileTypes: true })) {
    const path = join(root, entry.name);
    if (entry.isDirectory()) {
      files.push(...listManifestFiles(path));
    } else if (entry.name === "manifest.yaml") {
      files.push(path);
    }
  }
  return files.sort();
}
