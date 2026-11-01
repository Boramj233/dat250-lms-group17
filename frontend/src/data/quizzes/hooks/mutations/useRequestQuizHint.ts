// Requests a hint for a saved answer and refreshes attempts to reflect hint usage.

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { requestQuizHint } from "../../api/quizMutations";
import { quizKeys } from "../../queryKeys";

export function useRequestQuizHint() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: requestQuizHint,
    onSuccess: (_hint, { quizId, userId }) =>
      queryClient.invalidateQueries({ queryKey: quizKeys.attempts(quizId, userId) }),
  });
}
