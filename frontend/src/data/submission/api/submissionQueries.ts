import { apiClient } from "../../shared/apiClient";
import type { ContentId, UserId } from "../../shared/types";
import type { Submission, SubmissionId, SubmissionResult, SubmissionSummary } from "../types";

// Proposed endpoints. Backend restricts reads to owners/group members and course teachers.
// A user's list includes both individual submissions and submissions on their group's behalf.
export function getMySubmissions(
  contentId: ContentId,
  userId: UserId
): Promise<SubmissionSummary[]> {
  return apiClient<SubmissionSummary[]>(
    `/api/users/${encodeURIComponent(String(userId))}/content/${encodeURIComponent(String(contentId))}/submissions`
  );
}

// Teacher-only list for either an assignment or a coding exercise.
export function getAssignmentSubmissions(contentId: ContentId): Promise<SubmissionSummary[]> {
  return apiClient<SubmissionSummary[]>(
    `/api/content/${encodeURIComponent(String(contentId))}/submissions`
  );
}

export function getSubmission(submissionId: SubmissionId): Promise<Submission> {
  return apiClient<Submission>(
    `/api/submissions/${encodeURIComponent(String(submissionId))}`
  );
}

// The backend filters unpublished grades and hidden test details for the authenticated viewer.
export function getSubmissionResult(submissionId: SubmissionId): Promise<SubmissionResult> {
  return apiClient<SubmissionResult>(
    `/api/submissions/${encodeURIComponent(String(submissionId))}/result`
  );
}
