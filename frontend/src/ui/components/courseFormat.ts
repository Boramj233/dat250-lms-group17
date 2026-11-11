import type { Course, Term } from '../../data/courses/types'

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
  return b.year - a.year
    || TERM_ORDER[b.term] - TERM_ORDER[a.term]
    || a.title.localeCompare(b.title)
}
