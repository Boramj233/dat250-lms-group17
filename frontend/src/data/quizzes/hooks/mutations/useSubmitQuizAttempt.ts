// Submits saved answers and refreshes attempts, content completion, and module access.

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { contentKeys } from "../../../content/queryKeys";
import { moduleKeys } from "../../../module/queryKeys";
import { submitQuizAttempt } from "../../api/quizMutations";
import { quizKeys } from "../../queryKeys";

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
