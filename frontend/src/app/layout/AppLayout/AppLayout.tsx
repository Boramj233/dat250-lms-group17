import { useState } from 'react'
import { Outlet, useLocation } from 'react-router'
import { paths } from '@shared/paths'
import Sidebar from '../Sidebar'
import Topbar from '../Topbar'
import './AppLayout.css'

export default function AppLayout() {
  const location = useLocation()
  const [sidebarOverride, setSidebarOverride] = useState<{
    locationKey: string
    open: boolean
  } | null>(null)
  const sidebarOpen = sidebarOverride?.locationKey === location.key
    ? sidebarOverride.open
    : location.pathname === paths.home

  function setSidebarOpen(open: boolean) {
    setSidebarOverride({ locationKey: location.key, open })
  }

  return (
    <div className="app-layout">
      <Topbar
        sidebarOpen={sidebarOpen}
        onToggleSidebar={() => setSidebarOpen(!sidebarOpen)}
      />

      <div className="app-body">
        <Sidebar isOpen={sidebarOpen} onNavigate={() => setSidebarOpen(false)} />
        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
