import type { QuizContentQuestion, QuizQuestionType } from '@domain/content/types'

/** The question's type; the backend's default is MULTIPLE_CHOICE. */
export const typeOf = (question: QuizContentQuestion): QuizQuestionType => question.type ?? 'MULTIPLE_CHOICE'

/**
 * Whether the quiz form can answer the question: choice questions (option ids). Fill-in,
 * ordering and matching questions have no answer fields here yet, and the backend
 * rejects the whole attempt (400) when one of them is not answered.
 */
export const canAnswer = (question: QuizContentQuestion): boolean => {
  const type = typeOf(question)
  return type === 'MULTIPLE_CHOICE' || type === 'TRUE_FALSE'
}
