// Fetches and caches a quiz's settings and student-facing questions.

import { useQuery } from "@tanstack/react-query";
import { getQuiz } from "../../api/quizQueries";
import { quizKeys } from "../../queryKeys";
import type { QuizId } from "../../types";

export function useQuiz(quizId: QuizId) {
  return useQuery({
    queryKey: quizKeys.quiz(quizId),
    queryFn: () => getQuiz(quizId),
  });
}
