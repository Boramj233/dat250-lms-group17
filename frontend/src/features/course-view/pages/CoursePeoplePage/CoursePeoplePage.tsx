import { Link } from 'react-router'
import { paths } from '@shared/paths'
import PageHeader from '@shared/ui/PageHeader'
import QueryState from '@shared/ui/QueryState'
import { useCourseEnrollments } from '@domain/courses/hooks'
import type { CourseEnrollment, CourseRole } from '@domain/courses/types'
import DataTable from '@shared/ui/DataTable'
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
      <QueryState query={members} loading="Loading members…" error="Could not load the members of this course.">
        {all => (
          <>
            <DataTable<CourseEnrollment>
              caption={`Members of ${course.title}`}
              // Teachers first, then by last name and first name.
              rows={[...all].sort(
                (a, b) =>
                  ROLE_ORDER[a.role] - ROLE_ORDER[b.role] ||
                  a.lastName.localeCompare(b.lastName) ||
                  a.firstName.localeCompare(b.firstName),
              )}
              rowKey={member => String(member.userId)}
              empty="This course has no members yet."
              columns={[
                { header: 'Name', cell: member => `${member.firstName} ${member.lastName}` },
                { header: 'Email', cell: member => <a href={`mailto:${member.email}`}>{member.email}</a> },
                {
                  header: 'Role',
                  cell: member => (
                    <span className={member.role === 'TEACHER' ? 'data-table-strong' : 'text-meta'}>
                      {ROLE_LABEL[member.role]}
                    </span>
                  ),
                },
              ]}
            />
            <p className="course-people-count">
              {all.length} {all.length === 1 ? 'member' : 'members'}
            </p>
          </>
        )}
      </QueryState>
    </>
  )
}
