import type { ContentId, UserId } from "../../shared/types";
import type { SubmissionId } from "./types";

export const submissionKeys = {
  all: ["submissions"] as const,
  contentSubmissions: (contentId: ContentId) =>
    ["submissions", "content", String(contentId)] as const,
  myHistory: (contentId: ContentId) =>
    ["submissions", "content", String(contentId), "me"] as const,
  mySubmissions: (contentId: ContentId, userId: UserId) =>
    ["submissions", "content", String(contentId), "user", String(userId)] as const,
  assignmentSubmissions: (contentId: ContentId, viewerId: UserId) =>
    ["submissions", "content", String(contentId), "teacher", String(viewerId)] as const,
  details: (submissionId: SubmissionId) =>
    ["submissions", "detail", String(submissionId)] as const,
  submission: (submissionId: SubmissionId, viewerId: UserId) =>
    ["submissions", "detail", String(submissionId), String(viewerId)] as const,
  results: (submissionId: SubmissionId) =>
    ["submissions", "result", String(submissionId)] as const,
  result: (submissionId: SubmissionId, viewerId: UserId) =>
    ["submissions", "result", String(submissionId), String(viewerId)] as const,
};
