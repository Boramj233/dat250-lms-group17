import { useMutation, useQueryClient } from "@tanstack/react-query";
import { updateCourseRole } from "../../api/courseMutations";
import { courseKeys } from "../../queryKeys";

export function useUpdateCourseRole() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateCourseRole,
    onSuccess: async (_data, { courseId, userId }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: courseKeys.enrollments(courseId) }),
        queryClient.invalidateQueries({ queryKey: courseKeys.myCourses(userId) }),
      ]);
    },
  });
}
