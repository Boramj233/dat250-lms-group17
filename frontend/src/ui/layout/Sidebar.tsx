import { useEffect, useRef, useState } from 'react'
import { NavLink } from 'react-router'
import { paths } from '../../app/paths'
import CoursesPanel, { COURSES_PANEL_ID } from './CoursesPanel'
import './Sidebar.css'

type SidebarProps = {
  isOpen: boolean
  onNavigate: () => void
}

export default function Sidebar({ isOpen }: SidebarProps) {
  const [coursesOpen, setCoursesOpen] = useState(false)
  const sidebarRef = useRef<HTMLElement>(null)
  const coursesButtonRef = useRef<HTMLButtonElement>(null)

  // Close the courses panel on Escape or a click outside the sidebar and the panel.
  useEffect(() => {
    if (!coursesOpen) return
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setCoursesOpen(false)
        coursesButtonRef.current?.focus()
      }
    }
    function onPointerDown(event: PointerEvent) {
      if (!sidebarRef.current?.contains(event.target as Node)) setCoursesOpen(false)
    }
    document.addEventListener('keydown', onKeyDown)
    document.addEventListener('pointerdown', onPointerDown)
    return () => {
      document.removeEventListener('keydown', onKeyDown)
      document.removeEventListener('pointerdown', onPointerDown)
    }
  }, [coursesOpen])

  return (
    <nav
      ref={sidebarRef}
      id="sidebar-navigation"
      className={`sidebar${isOpen ? ' is-open' : ''}`}
      aria-label="Sidebar navigation"
    >
      <NavLink to={paths.home} end onClick={() => setCoursesOpen(false)}>
        Overview
      </NavLink>

      <button
        ref={coursesButtonRef}
        type="button"
        className={`sidebar-item${coursesOpen ? ' active' : ''}`}
        aria-expanded={coursesOpen}
        aria-controls={COURSES_PANEL_ID}
        onClick={() => setCoursesOpen(!coursesOpen)}
      >
        Courses
      </button>

      {coursesOpen && (
        <CoursesPanel
          onClose={() => {
            setCoursesOpen(false)
            coursesButtonRef.current?.focus()
          }}
        />
      )}
    </nav>
  )
}
