import { useState } from 'react'
import { useCurrentUser } from '../../../../shared/auth/useAuth'
import type { UserId } from '../../../../shared/types'
import PageHeader from '../../../../shared/ui/PageHeader'
import QueryState from '../../../../shared/ui/QueryState'
import { useCourseEnrollments } from '../../../courses/hooks'
import { groupName, useCourseGroups } from '../../../groups/hooks'
import type { CourseGroup } from '../../../groups/types'
import { useCourseView } from '../../courseContext'
import '../../../../shared/styles/forms.css'
import './CourseGroupsPage.css'

const isMember = (group: CourseGroup, userId: UserId) =>
  group.memberIds.some(id => String(id) === String(userId))

/**
 * The course's groups, each a box that unfolds to show its members. A button at the
 * top shows only the groups the user is in.
 */
export default function CourseGroupsPage() {
  const { course } = useCourseView()
  const user = useCurrentUser().data!
  const groups = useCourseGroups(course.id)
  // Names for members when the backend sends only ids (see features/groups/README.md).
  const enrollments = useCourseEnrollments(course.id)
  const [onlyMine, setOnlyMine] = useState(false)
  // Ids (strings) of the unfolded groups; all are folded at first.
  const [open, setOpen] = useState<Set<string>>(() => new Set())

  function toggle(groupId: string) {
    setOpen(previous => {
      const next = new Set(previous)
      if (next.has(groupId)) next.delete(groupId)
      else next.add(groupId)
      return next
    })
  }

  function memberName(group: CourseGroup, userId: UserId): string {
    const member = group.members?.find(m => String(m.userId) === String(userId))
      ?? enrollments.data?.find(m => String(m.userId) === String(userId))
    return member ? `${member.firstName} ${member.lastName}` : `User #${userId}`
  }

  return (
    <>
      <PageHeader title="Groups" kicker={course.title}>
        <button
          type="button"
          className={`${onlyMine ? 'button-primary' : 'button-secondary'} course-groups-filter`}
          aria-pressed={onlyMine}
          onClick={() => setOnlyMine(!onlyMine)}
        >
          Show only my groups
        </button>
      </PageHeader>

      <QueryState
        query={groups}
        loading="Loading groups…"
        error="Could not load the groups of this course."
        empty="This course has no groups yet."
      >
        {all => {
          const shown = onlyMine ? all.filter(group => isMember(group, user.userId)) : all
          if (shown.length === 0) return <p>You are not in any group in this course.</p>
          return (
            <ul className="course-groups">
              {shown.map(group => {
                const id = String(group.id)
                const expanded = open.has(id)
                const mine = isMember(group, user.userId)
                const count = group.memberIds.length
                return (
                  <li key={id} className={`course-group${expanded ? ' is-open' : ''}`}>
                    <h2 className="course-group-header">
                      <button
                        type="button"
                        className="course-group-toggle"
                        aria-expanded={expanded}
                        aria-controls={`course-group-${id}`}
                        onClick={() => toggle(id)}
                      >
                        <svg className="course-group-chevron" width="18" height="18" viewBox="0 0 24 24"
                          fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
                          strokeLinejoin="round" aria-hidden="true" focusable="false">
                          <path d="M9 6l6 6-6 6" />
                        </svg>
                        <span className="course-group-name">{groupName(group)}</span>
                        {mine && <span className="course-group-tag">Your group</span>}
                        <span className="course-group-count">{count} {count === 1 ? 'member' : 'members'}</span>
                      </button>
                    </h2>
                    <div id={`course-group-${id}`} className="course-group-body" hidden={!expanded}>
                      {count === 0
                        ? <p className="course-group-empty">No members yet.</p>
                        : (
                          <ul className="course-group-members">
                            {group.memberIds.map(userId => (
                              <li key={userId}>
                                {memberName(group, userId)}
                                {String(userId) === String(user.userId) && <span className="course-group-you"> (you)</span>}
                              </li>
                            ))}
                          </ul>
                        )}
                    </div>
                  </li>
                )
              })}
            </ul>
          )
        }}
      </QueryState>
    </>
  )
}
