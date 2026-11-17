import type { ContentId } from '@shared/types'

// A quiz is a content item. Attempt IDs are local to this domain.
export type QuizId = ContentId
export type QuizAttemptId = string | number

// Taking a quiz as the backend supports it (QuizAttemptController): all answers are
// sent at once and graded straight away.

// Matches the backend's QuizAttemptResponse. Whether it passed is not sent; see
// passRule.ts (the quiz's passingPercentage).
export interface QuizAttemptScore {
  id: QuizAttemptId
  quizId: QuizId
  /** ISO 8601 instant. */
  submittedAt: string
  correctCount: number
  totalQuestions: number
  /**
   * Hints for the questions answered wrong, in the answer to POST …/attempts; empty
   * (or missing) elsewhere.
   */
  hints?: QuizAttemptHint[]
}

// QuizAttemptResponse.HintResponse.
export interface QuizAttemptHint {
  questionId: string | number
  hint: string
}

// Matches the backend's SubmitQuizRequest.Answer. Which field is used depends on the
// question's type: selectedOptionIds (MULTIPLE_CHOICE, TRUE_FALSE; empty when not
// answered), textAnswer (FILL_IN_BLANK), orderedItemIds (ORDERING) or matches (MATCHING).
export interface QuizAnswerInput {
  questionId: string | number
  selectedOptionIds?: (string | number)[]
  textAnswer?: string
  orderedItemIds?: string[]
  matches?: { leftItemId: string; rightItemId: string }[]
}

// Matches the backend's SubmitQuizRequest.
export interface SubmitQuizAnswersInput {
  quizId: QuizId
  answers: QuizAnswerInput[]
}

/**
 * Proposed: one of the user's attempts with the answers (GET
 * /api/quizzes/{quizId}/attempts/{attemptId}). The backend already stores them
 * (QuizAnswer); it has no endpoint to read them yet.
 */
export interface QuizAttemptDetail extends QuizAttemptScore {
  answers: QuizAttemptAnswer[]
}

/**
 * How the user answered one question in an attempt, and whether it was right. The
 * backend stores all of these in QuizAnswer; which are set depends on the question's type.
 */
export interface QuizAttemptAnswer {
  questionId: string | number
  /** Choice questions; empty when the question was not answered. */
  selectedOptionIds: (string | number)[]
  textAnswer?: string | null
  orderedItemIds?: string[] | null
  matches?: { leftItemId: string; rightItemId: string }[] | null
  correct: boolean
}
