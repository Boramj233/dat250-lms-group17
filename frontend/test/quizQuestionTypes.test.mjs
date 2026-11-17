import { test } from 'node:test'
import assert from 'node:assert/strict'
import { questionRequest } from '../src/domain/content/questionRequest.ts'
import {
  emptyQuestion,
  firstInvalidQuestion,
  questionProblem,
  questionsFromContent,
  withType,
} from '../src/features/course-management/module/components/ContentForm/quizQuestions.ts'
import {
  emptyAnswer,
  fromAttemptAnswer,
  isAnswered,
  isSendable,
  toAnswerInput,
  withMatch,
  withMovedItem,
  withOption,
} from '../src/features/course-view/content/QuizView/quizAnswers.ts'
import { gradeAnswer, questionRequestProblem, storedQuestion } from '../mock/quizQuestions.ts'

// --- Teachers: writing questions -------------------------------------------------------

test('questionRequest sends only the fields of the question type', () => {
  const choice = questionRequest(
    {
      questionText: ' 2 + 2? ',
      hint: '',
      options: [
        { optionText: '4', correct: true },
        { optionText: '5', correct: false },
      ],
    },
    0,
  )
  assert.deepEqual(choice, {
    questionText: '2 + 2?',
    position: 0,
    hint: null,
    type: 'MULTIPLE_CHOICE',
    options: [
      { optionText: '4', correct: true, position: 0 },
      { optionText: '5', correct: false, position: 1 },
    ],
  })

  const fill = questionRequest(
    { type: 'FILL_IN_BLANK', questionText: 'A ___ test', options: [], acceptedAnswers: [' unit ', ''] },
    1,
  )
  assert.deepEqual(fill.acceptedAnswers, ['unit'])
  assert.deepEqual(fill.options, [])
})

test('questionRequest gives ordering and matching items keys for the answer key', () => {
  const ordering = questionRequest(
    { type: 'ORDERING', questionText: 'Order', options: [], orderingItems: ['First', 'Second'] },
    0,
  )
  assert.deepEqual(ordering.ordering, {
    items: [
      { key: 'item-0', text: 'First' },
      { key: 'item-1', text: 'Second' },
    ],
    correctOrder: ['item-0', 'item-1'],
  })

  const matching = questionRequest(
    {
      type: 'MATCHING',
      questionText: 'Match',
      options: [],
      matchingPairs: [
        { left: 'A', right: '1' },
        { left: 'B', right: '2' },
      ],
    },
    0,
  )
  assert.deepEqual(matching.matching.correctMatches, [
    { leftKey: 'left-0', rightKey: 'right-0' },
    { leftKey: 'left-1', rightKey: 'right-1' },
  ])
  // The mock backend accepts what the frontend sends.
  assert.equal(questionRequestProblem(ordering), null)
  assert.equal(questionRequestProblem(matching), null)
})

test('a new question of each type is incomplete until its answer key is filled in', () => {
  for (const type of ['MULTIPLE_CHOICE', 'FILL_IN_BLANK', 'ORDERING', 'MATCHING']) {
    assert.notEqual(questionProblem(emptyQuestion(type)), null, type)
  }
  assert.equal(questionProblem({ ...emptyQuestion('TRUE_FALSE'), questionText: 'Sky is blue' }), null)
  assert.equal(
    questionProblem({ ...emptyQuestion('ORDERING'), questionText: 'Order', orderingItems: ['a', 'b'] }),
    null,
  )
  assert.match(
    questionProblem({
      ...emptyQuestion('MATCHING'),
      questionText: 'Match',
      matchingPairs: [
        { left: 'a', right: 'Same' },
        { left: 'b', right: 'same' },
      ],
    }),
    /no right side twice/,
  )
  assert.match(
    firstInvalidQuestion([emptyQuestion('FILL_IN_BLANK')]),
    /^Question 1 needs a text and at least one accepted/,
  )
})

test('changing the type keeps the text and hint and starts the answers over', () => {
  const question = { ...emptyQuestion(), questionText: 'Q', hint: 'H' }
  const ordering = withType(question, 'ORDERING')
  assert.equal(ordering.questionText, 'Q')
  assert.equal(ordering.hint, 'H')
  assert.deepEqual(ordering.options, [])
  assert.deepEqual(ordering.orderingItems, ['', ''])
  assert.deepEqual(
    withType(question, 'TRUE_FALSE').options.map(o => o.optionText),
    ['True', 'False'],
  )
})

test('saved questions become editable ones; unknown answer keys start empty', () => {
  const [fill, matching] = questionsFromContent([
    { id: 1, questionText: 'Fill', position: 0, type: 'FILL_IN_BLANK', options: [] },
    {
      id: 2,
      questionText: 'Match',
      position: 1,
      type: 'MATCHING',
      options: [],
      leftItems: [{ id: 'l', text: 'Left' }],
      rightItems: [{ id: 'r', text: 'Right' }],
    },
  ])
  assert.deepEqual(fill.acceptedAnswers, [''])
  assert.deepEqual(matching.matchingPairs, [{ left: 'Left', right: '' }])
})

