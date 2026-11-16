import type { QuizAttemptScore } from "./types";

/**
 * Whether an attempt passed: the backend's `passed` when it sends it, otherwise
 * worked out from the quiz's requiredCorrectAnswers. Null when the quiz has no pass
 * requirement.
 */
export function attemptPassed(
  attempt: QuizAttemptScore,
  requiredCorrectAnswers: number | null | undefined,
): boolean | null {
  if (attempt.passed !== undefined) return attempt.passed;
  if (requiredCorrectAnswers === null || requiredCorrectAnswers === undefined) return null;
  return attempt.correctCount >= requiredCorrectAnswers;
}

/**
 * The quiz is passed once any attempt passed. Null without a pass requirement or
 * attempts.
 */
export function quizPassed(
  attempts: QuizAttemptScore[],
  requiredCorrectAnswers: number | null | undefined,
): boolean | null {
  const results = attempts.map(attempt => attemptPassed(attempt, requiredCorrectAnswers));
  if (results.includes(true)) return true;
  return results.includes(false) ? false : null;
}

/** "3 of 5 correct". */
export function passRequirementText(requiredCorrectAnswers: number, totalQuestions: number): string {
  return `${requiredCorrectAnswers} of ${totalQuestions} correct`;
}
