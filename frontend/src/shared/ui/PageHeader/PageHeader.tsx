import type { ReactNode } from 'react'
import './PageHeader.css'

type PageHeaderProps = {
  title: ReactNode
  /** Small line above the title, e.g. "Module 2 of 5" or the content type. */
  kicker?: ReactNode
  /** Shown below the title: a short description, facts or buttons. */
  children?: ReactNode
}

/** The heading of a page: an optional kicker, the title and what belongs below it. */
export default function PageHeader({ title, kicker, children }: PageHeaderProps) {
  return (
    <header className="page-header">
      {kicker && <p className="page-header-kicker">{kicker}</p>}
      <h1>{title}</h1>
      {children}
    </header>
  )
}
