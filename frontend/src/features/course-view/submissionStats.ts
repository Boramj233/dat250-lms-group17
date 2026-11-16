import type { ContentSubmission } from '../assignments/types'
import type { CourseEnrollment } from '../courses/types'

export type SubmissionStats = {
  /** Students who have handed in (themselves or through their group). */
  answered: number
  /** Students in the course; undefined while the members are loading. */
  students?: number
  /** All submissions, every attempt. */
  submissions: number
  /** Submissions with a grade (published or not). */
  graded: number
  /** Ids (strings) of the students who have handed in. */
  answeredIds: Set<string>
}

/** Counts for one assignment or coding exercise, for the teacher's Submissions pages. */
export function submissionStats(
  submissions: ContentSubmission[],
  members: CourseEnrollment[] | undefined,
): SubmissionStats {
  const studentIds = members && new Set(members.filter(m => m.role === 'STUDENT').map(m => String(m.userId)))
  const answeredIds = new Set<string>()
  for (const submission of submissions) {
    // A group submission counts for every member of the group.
    const ids = submission.group
      ? submission.group.members.map(member => String(member.userId))
      : [String(submission.student.userId)]
    for (const id of ids) if (!studentIds || studentIds.has(id)) answeredIds.add(id)
  }
  return {
    answered: answeredIds.size,
    students: studentIds?.size,
    submissions: submissions.length,
    graded: submissions.filter(submission => submission.grade).length,
    answeredIds,
  }
}

/** The students of the course, sorted by last name. */
export function studentsOf(members: CourseEnrollment[] | undefined): CourseEnrollment[] {
  return (members ?? [])
    .filter(member => member.role === 'STUDENT')
    .sort((a, b) => a.lastName.localeCompare(b.lastName) || a.firstName.localeCompare(b.firstName))
}
