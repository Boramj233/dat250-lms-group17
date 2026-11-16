import { Link, useParams } from 'react-router'
import { formatDateTime } from '@shared/lib/dateFormat'
import { Breadcrumbs } from '@shared/navigation/Trail'
import { paths } from '@shared/paths'
import PageHeader from '@shared/ui/PageHeader'
import QueryState from '@shared/ui/QueryState'
import { useGradeReview, useReviewHistory, useReviewSubmission, useReviewSubmissions } from '@domain/assignments/hooks'
import { formatScore } from '@domain/assignments/scoreFormat'
import type { ReviewSubmission } from '@domain/assignments/types'
import { isSubmittable } from '@domain/content/contentTypes'
import { useContent } from '@domain/content/hooks'
import DetailLayout, { Card } from '../../../components/DetailLayout'
import PassedBadge from '../../../components/PassedBadge'
import SubmissionAnswer from '../../../components/SubmissionAnswer'
import { useCourseView } from '../../../courseContext'
import { useCourseItem } from '../../../useCourseItems'
import { usePeopleNames } from '../../../usePeopleNames'
import GradeCard from './GradeCard'
import SubmissionHistory from './SubmissionHistory'
import './SubmissionGradingPage.css'

/**
 * Teachers: one submission with its earlier attempts and a form to grade it (a score
 * from 0 to 100 and feedback). Saving keeps the grade from the student; releasing shows
 * it to them, after which it cannot be changed. Only the latest submission can be graded.
 */
export default function SubmissionGradingPage() {
  const { contentId = '', submissionId = '' } = useParams()
  const { course } = useCourseView()
  const item = useCourseItem(contentId).found?.item
  const details = useContent(item?.type, contentId)
  const submission = useReviewSubmission(submissionId)
  const history = useReviewHistory(submissionId)
  const grade = useGradeReview(submissionId)
  // The latest submissions of the item, for Previous / Next.
  const latest = useReviewSubmissions(contentId)
  const people = usePeopleNames(course.id)

  const studentName = submission.data ? people.nameOf(submission.data.studentUserId) : '…'
  const crumbs = [
    { label: course.title, to: paths.course(course.id) },
    { label: 'Submissions', to: paths.courseSection(course.id, 'submissions') },
    { label: item?.title ?? '…', to: paths.courseSectionContent(course.id, 'submissions', contentId) },
    { label: studentName },
  ]
  const deadline = isSubmittable(details.data) ? details.data.deadline : undefined
  // The newest submission in the history is the one that can be graded.
  const newest = history.data?.[0]
  const isLatest = newest ? String(newest.id) === submissionId : undefined
  const list = latest.data ?? []
  const index = list.findIndex(s => String(s.id) === submissionId)

  /** The details panel: who handed in, when, the deadline and the grade so far. */
  function panel(data: ReviewSubmission) {
    const email = people.members?.find(m => String(m.userId) === String(data.studentUserId))?.email
    return (
      <dl className="submittable-facts">
        <dt>Student</dt>
        <dd>
          {studentName}
          {email && (
            <>
              <br />
              <span className="text-meta">{email}</span>
            </>
          )}
        </dd>
        {data.groupId !== null && (
          <>
            <dt>Handed in for group</dt>
            <dd>
              <strong>{people.groupLabel(data.groupId)}</strong>
              <ul className="submittable-members">
                {data.members.map(member => (
                  <li key={member.userId}>{people.nameOf(member.userId)}</li>
                ))}
              </ul>
            </dd>
          </>
        )}
        <dt>Submitted</dt>
        <dd>
          {formatDateTime(new Date(data.submittedAt))}
          {data.late && <span className="submittable-late"> (late)</span>}
          {history.data && history.data.length > 1 && (
            <span className="text-meta"> · {history.data.length} submissions in all</span>
          )}
        </dd>
        <dt>Deadline</dt>
        <dd>{deadline === undefined ? '…' : deadline ? formatDateTime(new Date(deadline)) : 'No deadline'}</dd>
        <dt>Score</dt>
        <dd>
          {grade.data ? <strong>{formatScore(grade.data.score)}</strong> : 'Not graded yet'}
          {grade.data && (
            <>
              {' '}
              <PassedBadge passed={grade.data.passed} />
            </>
          )}
          {grade.data && (
            <span className="text-meta">
              <br />
              {grade.data.releasedAt
                ? `Visible to the student since ${formatDateTime(new Date(grade.data.releasedAt))}`
                : grade.data.visibleFrom
                  ? `Visible to the student from ${formatDateTime(new Date(grade.data.visibleFrom))}`
                  : 'Saved, not visible to the student'}
            </span>
          )}
        </dd>
      </dl>
    )
  }

  return (
    <>
      <Breadcrumbs items={crumbs} />
      <QueryState query={submission} loading="Loading the submission…" error="Could not load the submission.">
        {data => (
          <>
            <PageHeader title={studentName} kicker={item?.title ?? 'Submission'} />
            <DetailLayout panelLabel="Submission details" panel={panel(data)}>
              <Card id="answer-title" title="Submission">
                <SubmissionAnswer submission={data} />
              </Card>

              {grade.isPending || isLatest === undefined ? (
                <p role="status">Loading the grade…</p>
              ) : grade.error ? (
                <p role="alert">Could not load the grade.</p>
              ) : (
                <GradeCard
                  key={data.id}
                  submission={data}
                  contentId={contentId}
                  courseId={course.id}
                  grade={grade.data ?? null}
                  isLatest={isLatest}
                  newestId={newest?.id}
                />
              )}

              {history.data && history.data.length > 1 && (
                <SubmissionHistory
                  courseId={course.id}
                  contentId={contentId}
                  history={history.data}
                  currentId={submissionId}
                />
              )}

              <nav className="grading-nav" aria-label="Other submissions">
                {index > 0 ? (
                  <Link to={paths.courseSubmission(course.id, contentId, list[index - 1].id)}>‹ Previous</Link>
                ) : (
                  <span />
                )}
                <Link to={paths.courseSectionContent(course.id, 'submissions', contentId)}>All submissions</Link>
                {index >= 0 && index < list.length - 1 ? (
                  <Link to={paths.courseSubmission(course.id, contentId, list[index + 1].id)}>Next ›</Link>
                ) : (
                  <span />
                )}
              </nav>
            </DetailLayout>
          </>
        )}
      </QueryState>
    </>
  )
}
