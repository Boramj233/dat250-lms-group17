import { apiClient, apiPath, jsonBody } from "../../shared/api/apiClient";
import type { CourseId, UserId } from "../../shared/types";
import type { Course, CourseEnrollment, CourseEnrollmentInput, CreateCourseInput, EnrolledCourse, EnrollInCourseInput, UpdateCourseInput, UpdateCourseRoleInput } from "./types";

//collection of all api calls that fetch data related to courses
export function getAllCourses(): Promise<Course[]> {
  return apiClient<Course[]>("/api/courses");
}

export function getMyCourses(userId: UserId): Promise<EnrolledCourse[]> {
  return apiClient<EnrolledCourse[]>(apiPath`/api/users/${userId}/courses`);
}

export function getCourse(courseId: CourseId): Promise<Course> {
  return apiClient<Course>(apiPath`/api/courses/${courseId}`);
}

export function getCourseEnrollments(
  courseId: CourseId
): Promise<CourseEnrollment[]> {
  return apiClient<CourseEnrollment[]>(apiPath`/api/courses/${courseId}/enrollments`);
}

// Proposed API contract is documented in ../README.md until backend is implemented.
export function createCourse(input: CreateCourseInput): Promise<Course> {
  return apiClient<Course>("/api/courses", jsonBody("POST", input));
}

export function updateCourse({
  courseId,
  changes,
}: UpdateCourseInput): Promise<Course> {
  return apiClient<Course>(apiPath`/api/courses/${courseId}`, jsonBody("PATCH", changes));
}

export function deleteCourse(courseId: CourseId): Promise<void> {
  return apiClient<void>(apiPath`/api/courses/${courseId}`, { method: "DELETE" });
}

export function enrollInCourse({
  courseId,
  userId,
  role,
}: EnrollInCourseInput): Promise<void> {
  return apiClient<void>(
    apiPath`/api/courses/${courseId}/enrollments`,
    jsonBody("POST", { userId, role })
  );
}

export function removeCourseEnrollment({
  courseId,
  userId,
}: CourseEnrollmentInput): Promise<void> {
  return apiClient<void>(
    apiPath`/api/courses/${courseId}/enrollments/${userId}`,
    { method: "DELETE" }
  );
}

export function updateCourseRole({
  courseId,
  userId,
  role,
}: UpdateCourseRoleInput): Promise<void> {
  return apiClient<void>(
    apiPath`/api/courses/${courseId}/enrollments/${userId}`,
    jsonBody("PATCH", { role })
  );
}
