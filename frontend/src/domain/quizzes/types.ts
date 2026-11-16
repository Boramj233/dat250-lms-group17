import type { ContentId } from '@shared/types'

// A quiz is a content item. Attempt IDs are local to this domain.
export type QuizId = ContentId
export type QuizAttemptId = string | number

// Taking a quiz as the backend supports it (QuizAttemptController): all answers are
// sent at once and graded straight away.

// Matches the backend's QuizAttemptResponse.
export interface QuizAttemptScore {
  id: QuizAttemptId
  quizId: QuizId
  /** ISO 8601 instant. */
  submittedAt: string
  correctCount: number
  totalQuestions: number
  /**
   * Proposed: whether the attempt reached the quiz's requiredCorrectAnswers. Null when
   * the quiz has no pass requirement; missing until the backend sends it.
   */
  passed?: boolean | null
}

// Matches the backend's SubmitQuizRequest; an unanswered question has no option ids.
export interface SubmitQuizAnswersInput {
  quizId: QuizId
  answers: { questionId: string | number; selectedOptionIds: (string | number)[] }[]
}

/**
 * Proposed: one of the user's attempts with the answers (GET
 * /api/quizzes/{quizId}/attempts/{attemptId}). The backend already stores them
 * (QuizAnswer); it has no endpoint to read them yet.
 */
export interface QuizAttemptDetail extends QuizAttemptScore {
  answers: QuizAttemptAnswer[]
}

/** How the user answered one question in an attempt, and whether it was right. */
export interface QuizAttemptAnswer {
  questionId: string | number
  /** Empty when the question was not answered. */
  selectedOptionIds: (string | number)[]
  correct: boolean
}
