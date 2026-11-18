import type { QuizContentQuestion } from '@domain/content/types'
import type { QuizAnswerInput, QuizAttemptAnswer, QuizQuestionKey } from '@domain/quizzes/types'
import { typeOf } from './questionType'

/**
 * A student's answer to one question while taking the quiz (or a stored one shown
 * again). Only the field of the question's type is used:
 * - `optionIds`: MULTIPLE_CHOICE, TRUE_FALSE
 * - `text`: FILL_IN_BLANK
 * - `order`: ORDERING, every item id once
 * - `matches`: MATCHING, right item id per left item id
 */
export type AnswerDraft = {
  optionIds: string[]
  text: string
  order: string[]
  matches: Record<string, string>
}

/** No answer yet. An ordering question starts with its items in the order they came. */
export function emptyAnswer(question: QuizContentQuestion): AnswerDraft {
  return {
    optionIds: [],
    text: '',
    order: typeOf(question) === 'ORDERING' ? (question.items ?? []).map(item => item.id) : [],
    matches: {},
  }
}

/** Chooses or unchooses an option: several for multiple choice, one for true/false. */
export function withOption(
  question: QuizContentQuestion,
  answer: AnswerDraft,
  optionId: string,
  checked: boolean,
): AnswerDraft {
  if (typeOf(question) !== 'MULTIPLE_CHOICE') return { ...answer, optionIds: [optionId] }
  const others = answer.optionIds.filter(id => id !== optionId)
  return { ...answer, optionIds: checked ? [...others, optionId] : others }
}

/** Moves an ordering item one step up (-1) or down (+1); nothing at the ends. */
export function withMovedItem(answer: AnswerDraft, itemId: string, step: -1 | 1): AnswerDraft {
  const from = answer.order.indexOf(itemId)
  const to = from + step
  if (from < 0 || to < 0 || to >= answer.order.length) return answer
  const order = [...answer.order]
  ;[order[from], order[to]] = [order[to], order[from]]
  return { ...answer, order }
}

/**
 * Matches a left item with a right item (or clears it with ''). Each right item is
 * used once: a left item that had it before loses it.
 */
export function withMatch(answer: AnswerDraft, leftId: string, rightId: string): AnswerDraft {
  const matches = Object.fromEntries(
    Object.entries(answer.matches).filter(([left, right]) => left !== leftId && right !== rightId),
  )
  if (rightId) matches[leftId] = rightId
  return { ...answer, matches }
}

/**
 * Whether the student has answered the question. An ordering question always counts as
 * answered, since its items are always in some order.
 */
export function isAnswered(question: QuizContentQuestion, answer: AnswerDraft): boolean {
  switch (typeOf(question)) {
    case 'MULTIPLE_CHOICE':
    case 'TRUE_FALSE':
      return answer.optionIds.length > 0
    case 'FILL_IN_BLANK':
      return answer.text.trim() !== ''
    case 'ORDERING':
      return true
    case 'MATCHING':
      return Object.keys(answer.matches).length > 0
  }
}

/**
 * Whether the answer can be sent. The backend refuses the whole attempt (400) when a
 * matching question does not match every left item, so those must be complete; any
 * other question may be left unanswered (it counts as wrong).
 */
export function isSendable(question: QuizContentQuestion, answer: AnswerDraft): boolean {
  if (typeOf(question) !== 'MATCHING') return true
  return (question.leftItems ?? []).every(item => answer.matches[item.id])
}

/** The answer as SubmitQuizRequest.Answer, with only the field of the question's type. */
export function toAnswerInput(question: QuizContentQuestion, answer: AnswerDraft): QuizAnswerInput {
  const questionId = question.id
  switch (typeOf(question)) {
    case 'MULTIPLE_CHOICE':
    case 'TRUE_FALSE':
      return { questionId, selectedOptionIds: answer.optionIds }
    case 'FILL_IN_BLANK':
      // The backend needs a text; an empty one counts as wrong.
      return { questionId, textAnswer: answer.text.trim() }
    case 'ORDERING':
      return { questionId, orderedItemIds: answer.order }
    case 'MATCHING':
      return {
        questionId,
        matches: Object.entries(answer.matches).map(([leftItemId, rightItemId]) => ({ leftItemId, rightItemId })),
      }
  }
}

/**
 * The answer key of a question as an answer, to show the correct one read-only. A
 * fill-in question shows every accepted answer, separated by " / ".
 */
export function fromAnswerKey(question: QuizContentQuestion, key: QuizQuestionKey | undefined): AnswerDraft {
  if (!key) return { ...emptyAnswer(question), order: [] }
  return {
    optionIds: key.correctOptionIds.map(String),
    text: key.acceptedAnswers.join(' / '),
    order: key.correctOrder,
    matches: Object.fromEntries(key.correctMatches.map(match => [match.leftItemId, match.rightItemId])),
  }
}

/** A stored answer of an earlier attempt, to show it read-only. */
export function fromAttemptAnswer(question: QuizContentQuestion, stored: QuizAttemptAnswer | undefined): AnswerDraft {
  const empty = emptyAnswer(question)
  if (!stored) return { ...empty, order: [] }
  return {
    optionIds: (stored.selectedOptionIds ?? []).map(String),
    text: stored.textAnswer ?? '',
    order: stored.orderedItemIds ?? [],
    matches: Object.fromEntries((stored.matches ?? []).map(match => [match.leftItemId, match.rightItemId])),
  }
}
