import { apiClient, apiPath, jsonBody } from '@shared/api/apiClient'
import type { CourseId, UserId } from '@shared/types'
import type {
  Course,
  CourseEnrollment,
  CourseEnrollmentInput,
  CreateCourseInput,
  EnrolledCourse,
  EnrollInCourseInput,
  UpdateCourseInput,
  UpdateCourseRoleInput,
} from './types'

export function getMyCourses(userId: UserId): Promise<EnrolledCourse[]> {
  return apiClient<EnrolledCourse[]>(apiPath`/api/users/${userId}/courses`)
}

export function getCourse(courseId: CourseId): Promise<Course> {
  return apiClient<Course>(apiPath`/api/courses/${courseId}`)
}

export function getCourseEnrollments(courseId: CourseId): Promise<CourseEnrollment[]> {
  return apiClient<CourseEnrollment[]>(apiPath`/api/courses/${courseId}/enrollments`)
}

export function createCourse(input: CreateCourseInput): Promise<Course> {
  return apiClient<Course>('/api/courses', jsonBody('POST', input))
}

export function updateCourse({ courseId, changes }: UpdateCourseInput): Promise<Course> {
  return apiClient<Course>(apiPath`/api/courses/${courseId}`, jsonBody('PATCH', changes))
}

/** 201 with the new member; 404 for an unknown user or course, 409 if already a member. */
export function enrollInCourse({ courseId, userId, role }: EnrollInCourseInput): Promise<CourseEnrollment> {
  return apiClient<CourseEnrollment>(apiPath`/api/courses/${courseId}/enrollments`, jsonBody('POST', { userId, role }))
}

export function removeCourseEnrollment({ courseId, userId }: CourseEnrollmentInput): Promise<void> {
  return apiClient<void>(apiPath`/api/courses/${courseId}/enrollments/${userId}`, { method: 'DELETE' })
}

/** Returns the member with the new role. */
export function updateCourseRole({ courseId, userId, role }: UpdateCourseRoleInput): Promise<CourseEnrollment> {
  return apiClient<CourseEnrollment>(
    apiPath`/api/courses/${courseId}/enrollments/${userId}`,
    jsonBody('PATCH', { role }),
  )
}
