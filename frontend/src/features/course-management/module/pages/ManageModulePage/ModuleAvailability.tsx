import { useState, type FormEvent } from 'react'
import { useUpdateModule } from '../../../../modules/hooks'
import { canRequire, checkAvailabilityDates } from '../../../../modules/prerequisites'
import type { CourseModule } from '../../../../modules/types'
import { fromDateTimeLocal, toDateTimeLocal } from '../../../../../shared/lib/dateFormat'
import { checkModuleStartAgainstContent } from '../../../../content/visibility'
import { useManageModule } from '../../manageModuleContext'
import type { CourseId } from '../../../../../shared/types'
import '../../../../../shared/styles/forms.css'

type ModuleAvailabilityProps = {
  courseId: CourseId
  module: CourseModule
  /** All modules of the course, sorted by position. */
  modules: CourseModule[]
}

/**
 * When students can open the module: a start date, the modules they must complete
 * first, and a date from which it is open to everyone anyway.
 */
export default function ModuleAvailability({ courseId, module, modules }: ModuleAvailabilityProps) {
  return (
    <section aria-labelledby="availability-title">
      <h2 id="availability-title" className="section-title">Availability</h2>
      <div className="module-availability">
        {/* key: start again from the saved dates when they change. */}
        <AvailabilityDates
          key={`${module.availableFrom ?? ''}|${module.openToAllFrom ?? ''}`}
          courseId={courseId}
          module={module}
        />
        <Prerequisites courseId={courseId} module={module} modules={modules} />
      </div>
    </section>
  )
}

/** The two dates, with a Save button (typing a date should not save half a date). */
function AvailabilityDates({ courseId, module }: { courseId: CourseId, module: CourseModule }) {
  const updateModule = useUpdateModule()
  // The module's content: its own visible dates cannot come before the module's start.
  const { content } = useManageModule()
  const [availableFrom, setAvailableFrom] = useState(toDateTimeLocal(module.availableFrom))
  const [openToAllFrom, setOpenToAllFrom] = useState(toDateTimeLocal(module.openToAllFrom))
  const [problem, setProblem] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  function submit(event: FormEvent) {
    event.preventDefault()
    setSaved(false)
    const changes = { availableFrom: fromDateTimeLocal(availableFrom), openToAllFrom: fromDateTimeLocal(openToAllFrom) }
    const dateProblem = checkAvailabilityDates(changes.availableFrom, changes.openToAllFrom)
      ?? checkModuleStartAgainstContent(changes.availableFrom, content)
    setProblem(dateProblem ?? null)
    if (dateProblem) return
    updateModule.mutate({ courseId, moduleId: module.id, changes }, { onSuccess: () => setSaved(true) })
  }

  return (
    <form className="form" onSubmit={submit}>
      <div className="form-row">
        <div className="form-field">
          <label htmlFor="module-available-from">Available from</label>
          <input id="module-available-from" type="datetime-local" value={availableFrom}
            onChange={event => { setAvailableFrom(event.target.value); setSaved(false) }} />
          <p className="form-hint">
            Closed to students before this. Empty: no start date. Content without its own visible date is visible from then.
          </p>
        </div>
        <div className="form-field">
          <label htmlFor="module-open-to-all">Available for all from</label>
          <input id="module-open-to-all" type="datetime-local" value={openToAllFrom}
            onChange={event => { setOpenToAllFrom(event.target.value); setSaved(false) }} />
          <p className="form-hint">Open to everyone, also those who have not completed the modules below.</p>
        </div>
      </div>
      {problem && <p className="form-error" role="alert">{problem}</p>}
      {updateModule.error && <p className="form-error" role="alert">Could not save the dates. Try again.</p>}
      <div className="form-actions">
        <button type="submit" className="button-primary" disabled={updateModule.isPending}>
          {updateModule.isPending ? 'Saving…' : 'Save dates'}
        </button>
        {saved && <span className="form-success" role="status">Saved.</span>}
      </div>
    </form>
  )
}

/**
 * Which modules students must complete before this one opens: a checkbox per other
 * module, saved at once. Modules that already require this one cannot be ticked,
 * since neither module could then ever be opened.
 */
function Prerequisites({ courseId, module, modules }: ModuleAvailabilityProps) {
  const updateModule = useUpdateModule()
  const selected = new Set((module.prerequisiteModuleIds ?? []).map(String))
  const others = modules
    .map((m, index) => ({ module: m, number: index + 1 }))
    .filter(({ module: m }) => m.id !== module.id)

  function toggle(otherId: CourseModule['id'], on: boolean) {
    const next = on
      ? [...(module.prerequisiteModuleIds ?? []), otherId]
      : (module.prerequisiteModuleIds ?? []).filter(id => String(id) !== String(otherId))
    updateModule.mutate({ courseId, moduleId: module.id, changes: { prerequisiteModuleIds: next } })
  }

  if (others.length === 0) return <p>This is the only module, so no other modules can be required first.</p>

  return (
    <fieldset className="module-prerequisites">
      <legend>Students can open this module when they have completed:</legend>
      {others.map(({ module: other, number }) => {
        const allowed = canRequire(modules, module.id, other.id)
        return (
          <label key={other.id} className={`form-check${allowed ? '' : ' is-disabled'}`}>
            <input
              type="checkbox"
              checked={selected.has(String(other.id))}
              disabled={!allowed || updateModule.isPending}
              onChange={event => toggle(other.id, event.target.checked)}
            />
            Module {number}: {other.title}
            {!allowed && <span className="module-prerequisites-note"> (requires this module)</span>}
          </label>
        )
      })}
      <p className="form-hint">
        {selected.size === 0 ? 'Nothing ticked: no other modules are required first.' : `${selected.size} required.`}
      </p>
      {updateModule.error && <p className="form-error" role="alert">Could not save. Try again.</p>}
    </fieldset>
  )
}
