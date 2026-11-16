import type { ReactNode } from 'react'
import '@shared/styles/forms.css'
import './SideMenuLayout.css'

type SideMenuLayoutProps = {
  /** "Course menu" or "Module menu": the menu's name for screen readers. */
  label: string
  /** Id of the menu's content, for the toggle button. */
  id: string
  open: boolean
  onToggle: () => void
  /** Shown next to the toggle button while the menu is open, e.g. the course title. */
  heading?: ReactNode
  /** The menu's content: links, buttons and headings. */
  menu: ReactNode
  /** The page next to the menu. */
  children: ReactNode
}

/**
 * A page with a menu on its left (above it on narrow screens) that can be collapsed to
 * just its toggle button (three lines). Style the menu's content with the side-menu-*
 * classes.
 */
export default function SideMenuLayout({ label, id, open, onToggle, heading, menu, children }: SideMenuLayoutProps) {
  const action = `${open ? 'Collapse' : 'Expand'} ${label.toLowerCase()}`
  return (
    <div className={`side-menu-layout${open ? '' : ' is-collapsed'}`}>
      <nav className="side-menu" aria-label={label}>
        {/* The heading and the toggle on one line. */}
        <div className={`side-menu-top${open && heading ? ' has-heading' : ''}`}>
          {open && heading && <div className="side-menu-top-heading">{heading}</div>}
          <button
            type="button"
            className="side-menu-toggle"
            aria-expanded={open}
            aria-controls={id}
            aria-label={action}
            title={action}
            onClick={onToggle}
          >
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              aria-hidden="true"
              focusable="false"
            >
              <path d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
        </div>
        <div id={id} className="side-menu-body" hidden={!open}>
          {menu}
        </div>
      </nav>
      <div className="side-menu-page">{children}</div>
    </div>
  )
}
