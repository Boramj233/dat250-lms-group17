// Global access levels, in the same order as the role hierarchy in the backend's
// SecurityConfig: ADMIN implies COURSE_MANAGER, which implies STANDARD.
//
// This is only for what the UI shows. The backend checks every API call itself.
// Course roles (TEACHER / STUDENT in one course) are separate: global access, even ADMIN,
// does not give a course role, so check those from the course's enrollment data instead.
import type { CurrentUser } from './auth.ts'

export type Access = CurrentUser['access']

const RANK: Record<Access, number> = {
  STANDARD: 0,
  COURSE_MANAGER: 1,
  ADMIN: 2,
}

/** Every access level, lowest first. */
export const ACCESS_LEVELS: Access[] = ['STANDARD', 'COURSE_MANAGER', 'ADMIN']

/** How an access level is shown to users. */
export const ACCESS_LABEL: Record<Access, string> = {
  STANDARD: 'Standard',
  COURSE_MANAGER: 'Course manager',
  ADMIN: 'Administrator',
}

/** True if `access` is `required` or higher. False when there is no user (undefined). */
export function hasAccess(access: Access | undefined, required: Access): boolean {
  return access !== undefined && RANK[access] >= RANK[required]
}
