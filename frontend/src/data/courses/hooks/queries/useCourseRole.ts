// The logged-in user's role in a course (TEACHER or STUDENT), from their course list.
// Undefined while loading or when the user is not enrolled.

import { useCurrentUser } from "../../../auth/useAuth";
import type { CourseId } from "../../../shared/types";
import type { CourseRole } from "../../types";
import { useMyCourses } from "./useMyCourses";

export function useCourseRole(courseId: CourseId): { role: CourseRole | undefined; isPending: boolean } {
  const user = useCurrentUser().data;
  const courses = useMyCourses(user?.userId ?? "");
  const course = courses.data?.find(c => String(c.id) === String(courseId));
  return { role: course?.role, isPending: courses.isPending };
}
