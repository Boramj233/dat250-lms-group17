import { useQuery } from '@tanstack/react-query'
import { useInvalidatingMutation } from '@shared/api/useInvalidatingMutation'
import type { CourseId, UserId } from '@shared/types'
import { useCurrentUser } from '@shared/auth/useAuth'
import {
  createCourse,
  enrollInCourse,
  getCourse,
  getCourseEnrollments,
  getMyCourses,
  removeCourseEnrollment,
  updateCourse,
  updateCourseRole,
} from './api'
import { courseKeys } from './queryKeys'
import type { CourseRole } from './types'

// Fetches and caches a specific course by its ID.
export function useCourse(courseId: CourseId) {
  return useQuery({
    queryKey: courseKeys.course(courseId),
    queryFn: () => getCourse(courseId),
  })
}

// Fetches and caches enrolled users and their roles for a specific course.
export function useCourseEnrollments(courseId: CourseId) {
  return useQuery({
    queryKey: courseKeys.enrollments(courseId),
    queryFn: () => getCourseEnrollments(courseId),
  })
}

// The logged-in user's role in a course (TEACHER or STUDENT), from their course list.
// Undefined while loading or when the user is not enrolled.
export function useCourseRole(courseId: CourseId): { role: CourseRole | undefined; isPending: boolean } {
  const user = useCurrentUser().data
  const courses = useMyCourses(user?.userId ?? '')
  const course = courses.data?.find(c => String(c.id) === String(courseId))
  return { role: course?.role, isPending: courses.isPending }
}

// Fetches and caches a user's enrolled courses, including their role in each course.
export function useMyCourses(userId: UserId) {
  return useQuery({
    queryKey: courseKeys.myCourses(userId),
    queryFn: () => getMyCourses(userId),
  })
}

// Creates a course and marks cached course data as outdated.
// Creating a course may also enroll its creator as a teacher.
export function useCreateCourse() {
  return useInvalidatingMutation(createCourse, () => [courseKeys.all])
}

// A change to one user's membership: the course's enrollments and that user's course list.
const membershipKeys = (_data: unknown, { courseId, userId }: { courseId: CourseId; userId: UserId }) => [
  courseKeys.enrollments(courseId),
  courseKeys.myCourses(userId),
]

// Enrolls a user in a course.
export function useEnrollInCourse() {
  return useInvalidatingMutation(enrollInCourse, membershipKeys)
}

// Removes a user's enrollment.
export function useRemoveCourseEnrollment() {
  return useInvalidatingMutation(removeCourseEnrollment, membershipKeys)
}

// Updates course information and marks its cached details and course lists as outdated.
export function useUpdateCourse() {
  return useInvalidatingMutation(updateCourse, (_course, { courseId }) => [
    courseKeys.course(courseId),
    courseKeys.allCourses,
    courseKeys.enrolledCourses,
  ])
}

// Changes a user's course role.
export function useUpdateCourseRole() {
  return useInvalidatingMutation(updateCourseRole, membershipKeys)
}
