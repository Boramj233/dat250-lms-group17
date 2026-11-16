// Quiz questions in the mock backend, following the real one (QuizService, QuizGrader,
// QuizDetailResponse): checking a CreateQuizQuestionRequest, storing it, showing it and
// grading an answer, for all five question types.
import type { CreateQuizQuestionRequest } from '../src/domain/content/questionRequest'
import type { MockQuizQuestion } from './data'

const MAX_ITEMS = 20

/** Why the backend would refuse the request (400), or null when it is valid. */
export function questionRequestProblem(request: CreateQuizQuestionRequest): string | null {
  if (!request.questionText?.trim()) return 'Question text is required'
  const type = request.type ?? 'MULTIPLE_CHOICE'
  const options = request.options ?? []
  const count = (n: number) => n >= 2 && n <= MAX_ITEMS
  switch (type) {
    case 'MULTIPLE_CHOICE':
      return options.length >= 2 && options.some(o => o.correct)
        ? null
        : 'Choice questions need two options, one correct'
    case 'TRUE_FALSE':
      return options.length === 2 && options.filter(o => o.correct).length === 1
        ? null
        : 'True/false questions need two options and exactly one correct option'
    case 'FILL_IN_BLANK': {
      const answers = request.acceptedAnswers ?? []
      return options.length === 0 && answers.length > 0 && answers.length <= MAX_ITEMS && answers.every(a => a.trim())
        ? null
        : 'Fill-in questions need 1–20 accepted answers and no options'
    }
    case 'ORDERING': {
      const ordering = request.ordering
      const keys = new Set(ordering?.items.map(item => item.key))
      return ordering &&
        options.length === 0 &&
        count(ordering.items.length) &&
        keys.size === ordering.items.length &&
        ordering.correctOrder.length === keys.size &&
        ordering.correctOrder.every(key => keys.has(key))
        ? null
        : 'Ordering questions need 2–20 items and every item once in the correct order'
    }
    case 'MATCHING': {
      const matching = request.matching
      const left = new Set(matching?.leftItems.map(item => item.key))
      const right = new Set(matching?.rightItems.map(item => item.key))
      const usedRight = new Set(matching?.correctMatches.map(match => match.rightKey))
      return matching &&
        options.length === 0 &&
        count(left.size) &&
        count(right.size) &&
        matching.correctMatches.length === left.size &&
        usedRight.size === left.size &&
        matching.correctMatches.every(match => left.has(match.leftKey) && right.has(match.rightKey))
        ? null
        : 'Matching questions need 2–20 items on each side and one correct match per left item'
    }
  }
}

/** The stored question for a valid request, with new ids (UUIDs for items, like the backend). */
export function storedQuestion(
  request: CreateQuizQuestionRequest,
  ids: { questionId: number; quizId: number; firstOptionId: number },
): MockQuizQuestion {
  const type = request.type ?? 'MULTIPLE_CHOICE'
  const base: MockQuizQuestion = {
    id: ids.questionId,
    quizId: ids.quizId,
    questionText: request.questionText.trim(),
    position: request.position,
    hint: request.hint?.trim() || null,
    type,
    options: (request.options ?? []).map((option, index) => ({ ...option, id: ids.firstOptionId + index })),
  }
  if (type === 'FILL_IN_BLANK') return { ...base, acceptedAnswers: request.acceptedAnswers }
  if (type === 'ORDERING' && request.ordering) {
    const idOf = new Map(request.ordering.items.map(item => [item.key, crypto.randomUUID()]))
    return {
      ...base,
      items: request.ordering.items.map(item => ({ id: idOf.get(item.key)!, text: item.text, kind: 'ORDERING' })),
      correctOrder: request.ordering.correctOrder.map(key => idOf.get(key)!),
    }
  }
  if (type === 'MATCHING' && request.matching) {
    const { leftItems, rightItems, correctMatches } = request.matching
    const leftIds = new Map(leftItems.map(item => [item.key, crypto.randomUUID()]))
    const rightIds = new Map(rightItems.map(item => [item.key, crypto.randomUUID()]))
    return {
      ...base,
      items: [
        ...leftItems.map(item => ({ id: leftIds.get(item.key)!, text: item.text, kind: 'MATCHING_LEFT' as const })),
        ...rightItems.map(item => ({ id: rightIds.get(item.key)!, text: item.text, kind: 'MATCHING_RIGHT' as const })),
      ],
      correctMatches: correctMatches.map(match => ({
        leftItemId: leftIds.get(match.leftKey)!,
        rightItemId: rightIds.get(match.rightKey)!,
      })),
    }
  }
  return base
}

