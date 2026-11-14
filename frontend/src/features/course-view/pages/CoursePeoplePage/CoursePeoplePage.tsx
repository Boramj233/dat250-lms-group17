import { Link } from 'react-router'
import { paths } from '../../../../shared/paths'
import PageHeader from '../../../../shared/ui/PageHeader'
import QueryState from '../../../../shared/ui/QueryState'
import { useCourseEnrollments } from '../../../courses/hooks'
import type { CourseRole } from '../../../courses/types'
import { useCourseView } from '../../courseContext'
import './CoursePeoplePage.css'

const ROLE_LABEL: Record<CourseRole, string> = { TEACHER: 'Teacher', STUDENT: 'Student' }
const ROLE_ORDER: Record<CourseRole, number> = { TEACHER: 0, STUDENT: 1 }

/** Everyone enrolled in the course in a table: name, email and role. */
export default function CoursePeoplePage() {
  const { course, isTeacher } = useCourseView()
  const members = useCourseEnrollments(course.id)

  return (
    <>
      <PageHeader title="People" kicker={course.title}>
        {isTeacher && (
          <Link className="button-secondary course-people-manage" to={paths.manageCourseMembers(course.id)}>
            Manage members
          </Link>
        )}
      </PageHeader>
      <QueryState
        query={members}
        loading="Loading members…"
        error="Could not load the members of this course."
        empty="This course has no members yet."
      >
        {all => {
          // Teachers first, then by last name and first name.
          const sorted = [...all].sort((a, b) => ROLE_ORDER[a.role] - ROLE_ORDER[b.role]
            || a.lastName.localeCompare(b.lastName) || a.firstName.localeCompare(b.firstName))
          return (
            <div className="course-people-scroll">
              <table className="course-people-table">
                <caption className="visually-hidden">Members of {course.title}</caption>
                <thead>
                  <tr>
                    <th scope="col">Name</th>
                    <th scope="col">Email</th>
                    <th scope="col">Role</th>
                  </tr>
                </thead>
                <tbody>
                  {sorted.map(member => (
                    <tr key={member.userId}>
                      <td>{member.firstName} {member.lastName}</td>
                      <td><a href={`mailto:${member.email}`}>{member.email}</a></td>
                      <td>
                        <span className={`course-people-role is-${member.role.toLowerCase()}`}>
                          {ROLE_LABEL[member.role]}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <p className="course-people-count">{all.length} {all.length === 1 ? 'member' : 'members'}</p>
            </div>
          )
        }}
      </QueryState>
    </>
  )
}
