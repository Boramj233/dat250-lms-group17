import { useState } from 'react'
import { ApiError } from '../../../../shared/api/apiClient'
import { useCurrentUser } from '../../../../shared/auth/useAuth'
import type { UserId } from '../../../../shared/types'
import PageHeader from '../../../../shared/ui/PageHeader'
import QueryState from '../../../../shared/ui/QueryState'
import { useCourseEnrollments } from '../../../courses/hooks'
import ConfirmDialog from '../../../../shared/ui/ConfirmDialog'
import {
  groupName, groupOfUser, SwitchGroupError, useCourseGroups, useDeleteGroup, useJoinGroup, useLeaveGroup, useSwitchGroup,
} from '../../../groups/hooks'
import type { CourseGroup } from '../../../groups/types'
import { useCourseView } from '../../courseContext'
import CreateGroupForm from './CreateGroupForm'
import '../../../../shared/styles/forms.css'
import './CourseGroupsPage.css'

const isMember = (group: CourseGroup, userId: UserId) =>
  group.memberIds.some(id => String(id) === String(userId))

type Notice = { kind: 'success' | 'error', text: string }

/** A student in `from` asked to join `to`: shown in the pop-up until they answer. */
type PendingSwitch = { from: CourseGroup, to: CourseGroup }

/**
 * The course's groups, each a box that unfolds to show its members.
 * Students join a group and can leave it again; they are in one group at a time, so joining
 * another group asks first and then moves them. Teachers create and delete groups.
 */
