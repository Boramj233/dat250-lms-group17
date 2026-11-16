import { apiClient, apiPath, jsonBody } from "../../shared/api/apiClient";
import type { CourseId } from "../../shared/types";
import type { CourseModule, CreateModuleInput, ReorderModulesInput, UpdateModuleInput } from "./types";

export function getCourseModules(courseId: CourseId): Promise<CourseModule[]> {
  return apiClient<CourseModule[]>(apiPath`/api/courses/${courseId}/modules`);
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

/**
 * Proposed: saves the order (rank) of all the course's modules at once, so it is never
 * half saved. Answers with the modules in their new order.
 */
export function reorderModules({ courseId, moduleIds }: ReorderModulesInput): Promise<CourseModule[]> {
  return apiClient<CourseModule[]>(
    apiPath`/api/courses/${courseId}/modules/order`,
    jsonBody("PUT", { moduleIds })
  );
}
