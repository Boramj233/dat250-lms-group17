import { useState, type FormEvent } from 'react'
import { useUpdateMyProfile } from '@domain/users/hooks'
import { errorMessage } from '@shared/api/errorMessage'
import { ACCESS_LABEL } from '@shared/auth/access'
import { useCurrentUser } from '@shared/auth/useAuth'
import { TrailPage } from '@shared/navigation/Trail'
import { Field, FormError, FormSuccess, Panel, SubmitButton } from '@shared/ui/Form'
import PageHeader from '@shared/ui/PageHeader'
import './ProfilePage.css'

/**
 * The logged-in user's account details, as the backend knows them, and a form to change
 * their names (PUT /api/users/me). Email and access cannot be changed here.
 */
export default function ProfilePage() {
  const user = useCurrentUser().data!
  const updateProfile = useUpdateMyProfile()
  const [firstName, setFirstName] = useState(user.firstName)
  const [lastName, setLastName] = useState(user.lastName)
  const [saved, setSaved] = useState(false)
  const changed = firstName.trim() !== user.firstName || lastName.trim() !== user.lastName

  function submit(event: FormEvent) {
    event.preventDefault()
    setSaved(false)
    updateProfile.mutate(
      { firstName: firstName.trim(), lastName: lastName.trim() },
      {
        onSuccess: updated => {
          setFirstName(updated.firstName)
          setLastName(updated.lastName)
          setSaved(true)
        },
      },
    )
  }

  function cancel() {
    setFirstName(user.firstName)
    setLastName(user.lastName)
    updateProfile.reset()
  }

  return (
    <TrailPage label="Profile">
      <PageHeader title="Profile" />
      <dl className="profile-facts">
        <dt>User ID</dt>
        <dd>{user.userId}</dd>
        <dt>Name</dt>
        <dd>
          {user.firstName} {user.lastName}
        </dd>
        <dt>Email</dt>
        <dd>{user.email}</dd>
        <dt>Access</dt>
        <dd>{ACCESS_LABEL[user.access]}</dd>
      </dl>

      <Panel title="Change your name">
        <form className="form" onSubmit={submit}>
          <div className="form-row">
            <Field label="First name" required>
              {props => (
                <input
                  {...props}
                  autoComplete="given-name"
                  maxLength={255}
                  value={firstName}
                  onChange={event => {
                    setFirstName(event.target.value)
                    setSaved(false)
                  }}
                />
              )}
            </Field>
            <Field label="Last name" required>
              {props => (
                <input
                  {...props}
                  autoComplete="family-name"
                  maxLength={255}
                  value={lastName}
                  onChange={event => {
                    setLastName(event.target.value)
                    setSaved(false)
                  }}
                />
              )}
            </Field>
          </div>
          <p className="form-hint">Your email and access are managed by an administrator.</p>
          <FormError>
            {updateProfile.error &&
              errorMessage(
                updateProfile.error,
                { 400: 'Both names are required, at most 255 characters each.' },
                'Could not save your name. Try again.',
              )}
          </FormError>
          <FormSuccess>{saved && 'Your name was saved.'}</FormSuccess>
          <div className="form-actions">
            <SubmitButton
              pending={updateProfile.isPending}
              disabled={!changed || !firstName.trim() || !lastName.trim()}
            >
              Save changes
            </SubmitButton>
            <button
              type="button"
              className="button-secondary"
              disabled={!changed || updateProfile.isPending}
              onClick={cancel}
            >
              Cancel
            </button>
          </div>
        </form>
      </Panel>
    </TrailPage>
  )
}
