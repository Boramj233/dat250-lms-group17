import { apiClient, apiPath, jsonBody } from "../../shared/api/apiClient";
import type { ContentId, CourseId, ModuleId } from "../../shared/types";
import type { AssignmentContent, CodingExerciseContent, Content, ContentSummary, ContentType, CreateContentInput, DeleteContentInput, MoveContentInput, QuizContent, StaticPageContent, UpdateContentInput } from "./types";

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
    apiPath`/api/courses/${courseId}/modules/${moduleId}/contents`
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
      return { ...(await apiClient<Omit<StaticPageContent, "type">>(apiPath`/api/contents/${contentId}`)), type };
    case "QUIZ":
      return { ...(await apiClient<Omit<QuizContent, "type">>(apiPath`/api/quizzes/${contentId}`)), type };
    case "ASSIGNMENT":
      return { ...(await apiClient<Omit<AssignmentContent, "type">>(apiPath`/api/assignments/${contentId}`)), type };
    case "CODING_EXERCISE":
      return { ...(await apiClient<Omit<CodingExerciseContent, "type">>(apiPath`/api/coding-exercises/${contentId}`)), type };
  }
}

function post<T>(url: string, body: unknown): Promise<T> {
  return apiClient<T>(url, jsonBody("POST", body));
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
  const url = apiPath`/api/courses/${courseId}/modules/${moduleId}/contents/${CREATE_PATH[type]}`;
  if (input.type !== "QUIZ") {
    return post<{ id: ContentId }>(url, fields);
  }
  const { questions, ...quizFields } = fields as Omit<typeof input, "courseId" | "moduleId" | "type">;
  const quiz = await post<{ id: ContentId }>(url, quizFields);
  for (const [position, question] of questions.entries()) {
    await post(apiPath`/api/quizzes/${quiz.id}/questions`, {
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
  return apiClient<Content>(apiPath`/api/content/${contentId}`, jsonBody("PATCH", changes));
}

export function deleteContent({ contentId }: DeleteContentInput): Promise<void> {
  return apiClient<void>(apiPath`/api/content/${contentId}`, { method: "DELETE" });
}

export function moveContent({
  contentId,
  targetModuleId,
  position,
}: MoveContentInput): Promise<Content> {
  return apiClient<Content>(
    apiPath`/api/content/${contentId}/move`,
    jsonBody("PATCH", { moduleId: targetModuleId, position })
  );
}
