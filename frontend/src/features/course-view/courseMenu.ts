import { paths, type CourseSection } from '../../shared/paths'
import type { CourseId } from '../../shared/types'

/** The course menu, in this order; Home has no section. */
export const COURSE_MENU_ITEMS: { label: string, section?: CourseSection, teacherOnly?: boolean }[] = [
  { label: 'Home' },
  { label: 'Modules', section: 'modules' },
  { label: 'Assignments', section: 'assignments' },
  { label: 'People', section: 'people' },
  { label: 'Groups', section: 'groups' },
  { label: 'Quizzes', section: 'quizzes' },
  { label: 'Grades', section: 'grades' },
  { label: 'Submissions', section: 'submissions', teacherOnly: true },
  { label: 'Files', section: 'files' },
  { label: 'Code Exercises', section: 'code-exercises' },
]

/** The menu items the user sees: Submissions only for the course's teachers. */
export function courseMenuItems(isTeacher: boolean) {
  return COURSE_MENU_ITEMS.filter(item => isTeacher || !item.teacherOnly)
}

/** Where a course menu item leads. */
export function courseMenuPath(courseId: CourseId, section: CourseSection | undefined): string {
  return section ? paths.courseSection(courseId, section) : paths.course(courseId)
}
