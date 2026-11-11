import { useState } from 'react'
import { paths } from '../../app/paths'
import { useCurrentUser } from '../../data/auth/useAuth'
import { useMyCourses } from '../../data/courses/hooks/queries/useMyCourses'
import { useUpcomingDeadlines } from '../../data/deadlines/hooks/queries/useUpcomingDeadlines'
import type { CourseId } from '../../data/shared/types'
import CourseGrid from '../components/CourseGrid'
import DeadlineList from '../components/DeadlineList'
import { TrailLink, TrailPage } from '../navigation/Trail'
import { byNewest, formatTerm, termOf } from '../components/courseFormat'
import './OverviewPage.css'

/**
 * The page after login: this term's courses with a link to each (all courses are on
 * the Courses page), and the upcoming deadlines from all courses in one list. Only shown to logged-in users (see AuthGate).
 */
export default function OverviewPage() {
  const user = useCurrentUser().data!
  const courses = useMyCourses(user.userId)
  const deadlines = useUpcomingDeadlines(user.userId)
  const [thisTerm] = useState(() => termOf(new Date()))
  const termCourses = courses.data
    ?.filter(course => course.term === thisTerm.term && course.year === thisTerm.year)
    .sort(byNewest)

  // Upcoming deadlines per course, for the course cards. Unknown until loaded.
  const deadlineCount = (courseId: CourseId) => deadlines.data
    ?.filter(item => String(item.courseId) === String(courseId)).length

  return (
    <TrailPage label="Overview">
      <div className="overview">
        <h1>Overview</h1>

        <div className="overview-grid">
          <section className="overview-section" aria-labelledby="overview-courses">
            <header className="overview-section-header">
              <h2 id="overview-courses">My courses · {formatTerm(thisTerm)}</h2>
              <TrailLink className="overview-all-link" to={paths.courses}>
                All courses
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                  strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
                  <path d="M9 6l6 6-6 6" />
                </svg>
              </TrailLink>
            </header>
            {courses.isPending && <p role="status">Loading courses…</p>}
            {courses.error && <p role="alert">Could not load your courses. Try again later.</p>}
            {termCourses?.length === 0 && (
              <p>
                You have no courses this term.
                {courses.data!.length > 0 && <> See <TrailLink to={paths.courses}>all courses</TrailLink>.</>}
              </p>
            )}
            {termCourses && termCourses.length > 0 && (
              <CourseGrid courses={termCourses} deadlineCount={deadlineCount} />
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
    </TrailPage>
  )
}
