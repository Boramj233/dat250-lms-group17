import { useCurrentUser } from '../../../../shared/auth/useAuth'
import { useCourseEnrollments, useRemoveCourseEnrollment, useUpdateCourseRole } from '../../../courses/hooks'
import type { CourseEnrollment, CourseRole } from '../../../courses/types'
import QueryState from '../../../../shared/ui/QueryState'
import { useConfirm } from '../../../../shared/ui/ConfirmDialog'
import { useManageCourse } from '../../manageCourseContext'
import { ROLE_LABEL } from '../../roleLabels'
import AddMemberForm from './AddMemberForm'
import '../../../../shared/styles/forms.css'
import './CourseMembersPage.css'

/** Teachers first, then by last name. */
function sortMembers(members: CourseEnrollment[]): CourseEnrollment[] {
  return [...members].sort((a, b) =>
    a.role === b.role ? a.lastName.localeCompare(b.lastName) : a.role === 'TEACHER' ? -1 : 1)
}

/** The Members tab: who is in the course, their roles, and adding or removing members. */
export default function CourseMembersPage() {
  const { course } = useManageCourse()
  const me = useCurrentUser().data!
  const enrollments = useCourseEnrollments(course.id)
  const updateRole = useUpdateCourseRole()
  const remove = useRemoveCourseEnrollment()
  const [confirm, confirmDialog] = useConfirm()

  const members = sortMembers(enrollments.data ?? [])
  const teacherCount = members.filter(member => member.role === 'TEACHER').length

  async function removeMember(member: CourseEnrollment) {
    const name = `${member.firstName} ${member.lastName}`
    const confirmed = await confirm({
      title: `Remove ${name}?`,
      message: `${name} will no longer be a member of ${course.title}.`,
      confirmLabel: 'Remove',
    })
    if (!confirmed) return
    remove.mutate({ courseId: course.id, userId: member.userId })
  }

  return (
    <>
      <AddMemberForm courseId={course.id} members={members} />

      <h2 className="section-title">
        In this course
        {enrollments.data && ` · ${teacherCount} ${teacherCount === 1 ? 'teacher' : 'teachers'}, ${members.length - teacherCount} students`}
      </h2>
      {(updateRole.error || remove.error) && (
        <p className="form-error" role="alert">The change could not be saved. Try again.</p>
      )}
      <QueryState query={enrollments} loading="Loading members…" error="Could not load the members of this course.">
        {() => (
          <ul className="member-list">
            {members.map(member => {
              const isMe = String(member.userId) === String(me.userId)
              // Keep at least one teacher, so the course can still be managed.
              const lastTeacher = member.role === 'TEACHER' && teacherCount === 1
              return (
                <li key={member.userId} className="member-row">
                  <div className="member-name">
                    <span className="member-full-name">
                      {member.firstName} {member.lastName}{isMe && ' (you)'}
                    </span>
                    <span className="member-email">{member.email}</span>
                  </div>
                  <label className="visually-hidden" htmlFor={`role-${member.userId}`}>
                    Role of {member.firstName} {member.lastName}
                  </label>
                  <select
                    id={`role-${member.userId}`}
                    className="member-role"
                    value={member.role}
                    disabled={lastTeacher || updateRole.isPending}
                    title={lastTeacher ? 'A course needs at least one teacher' : undefined}
                    onChange={event => updateRole.mutate({
                      courseId: course.id,
                      userId: member.userId,
                      role: event.target.value as CourseRole,
                    })}
                  >
                    <option value="STUDENT">{ROLE_LABEL.STUDENT}</option>
                    <option value="TEACHER">{ROLE_LABEL.TEACHER}</option>
                  </select>
                  <button
                    type="button"
                    className="button-secondary"
                    disabled={lastTeacher || remove.isPending}
                    onClick={() => removeMember(member)}
                  >
                    Remove<span className="visually-hidden"> {member.firstName} {member.lastName}</span>
                  </button>
                </li>
              )
            })}
          </ul>
        )}
      </QueryState>
      {confirmDialog}
    </>
  )
}
