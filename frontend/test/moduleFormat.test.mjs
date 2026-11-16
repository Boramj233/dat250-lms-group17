import { test } from 'node:test'
import assert from 'node:assert/strict'
import { availabilityText } from '../src/domain/modules/moduleFormat.ts'
import { formatDateTime } from '../src/shared/lib/dateFormat.ts'

const module = (id, changes = {}) => ({ id, title: `Module ${id}`, position: id, ...changes })
const modules = [module(10), module(20), module(30), module(40)]

test('a module without dates or prerequisites is open from the start', () => {
  assert.equal(availabilityText(modules[0], modules), 'Open from the start')
})

test('prerequisites are named by their number in the course', () => {
  assert.equal(availabilityText(module(40, { prerequisiteModuleIds: [20] }), modules), 'After module 2')
  assert.equal(availabilityText(module(40, { prerequisiteModuleIds: ['30', 10] }), modules), 'After module 1 and 3')
  assert.equal(
    availabilityText(module(40, { prerequisiteModuleIds: [10, 20, 30] }), modules),
    'After module 1, 2 and 3',
  )
})

test('dates and prerequisites are joined in order', () => {
  const from = '2030-01-10T08:00:00Z'
  const toAll = '2030-02-01T08:00:00Z'
  assert.equal(
    availabilityText(module(40, { availableFrom: from, openToAllFrom: toAll, prerequisiteModuleIds: [10] }), modules),
    `From ${formatDateTime(new Date(from))} · After module 1 · For all from ${formatDateTime(new Date(toAll))}`,
  )
})
