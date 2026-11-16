import type { QuizContentQuestion, QuizQuestionType } from '@domain/content/types'

/** The question's type; the backend's default is MULTIPLE_CHOICE. */
export const typeOf = (question: QuizContentQuestion): QuizQuestionType => question.type ?? 'MULTIPLE_CHOICE'
