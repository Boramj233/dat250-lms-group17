import { toDateTimeLocal } from '../../../../../shared/lib/dateFormat'
import type { CourseModule } from '../../../../modules/types'

/** The availability fields as edited: datetime-local values and module ids (as strings). */
export type AvailabilityDraft = {
  availableFrom: string
  openToAllFrom: string
  prerequisites: string[]
}

/** The module's saved availability, as the fields start. */
export function savedAvailability(module: CourseModule): AvailabilityDraft {
  return {
    availableFrom: toDateTimeLocal(module.availableFrom),
    openToAllFrom: toDateTimeLocal(module.openToAllFrom),
    prerequisites: (module.prerequisiteModuleIds ?? []).map(String),
  }
}

/** How many of the fields differ from what is saved (each date, and the prerequisites). */
export function availabilityChanges(draft: AvailabilityDraft, saved: AvailabilityDraft): number {
  const prerequisitesChanged = draft.prerequisites.length !== saved.prerequisites.length
    || draft.prerequisites.some(id => !saved.prerequisites.includes(id))
  return Number(draft.availableFrom !== saved.availableFrom)
    + Number(draft.openToAllFrom !== saved.openToAllFrom)
    + Number(prerequisitesChanged)
}
