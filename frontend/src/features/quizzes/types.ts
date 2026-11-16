import type { ContentId } from "../../shared/types";

// A quiz is a content item. Attempt IDs are local to this domain.
export type QuizId = ContentId;
export type QuizAttemptId = string | number;

// Taking a quiz as the backend supports it (QuizAttemptController): all answers are
// sent at once and graded straight away.

// Matches the backend's QuizAttemptResponse.
export interface QuizAttemptScore {
  id: QuizAttemptId;
  quizId: QuizId;
  /** ISO 8601 instant. */
  submittedAt: string;
  correctCount: number;
  totalQuestions: number;
  /**
   * Proposed: whether the attempt reached the quiz's requiredCorrectAnswers. Null when
   * the quiz has no pass requirement; missing until the backend sends it.
   */
  passed?: boolean | null;
}

// Matches the backend's SubmitQuizRequest; an unanswered question has no option ids.
export interface SubmitQuizAnswersInput {
  quizId: QuizId;
  answers: { questionId: string | number; selectedOptionIds: (string | number)[] }[];
}
