import type { ContentId } from "../../shared/types";
import type { SubmissionId } from "./types";

export const submissionKeys = {
  myHistory: (contentId: ContentId) =>
    ["submissions", "content", String(contentId), "me"] as const,
  myGrade: (submissionId: SubmissionId) =>
    ["submissions", "detail", String(submissionId), "my-grade"] as const,
  /** Teachers: the latest submission per student or group for a content item. */
  review: (contentId: ContentId) =>
    ["submissions", "content", String(contentId), "review"] as const,
  reviewDetail: (submissionId: SubmissionId) =>
    ["submissions", "detail", String(submissionId), "review"] as const,
  reviewHistory: (submissionId: SubmissionId) =>
    ["submissions", "detail", String(submissionId), "history"] as const,
  gradeReview: (submissionId: SubmissionId) =>
    ["submissions", "detail", String(submissionId), "grade"] as const,
};
