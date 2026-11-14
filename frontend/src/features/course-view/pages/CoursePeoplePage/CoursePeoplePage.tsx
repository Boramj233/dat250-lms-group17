import { Link } from 'react-router'
import { paths } from '../../../../shared/paths'
import PageHeader from '../../../../shared/ui/PageHeader'
import QueryState from '../../../../shared/ui/QueryState'
import { useCourseEnrollments } from '../../../courses/hooks'
import type { CourseId } from '../../../../shared/types'
import type { CourseRole } from '../../../courses/types'
import { useCourseView } from '../../courseContext'
import './CoursePeoplePage.css'

const GROUPS: { role: CourseRole, heading: string }[] = [
  { role: 'TEACHER', heading: 'Teachers' },
  { role: 'STUDENT', heading: 'Students' },
]

/** The course's members by role. The backend only lets teachers list them for now. */
export default function CoursePeoplePage() {
  const { course, isTeacher } = useCourseView()

  return (
    <>
      <PageHeader title="People">
        {isTeacher && (
          <Link className="button-secondary course-people-manage" to={paths.manageCourseMembers(course.id)}>
            Manage members
          </Link>
        )}
      </PageHeader>
      {isTeacher
        ? <MemberList courseId={course.id} />
        : <p>Only teachers can see the members of a course for now.</p>}
    </>
  )
}

function MemberList({ courseId }: { courseId: CourseId }) {
  const members = useCourseEnrollments(courseId)
  return (
    <QueryState
      query={members}
      loading="Loading members…"
      error="Could not load the members of this course."
      empty="This course has no members yet."
    >
      {all => GROUPS.map(({ role, heading }) => {
        const inRole = all
          .filter(member => member.role === role)
          .sort((a, b) => a.lastName.localeCompare(b.lastName) || a.firstName.localeCompare(b.firstName))
        if (inRole.length === 0) return null
        return (
          <section key={role} className="course-people-group" aria-labelledby={`people-${role}`}>
            <h2 id={`people-${role}`} className="section-title">{heading} ({inRole.length})</h2>
            <ul className="course-people-list">
              {inRole.map(member => (
                <li key={member.userId}>
                  <span className="course-people-name">{member.firstName} {member.lastName}</span>
                  <span className="course-people-email">{member.email}</span>
                </li>
              ))}
            </ul>
          </section>
        )
      })}
    </QueryState>
  )
}
