import { apiClient } from "../../shared/apiClient";
import type { ContentId, CourseId, ModuleId, UserId } from "../../shared/types";
import type {
  AssignmentContent,
  CompletedContent,
  CodingExerciseContent,
  Content,
  ContentCompletion,
  ContentSummary,
  ContentType,
  QuizContent,
  StaticPageContent,
} from "../types";

const id = (value: CourseId | ModuleId | ContentId) => encodeURIComponent(String(value));

// The backend sends "Assignment" for assignments in content lists, unlike the other
// types (STATIC_PAGE, QUIZ, CODING_EXERCISE). Accept both spellings.
function normalizeType(type: string): ContentType {
  return type === "Assignment" ? "ASSIGNMENT" : (type as ContentType);
}

/** A module's content, sorted by position. */
export async function getModuleContent(
  courseId: CourseId,
  moduleId: ModuleId
): Promise<ContentSummary[]> {
  const items = await apiClient<(Omit<ContentSummary, "type"> & { type: string })[]>(
    `/api/courses/${id(courseId)}/modules/${id(moduleId)}/contents`
  );
  return items
    .map(item => ({ ...item, type: normalizeType(item.type) }))
    .sort((a, b) => a.position - b.position);
}

/**
 * The details of one content item. The backend has one endpoint per type and the
 * responses have no `type` field, so the caller passes the type (from the summary) and
 * it is added to the result.
 */
export async function getContent(type: ContentType, contentId: ContentId): Promise<Content> {
  switch (type) {
    case "STATIC_PAGE":
      return { ...(await apiClient<Omit<StaticPageContent, "type">>(`/api/contents/${id(contentId)}`)), type };
    case "QUIZ":
      return { ...(await apiClient<Omit<QuizContent, "type">>(`/api/quizzes/${id(contentId)}`)), type };
    case "ASSIGNMENT":
      return { ...(await apiClient<Omit<AssignmentContent, "type">>(`/api/assignments/${id(contentId)}`)), type };
    case "CODING_EXERCISE":
      return { ...(await apiClient<Omit<CodingExerciseContent, "type">>(`/api/coding-exercises/${id(contentId)}`)), type };
  }
}

export function getContentCompletion(
  contentId: ContentId,
  userId: UserId
): Promise<ContentCompletion> {
  return apiClient<ContentCompletion>(
    `/api/users/${encodeURIComponent(String(userId))}/content/${encodeURIComponent(String(contentId))}/completion`
  );
}

/** What the logged-in user has completed in a course (proposed endpoint, see README). */
export function getMyCourseCompletions(courseId: CourseId): Promise<CompletedContent[]> {
  return apiClient<CompletedContent[]>(`/api/courses/${id(courseId)}/completions/me`);
}
