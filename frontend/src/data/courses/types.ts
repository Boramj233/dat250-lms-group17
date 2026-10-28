export interface Course {
  code: string;
  name: string;
}

export type CourseRole = "STUDENT" | "TEACHER";

export interface EnrolledCourse extends Course {
  role: CourseRole;
}
