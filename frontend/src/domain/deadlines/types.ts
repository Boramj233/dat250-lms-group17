import type { ContentId, CourseId, ModuleId } from '@shared/types'

// Content types that have a deadline in the backend (Assignment, CodingExercise).
export type DeadlineContentType = 'ASSIGNMENT' | 'CODING_EXERCISE'

/** One upcoming deadline in a course the user is enrolled in. */
export interface UpcomingDeadline {
  contentId: ContentId
  type: DeadlineContentType
  title: string
  /** ISO 8601 instant, as the backend's `Instant deadline`. */
  deadline: string
  allowLateSubmission: boolean
  /** Assignments only: whether it is mandatory. Left out for coding exercises. */
  mandatory?: boolean
  courseId: CourseId
  courseTitle: string
  moduleId: ModuleId
  // TODO: Add `submitted: boolean` (has the user handed in?) once the backend can
  // include it from the submission package, and show it in DeadlineList.
}
