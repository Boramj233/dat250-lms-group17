import { useParams } from 'react-router'
import { formatDateTime } from '../../../../shared/lib/dateFormat'
import { Breadcrumbs, TrailLink } from '../../../../shared/navigation/Trail'
import { paths } from '../../../../shared/paths'
import PageHeader from '../../../../shared/ui/PageHeader'
import QueryState from '../../../../shared/ui/QueryState'
import { useContentSubmissions } from '../../../assignments/hooks'
import type { ContentSubmission } from '../../../assignments/types'
import { contentTypeLabel } from '../../../content/contentTypes'
import { useContent, useCourseContent } from '../../../content/hooks'
import { useCourseEnrollments } from '../../../courses/hooks'
import DataTable from '../../components/DataTable'
import { useCourseView } from '../../courseContext'
import { studentsOf, submissionStats } from '../../submissionStats'
import './ContentSubmissionsPage.css'

/**
 * Teachers: every submission for one assignment or coding exercise, newest first, and
 * the students who have not handed in. A row opens the submission to grade it.
 */
export default function ContentSubmissionsPage() {
  const { contentId = '' } = useParams()
  const { course, modules, isTeacher } = useCourseView()
  const content = useCourseContent(course.id, modules.map(module => module.id))
  const item = modules.flatMap(module => content.byModule.get(String(module.id)) ?? [])
    .find(entry => String(entry.id) === contentId)
  const details = useContent(item?.type, contentId)
  const submissions = useContentSubmissions(contentId)
  const members = useCourseEnrollments(course.id)

  if (!isTeacher) return <p role="alert">Only teachers of {course.title} can see its submissions.</p>

  const crumbs = [
    { label: course.title, to: paths.course(course.id) },
    { label: 'Submissions', to: paths.courseSection(course.id, 'submissions') },
    { label: item?.title ?? '…' },
  ]
  const deadline = details.data && (details.data.type === 'ASSIGNMENT' || details.data.type === 'CODING_EXERCISE')
    ? details.data.deadline
    : undefined

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
          const stats = submissionStats(all, members.data)
          const missing = studentsOf(members.data).filter(student => !stats.answeredIds.has(String(student.userId)))
          return (
            <>
              <p className="content-submissions-facts">
                <strong>{stats.answered}/{stats.students ?? '…'}</strong> students have answered
                {' · '}{stats.submissions} {stats.submissions === 1 ? 'submission' : 'submissions'}
                {' · '}{stats.graded} graded
              </p>
              <DataTable<ContentSubmission>
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
                          {submission.student.firstName} {submission.student.lastName}
                        </TrailLink>
                        {submission.group && <span className="data-table-tag">for {submission.group.name}</span>}
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
                    cell: submission => (
                      <span className="data-table-muted">{submission.type === 'GIT_URL' ? 'Repository' : 'Text'}</span>
                    ),
                  },
                  {
                    header: 'Grade',
                    cell: submission => submission.grade
                      ? <span className="data-table-strong">{submission.grade.value}</span>
                      : <span className="data-table-muted">Not graded</span>,
                  },
                  {
                    header: 'Published',
                    cell: submission => !submission.grade
                      ? <span className="data-table-muted">–</span>
                      : submission.grade.publishedAt
                        ? 'Published'
                        : <span className="data-table-tag">Not published yet</span>,
                  },
                ]}
              />

              {members.data && (
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
