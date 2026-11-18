import type { UserId } from '@shared/types'
import type { QuizAttemptId, QuizId } from './types'

export const quizKeys = {
  quizAttempts: (quizId: QuizId) => ['quizzes', 'attempts', String(quizId)] as const,
  myAttempts: (quizId: QuizId) => ['quizzes', 'attempts', String(quizId), 'me'] as const,
  myAttemptCount: (quizId: QuizId) => ['quizzes', 'attempts', String(quizId), 'me', 'count'] as const,
  myAttempt: (quizId: QuizId, attemptId: QuizAttemptId) =>
    ['quizzes', 'attempts', String(quizId), 'me', String(attemptId)] as const,
  studentAttempts: (quizId: QuizId, studentUserId: UserId) =>
    ['quizzes', 'attempts', String(quizId), 'students', String(studentUserId)] as const,
  attemptReview: (quizId: QuizId, attemptId: QuizAttemptId) =>
    ['quizzes', 'attempts', String(quizId), 'review', String(attemptId)] as const,
  answerKey: (quizId: QuizId) => ['quizzes', 'answer-key', String(quizId)] as const,
}
