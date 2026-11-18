import { useEffect, useRef, useState } from 'react'
import { NavLink, useLocation } from 'react-router'
import { useHasAccess } from '@shared/auth/useAuth'
import { paths } from '@shared/paths'
import CoursesPanel, { COURSES_PANEL_ID } from '../CoursesPanel'
import './Sidebar.css'

type SidebarProps = {
  isOpen: boolean
  onNavigate: () => void
}

export default function Sidebar({ isOpen }: SidebarProps) {
  const [coursesOpen, setCoursesOpen] = useState(false)
  const isAdmin = useHasAccess('ADMIN')
  const sidebarRef = useRef<HTMLElement>(null)
  const coursesButtonRef = useRef<HTMLButtonElement>(null)
  // All courses, the course pages and Manage courses (with every page under it) are
  // reached from the panel.
  const { pathname } = useLocation()
  const inCourses =
    pathname === paths.courses ||
    pathname.startsWith(`${paths.courses}/`) ||
    pathname === paths.manageCourses ||
    pathname.startsWith(`${paths.manageCourses}/`)

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

  const closeCourses = () => setCoursesOpen(false)
  // Only one item is highlighted: Courses while its panel is open, otherwise the current page.
  const linkClass = ({ isActive }: { isActive: boolean }) => (isActive && !coursesOpen ? 'active' : undefined)

  return (
    <nav
      ref={sidebarRef}
      id="sidebar-navigation"
      className={`sidebar${isOpen ? ' is-open' : ''}`}
      aria-label="Sidebar navigation"
    >
      <NavLink to={paths.home} end className={linkClass} onClick={closeCourses}>
        Dashboard
      </NavLink>
      {/* Opens the courses panel; highlighted while it is open or on a page reached from it. */}
      <button
        ref={coursesButtonRef}
        type="button"
        className={`sidebar-item${coursesOpen || inCourses ? ' active' : ''}`}
        aria-expanded={coursesOpen}
        aria-controls={COURSES_PANEL_ID}
        onClick={() => setCoursesOpen(!coursesOpen)}
      >
        Courses
      </button>
      <NavLink to={paths.profile} className={linkClass} onClick={closeCourses}>
        Profile
      </NavLink>
      {isAdmin && (
        <NavLink to={paths.adminUsers} className={linkClass} onClick={closeCourses}>
          Users
        </NavLink>
      )}

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
