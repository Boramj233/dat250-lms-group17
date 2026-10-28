import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createCourse } from "../../api/courseMutations";
import { courseKeys } from "../../queryKeys";

export function useCreateCourse() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createCourse,
    // Creating a course may also enroll its creator as a teacher.
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: courseKeys.all }),
  });
}
