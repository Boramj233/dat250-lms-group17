// Which assignments, coding exercises and quizzes in a course the logged-in user has
// completed, as a set of content ids (strings) for quick lookups.

import { useQuery } from "@tanstack/react-query";
import type { CourseId } from "../../../shared/types";
import { getMyCourseCompletions } from "../../api/contentQueries";
import { contentKeys } from "../../queryKeys";

export function useCourseCompletions(courseId: CourseId) {
  return useQuery({
    queryKey: contentKeys.courseCompletions(courseId),
    queryFn: () => getMyCourseCompletions(courseId),
    select: completions => new Set(completions.map(item => String(item.contentId))),
  });
}
