import type { QuizAttemptId, QuizId } from "./types";

export const quizKeys = {
  quizAttempts: (quizId: QuizId) => ["quizzes", "attempts", String(quizId)] as const,
  myAttempts: (quizId: QuizId) => ["quizzes", "attempts", String(quizId), "me"] as const,
  myAttempt: (quizId: QuizId, attemptId: QuizAttemptId) =>
    ["quizzes", "attempts", String(quizId), "me", String(attemptId)] as const,
};
