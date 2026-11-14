import { apiClient, apiPath } from "../../shared/api/apiClient";
import type { CourseId } from "../../shared/types";
import type { CourseGroup } from "./types";

/** All groups in a course (backend). */
export function getCourseGroups(courseId: CourseId): Promise<CourseGroup[]> {
  return apiClient<CourseGroup[]>(apiPath`/api/courses/${courseId}/groups`);
}
