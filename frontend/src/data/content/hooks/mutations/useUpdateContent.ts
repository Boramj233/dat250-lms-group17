// Updates content and refreshes its details, module content list, and user progress.
//only for teachers, not for students

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { moduleKeys } from "../../../module/queryKeys";
import { updateContent } from "../../api/contentMutations";
import { contentKeys } from "../../queryKeys";

export function useUpdateContent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateContent,
    onSuccess: async (_content, { contentId, moduleId }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: contentKeys.content(contentId) }),
        queryClient.invalidateQueries({ queryKey: contentKeys.moduleContent(moduleId) }),
        queryClient.invalidateQueries({ queryKey: moduleKeys.progress }),
      ]);
    },
  });
}
