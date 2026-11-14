import { useHasAccess } from '../../../../shared/auth/useAuth'
import { TrailPage } from '../../../../shared/navigation/Trail'
import CourseManagerDashboard from './CourseManagerDashboard'
import StudentDashboard from './StudentDashboard'
import './DashboardPage.css'

/**
 * The page after login. Course managers (and admins) see the courses they teach and
 * can create courses; everyone else sees this term's courses and upcoming deadlines.
 * Only shown to logged-in users (see AuthGate).
 */
export default function DashboardPage() {
  const isCourseManager = useHasAccess('COURSE_MANAGER')
  return (
    <TrailPage label="Dashboard">
      <div className="dashboard">
        <h1>Dashboard</h1>
        {isCourseManager ? <CourseManagerDashboard /> : <StudentDashboard />}
      </div>
    </TrailPage>
  )
}
