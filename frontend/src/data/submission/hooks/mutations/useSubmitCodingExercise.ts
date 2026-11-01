// Submits a repository and commit reference and refreshes submissions and progress.

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { submitCodingExercise } from "../../api/submissionMutations";
import { invalidateSubmissionData } from "../../cache";

export function useSubmitCodingExercise() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: submitCodingExercise,
    onSuccess: (submission) => invalidateSubmissionData(queryClient, submission),
  });
}
