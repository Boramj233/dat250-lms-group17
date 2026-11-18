import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { CourseId, GroupId, UserId } from '@shared/types'
import { addGroupMember, createGroup, deleteGroup, getCourseGroups, removeGroupMember } from './api'
import { groupKeys } from './queryKeys'
import type { CourseGroup } from './types'

// Fetches and caches a course's groups, sorted by group number.
export function useCourseGroups(courseId: CourseId) {
  return useQuery({
    queryKey: groupKeys.course(courseId),
    queryFn: () => getCourseGroups(courseId),
    select: groups => [...groups].sort((a, b) => a.groupNumber - b.groupNumber),
  })
}

/** "Group 7". */
export function groupName(group: CourseGroup): string {
  return `Group ${group.groupNumber}`
}

/** The group the user is in; a student is in at most one group per course. */
export function groupOfUser(groups: CourseGroup[], userId: UserId): CourseGroup | undefined {
  return groups.find(group => group.memberIds.some(id => String(id) === String(userId)))
}

/** The lowest group number not taken yet, as a suggestion for a new group. */
export function nextGroupNumber(groups: CourseGroup[]): number {
  const taken = new Set(groups.map(group => group.groupNumber))
  let number = 1
  while (taken.has(number)) number++
  return number
}

// Every change reloads the course's groups (members are shown on several pages).
function useGroupMutation<V>(courseId: CourseId, run: (variables: V) => Promise<unknown>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: run,
    onSettled: () => queryClient.invalidateQueries({ queryKey: groupKeys.course(courseId) }),
  })
}

/** Teachers: creates a group with the given number. */
export function useCreateGroup(courseId: CourseId) {
  return useGroupMutation(courseId, (groupNumber: number) => createGroup(courseId, groupNumber))
}

/** Teachers: deletes a group. */
export function useDeleteGroup(courseId: CourseId) {
  return useGroupMutation(courseId, (groupId: GroupId) => deleteGroup(courseId, groupId))
}

/** Teachers: puts a student of the course in the group (409 when already in a group). */
export function useAddGroupMember(courseId: CourseId) {
  return useGroupMutation(courseId, ({ groupId, userId }: { groupId: GroupId; userId: UserId }) =>
    addGroupMember(courseId, groupId, userId),
  )
}

/** Teachers: takes a member out of the group. */
export function useRemoveGroupMember(courseId: CourseId) {
  return useGroupMutation(courseId, ({ groupId, userId }: { groupId: GroupId; userId: UserId }) =>
    removeGroupMember(courseId, groupId, userId),
  )
}
