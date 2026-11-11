import type { CourseId, UserId } from "../shared/types";

// Matches the backend's Term enum (course/domain/Term.java).
export type Term = "SPRING" | "AUTUMN";

// Matches the backend's CourseResponse (course/api/dto/CourseResponse.java).
export interface Course {
  id: CourseId;
  title: string;
  description: string;
  term: Term;
  year: number;
}

export type CourseRole = "STUDENT" | "TEACHER";

export interface EnrolledCourse extends Course {
  role: CourseRole;
}

export interface CourseEnrollment {
  userId: UserId;
  role: CourseRole;
}

// Matches the backend's CreateCourseRequest (course/api/dto/CreateCourseRequest.java).
export interface CreateCourseInput {
  title: string;
  description: string;
  term: Term;
  year: number;
}

export interface UpdateCourseInput {
  courseId: CourseId;
  changes: Partial<CreateCourseInput>;
}

export interface CourseEnrollmentInput {
  courseId: CourseId;
  userId: UserId;
}

export interface UpdateCourseRoleInput extends CourseEnrollmentInput {
  role: CourseRole;
}
