import type { CourseEnrollment, CourseRole } from './types'

/** "Student" / "Teacher" */
export const ROLE_LABEL: Record<CourseRole, string> = {
  STUDENT: 'Student',
  TEACHER: 'Teacher',
}

const ROLE_ORDER: Record<CourseRole, number> = { TEACHER: 0, STUDENT: 1 }
/** For sorting a course's members: teachers first, then by last name and first name. */
export function byRoleThenName(a: CourseEnrollment, b: CourseEnrollment): number {
  return (
    ROLE_ORDER[a.role] - ROLE_ORDER[b.role] ||
    a.lastName.localeCompare(b.lastName) ||
    a.firstName.localeCompare(b.firstName)
  )
}
