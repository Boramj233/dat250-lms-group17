// Starts a quiz attempt and refreshes the user's attempt history.

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { startQuizAttempt } from "../../api/quizMutations";
import { quizKeys } from "../../queryKeys";

export function useStartQuizAttempt() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: startQuizAttempt,
    onSuccess: (_attempt, { quizId, userId }) =>
      queryClient.invalidateQueries({ queryKey: quizKeys.attempts(quizId, userId) }),
  });
}
