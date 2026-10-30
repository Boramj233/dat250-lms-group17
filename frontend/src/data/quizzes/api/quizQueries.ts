import { apiClient } from "../../shared/apiClient";
import type { UserId } from "../../shared/types";
import type { Quiz, QuizAttempt, QuizAttemptId, QuizAttemptSummary, QuizDefinition, QuizId } from "../types";

// Proposed endpoints until the backend contract is agreed on.
export function getQuiz(quizId: QuizId): Promise<Quiz> {
  return apiClient<Quiz>(`/api/quizzes/${encodeURIComponent(String(quizId))}`);
}

// Teacher-only endpoint; the student endpoint must never include answer keys.
export function getQuizDefinition(quizId: QuizId): Promise<QuizDefinition> {
  return apiClient<QuizDefinition>(
    `/api/quizzes/${encodeURIComponent(String(quizId))}/definition`
  );
}

export function getQuizAttempts(
  quizId: QuizId,
  userId: UserId
): Promise<QuizAttemptSummary[]> {
  return apiClient<QuizAttemptSummary[]>(
    `/api/users/${encodeURIComponent(String(userId))}/quizzes/${encodeURIComponent(String(quizId))}/attempts`
  );
}

export function getQuizAttempt(
  quizId: QuizId,
  attemptId: QuizAttemptId
): Promise<QuizAttempt> {
  return apiClient<QuizAttempt>(
    `/api/quizzes/${encodeURIComponent(String(quizId))}/attempts/${encodeURIComponent(String(attemptId))}`
  );
}
