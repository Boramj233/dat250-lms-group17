import { useState, type FormEvent } from 'react'
import { useEnrollInCourse } from '@domain/courses/hooks'
import type { CourseEnrollment, CourseRole } from '@domain/courses/types'
import { useUserSearch } from '@domain/users/hooks'
import type { UserLookup } from '@domain/users/types'
import { useDebounced } from '@shared/lib/useDebounced'
import type { CourseId } from '@shared/types'
import { ROLE_LABEL } from '@domain/courses/members'
import { fullName } from '@shared/lib/fullName'
import { Field, FormError, FormSuccess, Panel, SubmitButton } from '@shared/ui/Form'

type AddMemberFormProps = {
  courseId: CourseId
  /** Current members; they are shown but cannot be added again. */
  members: CourseEnrollment[]
}

/** Find a user by name or email and add them as a student or a teacher. */
export default function AddMemberForm({ courseId, members }: AddMemberFormProps) {
  const [search, setSearch] = useState('')
  const [selected, setSelected] = useState<UserLookup | null>(null)
  const [role, setRole] = useState<CourseRole>('STUDENT')
  const [added, setAdded] = useState<string | null>(null)
  const results = useUserSearch(useDebounced(search))
  const enroll = useEnrollInCourse()
  const memberIds = new Set(members.map(member => String(member.userId)))

  function submit(event: FormEvent) {
    event.preventDefault()
    if (!selected) return
    enroll.mutate(
      { courseId, userId: selected.userId, role },
      {
        onSuccess: () => {
          setAdded(`${fullName(selected)} was added as ${ROLE_LABEL[role].toLowerCase()}.`)
          setSelected(null)
          setSearch('')
        },
      },
    )
  }

  return (
    <Panel title="Add a member">
      <form className="form" onSubmit={submit}>
        <Field label="Name or email" required>
          {props => (
            <input
              {...props}
              // Not required for the browser: the button stays off until a user is chosen.
              required={undefined}
              aria-required="true"
              type="search"
              autoComplete="off"
              value={search}
              onChange={event => {
                setSearch(event.target.value)
                setSelected(null)
                setAdded(null)
              }}
              placeholder="Search for a user"
            />
          )}
        </Field>

        {!selected && search.trim().length >= 2 && (
          <div className="member-results" aria-live="polite">
            {results.isFetching && <p className="form-hint">Searching…</p>}
            {results.error && <p className="form-error">Searching for users failed.</p>}
            {results.data?.users.length === 0 && <p className="form-hint">No users found.</p>}
            {results.data && results.data.users.length > 0 && (
              <ul className="member-result-list">
                {results.data.users.map(user => {
                  const enrolled = memberIds.has(String(user.userId))
                  return (
                    <li key={user.userId}>
                      <button
                        type="button"
                        className="member-result"
                        disabled={enrolled}
                        onClick={() => setSelected(user)}
                      >
                        <span className="member-full-name">{fullName(user)}</span>
                        <span className="member-email">
                          {user.email}
                          {enrolled && ' · already in the course'}
                        </span>
                      </button>
                    </li>
                  )
                })}
              </ul>
            )}
          </div>
        )}

        {selected && (
          <div className="form-row">
            <p className="member-selected">
              <span className="member-full-name">{fullName(selected)}</span>
              <span className="member-email">{selected.email}</span>
            </p>
            <Field label="Role" required>
              {props => (
                <select {...props} value={role} onChange={event => setRole(event.target.value as CourseRole)}>
                  <option value="STUDENT">{ROLE_LABEL.STUDENT}</option>
                  <option value="TEACHER">{ROLE_LABEL.TEACHER}</option>
                </select>
              )}
            </Field>
          </div>
        )}

        <FormError>{enroll.error && 'Could not add the member. Try again.'}</FormError>
        <FormSuccess>{added}</FormSuccess>
        <div className="form-actions">
          <SubmitButton pending={enroll.isPending} pendingLabel="Adding…" disabled={!selected}>
            Add to course
          </SubmitButton>
        </div>
      </form>
    </Panel>
  )
}
