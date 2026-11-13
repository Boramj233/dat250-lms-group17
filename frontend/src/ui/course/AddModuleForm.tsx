import { useState, type FormEvent } from 'react'
import { useCreateModule } from '../../data/module/hooks/mutations/useCreateModule'
import { useCourseContext } from './courseContext'
import '../components/forms.css'

/** Teachers: add a module at the end of the course. */
export default function AddModuleForm() {
  const { course, modules } = useCourseContext()
  const createModule = useCreateModule()
  const [title, setTitle] = useState('')

  function submit(event: FormEvent) {
    event.preventDefault()
    createModule.mutate(
      { courseId: course.id, title: title.trim(), position: modules.length },
      { onSuccess: () => setTitle('') },
    )
  }

  return (
    <section className="teacher-panel" aria-labelledby="add-module-title">
      <h2 id="add-module-title" className="teacher-panel-title">
        Add a module
      </h2>
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
