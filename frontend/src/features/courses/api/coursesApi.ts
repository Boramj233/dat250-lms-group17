import { apiClient } from "../../../data/api/apiClient";
import type { Course } from "../types/course";

export function getMyCourses(): Promise<Course[]> {
  return apiClient<Course[]>("/api/me/courses");
}