import { formatDateTime } from '@shared/lib/dateFormat'
import type { CourseModule } from './types'

/**
 * When students can open the module, e.g. "From 21 Nov 08:00 · After module 1 and 2 ·
 * For all from 5 Dec 08:00", or "Open from the start". Prerequisites are named by their
 * number in `modules` (the course's modules in order).
 */
export function availabilityText(module: CourseModule, modules: CourseModule[]): string {
  const numbers = modules
    .map((m, index) => ({ id: String(m.id), number: index + 1 }))
    .filter(m => (module.prerequisiteModuleIds ?? []).some(id => String(id) === m.id))
    .map(m => m.number)
  const parts: string[] = []
  if (module.availableFrom) parts.push(`From ${formatDateTime(new Date(module.availableFrom))}`)
  if (numbers.length > 0) {
    const list = numbers.length === 1 ? `${numbers[0]}` : `${numbers.slice(0, -1).join(', ')} and ${numbers.at(-1)}`
    parts.push(`After module ${list}`)
  }
  if (module.openToAllFrom) parts.push(`For all from ${formatDateTime(new Date(module.openToAllFrom))}`)
  return parts.length > 0 ? parts.join(' · ') : 'Open from the start'
}
