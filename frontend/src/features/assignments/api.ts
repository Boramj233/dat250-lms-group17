import { apiClient, apiClientOrNull, apiPath, jsonBody } from "../../shared/api/apiClient";
import type { ContentId } from "../../shared/types";
import type { GradeReview, MySubmission, Page, ReleasedGrade, ReleaseGradeInput, ReviewSubmission, SaveGradeInput, SubmissionId, SubmitFileInput, SubmitMySubmissionInput } from "./types";

// --- Students (SubmissionController, SubmissionFileController) ----------------------

/** The logged-in user's submissions (and their groups') for an assignment or exercise. */
export function getMySubmissionHistory(contentId: ContentId): Promise<MySubmission[]> {
  return apiClient<MySubmission[]>(apiPath`/api/contents/${contentId}/submissions/me`);
}

/** Hands in a text or a repository URL, for yourself or a group you are in. */
export function submitMySubmission({ contentId, type, payload, groupId }: SubmitMySubmissionInput): Promise<MySubmission> {
  return apiClient<MySubmission>(
    apiPath`/api/contents/${contentId}/submissions`,
    jsonBody("POST", { type, payload, groupId })
  );
}

/** Hands in one PDF file (multipart, part "file"), for yourself or a group you are in. */
export function submitFile({ contentId, file, groupId }: SubmitFileInput): Promise<MySubmission> {
  const body = new FormData();
  body.append("file", file);
  if (groupId !== undefined) body.append("groupId", String(groupId));
  // Let fetch set the multipart Content-Type with its boundary.
  return apiClient<MySubmission>(apiPath`/api/contents/${contentId}/submissions/file`, { method: "POST", body });
}

/** The released grade of one of your submissions; null until the teacher releases it. */
export function getMyReleasedGrade(submissionId: SubmissionId): Promise<ReleasedGrade | null> {
  return apiClientOrNull<ReleasedGrade>(apiPath`/api/submissions/${submissionId}/grade`);
}

// --- Teachers (SubmissionReviewController, GradeController) -------------------------

/** The latest submission per student or group for a content item, all pages. */
export async function getReviewSubmissions(contentId: ContentId): Promise<ReviewSubmission[]> {
  const all: ReviewSubmission[] = [];
  for (let page = 0; ; page++) {
    const query = new URLSearchParams({ page: String(page), size: "100" });
    const result = await apiClient<Page<ReviewSubmission>>(
      `${apiPath`/api/contents/${contentId}/submissions/review`}?${query}`
    );
    all.push(...result.content);
    if (result.last || result.content.length === 0) return all;
  }
}

/** One submission, as a teacher of the course sees it. */
export function getReviewSubmission(submissionId: SubmissionId): Promise<ReviewSubmission> {
  return apiClient<ReviewSubmission>(apiPath`/api/submissions/${submissionId}/review`);
}

/** Every submission by the same student (or group) for the same item, newest first. */
export function getReviewHistory(submissionId: SubmissionId): Promise<ReviewSubmission[]> {
  return apiClient<ReviewSubmission[]>(apiPath`/api/submissions/${submissionId}/review/history`);
}

/** The grade of a submission, released or not; null when it is not graded yet. */
export function getGradeReview(submissionId: SubmissionId): Promise<GradeReview | null> {
  return apiClientOrNull<GradeReview>(apiPath`/api/submissions/${submissionId}/grade/review`);
}

/** Creates (version null) or changes the grade; the student sees it once released. */
export function saveGrade({ submissionId, score, feedback, version }: SaveGradeInput): Promise<GradeReview> {
  return apiClient<GradeReview>(
    apiPath`/api/submissions/${submissionId}/grade`,
    jsonBody("PUT", { score, feedback, version })
  );
}

/** Shows the grade to the student. After this it cannot be changed. */
export function releaseGrade({ submissionId, version }: ReleaseGradeInput): Promise<GradeReview> {
  return apiClient<GradeReview>(
    apiPath`/api/submissions/${submissionId}/grade/release`,
    jsonBody("POST", { version })
  );
}
