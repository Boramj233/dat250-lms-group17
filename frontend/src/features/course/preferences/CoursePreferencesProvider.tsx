import { useState } from 'react'
import type { ReactNode } from 'react'
import { demoEnrolledCourses } from '../data/demoEnrolledCourses'
import { moveCourse, normalizeCoursePreferences } from './coursePreferences'
import type { CoursePreference } from './coursePreferences'
import { CoursePreferencesContext } from './useCoursePreferences'

const storageKey = 'dat250-lms.course-preferences'
const courseIds = demoEnrolledCourses.map(course => course.id)

function readPreferences() {
  try {
    return normalizeCoursePreferences(JSON.parse(localStorage.getItem(storageKey) ?? 'null'), courseIds)
  } catch {
    return normalizeCoursePreferences(null, courseIds)
  }
}

export function CoursePreferencesProvider({ children }: { children: ReactNode }) {
  const [preferences, setPreferences] = useState(readPreferences)
  const [savedLocally, setSavedLocally] = useState(true)

  function updatePreferences(next: CoursePreference[]) {
    setPreferences(next)
    try {
      localStorage.setItem(storageKey, JSON.stringify(next))
      setSavedLocally(true)
    } catch {
      setSavedLocally(false)
    }
  }

  const courses = preferences.map(preference => ({
    ...preference,
    name: demoEnrolledCourses.find(course => course.id === preference.id)!.name,
  }))

  return (
    <CoursePreferencesContext.Provider value={{
      courses,
      setVisible: (id, visible) => updatePreferences(
        preferences.map(course => course.id === id ? { ...course, visible } : course)),
      reorder: (id, direction) => updatePreferences(moveCourse(preferences, id, direction)),
      savedLocally,
    }}>
      {children}
    </CoursePreferencesContext.Provider>
  )
}

