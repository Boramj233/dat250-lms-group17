// Rules for module prerequisites (prerequisiteModuleIds), shared by the UI and the
// mock. The backend must enforce the same rules; see README.md.

import type { ModuleId } from '@shared/types'

type ModuleLike = {
  id: ModuleId
  prerequisiteModuleIds?: ModuleId[]
  availableFrom?: string | null
  openToAllFrom?: string | null
}

const same = (a: ModuleId, b: ModuleId) => String(a) === String(b)

/** True if `fromId` needs `targetId` to be completed first, directly or through other modules. */
export function dependsOn(modules: ModuleLike[], fromId: ModuleId, targetId: ModuleId): boolean {
  const seen = new Set<string>()
  const stack = [fromId]
  while (stack.length > 0) {
    const current = stack.pop()!
    if (seen.has(String(current))) continue
    seen.add(String(current))
    const module = modules.find(m => same(m.id, current))
    for (const prerequisite of module?.prerequisiteModuleIds ?? []) {
      if (same(prerequisite, targetId)) return true
      stack.push(prerequisite)
    }
  }
  return false
}

/**
 * Whether `moduleId` may require `candidateId`: not itself, and not a module that
 * already (directly or indirectly) requires `moduleId`, since that would be a cycle
 * and neither module could ever be opened.
 */
export function canRequire(modules: ModuleLike[], moduleId: ModuleId, candidateId: ModuleId): boolean {
  return !same(moduleId, candidateId) && !dependsOn(modules, candidateId, moduleId)
}

/** Whether all a module's prerequisites are completed. */
export function isUnlocked(module: ModuleLike, completedModuleIds: Set<string>): boolean {
  return (module.prerequisiteModuleIds ?? []).every(id => completedModuleIds.has(String(id)))
}

/**
 * Whether a student can open a module now: its start date (availableFrom) has come,
 * and either its prerequisites are completed or it is open to all (openToAllFrom).
 */
export function isAccessible(module: ModuleLike, completedModuleIds: Set<string>, now: Date): boolean {
  const reached = (date: string | null | undefined) => !!date && Date.parse(date) <= now.getTime()
  if (module.availableFrom && !reached(module.availableFrom)) return false
  return isUnlocked(module, completedModuleIds) || reached(module.openToAllFrom)
}

/** A problem with the two dates, or undefined if they fit together. */
export function checkAvailabilityDates(availableFrom: string | null, openToAllFrom: string | null): string | undefined {
  if (availableFrom && openToAllFrom && Date.parse(openToAllFrom) < Date.parse(availableFrom)) {
    return '“Available for all” cannot be before “Available from”.'
  }
  return undefined
}
