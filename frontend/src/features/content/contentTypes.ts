import type { ContentSection } from '../../shared/paths'
import type { ContentType } from './types'

export type ContentTypeInfo = {
  type: ContentType
  /** In URLs, e.g. /new/coding-exercise. */
  slug: string
  /** In the course's and module's URLs, e.g. /modules/2/code-exercises/102. */
  section: ContentSection
  label: string
  /** Shown on the "+ …" button for the type. */
  description: string
}

/** The content types a teacher can create, in the order they are offered. */
export const CONTENT_TYPES: ContentTypeInfo[] = [
  { type: 'ASSIGNMENT', slug: 'assignment', section: 'assignments', label: 'Assignment', description: 'Students hand in files, alone or as a group, with a deadline.' },
  { type: 'QUIZ', slug: 'quiz', section: 'quizzes', label: 'Quiz', description: 'Questions with answers to choose from.' },
  { type: 'CODING_EXERCISE', slug: 'coding-exercise', section: 'code-exercises', label: 'Coding exercise', description: 'Students start from a repository and hand in their code.' },
  { type: 'STATIC_PAGE', slug: 'page', section: 'pages', label: 'Page', description: 'Text to read, e.g. notes or a reading list.' },
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

/** The type's part of a content URL: "assignments", "quizzes", "code-exercises" or "pages". */
export function contentSection(type: ContentType): ContentSection {
  return contentTypeInfo(type).section
}
