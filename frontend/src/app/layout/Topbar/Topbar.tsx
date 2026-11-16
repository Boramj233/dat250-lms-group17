import { Link } from 'react-router'
import { paths } from '@shared/paths'
import { logout } from '@shared/auth/auth'
import { useCurrentUser } from '@shared/auth/useAuth'
import { fullName } from '@shared/lib/fullName'
import '@shared/styles/forms.css'
import './Topbar.css'
import { CloseIcon, MenuIcon } from '@shared/ui/icons'

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
        {sidebarOpen ? <CloseIcon size={24} /> : <MenuIcon size={24} />}
      </button>
      <Link className="brand" to={paths.home}>
        DAT250 LMS
      </Link>
      <nav className="main-nav" aria-label="Main navigation">
        {currentUser.data && (
          <>
            <span className="current-user">{fullName(currentUser.data)}</span>
            <button type="button" className="button-secondary" onClick={logout}>
              Log out
            </button>
          </>
        )}
      </nav>
    </header>
  )
}
