import type { ModuleId } from '@shared/types'
import type { ContentSummary } from './types'

/** One content item of a course, with the module it is in. */
export type CourseItem<M> = { item: ContentSummary; module: M }

/**
 * The content of a course's modules as one list, in module order (and by position within
 * a module), each item with its module. `byModule` is what useCourseContent gives;
 * modules that are still loading are left out.
 */
export function courseItems<M extends { id: ModuleId }>(
  byModule: ReadonlyMap<string, ContentSummary[]>,
  modules: M[],
  include: (item: ContentSummary) => boolean = () => true,
): CourseItem<M>[] {
  return modules.flatMap(module =>
    (byModule.get(String(module.id)) ?? []).filter(include).map(item => ({ item, module })),
  )
}
