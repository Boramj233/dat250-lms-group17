import { createContext, useContext } from 'react'
import type { ContentSummary } from '@domain/content/types'
import type { CourseModule } from '@domain/modules/types'

/** What ModuleLayout has loaded, for the pages of one module. */
export type ModuleViewContextValue = {
  module: CourseModule
  /** The content the user may see, sorted by position. */
  items: ContentSummary[]
}

export const ModuleViewContext = createContext<ModuleViewContextValue | null>(null)

/** The module being viewed. Only for pages inside ModuleLayout. */
export function useModuleView(): ModuleViewContextValue {
  const value = useContext(ModuleViewContext)
  if (!value) throw new Error('useModuleView is used outside ModuleLayout')
  return value
}
