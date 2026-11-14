import { paths, type CourseSection } from '../../shared/paths'
import type { CourseId } from '../../shared/types'

/** The course menu, in this order; Home has no section. */
export const COURSE_MENU_ITEMS: { label: string, section?: CourseSection }[] = [
  { label: 'Home' },
  { label: 'Modules', section: 'modules' },
  { label: 'Assignments', section: 'assignments' },
  { label: 'People', section: 'people' },
  { label: 'Groups', section: 'groups' },
  { label: 'Quizzes', section: 'quizzes' },
  { label: 'Grades', section: 'grades' },
  { label: 'Files', section: 'files' },
  { label: 'Code Exercises', section: 'code-exercises' },
]

/** Where a course menu item leads. */
export function courseMenuPath(courseId: CourseId, section: CourseSection | undefined): string {
  return section ? paths.courseSection(courseId, section) : paths.course(courseId)
}
