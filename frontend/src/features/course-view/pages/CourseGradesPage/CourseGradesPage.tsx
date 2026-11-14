import { formatDateTime } from '../../../../shared/lib/dateFormat'
import PageHeader from '../../../../shared/ui/PageHeader'
import QueryState from '../../../../shared/ui/QueryState'
import { contentTypeLabel } from '../../../content/contentTypes'
import { useMyGrades } from '../../../grades/hooks'
import { useCourseView } from '../../courseContext'
import '../../components/ContentList/ContentList.css'

/**
 * Students: their graded and awaiting-grade submissions in this course. Uses the same
 * request as the user's grades across all courses (GET /api/users/me/grades).
 */
export default function CourseGradesPage() {
  const { course, isTeacher } = useCourseView()
  return (
    <>
      <PageHeader title="Grades" kicker={course.title} />
      {isTeacher ? <p>Grading is not available in the course view yet.</p> : <MyGrades courseId={String(course.id)} />}
    </>
  )
}

function MyGrades({ courseId }: { courseId: string }) {
  const grades = useMyGrades()
  return (
    <QueryState
      query={grades}
      loading="Loading grades…"
      error="Could not load your grades. Try again later."
      empty="You have no submissions in this course yet."
      isEmpty={all => !all.some(entry => String(entry.courseId) === courseId)}
    >
      {all => (
        <ul className="content-list">
          {all.filter(entry => String(entry.courseId) === courseId).map(entry => (
            <li key={entry.submissionId}>
              <span className="content-list-meta">{contentTypeLabel(entry.type)}</span>
              <span className="content-list-title">
                {entry.title}
                <span className="content-list-note">
                  {' '}· Submitted {formatDateTime(new Date(entry.submittedAt))}{entry.late && ' (late)'}
                </span>
              </span>
              <strong>{entry.grade ? entry.grade.value : 'Awaiting grade'}</strong>
            </li>
          ))}
        </ul>
      )}
    </QueryState>
  )
}
