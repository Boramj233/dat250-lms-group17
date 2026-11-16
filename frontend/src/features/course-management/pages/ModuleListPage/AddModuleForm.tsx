import { useState, type FormEvent } from 'react'
import { useCreateModule } from '@domain/modules/hooks'
import type { CourseId } from '@shared/types'
import { Field, FormError, Panel, SubmitButton } from '@shared/ui/Form'

type AddModuleFormProps = {
  courseId: CourseId
  /** Where the new module goes: the number of modules, so it ends up last. */
  position: number
}

/** Add a module at the end of the course. */
export default function AddModuleForm({ courseId, position }: AddModuleFormProps) {
  const createModule = useCreateModule()
  const [title, setTitle] = useState('')

  function submit(event: FormEvent) {
    event.preventDefault()
    createModule.mutate({ courseId, title: title.trim(), position }, { onSuccess: () => setTitle('') })
  }

  return (
    <Panel title="Add a module">
      <form className="form" onSubmit={submit}>
        <Field label="Title" required>
          {props => (
            <input
              {...props}
              maxLength={255}
              value={title}
              onChange={event => setTitle(event.target.value)}
              placeholder="e.g. Messaging"
            />
          )}
        </Field>
        <FormError>{createModule.error && 'Could not add the module. Try again.'}</FormError>
        <div className="form-actions">
          <SubmitButton pending={createModule.isPending} pendingLabel="Adding…">
            Add module
          </SubmitButton>
        </div>
      </form>
    </Panel>
  )
}
