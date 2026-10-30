// Enrolls a user in a course and marks its enrollments and the user's course list as outdated.

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { enrollInCourse } from "../../api/courseMutations";
import { courseKeys } from "../../queryKeys";

export function useEnrollInCourse() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: enrollInCourse,
    onSuccess: async (_data, { courseId, userId }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: courseKeys.enrollments(courseId) }),
        queryClient.invalidateQueries({ queryKey: courseKeys.myCourses(userId) }),
      ]);
    },
  });
}
