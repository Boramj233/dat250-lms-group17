import { apiClient } from "../../shared/apiClient";

import type {
  Content,
  ContentCompletion,
  CreateContentInput,
  DeleteContentInput,
  MoveContentInput,
  SetContentCompletionInput,
  UpdateContentInput,
} from "../types";

export function createContent({
  moduleId,
  ...input
}: CreateContentInput): Promise<Content> {
  return apiClient<Content>(
    `/api/modules/${encodeURIComponent(String(moduleId))}/content`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    }
  );
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
