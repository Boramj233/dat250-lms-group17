import { useEffect, useId, useRef, type ReactNode } from 'react'
import '../../styles/forms.css'
import './ConfirmDialog.css'

type ConfirmDialogProps = {
  /** Shown while true. */
  open: boolean
  title: string
  children: ReactNode
  /** The confirm button's text, e.g. "Yes, leave and join". */
  confirmLabel: string
  cancelLabel?: string
  /** Disables the buttons, e.g. while the action runs. */
  busy?: boolean
  /** A destructive action (delete, remove, discard): the confirm button is red. */
  danger?: boolean
  onConfirm: () => void
  /** Cancel, Esc, or a click outside the box. */
  onCancel: () => void
}

/**
 * A modal pop-up asking the user to confirm an action. Built on <dialog>, so focus stays
 * inside it and Esc closes it. Cancel has the focus at first, so Enter does not confirm
 * by accident.
 */
export default function ConfirmDialog({
  open,
  title,
  children,
  confirmLabel,
  cancelLabel = 'Cancel',
  busy = false,
  danger = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  return (
    <dialog
      ref={ref}
      className="confirm-dialog"
      aria-labelledby={titleId}
      // Esc: let React state close it.
      onCancel={event => {
        event.preventDefault()
        if (!busy) onCancel()
      }}
      // A click on the backdrop (the dialog element itself, outside the box).
      onClick={event => {
        if (event.target === event.currentTarget && !busy) onCancel()
      }}
    >
      <div className="confirm-dialog-box">
        <h2 id={titleId} className="confirm-dialog-title">
          {title}
        </h2>
        <div className="confirm-dialog-body">{children}</div>
        <div className="confirm-dialog-actions">
          <button type="button" className="button-secondary" autoFocus disabled={busy} onClick={onCancel}>
            {cancelLabel}
          </button>
          <button
            type="button"
            className={danger ? 'button-danger' : 'button-primary'}
            disabled={busy}
            onClick={onConfirm}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </dialog>
  )
}
