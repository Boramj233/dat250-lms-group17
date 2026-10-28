// Creates a module and marks the course's module list and user progress as outdated.
//for teachers, not for students

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createModule } from "../../api/moduleMutations";
import { moduleKeys } from "../../queryKeys";

// Creates a module in a course and marks the cached module list as outdated.
export function useCreateModule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createModule,
    onSuccess: async (_module, { courseId }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: moduleKeys.courseModules(courseId) }),
        queryClient.invalidateQueries({ queryKey: moduleKeys.courseProgress(courseId) }),
      ]);
    },
  });
}
