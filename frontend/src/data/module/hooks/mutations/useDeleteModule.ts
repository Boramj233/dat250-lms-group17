// Deletes a module and its content and refreshes related lists and user progress.
//for teachers, not for students

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { contentKeys } from "../../../content/queryKeys";
import { deleteModule } from "../../api/moduleMutations";
import { moduleKeys } from "../../queryKeys";

// Deletes a module and its content, clears its cached content list, and marks related data as outdated.
export function useDeleteModule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deleteModule,
    onSuccess: async (_data, { courseId, moduleId }) => {
      queryClient.removeQueries({ queryKey: contentKeys.moduleContent(moduleId) });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: moduleKeys.courseModules(courseId) }),
        queryClient.invalidateQueries({ queryKey: moduleKeys.courseProgress(courseId) }),
        // A module deletion also deletes its content. Refresh cached details and
        // progress, including content not currently present in the module list.
        queryClient.invalidateQueries({ queryKey: contentKeys.all }),
      ]);
    },
  });
}
