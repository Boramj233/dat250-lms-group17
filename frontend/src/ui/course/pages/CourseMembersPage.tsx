import { useEffect, useState, type FormEvent } from 'react'
import { useCurrentUser } from '../../../data/auth/useAuth'
import { useEnrollInCourse } from '../../../data/courses/hooks/mutations/useEnrollInCourse'
import { useRemoveCourseEnrollment } from '../../../data/courses/hooks/mutations/useRemoveCourseEnrollment'
import { useUpdateCourseRole } from '../../../data/courses/hooks/mutations/useUpdateCourseRole'
import { useCourseEnrollments } from '../../../data/courses/hooks/queries/useCourseEnrollments'
import type { CourseEnrollment, CourseRole } from '../../../data/courses/types'
import { useUserSearch } from '../../../data/users/hooks/queries/useUserSearch'
import type { UserLookup } from '../../../data/users/types'
import { useCourseContext } from '../courseContext'
import '../../components/forms.css'
import './CoursePages.css'
import './CourseMembersPage.css'

const ROLE_LABEL: Record<CourseRole, string> = { TEACHER: 'Teacher', STUDENT: 'Student' }

/** Teachers: who is in the course, their roles, and adding or removing members. */
export default function CourseMembersPage() {
  const { course } = useCourseContext()
  const me = useCurrentUser().data!
  const enrollments = useCourseEnrollments(course.id)
  const updateRole = useUpdateCourseRole()
  const remove = useRemoveCourseEnrollment()

  const members = [...(enrollments.data ?? [])].sort((a, b) =>
    a.role === b.role ? a.lastName.localeCompare(b.lastName) : a.role === 'TEACHER' ? -1 : 1)
  const teacherCount = members.filter(member => member.role === 'TEACHER').length

  function removeMember(member: CourseEnrollment) {
    const name = `${member.firstName} ${member.lastName}`
    if (!window.confirm(`Remove ${name} from ${course.title}?`)) return
    remove.mutate({ courseId: course.id, userId: member.userId })
  }

  return (
    <>
      <header className="course-header">
        <h1>Members</h1>
        {enrollments.data && (
          <p>{teacherCount} {teacherCount === 1 ? 'teacher' : 'teachers'}, {members.length - teacherCount} students</p>
        )}
      </header>

      <AddMemberForm courseId={course.id} members={members} />

      <h2 className="course-section-title">In this course</h2>
      {enrollments.isPending && <p role="status">Loading members…</p>}
      {enrollments.error && <p role="alert">Could not load the members of this course.</p>}
      {(updateRole.error || remove.error) && (
        <p className="form-error" role="alert">The change could not be saved. Try again.</p>
      )}
      {members.length > 0 && (
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
    </>
  )
}

/** Search text that only updates after the user stopped typing for `delayMs`. */
function useDebounced(value: string, delayMs = 300): string {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(value), delayMs)
    return () => window.clearTimeout(timer)
  }, [value, delayMs])
  return debounced
}

type AddMemberFormProps = {
  courseId: string | number
  members: CourseEnrollment[]
}

/** Find a user by name or email and add them as a student or a teacher. */
function AddMemberForm({ courseId, members }: AddMemberFormProps) {
  const [search, setSearch] = useState('')
  const [selected, setSelected] = useState<UserLookup | null>(null)
  const [role, setRole] = useState<CourseRole>('STUDENT')
  const [added, setAdded] = useState<string | null>(null)
  const results = useUserSearch(useDebounced(search))
  const enroll = useEnrollInCourse()
  const memberIds = new Set(members.map(member => String(member.userId)))

  function submit(event: FormEvent) {
    event.preventDefault()
    if (!selected) return
    enroll.mutate({ courseId, userId: selected.userId, role }, {
      onSuccess: () => {
        setAdded(`${selected.firstName} ${selected.lastName} was added as ${ROLE_LABEL[role].toLowerCase()}.`)
        setSelected(null)
        setSearch('')
      },
    })
  }

  return (
    <section className="teacher-panel" aria-labelledby="add-member-title">
      <h2 id="add-member-title" className="teacher-panel-title">Add a member</h2>
      <form className="form" onSubmit={submit}>
        <div className="form-field">
          <label htmlFor="member-search">Name or email</label>
          <input
            id="member-search"
            type="search"
            autoComplete="off"
            value={search}
            onChange={event => { setSearch(event.target.value); setSelected(null); setAdded(null) }}
            placeholder="Search for a user"
          />
        </div>

        {!selected && search.trim().length >= 2 && (
          <div className="member-results" aria-live="polite">
            {results.isFetching && <p className="form-hint">Searching…</p>}
            {results.error && <p className="form-error">Searching for users failed.</p>}
            {results.data?.users.length === 0 && <p className="form-hint">No users found.</p>}
            {results.data && results.data.users.length > 0 && (
              <ul className="member-result-list">
                {results.data.users.map(user => {
                  const enrolled = memberIds.has(String(user.userId))
                  return (
                    <li key={user.userId}>
                      <button type="button" className="member-result" disabled={enrolled}
                        onClick={() => setSelected(user)}>
                        <span className="member-full-name">{user.firstName} {user.lastName}</span>
                        <span className="member-email">{user.email}{enrolled && ' · already in the course'}</span>
                      </button>
                    </li>
                  )
                })}
              </ul>
            )}
          </div>
        )}

        {selected && (
          <div className="form-row">
            <p className="member-selected">
              <span className="member-full-name">{selected.firstName} {selected.lastName}</span>
              <span className="member-email">{selected.email}</span>
            </p>
            <div className="form-field">
              <label htmlFor="member-role">Role</label>
              <select id="member-role" value={role} onChange={event => setRole(event.target.value as CourseRole)}>
                <option value="STUDENT">Student</option>
                <option value="TEACHER">Teacher</option>
              </select>
            </div>
          </div>
        )}

        {enroll.error && <p className="form-error" role="alert">Could not add the member. Try again.</p>}
        {added && <p className="form-success" role="status">{added}</p>}
        <div className="form-actions">
          <button type="submit" className="button-primary" disabled={!selected || enroll.isPending}>
            {enroll.isPending ? 'Adding…' : 'Add to course'}
          </button>
        </div>
      </form>
    </section>
  )
}
