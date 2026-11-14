import { canRequire } from '../../../../data/modules/prerequisites'
import { useUpdateModule } from '../../../../data/modules/hooks'
import type { CourseModule } from '../../../../data/modules/types'
import type { CourseId } from '../../../../shared/types'
import '../../../../shared/styles/forms.css'

type ModulePrerequisitesProps = {
  courseId: CourseId
  module: CourseModule
  /** All modules of the course, sorted by position. */
  modules: CourseModule[]
}

/**
 * Which modules students must complete before this one opens: a checkbox per other
 * module, saved at once. Modules that already require this one cannot be ticked,
 * since neither module could then ever be opened.
 */
export default function ModulePrerequisites({ courseId, module, modules }: ModulePrerequisitesProps) {
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

  return (
    <section aria-labelledby="unlock-title">
      <h2 id="unlock-title" className="section-title">Unlocking</h2>
      {others.length === 0
        ? <p>This is the only module, so it is open from the start.</p>
        : (
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
              {selected.size === 0 ? 'Nothing ticked: the module is open from the start.' : `${selected.size} required.`}
            </p>
          </fieldset>
        )}
      {updateModule.error && <p className="form-error" role="alert">Could not save. Try again.</p>}
    </section>
  )
}
