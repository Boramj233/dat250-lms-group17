import type { ContentTypeSlug } from '../../app/paths'
import type { ContentType } from '../../data/content/types'

export type ContentTypeInfo = {
  type: ContentType
  slug: ContentTypeSlug
  label: string
  plural: string
}

/** The content types in the order they are shown in menus. */
export const CONTENT_TYPES: ContentTypeInfo[] = [
  { type: 'STATIC_PAGE', slug: 'pages', label: 'Page', plural: 'Pages' },
  { type: 'QUIZ', slug: 'quizzes', label: 'Quiz', plural: 'Quizzes' },
  { type: 'ASSIGNMENT', slug: 'assignments', label: 'Assignment', plural: 'Assignments' },
  { type: 'CODING_EXERCISE', slug: 'coding-exercises', label: 'Coding exercise', plural: 'Coding exercises' },
]

export function contentTypeInfo(type: ContentType): ContentTypeInfo {
  return CONTENT_TYPES.find(info => info.type === type)!
}

export function contentTypeBySlug(slug: string | undefined): ContentTypeInfo | undefined {
  return CONTENT_TYPES.find(info => info.slug === slug)
}

/** "3 pages · 1 quiz · 2 assignments" (only the types that occur, in menu order). */
export function summarizeTypes(items: { type: ContentType }[]): string {
  return CONTENT_TYPES
    .map(info => ({ info, count: items.filter(item => item.type === info.type).length }))
    .filter(({ count }) => count > 0)
    .map(({ info, count }) => `${count} ${(count === 1 ? info.label : info.plural).toLowerCase()}`)
    .join(' · ')
}

/** Types the user completes by submitting (assignments, coding exercises) or taking (quizzes). */
export function isCompletable(type: ContentType): boolean {
  return type !== 'STATIC_PAGE'
}

/** Splits items into not yet completed and completed (by content id). */
export function splitByCompletion<T extends { id: string | number }>(
  items: T[],
  completed: Set<string>,
): { todo: T[], done: T[] } {
  return {
    todo: items.filter(item => !completed.has(String(item.id))),
    done: items.filter(item => completed.has(String(item.id))),
  }
}
