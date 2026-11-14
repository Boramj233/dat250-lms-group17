import { useQuery } from "@tanstack/react-query";
import type { CourseId } from "../../shared/types";
import { getCourseGroups } from "./api";
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
