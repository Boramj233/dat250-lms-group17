import type { ContentId, CourseId, ModuleId, UserId } from "../shared/types";

export const contentKeys = {
  all: ["content"] as const,
  courseCompletions: (courseId: CourseId) =>
    ["content", "courseCompletions", String(courseId)] as const,
  moduleContent: (moduleId: ModuleId) =>
    ["content", "module", String(moduleId)] as const,
  content: (contentId: ContentId) =>
    ["content", "detail", String(contentId)] as const,
  completions: (contentId: ContentId) =>
    ["content", "completion", String(contentId)] as const,
  completion: (contentId: ContentId, userId: UserId) =>
    ["content", "completion", String(contentId), String(userId)] as const,
};
