//collection of all api calls that fetch data related to courses

import { apiClient } from "../../shared/apiClient";
import type {
  Course,
  CourseEnrollment,
  CourseId,
  EnrolledCourse,
  UserId,
} from "../types";

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
