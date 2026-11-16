import type { GradeReview, ReviewSubmission } from '@domain/assignments/types'
import type { CourseEnrollment } from '@domain/courses/types'

export type SubmissionStats = {
  /** Students who have handed in (themselves or through their group). */
  answered: number
  /** Students in the course; undefined while the members are loading. */
  students?: number
  /** Latest submissions: one per student or group. */
  submissions: number
  /** Of those, how many have a grade (released or not). */
  graded: number
  /** Of those, how many grades are released to the students. */
  released: number
  /** Ids (strings) of the students who have handed in. */
  answeredIds: Set<string>
}

/**
 * Counts for one assignment or coding exercise, for the teacher's Submissions pages,
 * from the latest submission per student or group (GET …/submissions/review) and
 * their grades (undefined while loading, null when not graded).
 */
export function submissionStats(
  submissions: ReviewSubmission[],
  members: CourseEnrollment[] | undefined,
  gradeOf: (submission: ReviewSubmission) => GradeReview | null | undefined,
): SubmissionStats {
  const studentIds = members && new Set(members.filter(m => m.role === 'STUDENT').map(m => String(m.userId)))
  const answeredIds = new Set<string>()
  for (const submission of submissions) {
    // A group submission counts for every member of the group when it was handed in.
    const ids = submission.groupId !== null && submission.members.length > 0
      ? submission.members.map(member => String(member.userId))
      : [String(submission.studentUserId)]
    for (const id of ids) if (!studentIds || studentIds.has(id)) answeredIds.add(id)
  }
  const grades = submissions.map(gradeOf)
  return {
    answered: answeredIds.size,
    students: studentIds?.size,
    submissions: submissions.length,
    graded: grades.filter(grade => grade).length,
    released: grades.filter(grade => grade?.releasedAt).length,
    answeredIds,
  }
}

/** The students of the course, sorted by last name. */
export function studentsOf(members: CourseEnrollment[] | undefined): CourseEnrollment[] {
  return (members ?? [])
    .filter(member => member.role === 'STUDENT')
    .sort((a, b) => a.lastName.localeCompare(b.lastName) || a.firstName.localeCompare(b.firstName))
}

/** "Text", "Repository" or "PDF file". */
export function submissionKindLabel(submission: { type: ReviewSubmission['type'] }): string {
  return submission.type === 'GIT_URL' ? 'Repository' : submission.type === 'FILE' ? 'PDF file' : 'Text'
}
