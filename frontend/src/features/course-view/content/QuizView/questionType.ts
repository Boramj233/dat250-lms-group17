import type { QuizContentQuestion, QuizQuestionType } from '@domain/content/types'

/** The question's type; missing means single choice. */
export const typeOf = (question: QuizContentQuestion): QuizQuestionType => question.questionType ?? 'SINGLE_CHOICE'
