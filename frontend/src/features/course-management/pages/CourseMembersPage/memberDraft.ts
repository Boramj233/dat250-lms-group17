import type { CourseEnrollment, CourseRole } from '@domain/courses/types'

/** Unsaved role changes and removals, by user id (as a string). */
export type MemberDraft = { roles: Record<string, CourseRole>; removals: string[] }

export const EMPTY_DRAFT: MemberDraft = { roles: {}, removals: [] }

const idOf = (member: CourseEnrollment) => String(member.userId)

/** How many changes the draft holds. */
export function memberChanges(draft: MemberDraft): number {
  return Object.keys(draft.roles).length + draft.removals.length
}

/** The member's role after the draft is saved. */
export function roleAfter(draft: MemberDraft, member: CourseEnrollment): CourseRole {
  return draft.roles[idOf(member)] ?? member.role
}

export function isRemoved(draft: MemberDraft, member: CourseEnrollment): boolean {
  return draft.removals.includes(idOf(member))
}

export function isRoleChanged(draft: MemberDraft, member: CourseEnrollment): boolean {
  return idOf(member) in draft.roles
}

/**
 * Whether the member is the only teacher left after the draft. A course must keep at least
 * one teacher, so it can still be managed: that member cannot be removed or made a student.
 */
export function isLastTeacher(draft: MemberDraft, member: CourseEnrollment, members: CourseEnrollment[]): boolean {
  const teachersAfter = members.filter(m => !isRemoved(draft, m) && roleAfter(draft, m) === 'TEACHER').length
  return !isRemoved(draft, member) && roleAfter(draft, member) === 'TEACHER' && teachersAfter === 1
}

/** Changes the role; changing it back to the saved role is no longer a change. */
export function withRole(draft: MemberDraft, member: CourseEnrollment, role: CourseRole): MemberDraft {
  const roles = { ...draft.roles }
  if (role === member.role) delete roles[idOf(member)]
  else roles[idOf(member)] = role
  return { ...draft, roles }
}

/** Marks the member for removal, or undoes that. */
export function withRemoved(draft: MemberDraft, member: CourseEnrollment, removed: boolean): MemberDraft {
  const id = idOf(member)
  return { ...draft, removals: removed ? [...draft.removals, id] : draft.removals.filter(other => other !== id) }
}

/** The draft without a role change that has been saved, so a failed save can be tried again. */
export function withoutSavedRole(draft: MemberDraft, id: string): MemberDraft {
  return { ...draft, roles: Object.fromEntries(Object.entries(draft.roles).filter(([key]) => key !== id)) }
}

/** The draft without a removal that has been saved. */
export function withoutSavedRemoval(draft: MemberDraft, id: string): MemberDraft {
  return { ...draft, removals: draft.removals.filter(other => other !== id) }
}
