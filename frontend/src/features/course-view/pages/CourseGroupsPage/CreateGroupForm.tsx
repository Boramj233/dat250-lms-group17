import { useState, type FormEvent } from 'react'
import { errorMessage } from '@shared/api/errorMessage'
import type { CourseId } from '@shared/types'
import { nextGroupNumber, useCreateGroup } from '@domain/groups/hooks'
import type { CourseGroup } from '@domain/groups/types'
import '@shared/styles/forms.css'

type CreateGroupFormProps = {
  courseId: CourseId
  /** The course's groups; their numbers are taken. */
  groups: CourseGroup[]
}

/** Teachers: a new, empty group. Students join it themselves. */
export default function CreateGroupForm({ courseId, groups }: CreateGroupFormProps) {
  // Empty: suggest the lowest free number.
  const [number, setNumber] = useState('')
  const [created, setCreated] = useState<string | null>(null)
  const create = useCreateGroup(courseId)
  const suggested = nextGroupNumber(groups)
  const groupNumber = number === '' ? suggested : Number(number)
  const taken = groups.some(group => group.groupNumber === groupNumber)
  const invalid = !Number.isInteger(groupNumber) || groupNumber < 1

  function submit(event: FormEvent) {
    event.preventDefault()
    if (invalid || taken) return
    create.mutate(groupNumber, {
      onSuccess: () => {
        setCreated(`Group ${groupNumber} was created.`)
        setNumber('')
      },
    })
  }

  return (
    <section className="teacher-panel" aria-labelledby="create-group-title">
      <h2 id="create-group-title" className="teacher-panel-title">Create a group</h2>
      <form className="form" onSubmit={submit} noValidate>
        <div className="form-row course-groups-create">
          <div className="form-field">
            <label htmlFor="group-number">Group number</label>
            <input
              id="group-number"
              type="number"
              min={1}
              step={1}
              inputMode="numeric"
              value={number}
              placeholder={String(suggested)}
              aria-describedby="group-number-hint"
              onChange={event => { setNumber(event.target.value); setCreated(null); create.reset() }}
            />
          </div>
          <div className="form-actions">
            <button type="submit" className="button-primary" disabled={create.isPending || invalid || taken}>
              {create.isPending ? 'Creating…' : 'Create group'}
            </button>
          </div>
        </div>
        <p id="group-number-hint" className="form-hint">
          {invalid
            ? 'The number must be a whole number of 1 or more.'
            : taken
              ? `Group ${groupNumber} already exists.`
              : 'The group starts empty; students join it themselves, one group each.'}
        </p>
        {create.error && <p className="form-error" role="alert">{createError(create.error, groupNumber)}</p>}
        {created && <p className="form-success" role="status">{created}</p>}
      </form>
    </section>
  )
}

function createError(error: Error, groupNumber: number): string {
  return errorMessage(error, {
    409: `Group ${groupNumber} already exists.`,
    400: 'The number must be a whole number of 1 or more.',
    403: 'Only teachers of the course can create groups.',
  }, 'The group could not be created. Try again.')
}
