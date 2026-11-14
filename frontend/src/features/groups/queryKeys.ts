import type { CourseId } from "../../shared/types";

export const groupKeys = {
  all: ["groups"] as const,
  course: (courseId: CourseId) => ["groups", "course", String(courseId)] as const,
};
