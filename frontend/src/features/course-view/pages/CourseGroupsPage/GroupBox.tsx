import type { UserId } from '@shared/types'
import { groupName } from '@domain/groups/hooks'
import type { CourseGroup } from '@domain/groups/types'

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
  onJoin: () => void
  onLeave: () => void
  onDelete: () => void
}

/** One group: a header that unfolds its members, with Join, Leave or Delete next to it. */
export default function GroupBox({
  group,
  userId,
  isTeacher,
  mine,
  expanded,
  onToggle,
  busy,
  memberName,
  onJoin,
  onLeave,
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
            <svg
              className="disclosure-chevron"
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
              focusable="false"
            >
              <path d="M9 6l6 6-6 6" />
            </svg>
            <span className="course-group-name">{label}</span>
            {mine && <span className="tag">Your group</span>}
            <span className="course-group-count">
              {count} {count === 1 ? 'member' : 'members'}
            </span>
          </button>
        </h2>
        <GroupAction
          isTeacher={isTeacher}
          mine={mine}
          label={label}
          disabled={busy}
          onJoin={onJoin}
          onLeave={onLeave}
          onDelete={onDelete}
        />
      </div>
      <div id={`course-group-${id}`} className="course-group-body" hidden={!expanded}>
        {count === 0 ? (
          <p className="course-group-empty">No members yet.</p>
        ) : (
          <ul className="course-group-members">
            {group.memberIds.map(memberId => (
              <li key={memberId}>
                {memberName(memberId)}
                {String(memberId) === String(userId) && <span className="course-group-you"> (you)</span>}
              </li>
            ))}
          </ul>
        )}
      </div>
    </li>
  )
}

type GroupActionProps = {
  isTeacher: boolean
  mine: boolean
  label: string
  disabled: boolean
  onJoin: () => void
  onLeave: () => void
  onDelete: () => void
}

/** Teachers: Delete. Students: Leave on their own group, Join on the others. */
function GroupAction({ isTeacher, mine, label, disabled, onJoin, onLeave, onDelete }: GroupActionProps) {
  if (isTeacher) {
    return (
      <div className="course-group-actions">
        <button
          type="button"
          className="button-secondary"
          disabled={disabled}
          aria-label={`Delete ${label}`}
          onClick={onDelete}
        >
          Delete
        </button>
      </div>
    )
  }
  if (mine) {
    return (
      <div className="course-group-actions">
        <button
          type="button"
          className="button-secondary"
          disabled={disabled}
          aria-label={`Leave ${label}`}
          onClick={onLeave}
        >
          Leave
        </button>
      </div>
    )
  }
  return (
    <div className="course-group-actions">
      <button
        type="button"
        className="button-primary"
        disabled={disabled}
        aria-label={`Join ${label}`}
        onClick={onJoin}
      >
        Join
      </button>
    </div>
  )
}
