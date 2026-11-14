import { apiClient } from "../apiClient";
import type { GradeEntry } from "./types";

// Grades of the logged-in user across all courses (proposed endpoint, see README).
export function getMyGrades(): Promise<GradeEntry[]> {
  return apiClient<GradeEntry[]>("/api/users/me/grades");
}
