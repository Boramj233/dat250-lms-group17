import type { QuizContentQuestion } from '@domain/content/types'
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
 * Matches the backend's QuizAttemptDetailResponse: one attempt with the answers. The
 * student's own: GET /api/quizzes/{quizId}/attempts/{attemptId}; any student's, for
 * teachers of the course: GET …/attempts/{attemptId}/review. Never the answer key.
 */
export interface QuizAttemptDetail extends QuizAttemptScore {
  answers: QuizAttemptAnswer[]
}

// Matches the backend's QuizAttemptCountResponse: GET /api/quizzes/{quizId}/attempts/me/count.
export interface QuizAttemptCount {
  usedAttempts: number
}

/**
 * Matches the backend's QuizAnswerKeyResponse (teachers of the course only):
 * GET /api/quizzes/{quizId}/answer-key. One entry per question, in order.
 */
export interface QuizAnswerKey {
  quizId: QuizId
  questions: QuizQuestionKey[]
}

// QuizAnswerKeyResponse.QuestionKeyResponse. Which fields are filled depends on the type.
export interface QuizQuestionKey {
  /** The question as students see it (QuizDetailResponse.QuestionResponse). */
  question: QuizContentQuestion
  hint: string | null
  /** MULTIPLE_CHOICE, TRUE_FALSE. */
  correctOptionIds: (string | number)[]
  /** FILL_IN_BLANK: every answer that counts as correct (compared without case). */
  acceptedAnswers: string[]
  /** ORDERING: the item ids in the correct order. */
  correctOrder: string[]
  /** MATCHING: the correct pairs. */
  correctMatches: { leftItemId: string; rightItemId: string }[]
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
