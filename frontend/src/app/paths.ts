import type { CourseId } from '../data/shared/types'

export const paths = {
  home: '/',
  login: '/login',
  // Route pattern for <Route path>; use course(id) to link to a course.
  courseRoute: '/courses/:courseId',
  course: (courseId: CourseId) => `/courses/${encodeURIComponent(String(courseId))}`,
} as const
