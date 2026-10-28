// Fetches and caches a specific course by its ID.

import { useQuery } from "@tanstack/react-query";
import { getCourse } from "../../api/courseQueries";
import { courseKeys } from "../../queryKeys";
import type { CourseId } from "../../../shared/types";

export function useCourse(courseId: CourseId) {
  return useQuery({
    queryKey: courseKeys.course(courseId),
    queryFn: () => getCourse(courseId),
  });
}
