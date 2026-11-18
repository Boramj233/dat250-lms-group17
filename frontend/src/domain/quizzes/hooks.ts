import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useQueriesById } from '@shared/api/useQueriesById'
import type { UserId } from '@shared/types'
import {
  getMyQuizAttempt,
  getMyQuizAttemptCount,
  getMyQuizAttempts,
  getQuizAnswerKey,
  getQuizAttemptReview,
  getStudentQuizAttempts,
  submitQuizAnswers,
} from './api'
import { quizKeys } from './queryKeys'
import type { QuizAttemptId, QuizAttemptScore, QuizId } from './types'
import { newestFirst } from '@shared/lib/newestFirst'

// The logged-in user's attempts at a quiz, newest first.
export function useMyQuizAttempts(quizId: QuizId, { enabled = true } = {}) {
  return useQuery({
    queryKey: quizKeys.myAttempts(quizId),
    queryFn: () => getMyQuizAttempts(quizId),
    select: newestFirst,
    enabled,
  })
}

// How many attempts the logged-in user has used, as the backend counts them.
export function useMyQuizAttemptCount(quizId: QuizId, { enabled = true } = {}) {
  return useQuery({
    queryKey: quizKeys.myAttemptCount(quizId),
    queryFn: () => getMyQuizAttemptCount(quizId),
    select: count => count.usedAttempts,
    enabled,
  })
}

// Hands in a quiz and refreshes the user's attempts (and their count), so the count and
// best score update at once.
export function useSubmitQuizAnswers() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: submitQuizAnswers,
    onSuccess: attempt => queryClient.invalidateQueries({ queryKey: quizKeys.myAttempts(attempt.quizId) }),
  })
}

// The logged-in user's attempts at several quizzes at once, e.g. for a table of
// quizzes. Shares the cache with useMyQuizAttempts (same query keys).
export function useMyQuizAttemptsFor(quizIds: QuizId[]) {
  const { byId, isPending } = useQueriesById(quizIds, {
    queryKey: quizId => quizKeys.myAttempts(quizId),
    queryFn: (quizId): Promise<QuizAttemptScore[]> => getMyQuizAttempts(quizId),
  })
  /** Attempts per quiz id (as string); missing while loading or if it failed. */
  return { byQuiz: byId, isPending }
}

/**
 * One attempt with the answers. An attempt never changes, so it is fetched once. The
 * user's own (`review` false), or any student's for a teacher of the course (`review` true).
 */
export function useQuizAttemptDetail(quizId: QuizId, attemptId: QuizAttemptId, review = false) {
  return useQuery({
    queryKey: review ? quizKeys.attemptReview(quizId, attemptId) : quizKeys.myAttempt(quizId, attemptId),
    queryFn: () => (review ? getQuizAttemptReview(quizId, attemptId) : getMyQuizAttempt(quizId, attemptId)),
    staleTime: Infinity,
  })
}

// Teachers: one student's attempts at a quiz, newest first. Waits until a student is chosen.
export function useStudentQuizAttempts(quizId: QuizId, studentUserId: UserId | null) {
  return useQuery({
    queryKey: quizKeys.studentAttempts(quizId, studentUserId ?? ''),
    queryFn: () => getStudentQuizAttempts(quizId, studentUserId!),
    select: newestFirst,
    enabled: studentUserId !== null,
  })
}

// Teachers: the quiz's correct answers. Only fetched when `enabled` (e.g. once shown).
export function useQuizAnswerKey(quizId: QuizId, { enabled = true } = {}) {
  return useQuery({
    queryKey: quizKeys.answerKey(quizId),
    queryFn: () => getQuizAnswerKey(quizId),
    enabled,
  })
}
