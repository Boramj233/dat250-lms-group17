import { useId, useState, type FormEvent } from 'react'
import { errorMessage } from '@shared/api/errorMessage'
import type { CourseId } from '@shared/types'
import { nextGroupNumber, useCreateGroup } from '@domain/groups/hooks'
import type { CourseGroup } from '@domain/groups/types'
import { Field, FormError, FormSuccess, Panel, SubmitButton } from '@shared/ui/Form'

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
  const hintId = useId()
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
    <Panel title="Create a group">
      <form className="form" onSubmit={submit} noValidate>
        <div className="form-row course-groups-create">
          <Field label="Group number">
            {props => (
              <input
                {...props}
                type="number"
                min={1}
                step={1}
                inputMode="numeric"
                value={number}
                placeholder={String(suggested)}
                aria-describedby={hintId}
                onChange={event => {
                  setNumber(event.target.value)
                  setCreated(null)
                  create.reset()
                }}
              />
            )}
          </Field>
          <div className="form-actions">
            <SubmitButton pending={create.isPending} pendingLabel="Creating…" disabled={invalid || taken}>
              Create group
            </SubmitButton>
          </div>
        </div>
        {/* Below the row with the button, so the field and the button line up. */}
        <p id={hintId} className="form-hint">
          {invalid
            ? 'The number must be a whole number of 1 or more.'
            : taken
              ? `Group ${groupNumber} already exists.`
              : 'The group starts empty; students join it themselves, one group each.'}
        </p>
        <FormError>{create.error && createError(create.error, groupNumber)}</FormError>
        <FormSuccess>{created}</FormSuccess>
      </form>
    </Panel>
  )
}

function createError(error: Error, groupNumber: number): string {
  return errorMessage(
    error,
    {
      409: `Group ${groupNumber} already exists.`,
      400: 'The number must be a whole number of 1 or more.',
      403: 'Only teachers of the course can create groups.',
    },
    'The group could not be created. Try again.',
  )
}
