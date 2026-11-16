import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useQueriesById } from '@shared/api/useQueriesById'
import { getMyQuizAttempt, getMyQuizAttempts, submitQuizAnswers } from './api'
import { quizKeys } from './queryKeys'
import type { QuizAttemptId, QuizAttemptScore, QuizId } from './types'

// The logged-in user's attempts at a quiz, newest first (proposed endpoint).
export function useMyQuizAttempts(quizId: QuizId) {
  return useQuery({
    queryKey: quizKeys.myAttempts(quizId),
    queryFn: () => getMyQuizAttempts(quizId),
    select: attempts => [...attempts].sort((a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt)),
  })
}

// Hands in a quiz (backend) and refreshes the user's attempts, so the count and best
// score update at once.
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

// One of the user's attempts with the answers (proposed endpoint). An attempt never
// changes, so it is fetched once.
export function useMyQuizAttempt(quizId: QuizId, attemptId: QuizAttemptId) {
  return useQuery({
    queryKey: quizKeys.myAttempt(quizId, attemptId),
    queryFn: () => getMyQuizAttempt(quizId, attemptId),
    staleTime: Infinity,
  })
}
