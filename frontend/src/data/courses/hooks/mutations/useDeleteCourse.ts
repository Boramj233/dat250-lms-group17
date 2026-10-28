import { useMutation, useQueryClient } from "@tanstack/react-query";
import { deleteCourse } from "../../api/courseMutations";
import { courseKeys } from "../../queryKeys";

export function useDeleteCourse() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deleteCourse,
    onSuccess: async (_data, courseId) => {
      queryClient.removeQueries({ queryKey: courseKeys.course(courseId) });
      queryClient.removeQueries({ queryKey: courseKeys.enrollments(courseId) });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: courseKeys.allCourses }),
        queryClient.invalidateQueries({ queryKey: courseKeys.enrolledCourses }),
      ]);
    },
  });
}
