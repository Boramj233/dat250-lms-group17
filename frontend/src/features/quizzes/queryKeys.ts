import type { QuizId } from "./types";

export const quizKeys = {
  quizAttempts: (quizId: QuizId) => ["quizzes", "attempts", String(quizId)] as const,
  myAttempts: (quizId: QuizId) => ["quizzes", "attempts", String(quizId), "me"] as const,
};
