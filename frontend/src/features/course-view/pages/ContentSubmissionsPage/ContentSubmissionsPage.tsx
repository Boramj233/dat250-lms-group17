import { useParams } from 'react-router'
import { formatDateTime } from '../../../../shared/lib/dateFormat'
import { Breadcrumbs, TrailLink } from '../../../../shared/navigation/Trail'
import { paths } from '../../../../shared/paths'
import PageHeader from '../../../../shared/ui/PageHeader'
import QueryState from '../../../../shared/ui/QueryState'
import { useGradeReviewsFor, useReviewSubmissions } from '../../../assignments/hooks'
import { formatScore } from '../../../assignments/scoreFormat'
import type { ReviewSubmission } from '../../../assignments/types'
import { contentTypeLabel } from '../../../content/contentTypes'
import { useContent, useCourseContent } from '../../../content/hooks'
import DataTable from '../../components/DataTable'
import PassedBadge from '../../components/PassedBadge'
import { useCourseView } from '../../courseContext'
import { studentsOf, submissionKindLabel, submissionStats } from '../../submissionStats'
import { usePeopleNames } from '../../usePeopleNames'
import './ContentSubmissionsPage.css'

/**
 * Teachers: the latest submission of each student or group for one assignment or
 * coding exercise, newest first, and the students who have not handed in. A row opens
 * the submission (with earlier attempts) to grade it.
 */
export default function ContentSubmissionsPage() {
  const { contentId = '' } = useParams()
  const { course, modules, isTeacher } = useCourseView()
  const content = useCourseContent(course.id, modules.map(module => module.id))
  const item = modules.flatMap(module => content.byModule.get(String(module.id)) ?? [])
    .find(entry => String(entry.id) === contentId)
  const details = useContent(item?.type, contentId)
  const submissions = useReviewSubmissions(contentId)
  const grades = useGradeReviewsFor((submissions.data ?? []).map(submission => submission.id))
  const people = usePeopleNames(course.id)

  if (!isTeacher) return <p role="alert">Only teachers of {course.title} can see its submissions.</p>

  const crumbs = [
    { label: course.title, to: paths.course(course.id) },
    { label: 'Submissions', to: paths.courseSection(course.id, 'submissions') },
    { label: item?.title ?? '…' },
  ]
  const deadline = details.data && (details.data.type === 'ASSIGNMENT' || details.data.type === 'CODING_EXERCISE')
    ? details.data.deadline
    : undefined
  const gradeOf = (submission: ReviewSubmission) => grades.bySubmission.get(String(submission.id))

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
                {' · '}{stats.graded} graded, {stats.released} released
              </p>
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
                    header: 'Released',
                    cell: submission => {
                      const grade = gradeOf(submission)
                      if (!grade) return <span className="data-table-muted">–</span>
                      return grade.releasedAt ? 'Released' : <span className="data-table-tag">Not released yet</span>
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
    </>
  )
}
