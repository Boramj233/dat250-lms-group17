import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { UserId } from "../../shared/types";
import { contentKeys } from "../content/queryKeys";
import { moduleKeys } from "../modules/queryKeys";
import { getQuiz, getQuizAttempt, getQuizAttempts, getQuizDefinition, requestQuizHint, saveQuizAnswer, startQuizAttempt, submitQuizAttempt, updateQuiz } from "./api";
import { quizKeys } from "./queryKeys";
import type { QuizAttemptId, QuizId } from "./types";

// Fetches and caches a quiz's settings and student-facing questions.
export function useQuiz(quizId: QuizId) {
  return useQuery({
    queryKey: quizKeys.quiz(quizId),
    queryFn: () => getQuiz(quizId),
  });
}

// Fetches an attempt's questions, saved answers, status, and released result.
export function useQuizAttempt(quizId: QuizId, attemptId: QuizAttemptId, userId: UserId) {
  return useQuery({
    queryKey: quizKeys.attempt(quizId, userId, attemptId),
    queryFn: () => getQuizAttempt(quizId, attemptId),
  });
}

// Fetches and caches a specific user's attempt history for a quiz.
export function useQuizAttempts(quizId: QuizId, userId: UserId) {
  return useQuery({
    queryKey: quizKeys.attempts(quizId, userId),
    queryFn: () => getQuizAttempts(quizId, userId),
  });
}

// Fetches teacher-only quiz settings, questions, correct answers, and hints for editing.
// Backend must restrict this endpoint to teachers of the course.
export function useQuizDefinition(quizId: QuizId) {
  return useQuery({
    queryKey: quizKeys.definition(quizId),
    queryFn: () => getQuizDefinition(quizId),
  });
}

// Requests a hint for a saved answer and refreshes attempts to reflect hint usage.
export function useRequestQuizHint() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: requestQuizHint,
    onSuccess: (_hint, { quizId, userId }) =>
      queryClient.invalidateQueries({ queryKey: quizKeys.attempts(quizId, userId) }),
  });
}

// Saves or replaces one answer and refreshes the user's cached quiz attempts.
export function useSaveQuizAnswer() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: saveQuizAnswer,
    onSuccess: (_attempt, { quizId, userId }) =>
      queryClient.invalidateQueries({ queryKey: quizKeys.attempts(quizId, userId) }),
  });
}

// Starts a quiz attempt and refreshes the user's attempt history.
export function useStartQuizAttempt() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: startQuizAttempt,
    onSuccess: (_attempt, { quizId, userId }) =>
      queryClient.invalidateQueries({ queryKey: quizKeys.attempts(quizId, userId) }),
  });
}

// Submits saved answers and refreshes attempts, content completion, and module access.
export function useSubmitQuizAttempt() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: submitQuizAttempt,
    onSuccess: async (_attempt, { quizId, userId, courseId }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: quizKeys.attempts(quizId, userId) }),
        queryClient.invalidateQueries({ queryKey: contentKeys.completion(quizId, userId) }),
        queryClient.invalidateQueries({ queryKey: moduleKeys.userModuleProgress(courseId, userId) }),
      ]);
    },
  });
}

// Updates quiz questions or settings and refreshes quiz and content data.
// For course teachers; permissions are enforced by the backend.
export function useUpdateQuiz() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateQuiz,
    onSuccess: async (_quiz, { quizId, moduleId }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: quizKeys.quiz(quizId) }),
        queryClient.invalidateQueries({ queryKey: quizKeys.definition(quizId) }),
        queryClient.invalidateQueries({ queryKey: contentKeys.content(quizId) }),
        queryClient.invalidateQueries({ queryKey: contentKeys.moduleContent(moduleId) }),
      ]);
    },
  });
}
