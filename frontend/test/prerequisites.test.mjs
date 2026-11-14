import { test } from 'node:test'
import assert from 'node:assert/strict'
import { canRequire, dependsOn, isUnlocked } from '../src/data/modules/prerequisites.ts'

// 3 requires 2, 2 requires 1; 4 has none.
const modules = [
  { id: 1 },
  { id: 2, prerequisiteModuleIds: [1] },
  { id: 3, prerequisiteModuleIds: ['2'] },
  { id: 4, prerequisiteModuleIds: [] },
]

test('dependsOn follows prerequisites through other modules', () => {
  assert.equal(dependsOn(modules, 3, 1), true)
  assert.equal(dependsOn(modules, 3, '2'), true)
  assert.equal(dependsOn(modules, 1, 3), false)
  assert.equal(dependsOn(modules, 4, 1), false)
})

test('a module cannot require itself or a module that requires it', () => {
  assert.equal(canRequire(modules, 1, 1), false)
  assert.equal(canRequire(modules, 1, 3), false) // 3 requires 1 through 2
  assert.equal(canRequire(modules, 3, 1), true)
  assert.equal(canRequire(modules, 4, 3), true)
})

test('a module is unlocked when all its prerequisites are completed', () => {
  assert.equal(isUnlocked(modules[0], new Set()), true)
  assert.equal(isUnlocked(modules[2], new Set()), false)
  assert.equal(isUnlocked(modules[2], new Set(['2'])), true)
})

test('a cycle in the data does not make dependsOn loop forever', () => {
  const cyclic = [{ id: 1, prerequisiteModuleIds: [2] }, { id: 2, prerequisiteModuleIds: [1] }]
  assert.equal(dependsOn(cyclic, 1, 3), false)
})

test('a module is accessible from its start date, when unlocked or open to all', async () => {
  const { isAccessible, checkAvailabilityDates } = await import('../src/data/modules/prerequisites.ts')
  const now = new Date('2026-11-14T12:00:00Z')
  const before = '2026-11-10T00:00:00Z'
  const after = '2026-11-20T00:00:00Z'
  const locked = { id: 5, prerequisiteModuleIds: [1] }

  assert.equal(isAccessible({ id: 9 }, new Set(), now), true)
  assert.equal(isAccessible({ id: 9, availableFrom: after }, new Set(), now), false)
  assert.equal(isAccessible({ id: 9, availableFrom: before }, new Set(), now), true)
  assert.equal(isAccessible(locked, new Set(), now), false)
  assert.equal(isAccessible(locked, new Set(['1']), now), true)
  assert.equal(isAccessible({ ...locked, openToAllFrom: before }, new Set(), now), true)
  assert.equal(isAccessible({ ...locked, openToAllFrom: after }, new Set(), now), false)
  // Open to all does not open it before its start date.
  assert.equal(isAccessible({ ...locked, availableFrom: after, openToAllFrom: before }, new Set(), now), false)

  assert.equal(checkAvailabilityDates(before, after), undefined)
  assert.equal(checkAvailabilityDates(null, before), undefined)
  assert.ok(checkAvailabilityDates(after, before))
})
