import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  EMPTY_DRAFT,
  isLastTeacher,
  isRemoved,
  isRoleChanged,
  memberChanges,
  roleAfter,
  withoutSavedRemoval,
  withoutSavedRole,
  withRemoved,
  withRole,
} from '../src/features/course-management/pages/CourseMembersPage/memberDraft.ts'

const member = (userId, role) => ({ userId, role, firstName: 'A', lastName: String(userId), email: '' })
const teacher = member(1, 'TEACHER')
const other = member(2, 'TEACHER')
const student = member(3, 'STUDENT')
const members = [teacher, other, student]

test('a role changed back to the saved one is no longer a change', () => {
  const changed = withRole(EMPTY_DRAFT, student, 'TEACHER')
  assert.equal(roleAfter(changed, student), 'TEACHER')
  assert.equal(isRoleChanged(changed, student), true)
  assert.equal(memberChanges(changed), 1)
  assert.equal(memberChanges(withRole(changed, student, 'STUDENT')), 0)
})

test('a removal can be undone', () => {
  const removed = withRemoved(EMPTY_DRAFT, student, true)
  assert.equal(isRemoved(removed, student), true)
  assert.equal(isRemoved(withRemoved(removed, student, false), student), false)
})

test('the last teacher cannot be removed or made a student', () => {
  // Two teachers: neither is the last one.
  assert.equal(isLastTeacher(EMPTY_DRAFT, teacher, members), false)
  // One teacher removed: the other is the last one.
  const oneLeft = withRemoved(EMPTY_DRAFT, other, true)
  assert.equal(isLastTeacher(oneLeft, teacher, members), true)
  assert.equal(isLastTeacher(oneLeft, other, members), false)
  // One teacher made a student: the same.
  assert.equal(isLastTeacher(withRole(EMPTY_DRAFT, other, 'STUDENT'), teacher, members), true)
  // A student made a teacher: two teachers again.
  assert.equal(isLastTeacher(withRole(oneLeft, student, 'TEACHER'), teacher, members), false)
  // A student is never the last teacher.
  assert.equal(isLastTeacher(oneLeft, student, members), false)
})

test('saved changes leave the draft one by one', () => {
  let draft = withRole(EMPTY_DRAFT, student, 'TEACHER')
  draft = withRemoved(draft, other, true)
  assert.equal(memberChanges(draft), 2)
  draft = withoutSavedRole(draft, '3')
  assert.deepEqual(draft, { roles: {}, removals: ['2'] })
  assert.deepEqual(withoutSavedRemoval(draft, '2'), EMPTY_DRAFT)
})
