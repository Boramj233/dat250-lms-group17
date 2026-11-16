import { useState } from 'react'
import { useCurrentUser } from '@shared/auth/useAuth'
import { useCourseEnrollments, useRemoveCourseEnrollment, useUpdateCourseRole } from '@domain/courses/hooks'
import QueryState from '@shared/ui/QueryState'
import { useConfirm } from '@shared/ui/ConfirmDialog'
import SaveBar from '@shared/ui/SaveBar'
import { useManageCourse } from '../../manageCourseContext'
import { fullName } from '@shared/lib/fullName'
import { byRoleThenName } from '@domain/courses/members'
import AddMemberForm from './AddMemberForm'
import {
  EMPTY_DRAFT,
  isLastTeacher,
  isRemoved,
  isRoleChanged,
  memberChanges,
  roleAfter,
  withoutSavedRemoval,
  withoutSavedRole,
  withRemoved,
  withRole,
  type MemberDraft,
} from './memberDraft'
import MemberRow from './MemberRow'
import { formatCount } from '@shared/lib/formatCount'
import '@shared/styles/forms.css'
import './CourseMembersPage.css'

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
  const [draft, setDraft] = useState<MemberDraft>(EMPTY_DRAFT)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState<string | null>(null)

  const members = [...(enrollments.data ?? [])].sort(byRoleThenName)
  const teacherCount = members.filter(member => member.role === 'TEACHER').length

  /** Changes the draft, and clears the last save's message. */
  function change(update: (draft: MemberDraft) => MemberDraft) {
    setDraft(update)
    setSaved(null)
    setError(null)
  }

  async function save() {
    const removing = members.filter(member => isRemoved(draft, member))
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
        setDraft(d => withoutSavedRole(d, id))
      }
      for (const id of draft.removals) {
        await remove.mutateAsync({ courseId: course.id, userId: id })
        setDraft(d => withoutSavedRemoval(d, id))
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
        {enrollments.data && ` · ${formatCount(teacherCount, 'teacher')}, ${members.length - teacherCount} students`}
      </h2>
      <QueryState query={enrollments} loading="Loading members…" error="Could not load the members of this course.">
        {() => (
          <ul className="member-list">
            {members.map(member => (
              <MemberRow
                key={member.userId}
                member={member}
                isMe={String(member.userId) === String(me.userId)}
                role={roleAfter(draft, member)}
                roleChanged={isRoleChanged(draft, member)}
                removed={isRemoved(draft, member)}
                lastTeacher={isLastTeacher(draft, member, members)}
                saving={saving}
                onRoleChange={role => change(d => withRole(d, member, role))}
                onRemovedChange={removed => change(d => withRemoved(d, member, removed))}
              />
            ))}
          </ul>
        )}
      </QueryState>
      <SaveBar
        changes={memberChanges(draft)}
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
