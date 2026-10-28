// Removes a user's enrollment and marks the affected enrollment and course lists as outdated.

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { removeCourseEnrollment } from "../../api/courseMutations";
import { courseKeys } from "../../queryKeys";

export function useRemoveCourseEnrollment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: removeCourseEnrollment,
    onSuccess: async (_data, { courseId, userId }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: courseKeys.enrollments(courseId) }),
        queryClient.invalidateQueries({ queryKey: courseKeys.myCourses(userId) }),
      ]);
    },
  });
}
