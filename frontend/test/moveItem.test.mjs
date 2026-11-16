import { test } from 'node:test'
import assert from 'node:assert/strict'
import { moveItem } from '../src/shared/lib/moveItem.ts'

test('moves an item down and up, shifting the others', () => {
  assert.deepEqual(moveItem(['a', 'b', 'c', 'd'], 0, 2), ['b', 'c', 'a', 'd'])
  assert.deepEqual(moveItem(['a', 'b', 'c', 'd'], 3, 1), ['a', 'd', 'b', 'c'])
})

test('leaves the list as it is for the same place or a place outside it', () => {
  const list = ['a', 'b']
  assert.deepEqual(moveItem(list, 1, 1), ['a', 'b'])
  assert.deepEqual(moveItem(list, 0, 5), ['a', 'b'])
  assert.notEqual(moveItem(list, 0, 1), list)
})
