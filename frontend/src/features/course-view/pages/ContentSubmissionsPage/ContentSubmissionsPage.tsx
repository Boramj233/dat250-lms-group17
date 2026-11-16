import { useState } from 'react'
import { useParams } from 'react-router'
import { errorMessage } from '@shared/api/errorMessage'
import { formatDateTime } from '@shared/lib/dateFormat'
import { Breadcrumbs, TrailLink } from '@shared/navigation/Trail'
import { paths } from '@shared/paths'
import PageHeader from '@shared/ui/PageHeader'
import QueryState from '@shared/ui/QueryState'
import { useConfirm } from '@shared/ui/ConfirmDialog'
import { useGradeReviewsFor, useReleaseGrade, useReviewSubmissions } from '@domain/assignments/hooks'
import { formatScore } from '@domain/assignments/scoreFormat'
import type { ReviewSubmission } from '@domain/assignments/types'
import { contentTypeLabel, isSubmittable } from '@domain/content/contentTypes'
import { useContent } from '@domain/content/hooks'
import DataTable from '../../components/DataTable'
import PassedBadge from '../../components/PassedBadge'
import { useCourseView } from '../../courseContext'
import { useCourseItem } from '../../useCourseItems'
import { studentsOf, submissionKindLabel, submissionStats } from '../../submissionStats'
import { usePeopleNames } from '../../usePeopleNames'
import GradePublishing from './GradePublishing'
import './ContentSubmissionsPage.css'

/**
 * Teachers: the latest submission of each student or group for one assignment or
 * coding exercise, newest first, and the students who have not handed in. A row opens
 * the submission (with earlier attempts) to grade it. Each grade is visible to the student
 * from its own visible date, or from when it is published (one by one or all at once).
 */
