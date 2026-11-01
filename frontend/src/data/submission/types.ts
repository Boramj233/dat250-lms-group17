import type { ContentId, CourseId, GroupId, UserId } from "../shared/types";

export type SubmissionId = string | number;
export type SubmissionStatus = "SUBMITTED" | "GRADING" | "GRADED" | "GRADING_FAILED";

export type SubmissionOwner =
  | { type: "INDIVIDUAL"; userId: UserId }
  // Membership snapshot provided by the backend when the group submits.
  | { type: "GROUP"; groupId: GroupId; memberIds: UserId[] };

export interface SubmissionFile {
  id: string | number;
  name: string;
  mediaType: string;
  size: number;
  downloadUrl: string;
}

export type SubmissionPayload =
  | { type: "FILES"; files: SubmissionFile[] }
  | { type: "REPOSITORY"; repositoryUrl: string; commitRef: string };

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

export interface SubmissionSummary {
  id: SubmissionId;
  contentId: ContentId;
  courseId: CourseId;
  contentType: "ASSIGNMENT" | "CODING_EXERCISE";
  submittedBy: UserId;
  owner: SubmissionOwner;
  submittedAt: string;
  // Backend calculates lateness and rejects submissions if late delivery is forbidden.
  late: boolean;
  status: SubmissionStatus;
}

export interface Submission extends SubmissionSummary {
  payload: SubmissionPayload;
  result: SubmissionResult | null;
}

export interface SubmitAssignmentInput {
  contentId: ContentId;
  files: File[];
  // Omit for an individual submission. Backend validates group membership.
  groupId?: GroupId;
}

export interface SubmitCodingExerciseInput {
  contentId: ContentId;
  repositoryUrl: string;
  // Backend resolves and stores the exact commit so later repository edits do not alter a submission.
  commitRef: string;
  groupId?: GroupId;
}

export interface GradeSubmissionInput {
  submissionId: SubmissionId;
  grade: string;
  feedback?: string;
}
