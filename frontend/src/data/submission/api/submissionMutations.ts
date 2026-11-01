import { apiClient } from "../../shared/apiClient";
import type {
  GradeSubmissionInput,
  Submission,
  SubmissionId,
  SubmitAssignmentInput,
  SubmitCodingExerciseInput,
} from "../types";

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
