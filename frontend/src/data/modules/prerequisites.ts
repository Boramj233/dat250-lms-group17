// Rules for module prerequisites (prerequisiteModuleIds), shared by the UI and the
// mock. The backend must enforce the same rules; see README.md.

import type { ModuleId } from "../../shared/types";

type ModuleLike = { id: ModuleId; prerequisiteModuleIds?: ModuleId[] };

const same = (a: ModuleId, b: ModuleId) => String(a) === String(b);

/** True if `fromId` needs `targetId` to be completed first, directly or through other modules. */
export function dependsOn(modules: ModuleLike[], fromId: ModuleId, targetId: ModuleId): boolean {
  const seen = new Set<string>();
  const stack = [fromId];
  while (stack.length > 0) {
    const current = stack.pop()!;
    if (seen.has(String(current))) continue;
    seen.add(String(current));
    const module = modules.find(m => same(m.id, current));
    for (const prerequisite of module?.prerequisiteModuleIds ?? []) {
      if (same(prerequisite, targetId)) return true;
      stack.push(prerequisite);
    }
  }
  return false;
}

/**
 * Whether `moduleId` may require `candidateId`: not itself, and not a module that
 * already (directly or indirectly) requires `moduleId`, since that would be a cycle
 * and neither module could ever be opened.
 */
export function canRequire(modules: ModuleLike[], moduleId: ModuleId, candidateId: ModuleId): boolean {
  return !same(moduleId, candidateId) && !dependsOn(modules, candidateId, moduleId);
}

/** Whether a module is open: all its prerequisites are completed. */
export function isUnlocked(module: ModuleLike, completedModuleIds: Set<string>): boolean {
  return (module.prerequisiteModuleIds ?? []).every(id => completedModuleIds.has(String(id)));
}
