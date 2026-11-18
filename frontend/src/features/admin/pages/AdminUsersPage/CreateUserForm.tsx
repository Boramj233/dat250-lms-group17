import { useState, type FormEvent } from 'react'
import { useCreateUser } from '@domain/users/hooks'
import { errorMessage } from '@shared/api/errorMessage'
import { fullName } from '@shared/lib/fullName'
import { Field, FormError, FormSuccess, Panel, SubmitButton } from '@shared/ui/Form'

const EMPTY = { firstName: '', lastName: '', email: '' }

/**
 * Adds one profile (POST /api/admin/users). Only the profile: the person logs in with
 * the login service's account for the same email. New users get standard access.
 */
export default function CreateUserForm() {
  const createUser = useCreateUser()
  const [values, setValues] = useState(EMPTY)
  const [added, setAdded] = useState<string | null>(null)
  const complete = values.firstName.trim() && values.lastName.trim() && values.email.trim()

  const set = (key: keyof typeof EMPTY) => (value: string) => {
    setValues(v => ({ ...v, [key]: value }))
    setAdded(null)
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    createUser.mutate(
      { firstName: values.firstName.trim(), lastName: values.lastName.trim(), email: values.email.trim() },
      {
        onSuccess: user => {
          setAdded(`${fullName(user)} was added with user ID ${user.userId}.`)
          setValues(EMPTY)
        },
      },
    )
  }

  return (
    <Panel title="Add a user">
      <form className="form" onSubmit={submit}>
        <div className="form-row">
          <Field label="First name" required>
            {props => (
              <input
                {...props}
                maxLength={255}
                value={values.firstName}
                onChange={e => set('firstName')(e.target.value)}
              />
            )}
          </Field>
          <Field label="Last name" required>
            {props => (
              <input
                {...props}
                maxLength={255}
                value={values.lastName}
                onChange={e => set('lastName')(e.target.value)}
              />
            )}
          </Field>
        </div>
        <Field label="Email" required hint="The email of their account in the login service.">
          {props => (
            <input
              {...props}
              type="email"
              autoComplete="off"
              maxLength={255}
              value={values.email}
              onChange={e => set('email')(e.target.value)}
            />
          )}
        </Field>
        <FormError>
          {createUser.error &&
            errorMessage(
              createUser.error,
              {
                400: 'Check the names and the email.',
                409: 'A user with this email already exists.',
              },
              'Could not add the user. Try again.',
            )}
        </FormError>
        <FormSuccess>{added}</FormSuccess>
        <div className="form-actions">
          <SubmitButton pending={createUser.isPending} pendingLabel="Adding…" disabled={!complete}>
            Add user
          </SubmitButton>
        </div>
      </form>
    </Panel>
  )
}
