import type { QuizAttemptScore } from './types'

// The backend's rule (QuizAttemptRepository.countPassing): an attempt passes when
// correctCount * 100 >= passingPercentage * totalQuestions. Every quiz has a passing
// percentage (default 60). The backend does not send `passed` for an attempt.

/**
 * Whether an attempt passed the quiz's passing percentage. Null while the percentage
 * is not known (the quiz's details are loading).
 */
export function attemptPassed(attempt: QuizAttemptScore, passingPercentage: number | undefined): boolean | null {
  if (passingPercentage === undefined) return null
  return attempt.correctCount * 100 >= passingPercentage * attempt.totalQuestions
}

/**
 * The quiz is passed once any attempt passed. Null without attempts or while the
 * percentage is not known.
 */
export function quizPassed(attempts: QuizAttemptScore[], passingPercentage: number | undefined): boolean | null {
  const results = attempts.map(attempt => attemptPassed(attempt, passingPercentage))
  if (results.includes(true)) return true
  return results.includes(false) ? false : null
}

/** The fewest correct answers that pass, out of `totalQuestions`. */
export function correctAnswersToPass(passingPercentage: number, totalQuestions: number): number {
  return Math.ceil((passingPercentage * totalQuestions) / 100)
}

/** "60 % (3 of 5 correct)". */
export function passRequirementText(passingPercentage: number, totalQuestions: number): string {
  return `${passingPercentage} % (${correctAnswersToPass(passingPercentage, totalQuestions)} of ${totalQuestions} correct)`
}

/** The attempt with the most correct answers (the earliest in the list on a tie); undefined without attempts. */
export function bestAttempt<T extends QuizAttemptScore>(attempts: T[]): T | undefined {
  return attempts.reduce<T | undefined>(
    (top, attempt) => (!top || attempt.correctCount > top.correctCount ? attempt : top),
    undefined,
  )
}
