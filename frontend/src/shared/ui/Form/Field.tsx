import { useId, type ReactNode } from 'react'
import RequiredMark from '../RequiredMark'
import '../../styles/forms.css'

/** What Field gives its control: spread it on the input, select or textarea. */
export type FieldControlProps = {
  id: string
  required?: boolean
  'aria-describedby'?: string
  'aria-invalid'?: true
}

type FieldProps = {
  label: ReactNode
  /** Shows "*" after the label and makes the control required (it can override `required`). */
  required?: boolean
  /** Help below the control (or above it with `hintAbove`); screen readers read it with the control. */
  hint?: ReactNode
  hintAbove?: boolean
  /**
   * What is wrong, below the control; it replaces a hint below. Marks the control invalid.
   * Nothing (undefined, null, false or '') when it is fine.
   */
  error?: ReactNode
  /** The control's id; generated when left out. */
  id?: string
  /** More classes on the field, e.g. "is-unsaved". */
  className?: string
  /** The control, given its id, required and aria props: `{props => <input {...props} … />}`. */
  children: (control: FieldControlProps) => ReactNode
}

/**
 * One form field: the label (with "*" when required), the control, and a hint or an error,
 * linked to the control with aria-describedby. For a group of checkboxes or radios use a
 * <fieldset> with a <legend> instead.
 *
 *   <Field label="Title" required hint="Shown to students.">
 *     {props => <input {...props} value={title} onChange={…} />}
 *   </Field>
 */
export default function Field({ label, required, hint, hintAbove, error, id, className, children }: FieldProps) {
  const generatedId = useId()
  const controlId = id ?? generatedId
  const hintId = `${controlId}-hint`
  const errorId = `${controlId}-error`
  const hasError = error !== undefined && error !== null && error !== false && error !== ''
  // An error below replaces a hint below; a hint above stays.
  const showHint = hint !== undefined && (hintAbove || !hasError)
  const describedBy = [showHint && hintId, hasError && errorId].filter(Boolean).join(' ') || undefined
  const hintElement = showHint && (
    <p id={hintId} className="form-hint">
      {hint}
    </p>
  )
  return (
    <div className={className ? `form-field ${className}` : 'form-field'}>
      <label htmlFor={controlId}>
        {label}
        {required && <RequiredMark />}
      </label>
      {hintAbove && hintElement}
      {children({
        id: controlId,
        required: required || undefined,
        'aria-describedby': describedBy,
        'aria-invalid': hasError || undefined,
      })}
      {!hintAbove && hintElement}
      {hasError && (
        <p id={errorId} className="form-error" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}
