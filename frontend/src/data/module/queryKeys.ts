import type { CourseId, UserId } from "../shared/types";

export const moduleKeys = {
  all: ["modules"] as const,
  progress: ["modules", "progress"] as const,
  courseProgress: (courseId: CourseId) =>
    ["modules", "progress", String(courseId)] as const,
  userModuleProgress: (courseId: CourseId, userId: UserId) =>
    ["modules", "progress", String(courseId), String(userId)] as const,
  courseModules: (courseId: CourseId) =>
    ["modules", "course", String(courseId)] as const,
};
