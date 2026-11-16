import { formatDateTime } from '@shared/lib/dateFormat'
import { TrailLink } from '@shared/navigation/Trail'
import { paths } from '@shared/paths'
import PageHeader from '@shared/ui/PageHeader'
import QueryState from '@shared/ui/QueryState'
import { contentTypeLabel } from '@domain/content/contentTypes'
import { formatScore } from '@domain/assignments/scoreFormat'
import { useMyGrades } from '@domain/grades/hooks'
import type { GradeEntry } from '@domain/grades/types'
import DataTable from '@shared/ui/DataTable'
import PassedBadge from '../../components/PassedBadge'
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
  const { modules } = useCourseView()
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
                // Opens under Grades: /courses/1/grades/101 (Course › Grades › item).
                <TrailLink to={paths.courseSectionContent(entry.courseId, 'grades', entry.contentId)}>
                  {entry.title}
                </TrailLink>
              ),
            },
            {
              // The module the item is in, as a link to it.
              header: 'Module',
              cell: entry => {
                const module = modules.find(m => String(m.id) === String(entry.moduleId))
                return module
                  ? <TrailLink to={paths.courseModule(entry.courseId, module.id)}>{module.title}</TrailLink>
                  : <span className="text-meta">–</span>
              },
            },
            { header: 'Type', cell: entry => <span className="text-meta">{contentTypeLabel(entry.type)}</span> },
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
              header: 'Score',
              cell: entry => entry.grade
                ? <span className="data-table-strong">{formatScore(entry.grade.score)}</span>
                : <span className="text-meta">Awaiting grade</span>,
            },
            {
              header: 'Passed',
              cell: entry => entry.grade?.passed != null
                ? <PassedBadge passed={entry.grade.passed} />
                : <span className="text-meta">–</span>,
            },
            {
              header: 'Feedback',
              cell: entry => entry.grade?.feedback
                ? <span className="data-table-text">{entry.grade.feedback}</span>
                : <span className="text-meta">–</span>,
            },
          ]}
        />
      )}
    </QueryState>
  )
}
