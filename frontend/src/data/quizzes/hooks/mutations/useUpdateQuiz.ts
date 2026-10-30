// Updates quiz questions or settings and refreshes quiz and content data.
// For course teachers; permissions are enforced by the backend.

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { contentKeys } from "../../../content/queryKeys";
import { updateQuiz } from "../../api/quizMutations";
import { quizKeys } from "../../queryKeys";

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
