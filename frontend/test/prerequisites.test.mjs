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
