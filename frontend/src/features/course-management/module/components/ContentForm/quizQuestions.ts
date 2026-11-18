import type { NewQuizQuestion, QuizContentQuestion, QuizQuestionType } from '@domain/content/types'
import type { QuizAnswerKey } from '@domain/quizzes/types'

/** The backend's limits (CreateQuizQuestionRequest). */
const MAX_ITEMS = 20
const MAX_ITEM_TEXT = 255

/** The question types a teacher can choose, in the order of the menu. */
export const QUESTION_TYPES: { type: QuizQuestionType; label: string }[] = [
  { type: 'MULTIPLE_CHOICE', label: 'Multiple choice' },
  { type: 'TRUE_FALSE', label: 'True or false' },
  { type: 'FILL_IN_BLANK', label: 'Fill in the blank' },
  { type: 'ORDERING', label: 'Ordering' },
  { type: 'MATCHING', label: 'Matching' },
]

/** True or false always has these two answers; the teacher only picks the correct one. */
const TRUE_FALSE_OPTIONS = (trueIsCorrect: boolean) => [
  { optionText: 'True', correct: trueIsCorrect },
  { optionText: 'False', correct: !trueIsCorrect },
]

/** The fields of a new question of `type`, with empty texts. */
function typeFields(type: QuizQuestionType): Omit<NewQuizQuestion, 'questionText' | 'hint'> {
  switch (type) {
    case 'MULTIPLE_CHOICE':
      return {
        type,
        options: [
          { optionText: '', correct: true },
          { optionText: '', correct: false },
        ],
      }
    case 'TRUE_FALSE':
      return { type, options: TRUE_FALSE_OPTIONS(true) }
    case 'FILL_IN_BLANK':
      return { type, options: [], acceptedAnswers: [''] }
    case 'ORDERING':
      return { type, options: [], orderingItems: ['', ''] }
    case 'MATCHING':
      return {
        type,
        options: [],
        matchingPairs: [
          { left: '', right: '' },
          { left: '', right: '' },
        ],
      }
  }
}

/** A new multiple choice question (or of `type`) with empty texts. */
export const emptyQuestion = (type: QuizQuestionType = 'MULTIPLE_CHOICE'): NewQuizQuestion => ({
  questionText: '',
  hint: '',
  ...typeFields(type),
})

/** The question as another type: the text and hint stay, the answers start over. */
export function withType(question: NewQuizQuestion, type: QuizQuestionType): NewQuizQuestion {
  return { questionText: question.questionText, hint: question.hint, ...typeFields(type) }
}

const blank = (text: string) => !text.trim()
const tooLong = (text: string) => text.trim().length > MAX_ITEM_TEXT

/** What is missing in the question, as the end of "Question 2 needs …", or null when it is complete. */
export function questionProblem(question: NewQuizQuestion): string | null {
  const text = blank(question.questionText) ? 'a text and ' : ''
  switch (question.type ?? 'MULTIPLE_CHOICE') {
    case 'MULTIPLE_CHOICE': {
      const ok = question.options.length >= 2 && !question.options.some(o => blank(o.optionText))
      return ok && question.options.some(o => o.correct) && !text
        ? null
        : 'a text, text in every answer and at least one correct answer.'
    }
    case 'TRUE_FALSE':
      return question.options.filter(o => o.correct).length === 1 && !text
        ? null
        : 'a text and either True or False marked as correct.'
    case 'FILL_IN_BLANK': {
      const answers = (question.acceptedAnswers ?? []).filter(answer => !blank(answer))
      return answers.length > 0 && answers.length <= MAX_ITEMS && !answers.some(tooLong) && !text
        ? null
        : `${text}at least one accepted answer (at most ${MAX_ITEMS}, ${MAX_ITEM_TEXT} characters each).`
    }
    case 'ORDERING': {
      const items = question.orderingItems ?? []
      return items.length >= 2 && items.length <= MAX_ITEMS && !items.some(blank) && !items.some(tooLong) && !text
        ? null
        : `${text}2 to ${MAX_ITEMS} items, each with a text of at most ${MAX_ITEM_TEXT} characters.`
    }
    case 'MATCHING': {
      const pairs = question.matchingPairs ?? []
      const sides = pairs.flatMap(pair => [pair.left, pair.right])
      const rights = pairs.map(pair => pair.right.trim().toLowerCase())
      return pairs.length >= 2 &&
        pairs.length <= MAX_ITEMS &&
        !sides.some(blank) &&
        !sides.some(tooLong) &&
        new Set(rights).size === rights.length &&
        !text
        ? null
        : `${text}2 to ${MAX_ITEMS} pairs with text on both sides, and no right side twice.`
    }
  }
}

/** "Question 2 needs …" for the first question that is not complete, or undefined if all are. */
export function firstInvalidQuestion(questions: NewQuizQuestion[]): string | undefined {
  for (const [index, question] of questions.entries()) {
    const problem = questionProblem(question)
    if (problem) return `Question ${index + 1} needs ${problem}`
  }
  return undefined
}

/**
 * A saved quiz's questions as editable ones, in order, with the correct answers from the
 * answer key (GET /api/quizzes/{id}/answer-key) when it is given. The quiz itself
 * (GET /api/quizzes/{id}) has no answer key, so without it fill-in answers and the right
 * side of matching pairs start empty, no option is marked correct, and ordering items
 * come in the order they were sent.
 */
export function questionsFromContent(questions: QuizContentQuestion[], answerKey?: QuizAnswerKey): NewQuizQuestion[] {
  return [...questions]
    .sort((a, b) => a.position - b.position)
    .map(question => {
      const key = answerKey?.questions.find(entry => String(entry.question?.id) === String(question.id))
      const base = { questionText: question.questionText, hint: key?.hint ?? question.hint ?? '' }
      const type = question.type ?? 'MULTIPLE_CHOICE'
      switch (type) {
        case 'MULTIPLE_CHOICE':
        case 'TRUE_FALSE': {
          const correctIds = key?.correctOptionIds.map(String)
          return {
            ...base,
            type,
            options: [...question.options]
              .sort((a, b) => a.position - b.position)
              .map(option => ({
                optionText: option.optionText,
                correct: correctIds ? correctIds.includes(String(option.id)) : (option.correct ?? false),
              })),
          }
        }
        case 'FILL_IN_BLANK':
          return key?.acceptedAnswers.length
            ? { ...base, type, options: [], acceptedAnswers: [...key.acceptedAnswers] }
            : { ...base, ...typeFields(type) }
        case 'ORDERING': {
          const items = question.items ?? []
          const inOrder = key?.correctOrder.length
            ? key.correctOrder.flatMap(id => items.filter(item => item.id === id))
            : items
          return { ...base, type, options: [], orderingItems: inOrder.map(item => item.text) }
        }
        case 'MATCHING': {
          const rightText = (leftId: string) => {
            const rightId = key?.correctMatches.find(match => match.leftItemId === leftId)?.rightItemId
            return question.rightItems?.find(item => item.id === rightId)?.text ?? ''
          }
          return {
            ...base,
            type,
            options: [],
            matchingPairs: (question.leftItems ?? []).map(item => ({ left: item.text, right: rightText(item.id) })),
          }
        }
      }
    })
}
