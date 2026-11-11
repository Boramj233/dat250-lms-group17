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

/** True if `access` is `required` or higher. False when there is no user (undefined). */
export function hasAccess(access: Access | undefined, required: Access): boolean {
  return access !== undefined && RANK[access] >= RANK[required]
}
