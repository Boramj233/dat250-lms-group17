import { Link } from 'react-router'
import { paths } from '../../../app/paths'
import { logout } from '../../../data/auth/auth'
import { useCurrentUser } from '../../../data/auth/useAuth'
import './Topbar.css'

type TopbarProps = {
  sidebarOpen: boolean
  onToggleSidebar: () => void
}

export default function Topbar({ sidebarOpen, onToggleSidebar }: TopbarProps) {
  const currentUser = useCurrentUser()
  return (
    <header className="topbar">
      <button
        type="button"
        className="sidebar-toggle"
        aria-label={sidebarOpen ? 'Close menu' : 'Open menu'}
        aria-expanded={sidebarOpen}
        aria-controls="sidebar-navigation"
        onClick={onToggleSidebar}
      >
        <svg
          width="24"
          height="24"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          aria-hidden="true"
          focusable="false"
        >
          {sidebarOpen ? (
            <path d="M6 6l12 12M6 18L18 6" />
          ) : (
            <path d="M4 6h16M4 12h16M4 18h16" />
          )}
        </svg>
      </button>
      <Link className="brand" to={paths.home}>DAT250 LMS</Link>
      <nav className="main-nav" aria-label="Main navigation">
        {currentUser.data && (
          <>
            <span className="current-user">
              {currentUser.data.firstName} {currentUser.data.lastName}
            </span>
            <button type="button" onClick={logout}>Log out</button>
          </>
        )}
      </nav>
    </header>
  )
}
