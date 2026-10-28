// Creates text content and refreshes the module's content list and user progress.
//only for teachers, not for students

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { moduleKeys } from "../../../module/queryKeys";
import { createContent } from "../../api/contentMutations";
import { contentKeys } from "../../queryKeys";

export function useCreateContent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createContent,
    onSuccess: async (_content, { moduleId }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: contentKeys.moduleContent(moduleId) }),
        // Parent course IDs are not part of this mutation's input.
        queryClient.invalidateQueries({ queryKey: moduleKeys.progress }),
      ]);
    },
  });
}

