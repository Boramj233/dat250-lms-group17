import { apiClient } from "../api/apiClient";
import type { Course } from "./course";

export function getMyCourses(): Promise<Course[]> {
  return apiClient<Course[]>("/api/me/courses");
}