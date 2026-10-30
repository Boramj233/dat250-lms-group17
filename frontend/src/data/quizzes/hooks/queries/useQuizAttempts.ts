// Fetches and caches a specific user's attempt history for a quiz.

import { useQuery } from "@tanstack/react-query";
import type { UserId } from "../../../shared/types";
import { getQuizAttempts } from "../../api/quizQueries";
import { quizKeys } from "../../queryKeys";
import type { QuizId } from "../../types";

export function useQuizAttempts(quizId: QuizId, userId: UserId) {
  return useQuery({
    queryKey: quizKeys.attempts(quizId, userId),
    queryFn: () => getQuizAttempts(quizId, userId),
  });
}
