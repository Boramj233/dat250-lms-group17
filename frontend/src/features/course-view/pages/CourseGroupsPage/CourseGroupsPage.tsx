import { useState } from 'react'
import { useCurrentUser } from '@shared/auth/useAuth'
import type { UserId } from '@shared/types'
import PageHeader from '@shared/ui/PageHeader'
import QueryState from '@shared/ui/QueryState'
import { useCourseEnrollments } from '@domain/courses/hooks'
import { useConfirm } from '@shared/ui/ConfirmDialog'
import {
  groupName,
  groupOfUser,
  useAddGroupMember,
  useCourseGroups,
  useDeleteGroup,
  useRemoveGroupMember,
} from '@domain/groups/hooks'
import type { CourseGroup } from '@domain/groups/types'
import { useCourseView } from '../../courseContext'
import CreateGroupForm from './CreateGroupForm'
import GroupBox from './GroupBox'
import { addError, deleteError, removeError } from './groupErrors'
import { fullName } from '@shared/lib/fullName'
import { formatCount } from '@shared/lib/formatCount'
import '@shared/styles/forms.css'
import './CourseGroupsPage.css'

const isMember = (group: CourseGroup, userId: UserId) => group.memberIds.some(id => String(id) === String(userId))

type Notice = { kind: 'success' | 'error'; text: string }

/**
 * The course's groups, each a box that unfolds to show its members. Teachers create and
 * delete groups and put students in them (the backend lets only teachers change members);
 * a student is in at most one group per course. Students see the groups and their own.
 */
export default function CourseGroupsPage() {
  const { course, isTeacher } = useCourseView()
  const user = useCurrentUser().data!
  const groups = useCourseGroups(course.id)
  // The members' names; GroupDTO only has their ids.
  const enrollments = useCourseEnrollments(course.id)
  const add = useAddGroupMember(course.id)
  const removeMember = useRemoveGroupMember(course.id)
  const remove = useDeleteGroup(course.id)
  const busy = add.isPending || removeMember.isPending || remove.isPending
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

  function memberName(userId: UserId): string {
    const member = enrollments.data?.find(m => String(m.userId) === String(userId))
    return member ? fullName(member) : `User #${userId}`
  }

  function addMember(group: CourseGroup, userId: UserId) {
    const name = memberName(userId)
    setNotice(null)
    add.mutate(
      { groupId: group.id, userId },
      {
        onSuccess: () => setNotice({ kind: 'success', text: `${name} was added to ${groupName(group)}.` }),
        onError: error => setNotice({ kind: 'error', text: addError(error, name, group) }),
      },
    )
  }

  async function removeFromGroup(group: CourseGroup, userId: UserId) {
    const name = memberName(userId)
    const confirmed = await confirm({
      title: `Remove ${name} from ${groupName(group)}?`,
      message: 'Their earlier group submissions and grades are kept. They can be added to a group again.',
      confirmLabel: 'Remove',
    })
    if (!confirmed) return
    setNotice(null)
    removeMember.mutate(
      { groupId: group.id, userId },
      {
        onSuccess: () => setNotice({ kind: 'success', text: `${name} was removed from ${groupName(group)}.` }),
        onError: error => setNotice({ kind: 'error', text: removeError(error, name, group) }),
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
          // Students of the course who are in no group yet, by name.
          const addable = (enrollments.data ?? [])
            .filter(member => member.role === 'STUDENT' && !groupOfUser(all, member.userId))
            .map(member => ({ userId: member.userId, name: fullName(member) }))
            .sort((a, b) => a.name.localeCompare(b.name))
          return (
            <>
              {!isTeacher && (
                <p className="course-groups-status">
                  {myGroup ? (
                    <>
                      You are in <strong>{groupName(myGroup)}</strong>. You can hand in for the group on assignments and
                      coding exercises. Ask your teacher to change your group.
                    </>
                  ) : (
                    'You are not in a group. Your teacher puts students in groups.'
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
                      memberName={memberName}
                      addable={addable}
                      onAdd={userId => addMember(group, userId)}
                      onRemove={userId => void removeFromGroup(group, userId)}
                      onDelete={() => void deleteGroup(group)}
                    />
                  ))}
                </ul>
              )}

              {isTeacher && <CreateGroupForm courseId={course.id} groups={all} />}
            </>
          )
        }}
      </QueryState>
      {confirmDialog}
    </>
  )
}
