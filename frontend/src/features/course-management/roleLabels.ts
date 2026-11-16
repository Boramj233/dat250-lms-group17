import type { CourseRole } from '@domain/courses/types'

/** "Student" / "Teacher" */
export const ROLE_LABEL: Record<CourseRole, string> = {
  STUDENT: 'Student',
  TEACHER: 'Teacher',
}
