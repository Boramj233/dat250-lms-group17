import type { ContentType } from './types'

export type ContentTypeInfo = {
  type: ContentType
  /** In URLs, e.g. /new/coding-exercise. */
  slug: string
  label: string
  /** Shown on the "+ …" button for the type. */
  description: string
}

/** The content types a teacher can create, in the order they are offered. */
export const CONTENT_TYPES: ContentTypeInfo[] = [
  { type: 'ASSIGNMENT', slug: 'assignment', label: 'Assignment', description: 'Students hand in files, alone or as a group, with a deadline.' },
  { type: 'QUIZ', slug: 'quiz', label: 'Quiz', description: 'Questions with answers to choose from.' },
  { type: 'CODING_EXERCISE', slug: 'coding-exercise', label: 'Coding exercise', description: 'Students start from a repository and hand in their code.' },
  { type: 'STATIC_PAGE', slug: 'page', label: 'Page', description: 'Text to read, e.g. notes or a reading list.' },
]

export function contentTypeInfo(type: ContentType): ContentTypeInfo {
  return CONTENT_TYPES.find(info => info.type === type)!
}

export function contentTypeBySlug(slug: string | undefined): ContentTypeInfo | undefined {
  return CONTENT_TYPES.find(info => info.slug === slug)
}

/** "Page", "Assignment", "Coding exercise" or "Quiz". */
export function contentTypeLabel(type: ContentType): string {
  return contentTypeInfo(type)?.label ?? type
}
