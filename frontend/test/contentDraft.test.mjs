import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  contentChanges,
  EMPTY_DRAFT,
  itemDraft,
  withDeleted,
  withoutSaved,
  withRequired,
} from '../src/features/course-management/module/pages/ManageModulePage/contentDraft.ts'

const page = { id: 1, type: 'STATIC_PAGE', title: 'Welcome', position: 0 }
const quiz = { id: 2, type: 'QUIZ', title: 'Quiz', position: 1, requiredForModuleCompletion: true }

test('switching required back to the saved value is no longer a change', () => {
  const changed = withRequired(EMPTY_DRAFT, page, true)
  assert.deepEqual(changed.required, { 1: true })
  assert.equal(contentChanges(changed), 1)
  assert.deepEqual(withRequired(changed, page, false).required, {})
  // Missing requiredForModuleCompletion means not required; true is saved for the quiz.
  assert.deepEqual(withRequired(EMPTY_DRAFT, quiz, true).required, {})
})

test('a delete can be undone with false', () => {
  const deleted = withDeleted(EMPTY_DRAFT, quiz, true)
  assert.deepEqual(deleted.deletes, ['2'])
  assert.deepEqual(withDeleted(deleted, quiz, false).deletes, [])
})

test('withoutSaved removes only the saved change', () => {
  let draft = withRequired(EMPTY_DRAFT, page, true)
  draft = withDeleted(draft, quiz, true)
  assert.equal(contentChanges(draft), 2)

  draft = withoutSaved(draft, 'required', 1)
  assert.deepEqual(draft.required, {})
  assert.deepEqual(draft.deletes, ['2'])
  assert.deepEqual(withoutSaved(draft, 'deletes', '2').deletes, [])
})

test('itemDraft tells a row what will happen to its item', () => {
  const draft = withDeleted(withRequired(EMPTY_DRAFT, page, true), quiz, true)
  assert.deepEqual(itemDraft(draft, page), { deleted: false, requiredChanged: true, required: true })
  assert.deepEqual(itemDraft(draft, quiz), { deleted: true, requiredChanged: false, required: true })
})
