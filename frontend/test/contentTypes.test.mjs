import { test } from 'node:test'
import assert from 'node:assert/strict'
import { contentTypeBySlug, summarizeTypes } from '../src/ui/course/contentTypes.ts'

test('content types are found by their URL slug', () => {
  assert.equal(contentTypeBySlug('coding-exercises')?.type, 'CODING_EXERCISE')
  assert.equal(contentTypeBySlug('groups'), undefined)
  assert.equal(contentTypeBySlug(undefined), undefined)
})

test('a module summary counts the types that occur, in menu order', () => {
  const items = ['ASSIGNMENT', 'STATIC_PAGE', 'STATIC_PAGE', 'QUIZ', 'ASSIGNMENT'].map(type => ({ type }))
  assert.equal(summarizeTypes(items), '2 pages · 1 quiz · 2 assignments')
  assert.equal(summarizeTypes([]), '')
})

test('everything but static pages can be completed', async () => {
  const { isCompletable } = await import('../src/ui/course/contentTypes.ts')
  assert.equal(isCompletable('STATIC_PAGE'), false)
  assert.equal(isCompletable('QUIZ'), true)
  assert.equal(isCompletable('ASSIGNMENT'), true)
  assert.equal(isCompletable('CODING_EXERCISE'), true)
})

test('items are split into to do and completed, keeping their order', async () => {
  const { splitByCompletion } = await import('../src/ui/course/contentTypes.ts')
  const items = [{ id: 1 }, { id: 2 }, { id: '3' }, { id: 4 }]
  const { todo, done } = splitByCompletion(items, new Set(['2', '3']))
  assert.deepEqual(todo.map(i => i.id), [1, 4])
  assert.deepEqual(done.map(i => i.id), [2, '3'])
})
