import { Link, NavLink, Outlet } from 'react-router'
import { paths } from '../paths'
import './AppLayout.css'

export default function AppLayout() {
  return (
    <div className="app-layout">
      <header className="topbar">
        <Link className="brand" to={paths.home}>DAT250 LMS</Link>
        <nav className="main-nav" aria-label="Hovednavigasjon">
          <NavLink to={paths.courses}>Mine kurs</NavLink>
          <NavLink to={paths.login}>Logg inn</NavLink>
        </nav>
      </header>

      <main className="page-content"><Outlet /></main>
    </div>
  )
}
