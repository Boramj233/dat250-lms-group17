export type CoursePreference = {
  id: string
  visible: boolean
}

export function normalizeCoursePreferences(value: unknown, courseIds: readonly string[]): CoursePreference[] {
  const remaining = new Set(courseIds)
  const preferences: CoursePreference[] = []

  if (Array.isArray(value)) {
    for (const item of value) {
      if (
        typeof item === 'object' && item !== null &&
        typeof item.id === 'string' && typeof item.visible === 'boolean' &&
        remaining.delete(item.id)
      ) {
        preferences.push({ id: item.id, visible: item.visible })
      }
    }
  }

  return [...preferences, ...Array.from(remaining, id => ({ id, visible: true }))]
}

export function moveCourse(preferences: CoursePreference[], id: string, direction: -1 | 1) {
  const index = preferences.findIndex(course => course.id === id)
  const target = index + direction
  if (index < 0 || target < 0 || target >= preferences.length) return preferences

  const next = [...preferences]
  ;[next[index], next[target]] = [next[target], next[index]]
  return next
}
