import type { ContentId, UserId } from "../../shared/types";
import type { SubmissionId } from "./types";

export const submissionKeys = {
  myHistory: (contentId: ContentId) =>
    ["submissions", "content", String(contentId), "me"] as const,
  /** Teachers: all submissions for a content item. */
  contentSubmissions: (contentId: ContentId) =>
    ["submissions", "content", String(contentId), "all"] as const,
  result: (submissionId: SubmissionId, viewerId: UserId) =>
    ["submissions", "result", String(submissionId), String(viewerId)] as const,
};
