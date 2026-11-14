import { apiClient } from "../../shared/api/apiClient";
import type { CourseId, UserId } from "../../shared/types";
import type { CourseModule, CreateModuleInput, DeleteModuleInput, UpdateModuleInput, UserModuleProgress } from "./types";

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

export function createModule({
  courseId,
  ...input
}: CreateModuleInput): Promise<CourseModule> {
  return apiClient<CourseModule>(
    `/api/courses/${encodeURIComponent(String(courseId))}/modules`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    }
  );
}

export function updateModule({
  courseId,
  moduleId,
  changes,
}: UpdateModuleInput): Promise<CourseModule> {
  return apiClient<CourseModule>(
    `/api/courses/${encodeURIComponent(String(courseId))}/modules/${encodeURIComponent(String(moduleId))}`,
    {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(changes),
    }
  );
}

export function deleteModule({
  courseId,
  moduleId,
}: DeleteModuleInput): Promise<void> {
  return apiClient<void>(
    `/api/courses/${encodeURIComponent(String(courseId))}/modules/${encodeURIComponent(String(moduleId))}`,
    { method: "DELETE" }
  );
}
