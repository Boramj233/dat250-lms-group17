import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { CourseId, GroupId, UserId } from "@shared/types";
import { addGroupMember, createGroup, deleteGroup, getCourseGroups, removeGroupMember } from "./api";
import { groupKeys } from "./queryKeys";
import type { CourseGroup } from "./types";

// Fetches and caches a course's groups, sorted by group number.
export function useCourseGroups(courseId: CourseId) {
  return useQuery({
    queryKey: groupKeys.course(courseId),
    queryFn: () => getCourseGroups(courseId),
    select: groups => [...groups].sort((a, b) => a.groupNumber - b.groupNumber),
  });
}

/** "Rabbit Runners", or "Group 7" while the group has no name. */
export function groupName(group: CourseGroup): string {
  return group.name || `Group ${group.groupNumber}`;
}

/** The group the user is in; a student is in at most one group per course. */
export function groupOfUser(groups: CourseGroup[], userId: UserId): CourseGroup | undefined {
  return groups.find(group => group.memberIds.some(id => String(id) === String(userId)));
}

/** The lowest group number not taken yet, as a suggestion for a new group. */
export function nextGroupNumber(groups: CourseGroup[]): number {
  const taken = new Set(groups.map(group => group.groupNumber));
  let number = 1;
  while (taken.has(number)) number++;
  return number;
}

// Every change reloads the course's groups (members are shown on several pages).
function useGroupMutation<V>(courseId: CourseId, run: (variables: V) => Promise<unknown>) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: run,
    onSettled: () => queryClient.invalidateQueries({ queryKey: groupKeys.course(courseId) }),
  });
}

/** Teachers: creates a group with the given number. */
export function useCreateGroup(courseId: CourseId) {
  return useGroupMutation(courseId, (groupNumber: number) => createGroup(courseId, groupNumber));
}

/** Teachers: deletes a group. */
export function useDeleteGroup(courseId: CourseId) {
  return useGroupMutation(courseId, (groupId: GroupId) => deleteGroup(courseId, groupId));
}

/** Students: join a group (as themselves). */
export function useJoinGroup(courseId: CourseId) {
  return useGroupMutation(courseId, ({ groupId, userId }: { groupId: GroupId, userId: UserId }) =>
    addGroupMember(courseId, groupId, userId));
}

/**
 * Thrown by useSwitchGroup when the student left their old group but could not join the
 * new one. `backInOldGroup`: they were put back in the old group.
 */
export class SwitchGroupError extends Error {
  readonly backInOldGroup: boolean;

  constructor(cause: unknown, backInOldGroup: boolean) {
    super("Could not join the new group", { cause });
    this.backInOldGroup = backInOldGroup;
  }
}

/**
 * Students: move from one group to another (one group at a time). The backend has no
 * single call for this, so it leaves `fromGroupId` and then joins `toGroupId`; if joining
 * fails, it tries to rejoin the old group and throws SwitchGroupError. If leaving fails,
 * that error is thrown as it is and nothing has changed.
 */
export function useSwitchGroup(courseId: CourseId) {
  return useGroupMutation(courseId, async (
    { fromGroupId, toGroupId, userId }: { fromGroupId: GroupId, toGroupId: GroupId, userId: UserId },
  ) => {
    await removeGroupMember(courseId, fromGroupId, userId);
    try {
      return await addGroupMember(courseId, toGroupId, userId);
    } catch (error) {
      const backInOldGroup = await addGroupMember(courseId, fromGroupId, userId).then(() => true, () => false);
      throw new SwitchGroupError(error, backInOldGroup);
    }
  });
}

/** Students: leave a group (as themselves). */
export function useLeaveGroup(courseId: CourseId) {
  return useGroupMutation(courseId, ({ groupId, userId }: { groupId: GroupId, userId: UserId }) =>
    removeGroupMember(courseId, groupId, userId));
}
