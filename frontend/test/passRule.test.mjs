import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  attemptPassed,
  bestAttempt,
  correctAnswersToPass,
  passRequirementText,
  quizPassed,
} from '../src/domain/quizzes/passRule.ts'

const attempt = correctCount => ({
  id: 1,
  quizId: 1,
  submittedAt: '',
  correctCount,
  totalQuestions: 5,
})

test('an attempt passes at the passing percentage or above, like the backend', () => {
  assert.equal(attemptPassed(attempt(3), 60), true)
  assert.equal(attemptPassed(attempt(2), 60), false)
  assert.equal(attemptPassed(attempt(4), 70), true)
  assert.equal(attemptPassed(attempt(3), 70), false)
  assert.equal(attemptPassed(attempt(5), undefined), null)
})

test('a quiz is passed once any attempt passed', () => {
  assert.equal(quizPassed([attempt(1), attempt(4)], 60), true)
  assert.equal(quizPassed([attempt(1), attempt(2)], 60), false)
  assert.equal(quizPassed([attempt(1)], undefined), null)
  assert.equal(quizPassed([], 60), null)
})

test('the correct answers needed are rounded up', () => {
  assert.equal(correctAnswersToPass(60, 5), 3)
  assert.equal(correctAnswersToPass(70, 5), 4)
  assert.equal(correctAnswersToPass(100, 3), 3)
  assert.equal(passRequirementText(60, 5), '60 % (3 of 5 correct)')
})

test('bestAttempt is the one with the most correct answers, the first of them on a tie', () => {
  const first = { id: 1, correctCount: 3 }
  const second = { id: 2, correctCount: 5 }
  const third = { id: 3, correctCount: 5 }
  assert.equal(bestAttempt([first, second, third]), second)
  assert.equal(bestAttempt([]), undefined)
})
