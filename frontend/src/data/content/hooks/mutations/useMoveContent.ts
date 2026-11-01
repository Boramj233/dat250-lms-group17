// Moves or reorders content and refreshes its details, module lists, and user progress.
//only for teachers, not for students

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { moduleKeys } from "../../../module/queryKeys";
import { moveContent } from "../../api/contentMutations";
import { contentKeys } from "../../queryKeys";

export function useMoveContent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: moveContent,
    onSuccess: async (_content, { contentId, sourceModuleId, targetModuleId }) => {
      const affectedKeys = [
        contentKeys.content(contentId),
        contentKeys.moduleContent(sourceModuleId),
      ];
      if (String(sourceModuleId) !== String(targetModuleId)) {
        affectedKeys.push(contentKeys.moduleContent(targetModuleId));
      }
      await Promise.all([
        ...affectedKeys.map((queryKey) => queryClient.invalidateQueries({ queryKey })),
        queryClient.invalidateQueries({ queryKey: moduleKeys.progress }),
      ]);
    },
  });
}

