import { useCurrentUser } from '../../data/auth/useAuth'
import { useMyCourses } from '../../data/courses/hooks/queries/useMyCourses'
import { useUpcomingDeadlines } from '../../data/deadlines/hooks/queries/useUpcomingDeadlines'
import CourseCard from '../components/CourseCard'
import DeadlineList from '../components/DeadlineList'
import { byNewest } from '../components/courseFormat'
import './OverviewPage.css'

/**
 * The page after login: the user's courses with a link to each, and the upcoming
 * deadlines from all of them in one list. Only shown to logged-in users (see AuthGate).
 */
export default function OverviewPage() {
  const user = useCurrentUser().data!
  const courses = useMyCourses(user.userId)
  const deadlines = useUpcomingDeadlines(user.userId)

  // Upcoming deadlines per course, for the course cards. Unknown until loaded.
  const deadlineCount = (courseId: string | number) => deadlines.data
    ?.filter(item => String(item.courseId) === String(courseId)).length

  return (
    <div className="overview">
      <h1>Overview</h1>

      <div className="overview-grid">
        <section className="overview-section" aria-labelledby="overview-courses">
          <h2 id="overview-courses">My courses</h2>
          {courses.isPending && <p role="status">Loading courses…</p>}
          {courses.error && <p role="alert">Could not load your courses. Try again later.</p>}
          {courses.data?.length === 0 && <p>You are not enrolled in any courses yet.</p>}
          {courses.data && courses.data.length > 0 && (
            <ul className="course-grid">
              {[...courses.data].sort(byNewest).map(course => (
                <li key={course.id}>
                  <CourseCard course={course} upcomingDeadlines={deadlineCount(course.id)} />
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="overview-section overview-deadlines" aria-labelledby="overview-deadlines">
          <h2 id="overview-deadlines">Upcoming deadlines</h2>
          {deadlines.isPending && <p role="status">Loading deadlines…</p>}
          {deadlines.error && <p role="alert">Could not load your deadlines. Try again later.</p>}
          {deadlines.data?.length === 0 && <p>No upcoming deadlines.</p>}
          {deadlines.data && deadlines.data.length > 0 && <DeadlineList deadlines={deadlines.data} />}
        </section>
      </div>
    </div>
  )
}
