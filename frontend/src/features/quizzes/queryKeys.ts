import type { UserId } from "../../shared/types";
import type { QuizAttemptId, QuizId } from "./types";

export const quizKeys = {
  all: ["quizzes"] as const,
  quiz: (quizId: QuizId) => ["quizzes", "detail", String(quizId)] as const,
  definition: (quizId: QuizId) => ["quizzes", "definition", String(quizId)] as const,
  quizAttempts: (quizId: QuizId) => ["quizzes", "attempts", String(quizId)] as const,
  attempts: (quizId: QuizId, userId: UserId) =>
    ["quizzes", "attempts", String(quizId), String(userId)] as const,
  attempt: (quizId: QuizId, userId: UserId, attemptId: QuizAttemptId) =>
    ["quizzes", "attempts", String(quizId), String(userId), String(attemptId)] as const,
};
