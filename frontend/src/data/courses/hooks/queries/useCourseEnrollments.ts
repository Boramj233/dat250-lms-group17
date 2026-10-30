// Fetches and caches enrolled users and their roles for a specific course.

import { useQuery } from "@tanstack/react-query";
import { getCourseEnrollments } from "../../api/courseQueries";
import { courseKeys } from "../../queryKeys";
import type { CourseId } from "../../../shared/types";

export function useCourseEnrollments(courseId: CourseId) {
  return useQuery({
    queryKey: courseKeys.enrollments(courseId),
    queryFn: () => getCourseEnrollments(courseId),
  });
}
