import { apiClient } from "../../shared/apiClient";
import type {
  CourseModule,
  CreateModuleInput,
  DeleteModuleInput,
  UpdateModuleInput,
} from "../types";

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
