import { useHasAccess } from '../../../data/auth/useAuth'
import { TrailPage } from '../../navigation/Trail'
import CourseManagerOverview from './CourseManagerOverview'
import StudentOverview from './StudentOverview'
import './OverviewPage.css'

/**
 * The page after login. Course managers (and admins) see the courses they teach and
 * can create courses; everyone else sees this term's courses and upcoming deadlines.
 * Only shown to logged-in users (see AuthGate).
 */
export default function OverviewPage() {
  const isCourseManager = useHasAccess('COURSE_MANAGER')
  return (
    <TrailPage label="Overview">
      <div className="overview">
        <h1>Overview</h1>
        {isCourseManager ? <CourseManagerOverview /> : <StudentOverview />}
      </div>
    </TrailPage>
  )
}
