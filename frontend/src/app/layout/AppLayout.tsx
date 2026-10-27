import type { ReactNode } from 'react'
import './AppLayout.css'

type AppLayoutProps = {
  children: ReactNode
}

export default function AppLayout({ children }: AppLayoutProps) {
  return (
    <div className="app-layout">
      <header className="topbar">
        <span>DAT250 LMS</span>
      </header>

      <main className="page-content">{children}</main>
    </div>
  )
}