export default function CourseGroupsPage() {
  const { course, isTeacher } = useCourseView()
  const user = useCurrentUser().data!
  const groups = useCourseGroups(course.id)
  // Names for members when the backend sends only ids (see features/groups/README.md).
  const enrollments = useCourseEnrollments(course.id)
  const join = useJoinGroup(course.id)
  const leave = useLeaveGroup(course.id)
  const remove = useDeleteGroup(course.id)
  const switchGroup = useSwitchGroup(course.id)
  const busy = join.isPending || leave.isPending || remove.isPending || switchGroup.isPending
  const [pendingSwitch, setPendingSwitch] = useState<PendingSwitch | null>(null)
  const [onlyMine, setOnlyMine] = useState(false)
  const [notice, setNotice] = useState<Notice | null>(null)
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

  function joinGroup(group: CourseGroup, myGroup: CourseGroup | undefined) {
    // Already in another group: ask in the pop-up first.
    if (myGroup) {
      setPendingSwitch({ from: myGroup, to: group })
      return
    }
    setNotice(null)
    join.mutate({ groupId: group.id, userId: user.userId }, {
      onSuccess: () => {
        setNotice({ kind: 'success', text: `You joined ${groupName(group)}.` })
        setOpen(previous => new Set(previous).add(String(group.id)))
      },
      onError: error => setNotice({ kind: 'error', text: joinError(error) }),
    })
  }

  function confirmSwitch() {
    if (!pendingSwitch) return
    const { from, to } = pendingSwitch
    setNotice(null)
    switchGroup.mutate({ fromGroupId: from.id, toGroupId: to.id, userId: user.userId }, {
      onSuccess: () => {
        setNotice({ kind: 'success', text: `You left ${groupName(from)} and joined ${groupName(to)}.` })
        setOpen(previous => new Set(previous).add(String(to.id)))
      },
      onError: error => setNotice({ kind: 'error', text: switchError(error, from, to) }),
      onSettled: () => setPendingSwitch(null),
    })
  }

  function leaveGroup(group: CourseGroup) {
    if (!window.confirm(`Leave ${groupName(group)}? You can join a group again afterwards.`)) return
    setNotice(null)
    leave.mutate({ groupId: group.id, userId: user.userId }, {
      onSuccess: () => setNotice({ kind: 'success', text: `You left ${groupName(group)}.` }),
      onError: error => setNotice({ kind: 'error', text: leaveError(error) }),
    })
  }

  function deleteGroup(group: CourseGroup) {
    const count = group.memberIds.length
    const members = count === 0 ? '' : ` Its ${count} ${count === 1 ? 'member' : 'members'} will be without a group.`
    if (!window.confirm(`Delete ${groupName(group)}?${members} This cannot be undone.`)) return
    setNotice(null)
    remove.mutate(group.id, {
      onSuccess: () => setNotice({ kind: 'success', text: `${groupName(group)} was deleted.` }),
      onError: error => setNotice({ kind: 'error', text: deleteError(error, group) }),
    })
  }

  return (
    <>
      <PageHeader title="Groups" kicker={course.title}>
        {!isTeacher && (
          <button
            type="button"
            className={`${onlyMine ? 'button-primary' : 'button-secondary'} course-groups-filter`}
            aria-pressed={onlyMine}
            onClick={() => setOnlyMine(!onlyMine)}
          >
            Show only my group
          </button>
        )}
      </PageHeader>

      <QueryState
        query={groups}
        loading="Loading groups…"
        error="Could not load the groups of this course."
        // Teachers get the create form also when there are no groups.
        empty={isTeacher ? undefined : 'This course has no groups yet. Your teacher creates them.'}
      >
        {all => {
          const myGroup = groupOfUser(all, user.userId)
          const shown = onlyMine ? all.filter(group => isMember(group, user.userId)) : all
          return (
            <>
              {!isTeacher && (
                <p className="course-groups-status">
                  {myGroup
                    ? <>You are in <strong>{groupName(myGroup)}</strong>. To change group, join another one.</>
                    : 'You are not in a group yet. Join one below; you can be in one group at a time.'}
                </p>
              )}

              <div aria-live="polite">
                {notice && (
                  <p className={notice.kind === 'error' ? 'form-error' : 'form-success'}>{notice.text}</p>
                )}
              </div>

              {all.length === 0
                ? <p>This course has no groups yet. Create the first one below.</p>
                : shown.length === 0
                  ? <p>You are not in any group in this course.</p>
                  : (
                    <ul className="course-groups">
                      {shown.map(group => {
                        const id = String(group.id)
                        const expanded = open.has(id)
                        const mine = isMember(group, user.userId)
                        const count = group.memberIds.length
                        const label = groupName(group)
                        return (
                          <li key={id} className={`course-group${expanded ? ' is-open' : ''}`}>
                            <div className="course-group-row">
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
                                  <span className="course-group-name">{label}</span>
                                  {mine && <span className="course-group-tag">Your group</span>}
                                  <span className="course-group-count">{count} {count === 1 ? 'member' : 'members'}</span>
                                </button>
                              </h2>
                              <GroupAction
                                isTeacher={isTeacher}
                                mine={mine}
                                label={label}
                                disabled={busy}
                                onJoin={() => joinGroup(group, myGroup)}
                                onLeave={() => leaveGroup(group)}
                                onDelete={() => deleteGroup(group)}
                              />
                            </div>
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
                  )}

              {isTeacher && <CreateGroupForm courseId={course.id} groups={all} />}

              <ConfirmDialog
                open={pendingSwitch !== null}
                title="Change group?"
                confirmLabel={pendingSwitch
                  ? `Yes, leave ${groupName(pendingSwitch.from)} and join ${groupName(pendingSwitch.to)}`
                  : 'Yes'}
                busy={switchGroup.isPending}
                onConfirm={confirmSwitch}
                onCancel={() => setPendingSwitch(null)}
              >
                {pendingSwitch && (
                  <>
                    <p>You are already a member of <strong>{groupName(pendingSwitch.from)}</strong>.</p>
                    <p>
                      You can only be in one group in this course, so joining <strong>{groupName(pendingSwitch.to)}</strong> will
                      remove you from {groupName(pendingSwitch.from)}.
                    </p>
                  </>
                )}
              </ConfirmDialog>
            </>
          )
        }}
      </QueryState>
    </>
  )
}

type GroupActionProps = {
  isTeacher: boolean
  /** The user is in this group. */
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
        <button type="button" className="button-secondary" disabled={disabled}
          aria-label={`Delete ${label}`} onClick={onDelete}>
          Delete
        </button>
      </div>
    )
  }
  if (mine) {
    return (
      <div className="course-group-actions">
        <button type="button" className="button-secondary" disabled={disabled}
          aria-label={`Leave ${label}`} onClick={onLeave}>
          Leave
        </button>
      </div>
    )
  }
  return (
    <div className="course-group-actions">
      <button type="button" className="button-primary" disabled={disabled}
        aria-label={`Join ${label}`} onClick={onJoin}>
        Join
      </button>
    </div>
  )
}

function joinError(error: Error): string {
  if (error instanceof ApiError) {
    if (error.status === 409) return 'You are already in a group in this course. Leave it before you join another.'
    if (error.status === 403) return 'You are not allowed to join this group.'
    if (error.status === 404) return 'The group no longer exists.'
  }
  return 'You could not join the group. Try again.'
}

function switchError(error: Error, from: CourseGroup, to: CourseGroup): string {
  if (error instanceof SwitchGroupError) {
    const why = joinError(error.cause instanceof Error ? error.cause : error)
    return error.backInOldGroup
      ? `${why} You are still in ${groupName(from)}.`
      : `${why} You are no longer in ${groupName(from)} and not in ${groupName(to)}; join a group again.`
  }
  // Leaving the old group failed: nothing changed.
  if (error instanceof ApiError && error.status === 409) {
    return `You were no longer in ${groupName(from)}, so nothing was changed. Try joining ${groupName(to)} again.`
  }
  return `${leaveError(error)} You are still in ${groupName(from)}.`
}

function leaveError(error: Error): string {
  if (error instanceof ApiError) {
    if (error.status === 409) return 'You are no longer in this group.'
    if (error.status === 403) return 'You are not allowed to leave this group.'
    if (error.status === 404) return 'The group no longer exists.'
  }
  return 'You could not leave the group. Try again.'
}

function deleteError(error: Error, group: CourseGroup): string {
  if (error instanceof ApiError && error.status === 409) {
    return `${groupName(group)} has submissions or other history and cannot be deleted.`
  }
  return `${groupName(group)} could not be deleted. Try again.`
}
