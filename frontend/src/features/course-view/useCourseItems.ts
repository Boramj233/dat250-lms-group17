import { courseItems } from '@domain/content/courseItems'
import { useCourseContent } from '@domain/content/hooks'
import type { ContentSummary } from '@domain/content/types'
import { isVisibleTo, useCourseView } from './courseContext'

type CourseItemsOptions = {
  /** Only the items this accepts. */
  filter?: (item: ContentSummary) => boolean
  /** False: fetch nothing and give an empty list. Default true. */
  enabled?: boolean
}

/**
 * The content of every module of the course being viewed, in module order, each item
 * with its module. Students do not get inactive items. Uses the same queries (and cache)
 * as the modules' own content lists. Only for pages inside CourseLayout.
 */
export function useCourseItems({ filter, enabled = true }: CourseItemsOptions = {}) {
  const { course, modules, isTeacher } = useCourseView()
  const content = useCourseContent(course.id, enabled ? modules.map(module => module.id) : [])
  const items = courseItems(content.byModule, modules,
    item => isVisibleTo(item, isTeacher) && (filter?.(item) ?? true))
  return { items, isPending: content.isPending, isError: content.isError }
}

/**
 * The item with this id (from the URL) and its module, from useCourseItems.
 * `found` is undefined while the lists load or when the course has no such item.
 */
export function useCourseItem(contentId: string) {
  const { items, isPending, isError } = useCourseItems()
  return { found: items.find(({ item }) => String(item.id) === contentId), isPending, isError }
}
