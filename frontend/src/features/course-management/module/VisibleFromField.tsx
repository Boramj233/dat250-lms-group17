import { formatDateTime, toDateTimeLocal } from '../../../shared/lib/dateFormat'
import type { CourseModule } from '../../modules/types'
import '../../../shared/styles/forms.css'

type VisibleFromFieldProps = {
  /** A datetime-local value; empty inherits the module's start date. */
  value: string
  onChange: (value: string) => void
  module: CourseModule
  /** Shown instead of the hint, e.g. that the date is before the module's. */
  problem?: string
}

/**
 * When students can see a content item. Empty: as soon as the module is available. It
 * cannot be before the module's start date, so that is the input's minimum.
 */
export default function VisibleFromField({ value, onChange, module, problem }: VisibleFromFieldProps) {
  const moduleStart = module.availableFrom ? new Date(module.availableFrom) : null
  return (
    <div className="form-field">
      <label htmlFor="content-visible-from">Visible from</label>
      <input id="content-visible-from" type="datetime-local" value={value}
        min={module.availableFrom ? toDateTimeLocal(module.availableFrom) : undefined}
        aria-describedby="content-visible-from-hint" aria-invalid={problem ? true : undefined}
        onChange={event => onChange(event.target.value)} />
      <p id="content-visible-from-hint" className={problem ? 'form-error' : 'form-hint'}>
        {problem ?? (moduleStart
          ? `Empty: visible when the module is, ${formatDateTime(moduleStart)}. It cannot be earlier than that. Your local time.`
          : 'Empty: visible as soon as the module is. Your local time.')}
      </p>
    </div>
  )
}
