import { useMyGrades } from '../../../data/grades/hooks/queries/useMyGrades'
import GradeSections from '../../components/GradeSections'
import { CourseLink } from '../CourseLink'
import { useCourseContext } from '../courseContext'
import './CoursePages.css'

/** The user's graded and awaiting-grade assignments in this course. */
export default function CourseGradesPage() {
  const { course } = useCourseContext()
  const grades = useMyGrades()
  const entries = grades.data?.filter(entry => String(entry.courseId) === String(course.id))

  return (
    <>
      <header className="course-header">
        <h1>Grades</h1>
      </header>
      {grades.isPending && <p role="status">Loading grades…</p>}
      {grades.error && <p role="alert">Could not load your grades.</p>}
      {entries?.length === 0 && <p>You have not submitted any assignments in this course yet.</p>}
      {entries && entries.length > 0 && <GradeSections entries={entries} link={CourseLink} />}
    </>
  )
}
