import type { CourseId, GroupId, UserId } from '@shared/types'
import { useCourseEnrollments } from '@domain/courses/hooks'
import { groupName, useCourseGroups } from '@domain/groups/hooks'
import type { CourseGroup } from '@domain/groups/types'
import { fullName } from '@shared/lib/fullName'

/**
 * Names for the ids the backend sends with submissions (studentUserId, groupId,
 * members[].userId). They come from the course's members (People) and groups; when
 * neither knows a user (e.g. a student who may not list the members), "User #id".
 */
export function usePeopleNames(courseId: CourseId) {
  const members = useCourseEnrollments(courseId)
  const groups = useCourseGroups(courseId)

  function nameOf(userId: UserId): string {
    const member =
      members.data?.find(m => String(m.userId) === String(userId)) ??
      groups.data?.flatMap(g => g.members ?? []).find(m => String(m.userId) === String(userId))
    return member ? fullName(member) : `User #${userId}`
  }

  function groupOf(groupId: GroupId | null | undefined): CourseGroup | undefined {
    if (groupId === null || groupId === undefined) return undefined
    return groups.data?.find(g => String(g.id) === String(groupId))
  }

  function groupLabel(groupId: GroupId): string {
    const group = groupOf(groupId)
    return group ? groupName(group) : `Group #${groupId}`
  }

  return { members: members.data, groups: groups.data, nameOf, groupOf, groupLabel }
}