/**
 * The items of one kind as QuizDetailResponse shows them: sorted by id, so not in the
 * correct order (the backend does the same).
 */
function itemsOf(question: MockQuizQuestion, kind: 'ORDERING' | 'MATCHING_LEFT' | 'MATCHING_RIGHT') {
  return (question.items ?? [])
    .filter(item => item.kind === kind)
    .map(item => ({ id: item.id, text: item.text }))
    .sort((a, b) => a.id.localeCompare(b.id))
}

/**
 * QuizDetailResponse.QuestionResponse. For teachers of the course also the proposed `hint`
 * and `correct` fields (src/domain/content/README.md).
 */
export function questionResponse(question: MockQuizQuestion, forTeacher: boolean) {
  return {
    id: question.id,
    questionText: question.questionText,
    position: question.position,
    type: question.type ?? 'MULTIPLE_CHOICE',
    ...(forTeacher ? { hint: question.hint } : {}),
    options: [...question.options]
      .sort((a, b) => a.position - b.position)
      .map(o => ({
        id: o.id,
        optionText: o.optionText,
        position: o.position,
        ...(forTeacher ? { correct: o.correct } : {}),
      })),
    items: itemsOf(question, 'ORDERING'),
    leftItems: itemsOf(question, 'MATCHING_LEFT'),
    rightItems: itemsOf(question, 'MATCHING_RIGHT'),
  }
}

/** SubmitQuizRequest.Answer. */
export type AnswerRequest = {
  questionId: number
  selectedOptionIds?: number[] | null
  textAnswer?: string | null
  orderedItemIds?: string[] | null
  matches?: { leftItemId: string; rightItemId: string }[] | null
}

/**
 * Grades one answer like QuizGrader: whether it is correct, or the reason the backend
 * refuses the whole attempt (400). A missing choice answer counts as wrong.
 */
export function gradeAnswer(question: MockQuizQuestion, answer: AnswerRequest | undefined): boolean | string {
  switch (question.type ?? 'MULTIPLE_CHOICE') {
    case 'MULTIPLE_CHOICE':
    case 'TRUE_FALSE': {
      const chosen = new Set((answer?.selectedOptionIds ?? []).map(Number))
      const correctIds = question.options.filter(o => o.correct).map(o => o.id)
      return chosen.size === correctIds.length && correctIds.every(id => chosen.has(id))
    }
    case 'FILL_IN_BLANK': {
      const text = answer?.textAnswer
      if (text === undefined || text === null || text.length > 255) {
        return 'Text answer is required and must not exceed 255 characters'
      }
      const normalized = text.trim().toLowerCase()
      return normalized !== '' && (question.acceptedAnswers ?? []).some(a => a.trim().toLowerCase() === normalized)
    }
    case 'ORDERING': {
      const order = answer?.orderedItemIds ?? []
      const correct = question.correctOrder ?? []
      if (
        order.length !== correct.length ||
        new Set(order).size !== order.length ||
        !order.every(id => correct.includes(id))
      ) {
        return 'Submitted order must contain every question item exactly once'
      }
      return order.every((id, index) => id === correct[index])
    }
    case 'MATCHING': {
      const matches = answer?.matches ?? []
      const expected = question.correctMatches ?? []
      const lefts = new Set(matches.map(m => m.leftItemId))
      const rights = new Set(matches.map(m => m.rightItemId))
      const complete =
        matches.length === expected.length &&
        lefts.size === matches.length &&
        rights.size === matches.length &&
        expected.every(e => lefts.has(e.leftItemId) && rights.has(e.rightItemId))
      if (!complete) return 'Matches must cover both sides of this question completely'
      return expected.every(e => matches.some(m => m.leftItemId === e.leftItemId && m.rightItemId === e.rightItemId))
    }
  }
}
