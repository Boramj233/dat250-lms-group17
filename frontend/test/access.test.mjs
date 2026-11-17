import { test } from 'node:test'
import assert from 'node:assert/strict'
import { hasAccess } from '../src/shared/auth/access.ts'

test('access follows the backend hierarchy ADMIN > COURSE_MANAGER > STANDARD', () => {
  assert.equal(hasAccess('STANDARD', 'STANDARD'), true)
  assert.equal(hasAccess('STANDARD', 'COURSE_MANAGER'), false)
  assert.equal(hasAccess('STANDARD', 'ADMIN'), false)

  assert.equal(hasAccess('COURSE_MANAGER', 'STANDARD'), true)
  assert.equal(hasAccess('COURSE_MANAGER', 'COURSE_MANAGER'), true)
  assert.equal(hasAccess('COURSE_MANAGER', 'ADMIN'), false)

  assert.equal(hasAccess('ADMIN', 'STANDARD'), true)
  assert.equal(hasAccess('ADMIN', 'COURSE_MANAGER'), true)
  assert.equal(hasAccess('ADMIN', 'ADMIN'), true)
})

test('no user means no access', () => {
  assert.equal(hasAccess(undefined, 'STANDARD'), false)
})
