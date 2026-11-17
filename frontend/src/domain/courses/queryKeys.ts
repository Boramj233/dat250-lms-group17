import type { CourseId, UserId } from '@shared/types'

export const courseKeys = {
  all: ['courses'] as const,
  allCourses: ['courses', 'list'] as const,
  enrolledCourses: ['courses', 'enrolled'] as const,
  myCourses: (userId: UserId) => ['courses', 'enrolled', String(userId)] as const,
  course: (courseId: CourseId) => ['courses', 'detail', String(courseId)] as const,
  enrollments: (courseId: CourseId) => ['courses', 'enrollments', String(courseId)] as const,
}
