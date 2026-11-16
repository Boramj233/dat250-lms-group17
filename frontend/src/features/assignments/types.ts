import type { ContentId, GroupId, UserId } from "../../shared/types";

export type SubmissionId = string | number;

// Matches the backend's SubmissionType.
export type SubmissionKind = "TEXT" | "GIT_URL";

// Matches the backend's SubmissionResponse, from
// GET /api/contents/{contentId}/submissions/me (the user's own submission history).
export interface MySubmission {
  id: SubmissionId;
  contentId: ContentId;
  type: SubmissionKind;
  /** The text, or the repository URL for GIT_URL. */
  payload: string;
  /** ISO 8601 instant. */
  submittedAt: string;
  late: boolean;
  /**
   * Proposed (see content/README.md): the group that handed in, for a group assignment.
   * Missing or null for an individual submission. Every member's history includes
   * the group's submissions.
   */
  group?: SubmissionGroup | null;
}

/** A student group with its members' names (proposed; the backend has no groups yet). */
export interface SubmissionGroup {
  id: GroupId;
  name: string;
  members: { userId: UserId; firstName: string; lastName: string }[];
}

export type SubmissionStatus = "SUBMITTED" | "GRADING" | "GRADED" | "GRADING_FAILED";

export interface SubmissionGrade {
  // The course's grading scheme determines valid values, e.g. "A" or "Passed".
  value: string;
  feedback: string | null;
  publishedAt: string | null;
}

export interface CodingTestResult {
  id: string;
  name: string;
  status: "PASSED" | "FAILED" | "ERROR";
  message: string | null;
}

export interface SubmissionResult {
  submissionId: SubmissionId;
  status: SubmissionStatus;
  // Backend hides unpublished grades/feedback from students, returning null.
  grade: SubmissionGrade | null;
  // Only results the current viewer may see; hidden test definitions stay on the server.
  testResults: CodingTestResult[] | null;
  errorMessage: string | null;
}

// Matches the backend's CreateSubmissionRequest (POST /api/contents/{contentId}/submissions):
// TEXT for an assignment answer, GIT_URL for a coding exercise's repository.
export interface SubmitMySubmissionInput {
  contentId: ContentId;
  type: SubmissionKind;
  /** The text, or the repository URL; at most 10 000 characters. */
  payload: string;
}

// Teachers: grading (proposed, see README.md).

/**
 * One submission as a teacher sees it (GET /api/contents/{contentId}/submissions): the
 * student's SubmissionResponse plus who handed it in and the grade, also before it is
 * published (`publishedAt` null).
 */
export interface ContentSubmission extends MySubmission {
  student: { userId: UserId; firstName: string; lastName: string; email: string };
  grade: SubmissionGrade | null;
}

export interface GradeSubmissionInput {
  submissionId: SubmissionId;
  /** Cache scope: the content the submission belongs to. */
  contentId: ContentId;
  /** e.g. "Passed", "Not passed" or "B". */
  grade: string;
  feedback: string;
}

export interface PublishGradeInput {
  submissionId: SubmissionId;
  /** Cache scope: the content the submission belongs to. */
  contentId: ContentId;
}
