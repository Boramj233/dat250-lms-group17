import { test } from 'node:test'
import assert from 'node:assert/strict'
import { byRoleThenName } from '../src/domain/courses/members.ts'

const member = (role, firstName, lastName) => ({ userId: `${firstName}-${lastName}`, role, firstName, lastName })

test('byRoleThenName puts teachers first, then sorts by last name and first name', () => {
  const members = [
    member('STUDENT', 'Ola', 'Nordmann'),
    member('STUDENT', 'Kari', 'Nordmann'),
    member('TEACHER', 'Per', 'Berg'),
    member('STUDENT', 'Anna', 'Dahl'),
    member('TEACHER', 'Eva', 'Aune'),
  ]
  assert.deepEqual(
    [...members].sort(byRoleThenName).map(m => `${m.firstName} ${m.lastName}`),
    ['Eva Aune', 'Per Berg', 'Anna Dahl', 'Kari Nordmann', 'Ola Nordmann'],
  )
})
