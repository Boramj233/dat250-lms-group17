import { useId, useState } from 'react'
import type { UserId } from '@shared/types'
import { groupName } from '@domain/groups/hooks'
import type { CourseGroup } from '@domain/groups/types'
import { ChevronRightIcon } from '@shared/ui/icons'
import { formatCount } from '@shared/lib/formatCount'

type GroupBoxProps = {
  group: CourseGroup
  /** The logged-in user. */
  userId: UserId
  isTeacher: boolean
  /** The user is in this group. */
  mine: boolean
  expanded: boolean
  onToggle: () => void
  /** While another action runs. */
  busy: boolean
  memberName: (userId: UserId) => string
  /** Teachers: students of the course who are in no group, to add to this one. */
  addable: { userId: UserId; name: string }[]
  onAdd: (userId: UserId) => void
  onRemove: (userId: UserId) => void
  onDelete: () => void
}

/**
 * One group: a header that unfolds its members. Teachers also add students to it, remove
 * members and delete it; students only see it.
 */
export default function GroupBox({
  group,
  userId,
  isTeacher,
  mine,
  expanded,
  onToggle,
  busy,
  memberName,
  addable,
  onAdd,
  onRemove,
  onDelete,
}: GroupBoxProps) {
  const id = String(group.id)
  const count = group.memberIds.length
  const label = groupName(group)
  return (
    <li className={`course-group${expanded ? ' is-open' : ''}`}>
      <div className="course-group-row">
        <h2 className="course-group-header">
          <button
            type="button"
            className="course-group-toggle"
            aria-expanded={expanded}
            aria-controls={`course-group-${id}`}
            onClick={onToggle}
          >
            <ChevronRightIcon className="disclosure-chevron" size={18} />
            <span className="course-group-name">{label}</span>
            {mine && <span className="tag">Your group</span>}
            <span className="course-group-count">{formatCount(count, 'member')}</span>
          </button>
        </h2>
        {isTeacher && (
          <div className="course-group-actions">
            <button
              type="button"
              className="button-secondary"
              disabled={busy}
              aria-label={`Delete ${label}`}
              onClick={onDelete}
            >
              Delete
            </button>
          </div>
        )}
      </div>
      <div id={`course-group-${id}`} className="course-group-body" hidden={!expanded}>
        {count === 0 ? (
          <p className="course-group-empty">No members yet.</p>
        ) : (
          <ul className="course-group-members">
            {group.memberIds.map(memberId => (
              <li key={memberId} className="course-group-member">
                <span>
                  {memberName(memberId)}
                  {String(memberId) === String(userId) && <span className="course-group-you"> (you)</span>}
                </span>
                {isTeacher && (
                  <button
                    type="button"
                    className="button-secondary course-group-remove"
                    disabled={busy}
                    onClick={() => onRemove(memberId)}
                  >
                    Remove
                    <span className="visually-hidden">
                      {' '}
                      {memberName(memberId)} from {label}
                    </span>
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}
        {isTeacher && <AddMember label={label} addable={addable} busy={busy} onAdd={onAdd} />}
      </div>
    </li>
  )
}

type AddMemberProps = {
  label: string
  addable: { userId: UserId; name: string }[]
  busy: boolean
  onAdd: (userId: UserId) => void
}

/** Teachers: a menu of the students without a group, and Add. */
function AddMember({ label, addable, busy, onAdd }: AddMemberProps) {
  const selectId = useId()
  const [chosen, setChosen] = useState('')
  if (addable.length === 0) {
    return <p className="course-group-empty">Every student of the course is in a group.</p>
  }
  return (
    <div className="course-group-add">
      <label htmlFor={selectId}>Add a student to {label}</label>
      <div className="form-row">
        <select id={selectId} value={chosen} onChange={event => setChosen(event.target.value)}>
          <option value="">Choose a student…</option>
          {addable.map(student => (
            <option key={student.userId} value={String(student.userId)}>
              {student.name}
            </option>
          ))}
        </select>
        <button
          type="button"
          className="button-primary"
          disabled={busy || chosen === ''}
          onClick={() => {
            onAdd(chosen)
            setChosen('')
          }}
        >
          Add
        </button>
      </div>
    </div>
  )
}
