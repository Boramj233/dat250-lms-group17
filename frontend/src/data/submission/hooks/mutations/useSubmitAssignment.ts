// Uploads an individual or group file submission and refreshes submissions and progress.

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { submitAssignment } from "../../api/submissionMutations";
import { invalidateSubmissionData } from "../../cache";

export function useSubmitAssignment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: submitAssignment,
    onSuccess: (submission) => invalidateSubmissionData(queryClient, submission),
  });
}
