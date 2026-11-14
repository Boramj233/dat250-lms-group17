import { apiClient } from "../../shared/api/apiClient";
import type { ContentId, UserId } from "../../shared/types";
import type { ContentSubmission, GradeSubmissionInput, MySubmission, Submission, SubmissionId, SubmissionResult, SubmissionSummary, SubmitAssignmentInput, SubmitCodingExerciseInput } from "./types";

/** The logged-in user's submissions for an assignment or coding exercise (backend). */
export function getMySubmissionHistory(contentId: ContentId): Promise<MySubmission[]> {
  return apiClient<MySubmission[]>(
    `/api/contents/${encodeURIComponent(String(contentId))}/submissions/me`
  );
}

/** Teachers: every student's latest submission for one content item (proposed). */
export function getContentSubmissions(contentId: ContentId): Promise<ContentSubmission[]> {
  return apiClient<ContentSubmission[]>(
    `/api/contents/${encodeURIComponent(String(contentId))}/submissions`
  );
}

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

// Proposed endpoints. Backend identifies the actor, validates access, deadlines
// and group membership, records the interaction, and recalculates module progress.
export function submitAssignment({
  contentId,
  files,
  groupId,
}: SubmitAssignmentInput): Promise<Submission> {
  if (files.length === 0) {
    return Promise.reject(new Error("Select at least one file to submit."));
  }

  const body = new FormData();
  body.append("type", "FILES");
  for (const file of files) body.append("files", file);
  if (groupId !== undefined) body.append("groupId", String(groupId));

  // Let fetch set the multipart Content-Type and boundary.
  return apiClient<Submission>(
    `/api/content/${encodeURIComponent(String(contentId))}/submissions`,
    { method: "POST", body }
  );
}

export function submitCodingExercise({
  contentId,
  repositoryUrl,
  commitRef,
  groupId,
}: SubmitCodingExerciseInput): Promise<Submission> {
  return apiClient<Submission>(
    `/api/content/${encodeURIComponent(String(contentId))}/submissions`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ type: "REPOSITORY", repositoryUrl, commitRef, groupId }),
    }
  );
}

// Teacher-only operation. Saving a grade does not publish it; edited grades must
// be withheld from students until explicitly published again by the teacher.
export function gradeSubmission({
  submissionId,
  grade,
  feedback,
}: GradeSubmissionInput): Promise<Submission> {
  return apiClient<Submission>(
    `/api/submissions/${encodeURIComponent(String(submissionId))}/grade`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ grade, feedback }),
    }
  );
}

// Publishes the shared grade for the individual or all members of the submitting group.
export function publishSubmissionGrade(submissionId: SubmissionId): Promise<Submission> {
  return apiClient<Submission>(
    `/api/submissions/${encodeURIComponent(String(submissionId))}/grade/publish`,
    { method: "POST" }
  );
}
