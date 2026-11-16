import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { CourseId, UserId } from "../../shared/types";
import { useCurrentUser } from "../../shared/auth/useAuth";
import { createCourse, enrollInCourse, getCourse, getCourseEnrollments, getMyCourses, removeCourseEnrollment, updateCourse, updateCourseRole } from "./api";
import { courseKeys } from "./queryKeys";
import type { CourseRole } from "./types";

// Fetches and caches a specific course by its ID.
export function useCourse(courseId: CourseId) {
  return useQuery({
    queryKey: courseKeys.course(courseId),
    queryFn: () => getCourse(courseId),
  });
}

// Fetches and caches enrolled users and their roles for a specific course.
export function useCourseEnrollments(courseId: CourseId) {
  return useQuery({
    queryKey: courseKeys.enrollments(courseId),
    queryFn: () => getCourseEnrollments(courseId),
  });
}

// The logged-in user's role in a course (TEACHER or STUDENT), from their course list.
// Undefined while loading or when the user is not enrolled.
export function useCourseRole(courseId: CourseId): { role: CourseRole | undefined; isPending: boolean } {
  const user = useCurrentUser().data;
  const courses = useMyCourses(user?.userId ?? "");
  const course = courses.data?.find(c => String(c.id) === String(courseId));
  return { role: course?.role, isPending: courses.isPending };
}

// Fetches and caches a user's enrolled courses, including their role in each course.
export function useMyCourses(userId: UserId) {
  return useQuery({
    queryKey: courseKeys.myCourses(userId),
    queryFn: () => getMyCourses(userId),
  });
}

// Creates a course and marks cached course data as outdated.
export function useCreateCourse() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createCourse,
    // Creating a course may also enroll its creator as a teacher.
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: courseKeys.all }),
  });
}

// Enrolls a user in a course and marks its enrollments and the user's course list as outdated.
export function useEnrollInCourse() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: enrollInCourse,
    onSuccess: async (_data, { courseId, userId }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: courseKeys.enrollments(courseId) }),
        queryClient.invalidateQueries({ queryKey: courseKeys.myCourses(userId) }),
      ]);
    },
  });
}

// Removes a user's enrollment and marks the affected enrollment and course lists as outdated.
export function useRemoveCourseEnrollment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: removeCourseEnrollment,
    onSuccess: async (_data, { courseId, userId }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: courseKeys.enrollments(courseId) }),
        queryClient.invalidateQueries({ queryKey: courseKeys.myCourses(userId) }),
      ]);
    },
  });
}

// Updates course information and marks its cached details and course lists as outdated.
export function useUpdateCourse() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateCourse,
    onSuccess: async (_course, { courseId }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: courseKeys.course(courseId) }),
        queryClient.invalidateQueries({ queryKey: courseKeys.allCourses }),
        queryClient.invalidateQueries({ queryKey: courseKeys.enrolledCourses }),
      ]);
    },
  });
}

// Changes a user's course role and marks its enrollments and the user's course list as outdated.
export function useUpdateCourseRole() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateCourseRole,
    onSuccess: async (_data, { courseId, userId }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: courseKeys.enrollments(courseId) }),
        queryClient.invalidateQueries({ queryKey: courseKeys.myCourses(userId) }),
      ]);
    },
  });
}
