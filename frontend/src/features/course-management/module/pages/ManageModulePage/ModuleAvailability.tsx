import { canRequire } from '../../../../modules/prerequisites'
import type { CourseModule } from '../../../../modules/types'
import type { ModuleId } from '../../../../../shared/types'
import type { AvailabilityDraft } from './availabilityDraft'
import '../../../../../shared/styles/forms.css'

type ModuleAvailabilityProps = {
  module: CourseModule
  /** All modules of the course, sorted by position. */
  modules: CourseModule[]
  value: AvailabilityDraft
  /** Which fields differ from what is saved, to mark them. */
  saved: AvailabilityDraft
  disabled: boolean
  onChange: (value: AvailabilityDraft) => void
}

/**
 * When students can open the module: a start date, a date from which it is open to
 * everyone anyway, and the modules they must complete first. Only the fields: the page's
 * one Save changes saves them together with the rest of the page.
 */
export default function ModuleAvailability({ module, modules, value, saved, disabled, onChange }: ModuleAvailabilityProps) {
  const others = modules
    .map((m, index) => ({ module: m, number: index + 1 }))
    .filter(({ module: m }) => m.id !== module.id)
  const changedClass = (changed: boolean) => `form-field${changed ? ' is-unsaved' : ''}`

  function toggle(otherId: ModuleId, on: boolean) {
    const id = String(otherId)
    onChange({ ...value, prerequisites: on ? [...value.prerequisites, id] : value.prerequisites.filter(other => other !== id) })
  }

  return (
    <section aria-labelledby="availability-title">
      <h2 id="availability-title" className="section-title">Availability</h2>
      <div className="form module-availability">
        <div className="form-row">
          <div className={changedClass(value.availableFrom !== saved.availableFrom)}>
            <label htmlFor="module-available-from">Available from</label>
            <input id="module-available-from" type="datetime-local" value={value.availableFrom} disabled={disabled}
              onChange={event => onChange({ ...value, availableFrom: event.target.value })} />
            <p className="form-hint">
              Closed to students before this. Empty: no start date. Content without its own visible date is visible from then.
            </p>
          </div>
          <div className={changedClass(value.openToAllFrom !== saved.openToAllFrom)}>
            <label htmlFor="module-open-to-all">Available for all from</label>
            <input id="module-open-to-all" type="datetime-local" value={value.openToAllFrom} disabled={disabled}
              onChange={event => onChange({ ...value, openToAllFrom: event.target.value })} />
            <p className="form-hint">Open to everyone, also those who have not completed the modules below.</p>
          </div>
        </div>

        {others.length === 0
          ? <p>This is the only module, so no other modules can be required first.</p>
          : (
            <fieldset className="module-prerequisites">
              <legend>Students can open this module when they have completed:</legend>
              {others.map(({ module: other, number }) => {
                // Modules that already require this one cannot be ticked: neither could open.
                const allowed = canRequire(modules, module.id, other.id)
                const ticked = value.prerequisites.includes(String(other.id))
                const changed = ticked !== saved.prerequisites.includes(String(other.id))
                return (
                  <label key={other.id} className={`form-check${allowed ? '' : ' is-disabled'}${changed ? ' is-unsaved' : ''}`}>
                    <input
                      type="checkbox"
                      checked={ticked}
                      disabled={!allowed || disabled}
                      onChange={event => toggle(other.id, event.target.checked)}
                    />
                    Module {number}: {other.title}
                    {!allowed && <span className="module-prerequisites-note"> (requires this module)</span>}
                  </label>
                )
              })}
              <p className="form-hint">
                {value.prerequisites.length === 0
                  ? 'Nothing ticked: no other modules are required first.'
                  : `${value.prerequisites.length} required.`}
              </p>
            </fieldset>
          )}
      </div>
    </section>
  )
}
