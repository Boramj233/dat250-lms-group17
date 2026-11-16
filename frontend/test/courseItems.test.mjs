import { test } from 'node:test'
import assert from 'node:assert/strict'
import { courseItems } from '../src/domain/content/courseItems.ts'

const page = { id: 1, type: 'STATIC_PAGE', title: 'Welcome', position: 0 }
const quiz = { id: 2, type: 'QUIZ', title: 'Quiz 1', position: 1 }
const task = { id: 3, type: 'ASSIGNMENT', title: 'Task 1', position: 0 }
const first = { id: 10, title: 'Module 1' }
const second = { id: 20, title: 'Module 2' }
const third = { id: 30, title: 'Module 3 (still loading)' }
const byModule = new Map([
  ['10', [page, quiz]],
  ['20', [task]],
])

test('courseItems lists the content in module order, each item with its module', () => {
  assert.deepEqual(courseItems(byModule, [first, second, third]), [
    { item: page, module: first },
    { item: quiz, module: first },
    { item: task, module: second },
  ])
})

test('courseItems follows the order of the modules it is given', () => {
  assert.deepEqual(
    courseItems(byModule, [second, first]).map(({ item }) => item.id),
    [3, 1, 2],
  )
})

test('courseItems keeps only the items the filter accepts', () => {
  assert.deepEqual(
    courseItems(byModule, [first, second], item => item.type !== 'STATIC_PAGE'),
    [
      { item: quiz, module: first },
      { item: task, module: second },
    ],
  )
})
