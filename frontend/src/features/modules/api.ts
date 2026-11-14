import { apiClient, apiPath, jsonBody } from "../../shared/api/apiClient";
import type { CourseId, UserId } from "../../shared/types";
import type { CourseModule, CreateModuleInput, DeleteModuleInput, UpdateModuleInput, UserModuleProgress } from "./types";

export function getCourseModules(courseId: CourseId): Promise<CourseModule[]> {
  return apiClient<CourseModule[]>(apiPath`/api/courses/${courseId}/modules`);
}

export function getUserModuleProgress(
  courseId: CourseId,
  userId: UserId
): Promise<UserModuleProgress[]> {
  return apiClient<UserModuleProgress[]>(
    apiPath`/api/users/${userId}/courses/${courseId}/modules/progress`
  );
}

export function createModule({
  courseId,
  ...input
}: CreateModuleInput): Promise<CourseModule> {
  return apiClient<CourseModule>(apiPath`/api/courses/${courseId}/modules`, jsonBody("POST", input));
}

export function updateModule({
  courseId,
  moduleId,
  changes,
}: UpdateModuleInput): Promise<CourseModule> {
  return apiClient<CourseModule>(
    apiPath`/api/courses/${courseId}/modules/${moduleId}`,
    jsonBody("PATCH", changes)
  );
}

export function deleteModule({
  courseId,
  moduleId,
}: DeleteModuleInput): Promise<void> {
  return apiClient<void>(
    apiPath`/api/courses/${courseId}/modules/${moduleId}`,
    { method: "DELETE" }
  );
}
