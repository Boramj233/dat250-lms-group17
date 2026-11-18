import { apiClient, apiPath, jsonBody } from '@shared/api/apiClient'
import type { UserId } from '@shared/types'
import type {
  QuizAnswerKey,
  QuizAttemptCount,
  QuizAttemptDetail,
  QuizAttemptId,
  QuizAttemptScore,
  QuizId,
  SubmitQuizAnswersInput,
} from './types'

// --- Students (QuizAttemptController) -------------------------------------------------

/** Hands in all answers of a quiz and returns the score. */
export function submitQuizAnswers({ quizId, answers }: SubmitQuizAnswersInput): Promise<QuizAttemptScore> {
  return apiClient<QuizAttemptScore>(apiPath`/api/quizzes/${quizId}/attempts`, jsonBody('POST', { answers }))
}

/** The logged-in user's attempts at a quiz. */
export function getMyQuizAttempts(quizId: QuizId): Promise<QuizAttemptScore[]> {
  return apiClient<QuizAttemptScore[]>(apiPath`/api/quizzes/${quizId}/attempts/me`)
}

/** How many attempts the logged-in user has used. */
export function getMyQuizAttemptCount(quizId: QuizId): Promise<QuizAttemptCount> {
  return apiClient<QuizAttemptCount>(apiPath`/api/quizzes/${quizId}/attempts/me/count`)
}

/** One of the user's own attempts with the answers and which were correct. */
export function getMyQuizAttempt(quizId: QuizId, attemptId: QuizAttemptId): Promise<QuizAttemptDetail> {
  return apiClient<QuizAttemptDetail>(apiPath`/api/quizzes/${quizId}/attempts/${attemptId}`)
}

// --- Teachers of the course (QuizAttemptController, QuizController) -------------------

/** One student's attempts at a quiz. */
export function getStudentQuizAttempts(quizId: QuizId, studentUserId: UserId): Promise<QuizAttemptScore[]> {
  return apiClient<QuizAttemptScore[]>(apiPath`/api/quizzes/${quizId}/attempts/students/${studentUserId}`)
}

/** Any student's attempt with the answers. */
export function getQuizAttemptReview(quizId: QuizId, attemptId: QuizAttemptId): Promise<QuizAttemptDetail> {
  return apiClient<QuizAttemptDetail>(apiPath`/api/quizzes/${quizId}/attempts/${attemptId}/review`)
}

/** The correct answers of every question. */
export function getQuizAnswerKey(quizId: QuizId): Promise<QuizAnswerKey> {
  return apiClient<QuizAnswerKey>(apiPath`/api/quizzes/${quizId}/answer-key`)
}
