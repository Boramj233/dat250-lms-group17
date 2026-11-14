import { test } from 'node:test'
import assert from 'node:assert/strict'
import { formatTimeLeft, isSoon } from '../src/shared/lib/dateFormat.ts'

const now = new Date('2026-11-11T12:00:00Z')
const later = ms => new Date(now.getTime() + ms)
const MINUTE = 60_000
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR

test('time left is shown in minutes, hours or days', () => {
  assert.equal(formatTimeLeft(later(30 * MINUTE), now), 'in 30 minutes')
  assert.equal(formatTimeLeft(later(10 * 1000), now), 'in 1 minute')
  assert.equal(formatTimeLeft(later(5 * HOUR), now), 'in 5 hours')
  assert.equal(formatTimeLeft(later(DAY), now), 'tomorrow')
  assert.equal(formatTimeLeft(later(3 * DAY), now), 'in 3 days')
  assert.equal(formatTimeLeft(later(-MINUTE), now), 'passed')
})

test('deadlines less than two days away are soon', () => {
  assert.equal(isSoon(later(HOUR), now), true)
  assert.equal(isSoon(later(47 * HOUR), now), true)
  assert.equal(isSoon(later(2 * DAY), now), false)
  assert.equal(isSoon(later(-HOUR), now), false)
})

test('datetime-local values round-trip through ISO instants', async () => {
  const { fromDateTimeLocal, toDateTimeLocal } = await import('../src/shared/lib/dateFormat.ts')
  assert.equal(toDateTimeLocal(null), '')
  assert.equal(toDateTimeLocal('not a date'), '')
  assert.equal(fromDateTimeLocal(''), null)
  const value = '2026-11-14T09:30'
  assert.equal(toDateTimeLocal(fromDateTimeLocal(value)), value)
})
