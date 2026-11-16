import type { CourseId } from "../../shared/types";

export const moduleKeys = {
  courseModules: (courseId: CourseId) =>
    ["modules", "course", String(courseId)] as const,
};
