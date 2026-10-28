import { apiClient } from "../../shared/apiClient";
import type { ContentId, ModuleId, UserId } from "../../shared/types";
import type { Content, ContentCompletion, ContentSummary } from "../types";

export function getModuleContent(moduleId: ModuleId): Promise<ContentSummary[]> {
  return apiClient<ContentSummary[]>(
    `/api/modules/${encodeURIComponent(String(moduleId))}/content`
  );
}

export function getContent(contentId: ContentId): Promise<Content> {
  return apiClient<Content>(
    `/api/content/${encodeURIComponent(String(contentId))}`
  );
}

export function getContentCompletion(
  contentId: ContentId,
  userId: UserId
): Promise<ContentCompletion> {
  return apiClient<ContentCompletion>(
    `/api/users/${encodeURIComponent(String(userId))}/content/${encodeURIComponent(String(contentId))}/completion`
  );
}
