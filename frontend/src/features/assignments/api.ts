import { apiClient, apiPath, jsonBody } from "../../shared/api/apiClient";
import type { ContentId } from "../../shared/types";
import type { ContentSubmission, GradeSubmissionInput, MySubmission, PublishGradeInput, SubmissionId, SubmissionResult, SubmitMySubmissionInput } from "./types";

/** The logged-in user's submissions for an assignment or coding exercise (backend). */
export function getMySubmissionHistory(contentId: ContentId): Promise<MySubmission[]> {
  return apiClient<MySubmission[]>(apiPath`/api/contents/${contentId}/submissions/me`);
}

// The backend filters unpublished grades and hidden test details for the authenticated viewer.
export function getSubmissionResult(submissionId: SubmissionId): Promise<SubmissionResult> {
  return apiClient<SubmissionResult>(apiPath`/api/submissions/${submissionId}/result`);
}

/** Hands in the logged-in user's answer (backend): a text or a repository URL. */
export function submitMySubmission({ contentId, type, payload }: SubmitMySubmissionInput): Promise<MySubmission> {
  return apiClient<MySubmission>(
    apiPath`/api/contents/${contentId}/submissions`,
    jsonBody("POST", { type, payload })
  );
}

/** Teachers: every submission for an assignment or coding exercise (proposed). */
export function getContentSubmissions(contentId: ContentId): Promise<ContentSubmission[]> {
  return apiClient<ContentSubmission[]>(apiPath`/api/contents/${contentId}/submissions`);
}

/** Teachers: saves a grade and feedback; the student sees it once published (proposed). */
export function gradeSubmission({ submissionId, grade, feedback }: GradeSubmissionInput): Promise<ContentSubmission> {
  return apiClient<ContentSubmission>(
    apiPath`/api/submissions/${submissionId}/grade`,
    jsonBody("PUT", { grade, feedback })
  );
}

/** Teachers: shows the saved grade and feedback to the student (proposed). */
export function publishSubmissionGrade({ submissionId }: PublishGradeInput): Promise<ContentSubmission> {
  return apiClient<ContentSubmission>(
    apiPath`/api/submissions/${submissionId}/grade/publish`,
    { method: "POST" }
  );
}
