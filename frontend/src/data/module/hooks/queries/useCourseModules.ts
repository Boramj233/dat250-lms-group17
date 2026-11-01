// Fetches and caches the modules belonging to a specific course.

import { useQuery } from "@tanstack/react-query";
import type { CourseId } from "../../../shared/types";
import { getCourseModules } from "../../api/moduleQueries";
import { moduleKeys } from "../../queryKeys";

export function useCourseModules(courseId: CourseId) {
  return useQuery({
    queryKey: moduleKeys.courseModules(courseId),
    queryFn: () => getCourseModules(courseId),
  });
}
