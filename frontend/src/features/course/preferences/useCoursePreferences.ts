import { createContext, useContext } from 'react'

type CoursePreferencesValue = {
  courses: { id: string; name: string; visible: boolean }[]
  setVisible: (id: string, visible: boolean) => void
  reorder: (id: string, direction: -1 | 1) => void
  savedLocally: boolean
}

export const CoursePreferencesContext = createContext<CoursePreferencesValue | null>(null)

export function useCoursePreferences() {
  const context = useContext(CoursePreferencesContext)
  if (!context) throw new Error('useCoursePreferences requires CoursePreferencesProvider')
  return context
}
