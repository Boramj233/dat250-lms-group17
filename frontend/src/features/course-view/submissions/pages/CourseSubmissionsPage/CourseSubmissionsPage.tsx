import { paths } from '@shared/paths'
import PageHeader from '@shared/ui/PageHeader'
import { useGradeReviewsFor, useReviewSubmissionsFor } from '@domain/assignments/hooks'
import { isSubmittable } from '@domain/content/contentTypes'
import { useContents } from '@domain/content/hooks'
import type { Content, ContentSummary } from '@domain/content/types'
import { useCourseEnrollments } from '@domain/courses/hooks'
import type { CourseModule } from '@domain/modules/types'
import DataTable from '@shared/ui/DataTable'
import MandatoryLabel from '../../../components/MandatoryLabel'
import { deadlineColumn, moduleColumn, titleColumn, typeColumn } from '../../../components/contentColumns'
import { useCourseView } from '../../../courseContext'
import { useCourseItems } from '../../../useCourseItems'
import { submissionStats, type SubmissionStats } from '@domain/assignments/submissionStats'

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
  const { course } = useCourseView()
  const content = useCourseItems({ filter: item => isSubmittable(item) })
  const { items } = content
  const details = useContents(items.map(({ item }) => ({ type: item.type, id: item.id })))
  const submissions = useReviewSubmissionsFor(items.map(({ item }) => item.id))
  const allSubmissions = [...submissions.byContent.values()].flat()
  const grades = useGradeReviewsFor(allSubmissions.map(submission => submission.id))
  const members = useCourseEnrollments(course.id)

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
      {content.isPending ? (
        <p role="status">Loading…</p>
      ) : (
        <DataTable<Row>
          caption={`Submissions in ${course.title}`}
          rows={rows}
          rowKey={({ item }) => String(item.id)}
          empty="Nothing in this course is handed in: there are no assignments or code exercises yet."
          columns={[
            titleColumn(({ item }) => paths.courseSectionContent(course.id, 'submissions', item.id)),
            moduleColumn(course.id),
            typeColumn(),
            deadlineColumn(),
            {
              header: 'Answered',
              cell: row =>
                row.stats ? (
                  <span
                    className={
                      row.stats.students && row.stats.answered === row.stats.students ? 'data-table-strong' : undefined
                    }
                  >
                    {row.stats.answered}/{row.stats.students ?? '…'} students
                  </span>
                ) : (
                  noCounts
                ),
            },
            { header: 'Submissions', cell: row => (row.stats ? row.stats.submissions : noCounts) },
            {
              header: 'Graded',
              cell: row =>
                !row.stats ? (
                  noCounts
                ) : grades.isPending ? (
                  '…'
                ) : (
                  <span className={row.stats.graded < row.stats.submissions ? 'data-table-strong' : 'text-meta'}>
                    {row.stats.graded}/{row.stats.submissions}
                    {row.stats.graded > 0 && <span className="text-meta"> · {row.stats.released} published</span>}
                  </span>
                ),
            },
            { header: 'Mandatory', cell: ({ details }) => <MandatoryLabel content={details} /> },
          ]}
        />
      )}
      <p className="text-meta">
        Submissions counts the latest submission of each student or group; earlier attempts are on each submission.
      </p>
    </>
  )
}
