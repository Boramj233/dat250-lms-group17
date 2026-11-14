import { NavLink } from 'react-router'
import { paths } from '../../../app/paths'
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
    </nav>
  )
}
