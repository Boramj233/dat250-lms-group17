import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  formProblem,
  initialValues,
  toFields,
} from '../src/features/course-management/module/components/ContentForm/contentFormValues.ts'

const module = { id: 1, title: 'Module 1', position: 0, availableFrom: null }
const question = {
  questionText: '2 + 2?',
  questionType: 'SINGLE_CHOICE',
  options: [
    { optionText: '4', correct: true },
    { optionText: '5', correct: false },
  ],
}
const values = changes => ({ ...initialValues(undefined), title: '  Week 1  ', body: 'Read it.', ...changes })

test('a new form starts empty, with one empty question and one attempt', () => {
  const start = initialValues(undefined)
  assert.equal(start.title, '')
  assert.equal(start.maxAttempts, 1)
  assert.equal(start.questions.length, 1)
  assert.deepEqual(start.fileTypePresets, [])
})

test('editing starts from the saved item', () => {
  const start = initialValues({
    id: 7,
    type: 'ASSIGNMENT',
    title: 'Essay',
    instructions: 'Write.',
    deadline: null,
    allowLateSubmission: true,
    mandatory: true,
    acceptedFileTypes: ['.pdf', '.py'],
  })
  assert.equal(start.title, 'Essay')
  assert.equal(start.body, 'Write.')
  assert.equal(start.allowLate, true)
  assert.equal(start.mandatory, true)
  assert.deepEqual(start.fileTypePresets, ['PDF'])
  assert.equal(start.otherFileTypes, '.py')
})

test('formProblem: a coding exercise needs a starter repository or starter code', () => {
  assert.match(formProblem('CODING_EXERCISE', values({}), module), /starter repository, starter code, or both/)
  assert.equal(formProblem('CODING_EXERCISE', values({ starterCode: 'print()' }), module), null)
})

test('formProblem: a quiz needs complete questions and a pass requirement within the questions', () => {
  assert.match(formProblem('QUIZ', values({}), module), /^Question 1 needs/)
  assert.equal(formProblem('QUIZ', values({ questions: [question] }), module), null)
  assert.match(formProblem('QUIZ', values({ questions: [question], requiredCorrect: '2' }), module), /from 1 to 1/)
  assert.match(formProblem('QUIZ', values({ questions: [question], requiredCorrect: '0.5' }), module), /whole number/)
})

test('formProblem: content cannot be visible before its module opens', () => {
  const opensLater = { ...module, availableFrom: '2030-01-10T08:00:00Z' }
  assert.notEqual(formProblem('STATIC_PAGE', values({ visibleFrom: '2030-01-01T08:00' }), opensLater), null)
  assert.equal(formProblem('STATIC_PAGE', values({}), opensLater), null)
})

test('toFields sends only the fields of the type, trimmed', () => {
  assert.deepEqual(toFields('STATIC_PAGE', values({})), {
    title: 'Week 1',
    visibleFrom: null,
    type: 'STATIC_PAGE',
    body: 'Read it.',
  })
  assert.deepEqual(toFields('CODING_EXERCISE', values({ starterUrl: '  ', starterCode: 'x = 1' })), {
    title: 'Week 1',
    visibleFrom: null,
    type: 'CODING_EXERCISE',
    instructions: 'Read it.',
    deadline: null,
    allowLateSubmission: false,
    starterRepositoryUrl: null,
    starterCode: 'x = 1',
  })
  const assignment = toFields('ASSIGNMENT', values({ fileTypePresets: ['PDF'], otherFileTypes: 'py' }))
  assert.deepEqual(assignment.acceptedFileTypes, ['.pdf', '.py'])
  assert.equal('starterCode' in assignment, false)
})

test('toFields: an empty pass requirement is sent as null', () => {
  const quiz = toFields('QUIZ', values({ questions: [question], maxAttempts: 3 }))
  assert.equal(quiz.requiredCorrectAnswers, null)
  assert.equal(quiz.maxAttempts, 3)
  assert.equal(toFields('QUIZ', values({ questions: [question], requiredCorrect: '1' })).requiredCorrectAnswers, 1)
})
