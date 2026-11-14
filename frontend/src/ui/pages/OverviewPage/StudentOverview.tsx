import { useState } from 'react'
import { useCurrentUser } from '../../../data/auth/useAuth'
import { useMyCourses } from '../../../data/courses/hooks'
import { useUpcomingDeadlines } from '../../../data/deadlines/hooks'
import QueryState from '../../../shared/ui/QueryState'
import CourseGrid from '../../components/courses/CourseGrid'
import { byNewest, formatTerm, termOf } from '../../components/courses/courseFormat'
import DeadlineList from '../../components/DeadlineList'

/** This term's courses, and the upcoming deadlines from all courses in one list. */
export default function StudentOverview() {
  const user = useCurrentUser().data!
  const courses = useMyCourses(user.userId)
  const deadlines = useUpcomingDeadlines(user.userId)
  const [thisTerm] = useState(() => termOf(new Date()))

  return (
    <div className="overview-grid">
      <section className="overview-section" aria-labelledby="overview-courses">
        <h2 id="overview-courses">My courses · {formatTerm(thisTerm)}</h2>
        <QueryState query={courses} loading="Loading courses…" error="Could not load your courses. Try again later.">
          {all => {
            const termCourses = all
              .filter(course => course.term === thisTerm.term && course.year === thisTerm.year)
              .sort(byNewest)
            if (termCourses.length > 0) return <CourseGrid courses={termCourses} />
            return <p>You have no courses this term.</p>
          }}
        </QueryState>
      </section>

      <section className="overview-section overview-panel" aria-labelledby="overview-deadlines">
        <h2 id="overview-deadlines">Upcoming deadlines</h2>
        <QueryState
          query={deadlines}
          loading="Loading deadlines…"
          error="Could not load your deadlines. Try again later."
          empty="No upcoming deadlines."
        >
          {list => <DeadlineList deadlines={list} />}
        </QueryState>
      </section>
    </div>
  )
}
