export type CourseId = string | number;
export type UserId = string | number;

export interface Course {
  id: CourseId;
  code: string;
  name: string;
  description?: string;
}

export type CourseRole = "STUDENT" | "TEACHER";

export interface EnrolledCourse extends Course {
  role: CourseRole;
}

export interface CourseEnrollment {
  userId: UserId;
  role: CourseRole;
}

export interface CreateCourseInput {
  code: string;
  name: string;
  description?: string;
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
