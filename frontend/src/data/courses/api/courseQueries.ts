//collection of all api calls that fetch data related to courses

import { apiClient } from "../../shared/apiClient";
import type { Course, EnrolledCourse } from "../types";

export function getAllCourses(): Promise<Course[]> {
  return apiClient<Course[]>("/api/courses");
}

export function getMyCourses(userId: string | number): Promise<EnrolledCourse[]> {
  return apiClient<EnrolledCourse[]>(
    `/api/users/${encodeURIComponent(String(userId))}/courses`
  );
}
