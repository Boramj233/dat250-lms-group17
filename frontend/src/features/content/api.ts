import { apiClient } from "../../shared/api/apiClient";
import type { ContentId, CourseId, ModuleId, UserId } from "../../shared/types";
import type { AssignmentContent, CodingExerciseContent, CompletedContent, Content, ContentCompletion, ContentSummary, ContentType, CreateContentInput, DeleteContentInput, MoveContentInput, QuizContent, SetContentCompletionInput, StaticPageContent, UpdateContentInput } from "./types";

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

function post<T>(url: string, body: unknown): Promise<T> {
  return apiClient<T>(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
}

const CREATE_PATH: Record<CreateContentInput["type"], string> = {
  STATIC_PAGE: "pages",
  ASSIGNMENT: "assignments",
  CODING_EXERCISE: "coding-exercises",
  QUIZ: "quizzes",
};

/**
 * Creates content with the backend endpoint for its type. A quiz is created first and
 * its questions are then added one by one. Resolves to the new content's id.
 */
export async function createContent(input: CreateContentInput): Promise<{ id: ContentId }> {
  const { courseId, moduleId, type, ...fields } = input;
  const url = `/api/courses/${id(courseId)}/modules/${id(moduleId)}/contents/${CREATE_PATH[type]}`;
  if (input.type !== "QUIZ") {
    return post<{ id: ContentId }>(url, fields);
  }
  const { questions, ...quizFields } = fields as Omit<typeof input, "courseId" | "moduleId" | "type">;
  const quiz = await post<{ id: ContentId }>(url, quizFields);
  for (const [position, question] of questions.entries()) {
    await post(`/api/quizzes/${id(quiz.id)}/questions`, {
      questionText: question.questionText,
      position,
      hint: question.hint || null,
      options: question.options.map((option, index) => ({ ...option, position: index })),
    });
  }
  return quiz;
}

export function updateContent({
  contentId,
  changes,
}: UpdateContentInput): Promise<Content> {
  return apiClient<Content>(
    `/api/content/${encodeURIComponent(String(contentId))}`,
    {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(changes),
    }
  );
}

export function deleteContent({ contentId }: DeleteContentInput): Promise<void> {
  return apiClient<void>(
    `/api/content/${encodeURIComponent(String(contentId))}`,
    { method: "DELETE" }
  );
}

export function moveContent({
  contentId,
  targetModuleId,
  position,
}: MoveContentInput): Promise<Content> {
  return apiClient<Content>(
    `/api/content/${encodeURIComponent(String(contentId))}/move`,
    {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ moduleId: targetModuleId, position }),
    }
  );
}

export function setContentCompletion({
  contentId,
  userId,
  completed,
}: SetContentCompletionInput): Promise<ContentCompletion> {
  return apiClient<ContentCompletion>(
    `/api/users/${encodeURIComponent(String(userId))}/content/${encodeURIComponent(String(contentId))}/completion`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ completed }),
    }
  );
}
