import type { ContentId, GroupId, UserId } from '@shared/types'

export type SubmissionId = string | number

// Matches the backend's SubmissionType.
export type SubmissionKind = 'TEXT' | 'GIT_URL' | 'FILE'

// The backend's SubmissionFileResponse: metadata of a FILE submission. `downloadPath`
// needs the token, so download it with apiDownload, not a plain link.
export interface SubmissionFile {
  name: string
  mediaType: string
  sizeBytes: number
  /** /api/submissions/{id}/file for the student, …/review/file for a teacher. */
  downloadPath: string
}

// The backend's SubmissionMemberResponse: a member of the group that handed in.
export interface SubmissionMember {
  userId: UserId
  enrollmentId: number
}

// Matches the backend's SubmissionResponse: POST /api/contents/{contentId}/submissions
// (and …/submissions/file) and GET /api/contents/{contentId}/submissions/me.
export interface MySubmission {
  id: SubmissionId
  contentId: ContentId
  type: SubmissionKind
  /** The text, or the repository URL for GIT_URL; null for FILE. */
  payload: string | null
  /** ISO 8601 instant. */
  submittedAt: string
  late: boolean
  /** Only for FILE. */
  file: SubmissionFile | null
  /** The group that handed in; null for an individual submission. */
  groupId: GroupId | null
  submittedByUserId: UserId
  /** The group's members when it was handed in; empty for an individual submission. */
  members: SubmissionMember[]
}

// Matches the backend's SubmissionReviewResponse (teachers of the course):
// GET /api/contents/{contentId}/submissions/review (the latest per student or group),
// GET /api/submissions/{submissionId}/review and …/review/history.
export interface ReviewSubmission extends MySubmission {
  studentUserId: UserId
  enrollmentId: number
}

// Spring Data's Page, as the backend sends it.
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  /** Zero-based page number. */
  number: number
  size: number
  last: boolean
}

// Matches the backend's CreateSubmissionRequest: TEXT for an assignment answer,
// GIT_URL for a coding exercise's repository.
export interface SubmitMySubmissionInput {
  contentId: ContentId
  type: 'TEXT' | 'GIT_URL'
  /** The text, or the repository URL; at most 10 000 characters. */
  payload: string
  /** Hand in for this group (the user must be a member); leave out for yourself. */
  groupId?: GroupId
}

// POST /api/contents/{contentId}/submissions/file (multipart): one PDF, at most 10 MB.
export interface SubmitFileInput {
  contentId: ContentId
  file: File
  groupId?: GroupId
}

// --- Grades (GradeController) ---------------------------------------------------------

// The backend's GradeReviewResponse (teachers): GET/PUT /api/submissions/{id}/grade/review
// and POST …/grade/release. Not released while `releasedAt` is null.
export interface GradeReview {
  submissionId: SubmissionId
  /** 0–100, at most two decimals. */
  score: number
  feedback: string | null
  /** The teacher's passed / not passed verdict; null when not decided. */
  passed: boolean | null
  /**
   * When the grade becomes visible to the student by itself (the backend releases it
   * then); null when it waits to be published. Each grade has its own.
   */
  visibleFrom: string | null
  gradedByEnrollmentId: number
  gradedAt: string
  releasedByEnrollmentId: number | null
  releasedAt: string | null
  /** Send it back when changing or releasing the grade (optimistic locking). */
  version: number
}

// The backend's ReleasedGradeResponse (the student): GET /api/submissions/{id}/grade,
// 404 until the teacher releases it.
export interface ReleasedGrade {
  submissionId: SubmissionId
  score: number
  feedback: string | null
  /** The teacher's passed / not passed verdict; null when not decided. */
  passed: boolean | null
  releasedAt: string
}

// The backend's SaveGradeRequest: version null creates the grade; an update needs the
// version from GradeReview. A released grade cannot be changed, and only the latest
// submission can be graded.
export interface SaveGradeInput {
  submissionId: SubmissionId
  /** Cache scope: the content the submission belongs to. */
  contentId: ContentId
  score: number
  feedback: string
  /** Passed / not passed; null when not decided. */
  passed: boolean | null
  /**
   * ISO instant the grade becomes visible; null to wait for Publish. Needs `passed`; a
   * time that has passed publishes it at once.
   */
  visibleFrom: string | null
  version: number | null
}

// The backend's ReleaseGradeRequest.
export interface ReleaseGradeInput {
  submissionId: SubmissionId
  /** Cache scope: the content the submission belongs to. */
  contentId: ContentId
  version: number
}

/**
 * The backend's ReleaseAllGradesResponse: how many grades became visible, and how many
 * saved grades were left because they have no passed / not passed decision.
 */
export interface ReleaseAllGradesResult {
  released: number
  undecided: number
}
