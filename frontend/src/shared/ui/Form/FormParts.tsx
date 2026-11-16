import { useId, type ReactNode } from 'react'
import '../../styles/forms.css'

type SubmitButtonProps = {
  /** While saving: disabled, and shows `pendingLabel`. */
  pending: boolean
  /** E.g. "Adding…". */
  pendingLabel?: string
  /** Disabled for another reason, e.g. nothing chosen yet. */
  disabled?: boolean
  children: ReactNode
}

/** The form's main button: "Add module", or "Adding…" while it is saved. */
export function SubmitButton({ pending, pendingLabel = 'Saving…', disabled, children }: SubmitButtonProps) {
  return (
    <button type="submit" className="button-primary" disabled={pending || disabled}>
      {pending ? pendingLabel : children}
    </button>
  )
}

/** A message that saving failed or the form cannot be sent; read out at once. Nothing when empty. */
export function FormError({ children }: { children?: ReactNode }) {
  if (children === undefined || children === null || children === false || children === '') return null
  return (
    <p className="form-error" role="alert">
      {children}
    </p>
  )
}

/** A message that saving worked, e.g. "Saved."; `inline` to put it beside the buttons. Nothing when empty. */
export function FormSuccess({ children, inline }: { children?: ReactNode; inline?: boolean }) {
  if (children === undefined || children === null || children === false || children === '') return null
  const Element = inline ? 'span' : 'p'
  return (
    <Element className="form-success" role="status">
      {children}
    </Element>
  )
}

/** "Fields marked * are required.", at the top of a form with several fields. */
export function RequiredFieldsNote() {
  return (
    <p className="form-hint">
      Fields marked <span className="form-required">*</span> are required.
    </p>
  )
}

/** A box with a heading around a small form on a page, e.g. "Add a module". */
export function Panel({ title, children }: { title: ReactNode; children: ReactNode }) {
  const titleId = useId()
  return (
    <section className="teacher-panel" aria-labelledby={titleId}>
      <h2 id={titleId} className="teacher-panel-title">
        {title}
      </h2>
      {children}
    </section>
  )
}
