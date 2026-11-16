import { test } from 'node:test'
import assert from 'node:assert/strict'
import { attemptPassed, bestAttempt, quizPassed } from '../src/domain/quizzes/passRule.ts'

const attempt = (correctCount, passed) => ({
  id: 1,
  quizId: 1,
  submittedAt: '',
  correctCount,
  totalQuestions: 5,
  ...(passed === undefined ? {} : { passed }),
})

test('an attempt passes with at least the required number of correct answers', () => {
  assert.equal(attemptPassed(attempt(3), 3), true)
  assert.equal(attemptPassed(attempt(2), 3), false)
  assert.equal(attemptPassed(attempt(0), null), null)
  assert.equal(attemptPassed(attempt(5), undefined), null)
})

test("the backend's passed wins over the frontend's own check", () => {
  assert.equal(attemptPassed(attempt(1, true), 3), true)
  assert.equal(attemptPassed(attempt(5, false), 3), false)
  assert.equal(attemptPassed(attempt(5, null), 3), null)
})

test('a quiz is passed once any attempt passed', () => {
  assert.equal(quizPassed([attempt(1), attempt(4)], 3), true)
  assert.equal(quizPassed([attempt(1), attempt(2)], 3), false)
  assert.equal(quizPassed([attempt(1)], null), null)
  assert.equal(quizPassed([], 3), null)
})

test('bestAttempt is the one with the most correct answers, the first of them on a tie', () => {
  const first = { id: 1, correctCount: 3 }
  const second = { id: 2, correctCount: 5 }
  const third = { id: 3, correctCount: 5 }
  assert.equal(bestAttempt([first, second, third]), second)
  assert.equal(bestAttempt([]), undefined)
})
