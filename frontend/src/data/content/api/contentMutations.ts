import { apiClient } from "../../shared/apiClient";

import type { ContentId } from "../../shared/types";
import type {
  Content,
  ContentCompletion,
  CreateContentInput,
  DeleteContentInput,
  MoveContentInput,
  SetContentCompletionInput,
  UpdateContentInput,
} from "../types";

const id = (value: string | number) => encodeURIComponent(String(value));

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
