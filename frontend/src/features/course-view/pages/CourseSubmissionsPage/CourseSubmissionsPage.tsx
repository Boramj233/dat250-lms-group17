import { formatDateTime } from '../../../../shared/lib/dateFormat'
import { TrailLink } from '../../../../shared/navigation/Trail'
import { paths } from '../../../../shared/paths'
import PageHeader from '../../../../shared/ui/PageHeader'
import { useGradeReviewsFor, useReviewSubmissionsFor } from '../../../assignments/hooks'
import { contentTypeLabel } from '../../../content/contentTypes'
import { useContents, useCourseContent } from '../../../content/hooks'
import type { Content, ContentSummary } from '../../../content/types'
import { useCourseEnrollments } from '../../../courses/hooks'
import type { CourseModule } from '../../../modules/types'
import DataTable from '../../components/DataTable'
import MandatoryLabel from '../../components/MandatoryLabel'
import VisibleFromTag from '../../components/VisibleFromTag'
import { useCourseView } from '../../courseContext'
import { submissionStats, type SubmissionStats } from '../../submissionStats'

type Row = {
  item: ContentSummary
  module: CourseModule
  /** Undefined while loading. */
  details?: Content
  /** Undefined while loading or if it failed. */
  stats?: SubmissionStats
}

/**
 * Teachers: everything students hand in (assignments and coding exercises), with the
 * deadline, how many students have answered, the latest submissions and how many of
 * them are graded and published, and whether it is mandatory. A row opens its submissions.
 */
export default function CourseSubmissionsPage() {
  const { course, modules, isTeacher } = useCourseView()
  const content = useCourseContent(course.id, isTeacher ? modules.map(module => module.id) : [])
  const items = modules.flatMap(module =>
    (content.byModule.get(String(module.id)) ?? [])
      .filter(item => item.type === 'ASSIGNMENT' || item.type === 'CODING_EXERCISE')
      .map(item => ({ item, module })))
  const details = useContents(items.map(({ item }) => ({ type: item.type, id: item.id })))
  const submissions = useReviewSubmissionsFor(items.map(({ item }) => item.id))
  const allSubmissions = [...submissions.byContent.values()].flat()
  const grades = useGradeReviewsFor(allSubmissions.map(submission => submission.id))
  const members = useCourseEnrollments(course.id)

  if (!isTeacher) return <p role="alert">Only teachers of {course.title} can see its submissions.</p>

  const rows: Row[] = items.map(row => {
    const list = submissions.byContent.get(String(row.item.id))
    return {
      ...row,
      details: details.byId.get(String(row.item.id)),
      stats: list && submissionStats(list, members.data, s => grades.bySubmission.get(String(s.id))),
    }
  })
  // A cell without counts: still loading, or the list could not be loaded.
  const noCounts = submissions.isPending ? '…' : '–'

  return (
    <>
      <PageHeader title="Submissions" kicker={course.title} />
      {content.isPending
        ? <p role="status">Loading…</p>
        : (
          <DataTable<Row>
            caption={`Submissions in ${course.title}`}
            rows={rows}
            rowKey={({ item }) => String(item.id)}
            empty="Nothing in this course is handed in: there are no assignments or code exercises yet."
            columns={[
              {
                header: 'Title',
                cell: ({ item, module }) => (
                  <>
                    <TrailLink to={paths.courseSectionContent(course.id, 'submissions', item.id)}>{item.title}</TrailLink>
                    {item.active === false && <span className="data-table-tag">Hidden from students</span>}
                    <VisibleFromTag item={item} module={module} className="data-table-tag" />
                  </>
                ),
              },
              {
                header: 'Module',
                cell: ({ module }) => <TrailLink to={paths.courseModule(course.id, module.id)}>{module.title}</TrailLink>,
              },
              { header: 'Type', cell: ({ item }) => <span className="data-table-muted">{contentTypeLabel(item.type)}</span> },
              { header: 'Deadline', cell: ({ details }) => <Deadline content={details} /> },
              {
                header: 'Answered',
                cell: row => row.stats
                  ? <span className={row.stats.students && row.stats.answered === row.stats.students ? 'data-table-strong' : undefined}>
                      {row.stats.answered}/{row.stats.students ?? '…'} students
                    </span>
                  : noCounts,
              },
              { header: 'Submissions', cell: row => row.stats ? row.stats.submissions : noCounts },
              {
                header: 'Graded',
                cell: row => !row.stats ? noCounts : grades.isPending ? '…' : (
                  <span className={row.stats.graded < row.stats.submissions ? 'data-table-strong' : 'data-table-muted'}>
                    {row.stats.graded}/{row.stats.submissions}
                    {row.stats.graded > 0 && <span className="data-table-muted"> · {row.stats.released} published</span>}
                  </span>
                ),
              },
              { header: 'Mandatory', cell: ({ details }) => <MandatoryLabel content={details} /> },
            ]}
          />
        )}
      <p className="data-table-muted">
        Submissions counts the latest submission of each student or group; earlier attempts are on each submission.
      </p>
    </>
  )
}

function Deadline({ content }: { content?: Content }) {
  if (!content) return '…'
  if (content.type !== 'ASSIGNMENT' && content.type !== 'CODING_EXERCISE') return '–'
  return content.deadline ? formatDateTime(new Date(content.deadline)) : 'No deadline'
}

