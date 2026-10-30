// Fetches teacher-only quiz settings, questions, correct answers, and hints for editing.
// Backend must restrict this endpoint to teachers of the course.

import { useQuery } from "@tanstack/react-query";
import { getQuizDefinition } from "../../api/quizQueries";
import { quizKeys } from "../../queryKeys";
import type { QuizId } from "../../types";

export function useQuizDefinition(quizId: QuizId) {
  return useQuery({
    queryKey: quizKeys.definition(quizId),
    queryFn: () => getQuizDefinition(quizId),
  });
}