export default function ContentSubmissionsPage() {
  const { contentId = '' } = useParams()
  const { course, isTeacher } = useCourseView()
  const item = useCourseItem(contentId).found?.item
  const details = useContent(item?.type, contentId)
  const submissions = useReviewSubmissions(contentId)
  const grades = useGradeReviewsFor((submissions.data ?? []).map(submission => submission.id))
  const people = usePeopleNames(course.id)
  const release = useReleaseGrade()
  const [confirm, confirmDialog] = useConfirm()
  const [publishError, setPublishError] = useState<string | null>(null)

  if (!isTeacher) return <p role="alert">Only teachers of {course.title} can see its submissions.</p>

  const crumbs = [
    { label: course.title, to: paths.course(course.id) },
    { label: 'Submissions', to: paths.courseSection(course.id, 'submissions') },
    { label: item?.title ?? '…' },
  ]
  const deadline = isSubmittable(details.data)
    ? details.data.deadline
    : undefined
  const gradeOf = (submission: ReviewSubmission) => grades.bySubmission.get(String(submission.id))

  async function publish(submission: ReviewSubmission) {
    const grade = gradeOf(submission)
    if (!grade) return
    const name = people.nameOf(submission.studentUserId)
    const confirmed = await confirm({
      title: `Publish the grade for ${name}?`,
      message: `${submission.groupId !== null ? 'Everyone in the group sees' : `${name} sees`} the score and feedback right away. A published grade cannot be changed.`,
      confirmLabel: 'Publish',
    })
    if (!confirmed) return
    setPublishError(null)
    release.mutate({ submissionId: submission.id, contentId, version: grade.version }, {
      onError: error => setPublishError(errorMessage(error, {
        409: `The grade for ${name} was changed or published in the meantime. Reload the page and try again.`,
      }, `Could not publish the grade for ${name}. Try again.`)),
    })
  }

  return (
    <>
      <Breadcrumbs items={crumbs} />
      <PageHeader title={item?.title ?? '…'} kicker={item ? `Submissions · ${contentTypeLabel(item.type)}` : 'Submissions'}>
        {deadline !== undefined && (
          <p className="content-submissions-facts">
            {deadline ? `Deadline ${formatDateTime(new Date(deadline))}` : 'No deadline'}
          </p>
        )}
      </PageHeader>

      <QueryState query={submissions} loading="Loading submissions…" error="Could not load the submissions.">
        {all => {
          const stats = submissionStats(all, people.members, gradeOf)
          const missing = studentsOf(people.members).filter(student => !stats.answeredIds.has(String(student.userId)))
          return (
            <>
              <p className="content-submissions-facts">
                <strong>{stats.answered}/{stats.students ?? '…'}</strong> students have answered
                {' · '}{stats.submissions} {stats.submissions === 1 ? 'submission' : 'submissions'}
                {' · '}{stats.graded} graded, {stats.released} published
              </p>
              <GradePublishing
                contentId={contentId}
                unpublished={grades.isPending ? undefined : all.filter(s => { const g = gradeOf(s); return g && !g.releasedAt }).length}
              />
              {publishError && <p className="form-error" role="alert">{publishError}</p>}
              <DataTable<ReviewSubmission>
                caption={`Submissions for ${item?.title ?? 'this content'}`}
                rows={all}
                rowKey={submission => String(submission.id)}
                empty="No one has handed in yet."
                columns={[
                  {
                    header: 'Student',
                    cell: submission => (
                      <>
                        <TrailLink to={paths.courseSubmission(course.id, contentId, submission.id)}>
                          {people.nameOf(submission.studentUserId)}
                        </TrailLink>
                        {submission.groupId !== null && (
                          <span className="data-table-tag">for {people.groupLabel(submission.groupId)}</span>
                        )}
                      </>
                    ),
                  },
                  {
                    header: 'Submitted',
                    cell: submission => (
                      <>
                        {formatDateTime(new Date(submission.submittedAt))}
                        {submission.late && <span className="data-table-tag">late</span>}
                      </>
                    ),
                  },
                  {
                    header: 'Answer',
                    cell: submission => <span className="data-table-muted">{submissionKindLabel(submission)}</span>,
                  },
                  {
                    header: 'Score',
                    cell: submission => {
                      const grade = gradeOf(submission)
                      if (grade === undefined) return '…'
                      return grade
                        ? <span className="data-table-strong">{formatScore(grade.score)}</span>
                        : <span className="data-table-muted">Not graded</span>
                    },
                  },
                  {
                    header: 'Passed',
                    cell: submission => {
                      const grade = gradeOf(submission)
                      if (grade === undefined) return '…'
                      return grade?.passed != null
                        ? <PassedBadge passed={grade.passed} />
                        : <span className="data-table-muted">–</span>
                    },
                  },
                  {
                    // When the student sees the grade: the time it was published, or the
                    // grade's own visible date while it waits.
                    header: 'Visible from',
                    cell: submission => {
                      const grade = gradeOf(submission)
                      if (!grade) return <span className="data-table-muted">–</span>
                      if (grade.releasedAt) {
                        return formatDateTime(new Date(grade.releasedAt))
                      }
                      const busy = release.isPending && String(release.variables?.submissionId) === String(submission.id)
                      return (
                        <span className="content-submissions-publish">
                          <span className="data-table-muted">
                            {grade.visibleFrom ? `Scheduled ${formatDateTime(new Date(grade.visibleFrom))}` : 'Not published'}
                          </span>
                          <button type="button" className="button-secondary data-table-button" disabled={release.isPending}
                            onClick={() => void publish(submission)}>
                            {busy ? 'Publishing…' : 'Publish now'}
                            <span className="visually-hidden"> the grade for {people.nameOf(submission.studentUserId)}</span>
                          </button>
                        </span>
                      )
                    },
                  },
                ]}
              />

              {people.members && (
                <section className="content-submissions-missing" aria-labelledby="missing-title">
                  <h2 id="missing-title" className="section-title">Not handed in ({missing.length})</h2>
                  {missing.length === 0
                    ? <p>Every student has handed in.</p>
                    : (
                      <ul>
                        {missing.map(student => (
                          <li key={student.userId}>{student.firstName} {student.lastName}</li>
                        ))}
                      </ul>
                    )}
                </section>
              )}
            </>
          )
        }}
      </QueryState>
      {confirmDialog}
    </>
  )
}
