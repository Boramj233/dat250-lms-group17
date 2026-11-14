import { useState, type FormEvent } from 'react'
import { useCreateModule } from '../../../data/modules/hooks'
import type { CourseId } from '../../../shared/types'
import '../../../shared/styles/forms.css'

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
    createModule.mutate(
      { courseId, title: title.trim(), position },
      { onSuccess: () => setTitle('') },
    )
  }

  return (
    <section className="teacher-panel" aria-labelledby="add-module-title">
      <h2 id="add-module-title" className="teacher-panel-title">Add a module</h2>
      <form className="form" onSubmit={submit}>
        <div className="form-field">
          <label htmlFor="module-title">Title</label>
          <input id="module-title" required maxLength={255} value={title}
            onChange={event => setTitle(event.target.value)} placeholder="e.g. Messaging" />
        </div>
        {createModule.error && <p className="form-error" role="alert">Could not add the module. Try again.</p>}
        <div className="form-actions">
          <button type="submit" className="button-primary" disabled={createModule.isPending}>
            {createModule.isPending ? 'Adding…' : 'Add module'}
          </button>
        </div>
      </form>
    </section>
  )
}
