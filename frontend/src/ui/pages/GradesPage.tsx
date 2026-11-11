import { useMyGrades } from '../../data/grades/hooks/queries/useMyGrades'
import GradeSections from '../components/GradeSections'
import { TrailLink, TrailPage } from '../navigation/Trail'

/** The user's graded and awaiting-grade assignments in all courses. */
export default function GradesPage() {
  const grades = useMyGrades()
  return (
    <TrailPage label="Grades">
      <h1>Grades</h1>
      {grades.isPending && <p role="status">Loading grades…</p>}
      {grades.error && <p role="alert">Could not load your grades.</p>}
      {grades.data?.length === 0 && <p>You have not submitted any assignments yet.</p>}
      {grades.data && grades.data.length > 0 && (
        <GradeSections entries={grades.data} showCourse link={TrailLink} />
      )}
    </TrailPage>
  )
}
