import { test } from 'node:test'
import assert from 'node:assert/strict'
import { groupByTerm } from '../src/features/courses/courseFormat.ts'

const course = (id, title, term, year) => ({ id, title, description: '', term, year })

test('courses are grouped by term, newest term first and by title within a term', () => {
  const groups = groupByTerm([
    course(1, 'B course', 'AUTUMN', 2025),
    course(2, 'Z course', 'AUTUMN', 2026),
    course(3, 'A course', 'SPRING', 2026),
    course(4, 'A course', 'AUTUMN', 2026),
    course(5, 'A course', 'AUTUMN', 2025),
  ])
  assert.deepEqual(
    groups.map(group => [group.label, group.courses.map(c => c.id)]),
    [
      ['Autumn 2026', [4, 2]],
      ['Spring 2026', [3]],
      ['Autumn 2025', [5, 1]],
    ],
  )
})

test('no courses gives no groups', () => {
  assert.deepEqual(groupByTerm([]), [])
})

test('the term of a date: January–July is spring, August–December is autumn', async () => {
  const { termOf } = await import('../src/features/courses/courseFormat.ts')
  assert.deepEqual(termOf(new Date(2026, 0, 10)), { term: 'SPRING', year: 2026 })
  assert.deepEqual(termOf(new Date(2026, 6, 31)), { term: 'SPRING', year: 2026 })
  assert.deepEqual(termOf(new Date(2026, 7, 1)), { term: 'AUTUMN', year: 2026 })
  assert.deepEqual(termOf(new Date(2026, 11, 31)), { term: 'AUTUMN', year: 2026 })
})

test('courses are split into the current and later terms and the previous terms', async () => {
  const { splitByCurrentTerm } = await import('../src/features/courses/courseFormat.ts')
  const { current, previous } = splitByCurrentTerm([
    course(1, 'Old', 'AUTUMN', 2025),
    course(2, 'Now', 'AUTUMN', 2026),
    course(3, 'Last term', 'SPRING', 2026),
    course(4, 'Next term', 'SPRING', 2027),
  ], new Date(2026, 10, 11))
  assert.deepEqual(current.map(group => group.label), ['Autumn 2026', 'Spring 2027'])
  assert.deepEqual(previous.map(group => group.label), ['Spring 2026', 'Autumn 2025'])
})
