import type { ContentSection } from '../../shared/paths'
import type { ContentType } from '../content/types'

/** One menu item and page per content type, in menu order: /…/assignments. */
export const CONTENT_SECTIONS: {
  type: ContentType
  section: ContentSection
  label: string
}[] = [
  { type: 'STATIC_PAGE', section: 'pages', label: 'Pages' },
  { type: 'ASSIGNMENT', section: 'assignments', label: 'Assignments' },
  { type: 'QUIZ', section: 'quizzes', label: 'Quizzes' },
  { type: 'CODING_EXERCISE', section: 'code-exercises', label: 'Code Exercises' },
]
