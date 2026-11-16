import { test } from 'node:test'
import assert from 'node:assert/strict'
import { formatCount } from '../src/shared/lib/formatCount.ts'
import { newestFirst } from '../src/shared/lib/newestFirst.ts'

test('formatCount uses the singular only for one', () => {
  assert.equal(formatCount(1, 'item'), '1 item')
  assert.equal(formatCount(0, 'item'), '0 items')
  assert.equal(formatCount(3, 'unsaved change'), '3 unsaved changes')
  assert.equal(formatCount(2, 'child', 'children'), '2 children')
})

test('newestFirst sorts a copy by submittedAt, newest first', () => {
  const old = { id: 1, submittedAt: '2030-01-01T10:00:00Z' }
  const recent = { id: 2, submittedAt: '2030-03-01T10:00:00Z' }
  const items = [old, recent]
  assert.deepEqual(newestFirst(items), [recent, old])
  assert.deepEqual(items, [old, recent])
})
