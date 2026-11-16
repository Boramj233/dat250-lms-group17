import { useState } from 'react'
import { useCurrentUser } from '@shared/auth/useAuth'
import { useCourseEnrollments, useRemoveCourseEnrollment, useUpdateCourseRole } from '@domain/courses/hooks'
import type { CourseEnrollment, CourseRole } from '@domain/courses/types'
import QueryState from '@shared/ui/QueryState'
import { useConfirm } from '@shared/ui/ConfirmDialog'
import SaveBar from '@shared/ui/SaveBar'
import { useManageCourse } from '../../manageCourseContext'
import { ROLE_LABEL } from '../../roleLabels'
import AddMemberForm from './AddMemberForm'
import '@shared/styles/forms.css'
import './CourseMembersPage.css'

/** Teachers first, then by last name. */
function sortMembers(members: CourseEnrollment[]): CourseEnrollment[] {
  return [...members].sort((a, b) =>
    a.role === b.role ? a.lastName.localeCompare(b.lastName) : a.role === 'TEACHER' ? -1 : 1,
  )
}

/** Unsaved role changes and removals, by user id (as a string). */
type Draft = { roles: Record<string, CourseRole>; removals: string[] }
const EMPTY_DRAFT: Draft = { roles: {}, removals: [] }

/**
 * The Members tab: who is in the course, their roles, and adding or removing members.
 * Role changes and removals are marked on the rows first; Save changes applies them
 * (removing asks once more), Cancel throws them away. Adding a member is its own form.
 */
export default function CourseMembersPage() {
  const { course } = useManageCourse()
  const me = useCurrentUser().data!
  const enrollments = useCourseEnrollments(course.id)
  const updateRole = useUpdateCourseRole()
  const remove = useRemoveCourseEnrollment()
  const [confirm, confirmDialog] = useConfirm()
  const [draft, setDraft] = useState<Draft>(EMPTY_DRAFT)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState<string | null>(null)

  const members = sortMembers(enrollments.data ?? [])
  const teacherCount = members.filter(member => member.role === 'TEACHER').length
  const changes = Object.keys(draft.roles).length + draft.removals.length
  const fullName = (member: CourseEnrollment) => `${member.firstName} ${member.lastName}`
  const roleOf = (member: CourseEnrollment) => draft.roles[String(member.userId)] ?? member.role
  const removed = (member: CourseEnrollment) => draft.removals.includes(String(member.userId))
  // Teachers left after the unsaved changes: the course must keep at least one.
  const teachersAfter = members.filter(member => !removed(member) && roleOf(member) === 'TEACHER').length

  function change(update: (draft: Draft) => Draft) {
    setDraft(update)
    setSaved(null)
    setError(null)
  }

  function setRole(member: CourseEnrollment, role: CourseRole) {
    const id = String(member.userId)
    change(d => {
      const roles = { ...d.roles }
      if (role === member.role) delete roles[id]
      else roles[id] = role
      return { ...d, roles }
    })
  }

  function setRemoved(member: CourseEnrollment, on: boolean) {
    const id = String(member.userId)
    change(d => ({ ...d, removals: on ? [...d.removals, id] : d.removals.filter(other => other !== id) }))
  }

  async function save() {
    const removing = members.filter(removed)
    if (removing.length > 0) {
      const confirmed = await confirm({
        title: `Remove ${removing.length === 1 ? fullName(removing[0]) : `${removing.length} members`}?`,
        message: (
          <>
            <p>
              Saving also removes {removing.length === 1 ? 'this member' : 'these members'} from {course.title}. They
              can no longer see the course.
            </p>
            {removing.length > 1 && (
              <ul>
                {removing.map(member => (
                  <li key={member.userId}>{fullName(member)}</li>
                ))}
              </ul>
            )}
          </>
        ),
        confirmLabel: removing.length === 1 ? 'Remove and save' : `Remove ${removing.length} and save`,
        danger: true,
      })
      if (!confirmed) return
    }
    setSaving(true)
    setError(null)
    try {
      for (const [id, role] of Object.entries(draft.roles)) {
        if (!draft.removals.includes(id)) await updateRole.mutateAsync({ courseId: course.id, userId: id, role })
        setDraft(d => ({ ...d, roles: Object.fromEntries(Object.entries(d.roles).filter(([key]) => key !== id)) }))
      }
      for (const id of draft.removals) {
        await remove.mutateAsync({ courseId: course.id, userId: id })
        setDraft(d => ({ ...d, removals: d.removals.filter(other => other !== id) }))
      }
      setSaved('All changes are saved.')
    } catch {
      setError('Some changes could not be saved. Those still marked are not saved; try again.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <AddMemberForm courseId={course.id} members={members} />

      <h2 className="section-title">
        In this course
        {enrollments.data &&
          ` · ${teacherCount} ${teacherCount === 1 ? 'teacher' : 'teachers'}, ${members.length - teacherCount} students`}
      </h2>
      <QueryState query={enrollments} loading="Loading members…" error="Could not load the members of this course.">
        {() => (
          <ul className="member-list">
            {members.map(member => {
              const isMe = String(member.userId) === String(me.userId)
              const isRemoved = removed(member)
              const roleChanged = String(member.userId) in draft.roles
              // Keep at least one teacher, so the course can still be managed.
              const lastTeacher = !isRemoved && roleOf(member) === 'TEACHER' && teachersAfter === 1
              const rowClass = ['member-row', isRemoved ? 'is-removed' : roleChanged ? 'is-changed' : '']
                .filter(Boolean)
                .join(' ')
              return (
                <li key={member.userId} className={rowClass}>
                  <div className="member-name">
                    <span className="member-full-name">
                      {fullName(member)}
                      {isMe && ' (you)'}
                      {isRemoved && <span className="tag tag-danger member-change">Will be removed</span>}
                      {!isRemoved && roleChanged && (
                        <span className="tag member-change">Becomes {ROLE_LABEL[roleOf(member)].toLowerCase()}</span>
                      )}
                    </span>
                    <span className="member-email">{member.email}</span>
                  </div>
                  <label className="visually-hidden" htmlFor={`role-${member.userId}`}>
                    Role of {fullName(member)}
                  </label>
                  <select
                    id={`role-${member.userId}`}
                    className="member-role"
                    value={roleOf(member)}
                    disabled={lastTeacher || isRemoved || saving}
                    title={lastTeacher ? 'A course needs at least one teacher' : undefined}
                    onChange={event => setRole(member, event.target.value as CourseRole)}
                  >
                    <option value="STUDENT">{ROLE_LABEL.STUDENT}</option>
                    <option value="TEACHER">{ROLE_LABEL.TEACHER}</option>
                  </select>
                  {isRemoved ? (
                    <button
                      type="button"
                      className="button-secondary"
                      disabled={saving}
                      onClick={() => setRemoved(member, false)}
                    >
                      Undo<span className="visually-hidden"> removing {fullName(member)}</span>
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="button-secondary"
                      disabled={lastTeacher || saving}
                      title={lastTeacher ? 'A course needs at least one teacher' : undefined}
                      onClick={() => setRemoved(member, true)}
                    >
                      Remove<span className="visually-hidden"> {fullName(member)}</span>
                    </button>
                  )}
                </li>
              )
            })}
          </ul>
        )}
      </QueryState>
      <SaveBar
        changes={changes}
        saving={saving}
        error={error}
        saved={saved}
        onSave={() => void save()}
        onCancel={() => change(() => EMPTY_DRAFT)}
      />
      {confirmDialog}
    </>
  )
}
