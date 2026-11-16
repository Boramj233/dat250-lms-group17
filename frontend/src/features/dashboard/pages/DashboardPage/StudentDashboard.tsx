import { useState } from 'react'
import { useCurrentUser } from '@shared/auth/useAuth'
import { useMyCourses } from '@domain/courses/hooks'
import { useUpcomingDeadlines } from '@domain/deadlines/hooks'
import QueryState from '@shared/ui/QueryState'
import CourseGrid from '@domain/courses/components/CourseGrid'
import { byNewest, formatTerm, termOf } from '@domain/courses/courseFormat'
import DeadlineList from '@domain/deadlines/components/DeadlineList'

/** This term's courses, and the upcoming deadlines from all courses in one list. */
export default function StudentDashboard() {
  const user = useCurrentUser().data!
  const courses = useMyCourses(user.userId)
  const deadlines = useUpcomingDeadlines(user.userId)
  const [thisTerm] = useState(() => termOf(new Date()))

  return (
    <div className="dashboard-grid">
      <section className="dashboard-section" aria-labelledby="dashboard-courses">
        <h2 id="dashboard-courses">My courses · {formatTerm(thisTerm)}</h2>
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

      <section className="dashboard-section dashboard-panel" aria-labelledby="dashboard-deadlines">
        <h2 id="dashboard-deadlines">Upcoming deadlines</h2>
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
