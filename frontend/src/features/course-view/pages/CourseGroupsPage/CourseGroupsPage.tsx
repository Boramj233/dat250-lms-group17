import { useState } from 'react'
import { useCurrentUser } from '@shared/auth/useAuth'
import type { UserId } from '@shared/types'
import PageHeader from '@shared/ui/PageHeader'
import QueryState from '@shared/ui/QueryState'
import { useCourseEnrollments } from '@domain/courses/hooks'
import ConfirmDialog, { useConfirm } from '@shared/ui/ConfirmDialog'
import {
  groupName,
  groupOfUser,
  useCourseGroups,
  useDeleteGroup,
  useJoinGroup,
  useLeaveGroup,
  useSwitchGroup,
} from '@domain/groups/hooks'
import type { CourseGroup } from '@domain/groups/types'
import { useCourseView } from '../../courseContext'
import CreateGroupForm from './CreateGroupForm'
import GroupBox from './GroupBox'
import { deleteError, joinError, leaveError, switchError } from './groupErrors'
import { fullName } from '@shared/lib/fullName'
import { formatCount } from '@shared/lib/formatCount'
import '@shared/styles/forms.css'
import './CourseGroupsPage.css'

const isMember = (group: CourseGroup, userId: UserId) => group.memberIds.some(id => String(id) === String(userId))

type Notice = { kind: 'success' | 'error'; text: string }

/** A student in `from` asked to join `to`: shown in the pop-up until they answer. */
type PendingSwitch = { from: CourseGroup; to: CourseGroup }

/**
 * The course's groups, each a box that unfolds to show its members.
 * Students join a group and can leave it again; they are in one group at a time, so joining
 * another group asks first and then moves them. Teachers create and delete groups.
 */
export default function CourseGroupsPage() {
  const { course, isTeacher } = useCourseView()
  const user = useCurrentUser().data!
  const groups = useCourseGroups(course.id)
  // Names for members when the backend sends only ids (see domain/groups/README.md).
  const enrollments = useCourseEnrollments(course.id)
  const join = useJoinGroup(course.id)
  const leave = useLeaveGroup(course.id)
  const remove = useDeleteGroup(course.id)
  const switchGroup = useSwitchGroup(course.id)
  const busy = join.isPending || leave.isPending || remove.isPending || switchGroup.isPending
  const [pendingSwitch, setPendingSwitch] = useState<PendingSwitch | null>(null)
  // Leave and Delete; switching group has its own dialog, which stays open while it runs.
  const [confirm, confirmDialog] = useConfirm()
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
    const member =
      group.members?.find(m => String(m.userId) === String(userId)) ??
      enrollments.data?.find(m => String(m.userId) === String(userId))
    return member ? fullName(member) : `User #${userId}`
  }

  function joinGroup(group: CourseGroup, myGroup: CourseGroup | undefined) {
    // Already in another group: ask in the pop-up first.
    if (myGroup) {
      setPendingSwitch({ from: myGroup, to: group })
      return
    }
    setNotice(null)
    join.mutate(
      { groupId: group.id, userId: user.userId },
      {
        onSuccess: () => {
          setNotice({ kind: 'success', text: `You joined ${groupName(group)}.` })
          setOpen(previous => new Set(previous).add(String(group.id)))
        },
        onError: error => setNotice({ kind: 'error', text: joinError(error) }),
      },
    )
  }

  function confirmSwitch() {
    if (!pendingSwitch) return
    const { from, to } = pendingSwitch
    setNotice(null)
    switchGroup.mutate(
      { fromGroupId: from.id, toGroupId: to.id, userId: user.userId },
      {
        onSuccess: () => {
          setNotice({ kind: 'success', text: `You left ${groupName(from)} and joined ${groupName(to)}.` })
          setOpen(previous => new Set(previous).add(String(to.id)))
        },
        onError: error => setNotice({ kind: 'error', text: switchError(error, from, to) }),
        onSettled: () => setPendingSwitch(null),
      },
    )
  }

  async function leaveGroup(group: CourseGroup) {
    const confirmed = await confirm({
      title: `Leave ${groupName(group)}?`,
      message: 'You will no longer be in a group in this course. You can join a group again afterwards.',
      confirmLabel: `Leave ${groupName(group)}`,
    })
    if (!confirmed) return
    setNotice(null)
    leave.mutate(
      { groupId: group.id, userId: user.userId },
      {
        onSuccess: () => setNotice({ kind: 'success', text: `You left ${groupName(group)}.` }),
        onError: error => setNotice({ kind: 'error', text: leaveError(error) }),
      },
    )
  }

  async function deleteGroup(group: CourseGroup) {
    const count = group.memberIds.length
    const members = count === 0 ? '' : `Its ${formatCount(count, 'member')} will be without a group. `
    const confirmed = await confirm({
      title: `Delete ${groupName(group)}?`,
      message: `${members}This cannot be undone.`,
      confirmLabel: 'Delete group',
      danger: true,
    })
    if (!confirmed) return
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
                  {myGroup ? (
                    <>
                      You are in <strong>{groupName(myGroup)}</strong>. To change group, join another one.
                    </>
                  ) : (
                    'You are not in a group yet. Join one below; you can be in one group at a time.'
                  )}
                </p>
              )}

              <div aria-live="polite">
                {notice && <p className={notice.kind === 'error' ? 'form-error' : 'form-success'}>{notice.text}</p>}
              </div>

              {all.length === 0 ? (
                <p>This course has no groups yet. Create the first one below.</p>
              ) : shown.length === 0 ? (
                <p>You are not in any group in this course.</p>
              ) : (
                <ul className="course-groups">
                  {shown.map(group => (
                    <GroupBox
                      key={group.id}
                      group={group}
                      userId={user.userId}
                      isTeacher={isTeacher}
                      mine={isMember(group, user.userId)}
                      expanded={open.has(String(group.id))}
                      onToggle={() => toggle(String(group.id))}
                      busy={busy}
                      memberName={userId => memberName(group, userId)}
                      onJoin={() => joinGroup(group, myGroup)}
                      onLeave={() => leaveGroup(group)}
                      onDelete={() => deleteGroup(group)}
                    />
                  ))}
                </ul>
              )}

              {isTeacher && <CreateGroupForm courseId={course.id} groups={all} />}

              <ConfirmDialog
                open={pendingSwitch !== null}
                title="Change group?"
                confirmLabel={
                  pendingSwitch
                    ? `Yes, leave ${groupName(pendingSwitch.from)} and join ${groupName(pendingSwitch.to)}`
                    : 'Yes'
                }
                busy={switchGroup.isPending}
                onConfirm={confirmSwitch}
                onCancel={() => setPendingSwitch(null)}
              >
                {pendingSwitch && (
                  <>
                    <p>
                      You are already a member of <strong>{groupName(pendingSwitch.from)}</strong>.
                    </p>
                    <p>
                      You can only be in one group in this course, so joining{' '}
                      <strong>{groupName(pendingSwitch.to)}</strong> will remove you from{' '}
                      {groupName(pendingSwitch.from)}.
                    </p>
                  </>
                )}
              </ConfirmDialog>
            </>
          )
        }}
      </QueryState>
      {confirmDialog}
    </>
  )
}
