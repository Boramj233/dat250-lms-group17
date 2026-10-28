// Updates module details or prerequisites and refreshes the module list and progress.
//for teachers, not for students

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { updateModule } from "../../api/moduleMutations";
import { moduleKeys } from "../../queryKeys";

export function useUpdateModule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateModule,
    onSuccess: async (_module, { courseId }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: moduleKeys.courseModules(courseId) }),
        queryClient.invalidateQueries({ queryKey: moduleKeys.courseProgress(courseId) }),
      ]);
    },
  });
}
