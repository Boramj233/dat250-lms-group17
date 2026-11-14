import { createContext, useContext } from 'react'
import type { ContentSummary } from '../../../data/content/types'
import type { CourseModule } from '../../../data/modules/types'

/** What ManageModuleLayout has loaded, for the pages of one module. */
export type ManageModuleContextValue = {
  module: CourseModule
  /** The module's number in the course, from 1. */
  number: number
  /** The module's content, in order. */
  content: ContentSummary[]
}

export const ManageModuleContext = createContext<ManageModuleContextValue | null>(null)

/** The module being managed. Only for pages inside ManageModuleLayout. */
export function useManageModule(): ManageModuleContextValue {
  const value = useContext(ManageModuleContext)
  if (!value) throw new Error('useManageModule is used outside ManageModuleLayout')
  return value
}
