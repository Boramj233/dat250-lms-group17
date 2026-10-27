import { useEffect, useState } from 'react'
import { Outlet } from 'react-router'
import Sidebar from './Sidebar'
import Topbar from './Topbar'
import './AppLayout.css'

export default function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false)

  useEffect(() => {
    const desktop = window.matchMedia('(min-width: 48rem)')
    const closeSidebar = () => setSidebarOpen(false)

    desktop.addEventListener('change', closeSidebar)
    return () => desktop.removeEventListener('change', closeSidebar)
  }, [])

  return (
    <div className="app-layout">
      <Topbar
        sidebarOpen={sidebarOpen}
        onToggleSidebar={() => setSidebarOpen(open => !open)}
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