// --- Students: answering -----------------------------------------------------------------

const items = [
  { id: 'a', text: 'A' },
  { id: 'b', text: 'B' },
  { id: 'c', text: 'C' },
]
const orderingQuestion = { id: 1, questionText: 'Order', position: 0, type: 'ORDERING', options: [], items }
const matchingQuestion = {
  id: 2,
  questionText: 'Match',
  position: 1,
  type: 'MATCHING',
  options: [],
  leftItems: [
    { id: 'l1', text: 'L1' },
    { id: 'l2', text: 'L2' },
  ],
  rightItems: [
    { id: 'r1', text: 'R1' },
    { id: 'r2', text: 'R2' },
  ],
}
const choiceQuestion = { id: 3, questionText: 'Choose', position: 2, type: 'TRUE_FALSE', options: [] }

test('an ordering answer starts in the order sent and moves one step at a time', () => {
  const start = emptyAnswer(orderingQuestion)
  assert.deepEqual(start.order, ['a', 'b', 'c'])
  assert.deepEqual(withMovedItem(start, 'c', -1).order, ['a', 'c', 'b'])
  assert.equal(withMovedItem(start, 'a', -1), start)
  assert.deepEqual(toAnswerInput(orderingQuestion, start), { questionId: 1, orderedItemIds: ['a', 'b', 'c'] })
})

test('a matching answer uses each right item once and must be complete to send', () => {
  let answer = withMatch(emptyAnswer(matchingQuestion), 'l1', 'r1')
  assert.equal(isAnswered(matchingQuestion, answer), true)
  assert.equal(isSendable(matchingQuestion, answer), false)
  // Choosing r1 for l2 takes it away from l1.
  answer = withMatch(answer, 'l2', 'r1')
  assert.deepEqual(answer.matches, { l2: 'r1' })
  answer = withMatch(answer, 'l1', 'r2')
  assert.equal(isSendable(matchingQuestion, answer), true)
  assert.deepEqual(toAnswerInput(matchingQuestion, answer).matches, [
    { leftItemId: 'l2', rightItemId: 'r1' },
    { leftItemId: 'l1', rightItemId: 'r2' },
  ])
  assert.deepEqual(withMatch(answer, 'l1', '').matches, { l2: 'r1' })
})

test('fill-in answers are trimmed; true or false keeps one option', () => {
  const fill = { id: 4, questionText: 'Fill', position: 3, type: 'FILL_IN_BLANK', options: [] }
  assert.equal(isAnswered(fill, { ...emptyAnswer(fill), text: '   ' }), false)
  assert.deepEqual(toAnswerInput(fill, { ...emptyAnswer(fill), text: ' unit ' }), { questionId: 4, textAnswer: 'unit' })
  const one = withOption(choiceQuestion, emptyAnswer(choiceQuestion), '7', true)
  assert.deepEqual(withOption(choiceQuestion, one, '8', true).optionIds, ['8'])
})

test('a stored answer is shown again', () => {
  const shown = fromAttemptAnswer(matchingQuestion, {
    questionId: 2,
    selectedOptionIds: [],
    matches: [{ leftItemId: 'l1', rightItemId: 'r2' }],
    correct: false,
  })
  assert.deepEqual(shown.matches, { l1: 'r2' })
  assert.deepEqual(fromAttemptAnswer(orderingQuestion, undefined).order, [])
})

// --- The mock backend grades like the real one ------------------------------------------

test('the mock grades every type and refuses incomplete answers', () => {
  const stored = storedQuestion(
    questionRequest(
      {
        type: 'MATCHING',
        questionText: 'Match',
        options: [],
        matchingPairs: [
          { left: 'A', right: '1' },
          { left: 'B', right: '2' },
        ],
      },
      0,
    ),
    { questionId: 9, quizId: 1, firstOptionId: 1 },
  )
  const right = stored.correctMatches
  assert.equal(gradeAnswer(stored, { questionId: 9, matches: right }), true)
  const swapped = [
    { leftItemId: right[0].leftItemId, rightItemId: right[1].rightItemId },
    { leftItemId: right[1].leftItemId, rightItemId: right[0].rightItemId },
  ]
  assert.equal(gradeAnswer(stored, { questionId: 9, matches: swapped }), false)
  assert.equal(typeof gradeAnswer(stored, { questionId: 9, matches: [right[0]] }), 'string')

  const fill = {
    id: 5,
    quizId: 1,
    questionText: 'F',
    position: 0,
    hint: null,
    type: 'FILL_IN_BLANK',
    options: [],
    acceptedAnswers: ['Unit'],
  }
  assert.equal(gradeAnswer(fill, { questionId: 5, textAnswer: ' unit ' }), true)
  assert.equal(gradeAnswer(fill, { questionId: 5, textAnswer: '' }), false)
  assert.equal(typeof gradeAnswer(fill, undefined), 'string')
})
