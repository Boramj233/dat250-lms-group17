// Saves or replaces one answer and refreshes the user's cached quiz attempts.

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { saveQuizAnswer } from "../../api/quizMutations";
import { quizKeys } from "../../queryKeys";

export function useSaveQuizAnswer() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: saveQuizAnswer,
    onSuccess: (_attempt, { quizId, userId }) =>
      queryClient.invalidateQueries({ queryKey: quizKeys.attempts(quizId, userId) }),
  });
}
