import { fullName } from '@shared/lib/fullName'
import { ROLE_LABEL } from '@domain/courses/members'
import type { CourseEnrollment, CourseRole } from '@domain/courses/types'

type MemberRowProps = {
  member: CourseEnrollment
  /** The logged-in user: marked "(you)". */
  isMe: boolean
  /** The role after the unsaved changes. */
  role: CourseRole
  roleChanged: boolean
  removed: boolean
  /** The only teacher left: the role and Remove are locked (a course needs a teacher). */
  lastTeacher: boolean
  saving: boolean
  onRoleChange: (role: CourseRole) => void
  onRemovedChange: (removed: boolean) => void
}

/** One member: name and email, the role to choose, and Remove (or Undo). Unsaved changes are marked. */
export default function MemberRow(props: MemberRowProps) {
  const { member, isMe, role, roleChanged, removed, lastTeacher, saving, onRoleChange, onRemovedChange } = props
  const name = fullName(member)
  const lockedTitle = lastTeacher ? 'A course needs at least one teacher' : undefined
  const rowClass = ['member-row', removed ? 'is-removed' : roleChanged ? 'is-changed' : ''].filter(Boolean).join(' ')
  return (
    <li className={rowClass}>
      <div className="member-name">
        <span className="member-full-name">
          {name}
          {isMe && ' (you)'}
          {removed && <span className="tag tag-danger member-change">Will be removed</span>}
          {!removed && roleChanged && (
            <span className="tag member-change">Becomes {ROLE_LABEL[role].toLowerCase()}</span>
          )}
        </span>
        <span className="member-email">{member.email}</span>
      </div>
      <label className="visually-hidden" htmlFor={`role-${member.userId}`}>
        Role of {name}
      </label>
      <select
        id={`role-${member.userId}`}
        className="member-role"
        value={role}
        disabled={lastTeacher || removed || saving}
        title={lockedTitle}
        onChange={event => onRoleChange(event.target.value as CourseRole)}
      >
        <option value="STUDENT">{ROLE_LABEL.STUDENT}</option>
        <option value="TEACHER">{ROLE_LABEL.TEACHER}</option>
      </select>
      {removed ? (
        <button type="button" className="button-secondary" disabled={saving} onClick={() => onRemovedChange(false)}>
          Undo<span className="visually-hidden"> removing {name}</span>
        </button>
      ) : (
        <button
          type="button"
          className="button-secondary"
          disabled={lastTeacher || saving}
          title={lockedTitle}
          onClick={() => onRemovedChange(true)}
        >
          Remove<span className="visually-hidden"> {name}</span>
        </button>
      )}
    </li>
  )
}
