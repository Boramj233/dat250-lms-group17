import { useState, type FormEvent } from 'react'
import { useAdminUser, useUpdateUserAccess } from '@domain/users/hooks'
import { errorMessage } from '@shared/api/errorMessage'
import { ACCESS_LABEL, ACCESS_LEVELS, type Access } from '@shared/auth/access'
import { useCurrentUser } from '@shared/auth/useAuth'
import { fullName } from '@shared/lib/fullName'
import type { UserId } from '@shared/types'
import { Field, FormError, FormSuccess, Panel, SubmitButton } from '@shared/ui/Form'

const ACCESS_HINT: Record<Access, string> = {
  STANDARD: 'Takes part in the courses they are added to.',
  COURSE_MANAGER: 'Can also create courses and find users to add to them.',
  ADMIN: 'Can also manage every user and their access.',
}

type UserAccessPanelProps = {
  userId: UserId
  onClose: () => void
}

/**
 * Changes one user's global access. The user is loaded again (GET /api/admin/users/{id}),
 * so the form starts from what is saved now. An administrator cannot remove their own
 * ADMIN access (the backend answers 403).
 */
export default function UserAccessPanel({ userId, onClose }: UserAccessPanelProps) {
  const user = useAdminUser(userId)
  const me = useCurrentUser().data
  const updateAccess = useUpdateUserAccess()
  const [access, setAccess] = useState<Access | null>(null)
  const [saved, setSaved] = useState(false)

  if (user.isPending) return <p role="status">Loading the user…</p>
  if (user.error || !user.data) return <p role="alert">Could not load the user. Try again.</p>

  const account = user.data
  const chosen = access ?? account.access
  const isMe = me !== undefined && String(me.userId) === String(account.userId)

  function submit(event: FormEvent) {
    event.preventDefault()
    setSaved(false)
    updateAccess.mutate(
      { userId: account.userId, access: chosen },
      {
        onSuccess: () => {
          setAccess(null)
          setSaved(true)
        },
      },
    )
  }

  return (
    <Panel title={`Access for ${fullName(account)}`}>
      <form className="form" onSubmit={submit}>
        <p className="text-meta">
          {account.email} · now {ACCESS_LABEL[account.access].toLowerCase()}
        </p>
        <Field
          label="Access"
          required
          hint={isMe ? 'You cannot remove your own administrator access.' : ACCESS_HINT[chosen]}
        >
          {props => (
            <select
              {...props}
              value={chosen}
              onChange={event => {
                setAccess(event.target.value as Access)
                setSaved(false)
              }}
            >
              {ACCESS_LEVELS.map(level => (
                <option key={level} value={level} disabled={isMe && level !== 'ADMIN'}>
                  {ACCESS_LABEL[level]}
                </option>
              ))}
            </select>
          )}
        </Field>
        <FormError>
          {updateAccess.error &&
            errorMessage(
              updateAccess.error,
              {
                403: 'You cannot remove your own administrator access.',
                404: 'This user no longer exists.',
              },
              'Could not change the access. Try again.',
            )}
        </FormError>
        <FormSuccess>
          {saved && `${fullName(account)} is now ${ACCESS_LABEL[account.access].toLowerCase()}.`}
        </FormSuccess>
        <div className="form-actions">
          <SubmitButton pending={updateAccess.isPending} disabled={chosen === account.access}>
            Save access
          </SubmitButton>
          <button type="button" className="button-secondary" onClick={onClose}>
            Close
          </button>
        </div>
      </form>
    </Panel>
  )
}
