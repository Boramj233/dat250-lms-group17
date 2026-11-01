import { apiClient } from "../../shared/apiClient";
import type { CourseId, UserId } from "../../shared/types";
import type { CourseModule, UserModuleProgress } from "../types";

export function getCourseModules(courseId: CourseId): Promise<CourseModule[]> {
  return apiClient<CourseModule[]>(
    `/api/courses/${encodeURIComponent(String(courseId))}/modules`
  );
}

export function getUserModuleProgress(
  courseId: CourseId,
  userId: UserId
): Promise<UserModuleProgress[]> {
  return apiClient<UserModuleProgress[]>(
    `/api/users/${encodeURIComponent(String(userId))}/courses/${encodeURIComponent(String(courseId))}/modules/progress`
  );
}
