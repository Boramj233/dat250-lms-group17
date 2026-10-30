// Fetches an attempt's questions, saved answers, status, and released result.

import { useQuery } from "@tanstack/react-query";
import type { UserId } from "../../../shared/types";
import { getQuizAttempt } from "../../api/quizQueries";
import { quizKeys } from "../../queryKeys";
import type { QuizAttemptId, QuizId } from "../../types";

export function useQuizAttempt(quizId: QuizId, attemptId: QuizAttemptId, userId: UserId) {
  return useQuery({
    queryKey: quizKeys.attempt(quizId, userId, attemptId),
    queryFn: () => getQuizAttempt(quizId, attemptId),
  });
}
