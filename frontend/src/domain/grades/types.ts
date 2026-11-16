import type { ContentId, CourseId, ModuleId } from '@shared/types'
import type { ReleasedGrade, SubmissionId } from '@domain/assignments/types'

/**
 * One assignment or coding exercise the logged-in user has submitted: either graded
 * (grade released) or waiting for a grade.
 */
export interface GradeEntry {
  contentId: ContentId
  type: 'ASSIGNMENT' | 'CODING_EXERCISE'
  title: string
  courseId: CourseId
  courseTitle: string
  moduleId: ModuleId
  /** The latest submission, which is the one that is graded. */
  submissionId: SubmissionId
  /** ISO 8601 instant. */
  submittedAt: string
  late: boolean
  /** The released grade, as GET /api/submissions/{id}/grade; null while not released. */
  grade: Omit<ReleasedGrade, 'submissionId'> | null
}
