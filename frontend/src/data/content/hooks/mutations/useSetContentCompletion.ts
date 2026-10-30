// Records content completion and refreshes the user's module progress and access.
//for studentinteraction

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { moduleKeys } from "../../../module/queryKeys";
import { setContentCompletion } from "../../api/contentMutations";
import { contentKeys } from "../../queryKeys";

export function useSetContentCompletion() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: setContentCompletion,
    onSuccess: async (_completion, { courseId, contentId, userId }) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: contentKeys.completion(contentId, userId),
        }),
        queryClient.invalidateQueries({
          queryKey: moduleKeys.userModuleProgress(courseId, userId),
        }),
      ]);
    },
  });
}
