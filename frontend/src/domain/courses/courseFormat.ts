import type { Course, Term } from './types'

const TERM_LABEL: Record<Term, string> = {
  SPRING: 'Spring',
  AUTUMN: 'Autumn',
}

// Within a year, autumn comes after spring.
const TERM_ORDER: Record<Term, number> = { SPRING: 0, AUTUMN: 1 }

/** "Autumn 2026" */
export function formatTerm(course: Pick<Course, 'term' | 'year'>): string {
  return `${TERM_LABEL[course.term]} ${course.year}`
}

/** Sort compare function: newest courses first, then by title. */
export function byNewest(a: Course, b: Course): number {
  return b.year - a.year || TERM_ORDER[b.term] - TERM_ORDER[a.term] || a.title.localeCompare(b.title)
}

export type TermGroup<T extends Course> = {
  /** "AUTUMN-2026": stable key for React lists. */
  key: string
  /** "Autumn 2026" */
  label: string
  courses: T[]
}

/** Groups courses by term and year: newest term first, courses by title within a term. */
export function groupByTerm<T extends Course>(courses: T[]): TermGroup<T>[] {
  const groups = new Map<string, TermGroup<T>>()
  for (const course of [...courses].sort(byNewest)) {
    const key = `${course.term}-${course.year}`
    let group = groups.get(key)
    if (!group) {
      group = { key, label: formatTerm(course), courses: [] }
      groups.set(key, group)
    }
    group.courses.push(course)
  }
  // Map keeps insertion order, and the courses were sorted newest first.
  return [...groups.values()]
}

/** The term a date falls in: January–July is SPRING, August–December is AUTUMN. */
export function termOf(date: Date): Pick<Course, 'term' | 'year'> {
  return { term: date.getMonth() >= 7 ? 'AUTUMN' : 'SPRING', year: date.getFullYear() }
}

/** One number per term that grows with time, for comparing terms. */
function termIndex(course: Pick<Course, 'term' | 'year'>): number {
  return course.year * 2 + TERM_ORDER[course.term]
}

/**
 * Splits courses into the current (and any later) terms and the previous terms, each
 * grouped by term. `current` is the oldest term first (this term on top, then later
 * ones); `previous` is newest first.
 */
export function splitByCurrentTerm<T extends Course>(
  courses: T[],
  now: Date = new Date(),
): { current: TermGroup<T>[]; previous: TermGroup<T>[] } {
  const thisTerm = termIndex(termOf(now))
  const groups = groupByTerm(courses)
  return {
    current: groups.filter(group => termIndex(group.courses[0]) >= thisTerm).reverse(),
    previous: groups.filter(group => termIndex(group.courses[0]) < thisTerm),
  }
}
