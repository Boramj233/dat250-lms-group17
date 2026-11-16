import { useEffect, useId, useRef, useState } from 'react'
import './ActionMenu.css'

export type ActionMenuItem = {
  label: string
  /** What the item does. Leave out when the item has `items`. */
  onSelect?: () => void
  /** A submenu, e.g. the modules to move to; shown instead of the menu, with a back button. */
  items?: ActionMenuItem[]
  /** Shown in the danger color, e.g. "Delete". */
  danger?: boolean
  disabled?: boolean
}

type ActionMenuProps = {
  /** Name of the button for screen readers, e.g. "Actions for Week 1 notes". */
  label: string
  items: ActionMenuItem[]
}

/**
 * A "⋯" button that opens a small menu of actions. Closes on Escape, on a click
 * outside, and after an action is chosen.
 */
export default function ActionMenu({ label, items }: ActionMenuProps) {
  const [open, setOpen] = useState(false)
  const [submenu, setSubmenu] = useState<ActionMenuItem | null>(null)
  const rootRef = useRef<HTMLDivElement>(null)
  const buttonRef = useRef<HTMLButtonElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)
  const menuId = useId()

  function close(returnFocus = true) {
    setOpen(false)
    setSubmenu(null)
    if (returnFocus) buttonRef.current?.focus()
  }

  // Close on Escape or a click outside; focus the first item when the menu changes.
  useEffect(() => {
    if (!open) return
    menuRef.current?.querySelector<HTMLButtonElement>('[role="menuitem"]:not(:disabled)')?.focus()
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') close()
    }
    function onPointerDown(event: PointerEvent) {
      if (!rootRef.current?.contains(event.target as Node)) close(false)
    }
    document.addEventListener('keydown', onKeyDown)
    document.addEventListener('pointerdown', onPointerDown)
    return () => {
      document.removeEventListener('keydown', onKeyDown)
      document.removeEventListener('pointerdown', onPointerDown)
    }
  }, [open, submenu])

  function choose(item: ActionMenuItem) {
    if (item.items) {
      setSubmenu(item)
      return
    }
    close()
    item.onSelect?.()
  }

  const shown = submenu?.items ?? items

  return (
    <div className="action-menu" ref={rootRef}>
      <button
        ref={buttonRef}
        type="button"
        className="action-menu-button"
        aria-label={label}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={open ? menuId : undefined}
        onClick={() => (open ? close() : setOpen(true))}
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" focusable="false">
          <circle cx="5" cy="12" r="2" />
          <circle cx="12" cy="12" r="2" />
          <circle cx="19" cy="12" r="2" />
        </svg>
      </button>

      {open && (
        <div id={menuId} ref={menuRef} role="menu" aria-label={submenu?.label ?? label} className="action-menu-list">
          {submenu && (
            <button
              type="button"
              role="menuitem"
              className="action-menu-item action-menu-back"
              onClick={() => setSubmenu(null)}
            >
              ← {submenu.label}
            </button>
          )}
          {shown.map(item => (
            <button
              key={item.label}
              type="button"
              role="menuitem"
              className={`action-menu-item${item.danger ? ' is-danger' : ''}`}
              disabled={item.disabled}
              aria-haspopup={item.items ? 'menu' : undefined}
              onClick={() => choose(item)}
            >
              {item.label}
              {item.items && <span aria-hidden="true">›</span>}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
