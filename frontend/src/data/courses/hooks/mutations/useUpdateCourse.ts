import { useMutation, useQueryClient } from "@tanstack/react-query";
import { updateCourse } from "../../api/courseMutations";
import { courseKeys } from "../../queryKeys";

export function useUpdateCourse() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateCourse,
    onSuccess: async (_course, { courseId }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: courseKeys.course(courseId) }),
        queryClient.invalidateQueries({ queryKey: courseKeys.allCourses }),
        queryClient.invalidateQueries({ queryKey: courseKeys.enrolledCourses }),
      ]);
    },
  });
}
