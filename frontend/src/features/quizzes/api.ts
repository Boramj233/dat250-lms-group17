import { apiClient, apiPath, jsonBody } from "../../shared/api/apiClient";
import type { UserId } from "../../shared/types";
import type { Quiz, QuizAttempt, QuizAttemptId, QuizAttemptSummary, QuizDefinition, QuizHint, QuizId, RequestQuizHintInput, SaveQuizAnswerInput, StartQuizAttemptInput, SubmitQuizAttemptInput, UpdateQuizInput } from "./types";

// Proposed endpoints until the backend contract is agreed on.
export function getQuiz(quizId: QuizId): Promise<Quiz> {
  return apiClient<Quiz>(apiPath`/api/quizzes/${quizId}`);
}

// Teacher-only endpoint; the student endpoint must never include answer keys.
export function getQuizDefinition(quizId: QuizId): Promise<QuizDefinition> {
  return apiClient<QuizDefinition>(apiPath`/api/quizzes/${quizId}/definition`);
}

export function getQuizAttempts(
  quizId: QuizId,
  userId: UserId
): Promise<QuizAttemptSummary[]> {
  return apiClient<QuizAttemptSummary[]>(apiPath`/api/users/${userId}/quizzes/${quizId}/attempts`);
}

export function getQuizAttempt(
  quizId: QuizId,
  attemptId: QuizAttemptId
): Promise<QuizAttempt> {
  return apiClient<QuizAttempt>(apiPath`/api/quizzes/${quizId}/attempts/${attemptId}`);
}

export function updateQuiz({ quizId, changes }: UpdateQuizInput): Promise<QuizDefinition> {
  return apiClient<QuizDefinition>(apiPath`/api/quizzes/${quizId}`, jsonBody("PATCH", changes));
}

// Proposed endpoints. Backend enforces ownership, access, deadlines and attempt limits.
// userId and courseId scope frontend caches and are never proof of authorization.
export function startQuizAttempt({ quizId }: StartQuizAttemptInput): Promise<QuizAttempt> {
  return apiClient<QuizAttempt>(apiPath`/api/quizzes/${quizId}/attempts`, { method: "POST" });
}

// PUT replaces one saved answer. The backend must reject writes to closed attempts.
export function saveQuizAnswer({
  quizId,
  attemptId,
  answer,
}: SaveQuizAnswerInput): Promise<QuizAttempt> {
  return apiClient<QuizAttempt>(
    apiPath`/api/quizzes/${quizId}/attempts/${attemptId}/answers/${answer.questionId}`,
    jsonBody("PUT", answer)
  );
}

// Submit after pending answer saves have completed. Backend grades and records
// the interaction, then recalculates module progress before confirming submission.
export function submitQuizAttempt({
  quizId,
  attemptId,
}: SubmitQuizAttemptInput): Promise<QuizAttempt> {
  return apiClient<QuizAttempt>(
    apiPath`/api/quizzes/${quizId}/attempts/${attemptId}/submit`,
    { method: "POST" }
  );
}

// Backend decides whether a hint is allowed based on the saved answer and quiz rules.
export function requestQuizHint({
  quizId,
  attemptId,
  questionId,
}: RequestQuizHintInput): Promise<QuizHint> {
  return apiClient<QuizHint>(
    apiPath`/api/quizzes/${quizId}/attempts/${attemptId}/questions/${questionId}/hint`,
    { method: "POST" }
  );
}
