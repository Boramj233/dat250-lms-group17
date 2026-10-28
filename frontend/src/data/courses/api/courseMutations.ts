import { apiClient } from "../../shared/apiClient";
import type {
  Course,
  CourseEnrollmentInput,
  CourseId,
  CreateCourseInput,
  UpdateCourseInput,
  UpdateCourseRoleInput,
} from "../types";

// Proposed API contract is documented in ../README.md until backend is implemented.
export function createCourse(input: CreateCourseInput): Promise<Course> {
  return apiClient<Course>("/api/courses", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
}

export function updateCourse({
  courseId,
  changes,
}: UpdateCourseInput): Promise<Course> {
  return apiClient<Course>(
    `/api/courses/${encodeURIComponent(String(courseId))}`,
    {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(changes),
    }
  );
}

export function deleteCourse(courseId: CourseId): Promise<void> {
  return apiClient<void>(
    `/api/courses/${encodeURIComponent(String(courseId))}`,
    { method: "DELETE" }
  );
}

export function enrollInCourse({
  courseId,
  userId,
}: CourseEnrollmentInput): Promise<void> {
  return apiClient<void>(
    `/api/courses/${encodeURIComponent(String(courseId))}/enrollments`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ userId }),
    }
  );
}

export function removeCourseEnrollment({
  courseId,
  userId,
}: CourseEnrollmentInput): Promise<void> {
  return apiClient<void>(
    `/api/courses/${encodeURIComponent(String(courseId))}/enrollments/${encodeURIComponent(String(userId))}`,
    { method: "DELETE" }
  );
}

export function updateCourseRole({
  courseId,
  userId,
  role,
}: UpdateCourseRoleInput): Promise<void> {
  return apiClient<void>(
    `/api/courses/${encodeURIComponent(String(courseId))}/enrollments/${encodeURIComponent(String(userId))}`,
    {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ role }),
    }
  );
}
