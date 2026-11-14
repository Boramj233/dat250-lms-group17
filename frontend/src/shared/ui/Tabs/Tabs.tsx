import { NavLink } from 'react-router'
import './Tabs.css'

export type Tab = {
  label: string
  to: string
  /** Only active on exactly this URL (for the first tab, whose URL is the others' start). */
  end?: boolean
}

/** A row of tabs that are links; the tab for the current URL is marked active. */
export default function Tabs({ label, tabs }: { label: string, tabs: Tab[] }) {
  return (
    <nav className="tabs" aria-label={label}>
      {tabs.map(tab => (
        <NavLink key={tab.to} to={tab.to} end={tab.end}
          className={({ isActive }) => `tab${isActive ? ' is-active' : ''}`}>
          {tab.label}
        </NavLink>
      ))}
    </nav>
  )
}
