// Deletes content, clears its cached data, and refreshes the module list and progress.
//only for teachers, not for students

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { moduleKeys } from "../../../module/queryKeys";
import { quizKeys } from "../../../quizzes/queryKeys";
import { deleteContent } from "../../api/contentMutations";
import { contentKeys } from "../../queryKeys";

export function useDeleteContent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deleteContent,
    onSuccess: async (_data, { contentId, moduleId }) => {
      queryClient.removeQueries({ queryKey: contentKeys.content(contentId) });
      queryClient.removeQueries({ queryKey: quizKeys.quiz(contentId) });
      queryClient.removeQueries({ queryKey: quizKeys.definition(contentId) });
      queryClient.removeQueries({ queryKey: quizKeys.quizAttempts(contentId) });
      queryClient.removeQueries({ queryKey: contentKeys.completions(contentId) });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: contentKeys.moduleContent(moduleId) }),
        queryClient.invalidateQueries({ queryKey: moduleKeys.progress }),
      ]);
    },
  });
}
