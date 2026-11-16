import { apiClient, apiPath, jsonBody } from '@shared/api/apiClient'
import type { QuizAttemptDetail, QuizAttemptId, QuizId, SubmitQuizAnswersInput, QuizAttemptScore } from './types'

/** Hands in all answers of a quiz and returns the score (backend). */
export function submitQuizAnswers({ quizId, answers }: SubmitQuizAnswersInput): Promise<QuizAttemptScore> {
  return apiClient<QuizAttemptScore>(apiPath`/api/quizzes/${quizId}/attempts`, jsonBody('POST', { answers }))
}

/** The logged-in user's attempts at a quiz (proposed, see content/README.md). */
export function getMyQuizAttempts(quizId: QuizId): Promise<QuizAttemptScore[]> {
  return apiClient<QuizAttemptScore[]>(apiPath`/api/quizzes/${quizId}/attempts/me`)
}

/** Proposed: one of the user's own attempts with the answers and which were correct. */
export function getMyQuizAttempt(quizId: QuizId, attemptId: QuizAttemptId): Promise<QuizAttemptDetail> {
  return apiClient<QuizAttemptDetail>(apiPath`/api/quizzes/${quizId}/attempts/${attemptId}`)
}
