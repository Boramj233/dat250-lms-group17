import { useCurrentUser } from '@shared/auth/useAuth'
import { TrailPage } from '@shared/navigation/Trail'
import PageHeader from '@shared/ui/PageHeader'
import QueryState from '@shared/ui/QueryState'
import CourseGrid from '@domain/courses/components/CourseGrid'
import { splitByCurrentTerm, type TermGroup } from '@domain/courses/courseFormat'
import { useMyCourses } from '@domain/courses/hooks'
import type { EnrolledCourse } from '@domain/courses/types'
import './CoursesPage.css'

/** Every course the user is enrolled in: this (and later) terms first, then previous terms. */
export default function CoursesPage() {
  const user = useCurrentUser().data!
  const courses = useMyCourses(user.userId)

  return (
    <TrailPage label="Courses">
      <div className="courses-page">
        <PageHeader title="Courses" />
        <QueryState
          query={courses}
          loading="Loading courses…"
          error="Could not load your courses. Try again later."
          empty="You are not enrolled in any courses."
        >
          {all => {
            const { current, previous } = splitByCurrentTerm(all)
            return (
              <>
                {current.map(group => (
                  <TermSection key={group.key} group={group} />
                ))}
                {previous.length > 0 && (
                  <>
                    <h2 className="section-title">Previous terms</h2>
                    {previous.map(group => (
                      <TermSection key={group.key} group={group} />
                    ))}
                  </>
                )}
              </>
            )
          }}
        </QueryState>
      </div>
    </TrailPage>
  )
}

function TermSection({ group }: { group: TermGroup<EnrolledCourse> }) {
  const headingId = `courses-${group.key}`
  return (
    <section className="courses-term" aria-labelledby={headingId}>
      <h3 id={headingId}>{group.label}</h3>
      <CourseGrid courses={group.courses} />
    </section>
  )
}
