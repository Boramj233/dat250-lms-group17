import type { ReactNode } from 'react'
import { useLeaveGuard } from './useLeaveGuard'
import '../../styles/forms.css'
import './SaveBar.css'

type SaveBarProps = {
  /** How many changes are not saved yet; 0 hides the buttons. */
  changes: number
  saving: boolean
  /** Shown in the bar when saving failed. */
  error?: ReactNode
  /** Shown in the bar after a save, until the next change. */
  saved?: ReactNode
  onSave: () => void
  /** Throws the unsaved changes away. */
  onCancel: () => void
  saveLabel?: string
}

/**
 * Save changes / Cancel for a page whose edits are collected first: it sticks to the
 * bottom of the window while there are unsaved changes, and says how many. It also asks
 * before the user leaves the page with unsaved changes (useLeaveGuard).
 */
export default function SaveBar({
  changes,
  saving,
  error,
  saved,
  onSave,
  onCancel,
  saveLabel = 'Save changes',
}: SaveBarProps) {
  const leaveDialog = useLeaveGuard(changes > 0)
  const visible = changes > 0 || saving || Boolean(error) || Boolean(saved)

  return (
    <>
      {visible && (
        <div className={`save-bar${changes > 0 ? ' is-dirty' : ''}`} role="region" aria-label="Unsaved changes">
          <p className="save-bar-status" aria-live="polite">
            {saving ? (
              'Saving…'
            ) : error ? (
              <span className="save-bar-error" role="alert">
                {error}
              </span>
            ) : changes > 0 ? (
              <>
                <strong>
                  {changes} unsaved {changes === 1 ? 'change' : 'changes'}
                </strong>{' '}
                · save them or cancel
              </>
            ) : (
              <span className="save-bar-saved">{saved}</span>
            )}
          </p>
          {changes > 0 && (
            <div className="save-bar-actions">
              <button type="button" className="button-secondary" disabled={saving} onClick={onCancel}>
                Cancel
              </button>
              <button type="button" className="button-primary" disabled={saving} onClick={onSave}>
                {saving ? 'Saving…' : saveLabel}
              </button>
            </div>
          )}
        </div>
      )}
      {leaveDialog}
    </>
  )
}
