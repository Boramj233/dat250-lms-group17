import { apiClient } from "../../shared/api/apiClient";
import type { CourseId, UserId } from "../../shared/types";
import type { Course, CourseEnrollment, CourseEnrollmentInput, CreateCourseInput, EnrolledCourse, EnrollInCourseInput, UpdateCourseInput, UpdateCourseRoleInput } from "./types";

//collection of all api calls that fetch data related to courses
export function getAllCourses(): Promise<Course[]> {
  return apiClient<Course[]>("/api/courses");
}

export function getMyCourses(userId: UserId): Promise<EnrolledCourse[]> {
  return apiClient<EnrolledCourse[]>(
    `/api/users/${encodeURIComponent(String(userId))}/courses`
  );
}

export function getCourse(courseId: CourseId): Promise<Course> {
  return apiClient<Course>(
    `/api/courses/${encodeURIComponent(String(courseId))}`
  );
}

export function getCourseEnrollments(
  courseId: CourseId
): Promise<CourseEnrollment[]> {
  return apiClient<CourseEnrollment[]>(
    `/api/courses/${encodeURIComponent(String(courseId))}/enrollments`
  );
}

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
  role,
}: EnrollInCourseInput): Promise<void> {
  return apiClient<void>(
    `/api/courses/${encodeURIComponent(String(courseId))}/enrollments`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ userId, role }),
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
