import { NavLink } from 'react-router'
import { paths } from '../../../app/paths'
import RequireAccess from '../RequireAccess'
import './Sidebar.css'

type SidebarProps = {
  isOpen: boolean
  onNavigate: () => void
}

export default function Sidebar({ isOpen }: SidebarProps) {
  return (
    <nav
      id="sidebar-navigation"
      className={`sidebar${isOpen ? ' is-open' : ''}`}
      aria-label="Sidebar navigation"
    >
      <NavLink to={paths.home} end>
        Overview
      </NavLink>
      {/* Course managers (and admins) only. Active on every page under it. */}
      <RequireAccess level="COURSE_MANAGER">
        <NavLink to={paths.manageCourses}>
          Manage courses
        </NavLink>
      </RequireAccess>
    </nav>
  )
}
