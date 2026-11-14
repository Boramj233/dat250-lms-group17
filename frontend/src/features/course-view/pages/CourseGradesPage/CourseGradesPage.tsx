import { formatDateTime } from '../../../../shared/lib/dateFormat'
import { TrailLink } from '../../../../shared/navigation/Trail'
import { paths } from '../../../../shared/paths'
import PageHeader from '../../../../shared/ui/PageHeader'
import QueryState from '../../../../shared/ui/QueryState'
import { contentTypeLabel } from '../../../content/contentTypes'
import { useMyGrades } from '../../../grades/hooks'
import type { GradeEntry } from '../../../grades/types'
import DataTable from '../../components/DataTable'
import { useCourseView } from '../../courseContext'

/**
 * Students: their graded and awaiting-grade submissions in this course, as a table.
 * Uses the same request as the user's grades across all courses (GET /api/users/me/grades).
 */
export default function CourseGradesPage() {
  const { course, isTeacher } = useCourseView()
  return (
    <>
      <PageHeader title="Grades" kicker={course.title} />
      {isTeacher
        ? <p>Grading is not available in the course view yet.</p>
        : <MyGrades courseId={String(course.id)} courseTitle={course.title} />}
    </>
  )
}

function MyGrades({ courseId, courseTitle }: { courseId: string, courseTitle: string }) {
  const grades = useMyGrades()
  return (
    <QueryState query={grades} loading="Loading grades…" error="Could not load your grades. Try again later.">
      {all => (
        <DataTable<GradeEntry>
          caption={`Your grades in ${courseTitle}`}
          rows={all.filter(entry => String(entry.courseId) === courseId)}
          rowKey={entry => String(entry.submissionId)}
          empty="You have no submissions in this course yet."
          columns={[
            {
              header: 'Title',
              cell: entry => (
                <TrailLink to={paths.courseContent(entry.courseId, entry.moduleId, entry.contentId)}>
                  {entry.title}
                </TrailLink>
              ),
            },
            { header: 'Type', cell: entry => <span className="data-table-muted">{contentTypeLabel(entry.type)}</span> },
            {
              header: 'Submitted',
              cell: entry => (
                <>
                  {formatDateTime(new Date(entry.submittedAt))}
                  {entry.late && <span className="data-table-tag">late</span>}
                </>
              ),
            },
            {
              header: 'Grade',
              cell: entry => entry.grade
                ? <span className="data-table-strong">{entry.grade.value}</span>
                : <span className="data-table-muted">Awaiting grade</span>,
            },
            {
              header: 'Feedback',
              cell: entry => entry.grade?.feedback
                ? <span className="data-table-text">{entry.grade.feedback}</span>
                : <span className="data-table-muted">–</span>,
            },
          ]}
        />
      )}
    </QueryState>
  )
}
