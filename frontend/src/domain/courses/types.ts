import type { CourseId, UserId } from '@shared/types'

// Matches the backend's Term enum (course/domain/Term.java).
export type Term = 'SPRING' | 'AUTUMN'

// Matches the backend's CourseResponse (course/api/dto/CourseResponse.java).
export interface Course {
  id: CourseId
  title: string
  description: string
  term: Term
  year: number
}

// Matches the backend's CourseRole enum (enrollment/domain/CourseRole.java).
export type CourseRole = 'STUDENT' | 'TEACHER'

// Matches the backend's EnrolledCourseResponse (GET /api/users/{userId}/courses): the
// course plus the user's role in it. Active enrollments only, newest term first.
export interface EnrolledCourse extends Course {
  role: CourseRole
}

// Matches the backend's EnrollmentResponse (GET/POST/PATCH /api/courses/{courseId}/enrollments).
// The names and email let teachers see who is enrolled without a request per user.
export interface CourseEnrollment {
  userId: UserId
  role: CourseRole
  firstName: string
  lastName: string
  email: string
}

// Matches the backend's CreateCourseRequest (course/api/dto/CreateCourseRequest.java).
export interface CreateCourseInput {
  title: string
  description: string
  term: Term
  year: number
}

export interface UpdateCourseInput {
  courseId: CourseId
  changes: Partial<CreateCourseInput>
}

export interface CourseEnrollmentInput {
  courseId: CourseId
  userId: UserId
}

export interface EnrollInCourseInput extends CourseEnrollmentInput {
  /** Defaults to STUDENT on the backend. */
  role?: CourseRole
}

export interface UpdateCourseRoleInput extends CourseEnrollmentInput {
  role: CourseRole
}
