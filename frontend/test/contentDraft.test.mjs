import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  contentChanges,
  EMPTY_DRAFT,
  itemDraft,
  withDeleted,
  withMove,
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

test('a move can be undone with null, a delete with false', () => {
  const moved = withMove(EMPTY_DRAFT, page, 20)
  assert.deepEqual(moved.moves, { 1: 20 })
  assert.deepEqual(withMove(moved, page, null).moves, {})

  const deleted = withDeleted(EMPTY_DRAFT, quiz, true)
  assert.deepEqual(deleted.deletes, ['2'])
  assert.deepEqual(withDeleted(deleted, quiz, false).deletes, [])
})

test('withoutSaved removes only the saved change', () => {
  let draft = withRequired(EMPTY_DRAFT, page, true)
  draft = withMove(draft, quiz, 20)
  draft = withDeleted(draft, page, true)
  assert.equal(contentChanges(draft), 3)

  draft = withoutSaved(draft, 'moves', 2)
  assert.deepEqual(draft.moves, {})
  assert.deepEqual(draft.required, { 1: true })
  assert.deepEqual(withoutSaved(draft, 'deletes', '1').deletes, [])
})

test('itemDraft tells a row what will happen to its item', () => {
  const draft = withMove(withRequired(EMPTY_DRAFT, page, true), quiz, 20)
  assert.deepEqual(itemDraft(draft, page), {
    deleted: false,
    movedTo: undefined,
    requiredChanged: true,
    required: true,
  })
  assert.deepEqual(itemDraft(draft, quiz), { deleted: false, movedTo: 20, requiredChanged: false, required: true })
})
