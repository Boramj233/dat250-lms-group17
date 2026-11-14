import { createContext } from 'react'
import type { Crumb } from './trailState'

export type TrailContextValue = {
  /** How the user got to this page. */
  trail: Crumb[]
  /** This page, as it would appear in the next page's trail. */
  current: Crumb
}

/** Set by TrailPage; read by TrailLink. */
export const TrailContext = createContext<TrailContextValue | null>(null)
