// Publishes a submission's grade and feedback and refreshes cached results and progress.
// Backend restricts this operation to course teachers.

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { publishSubmissionGrade } from "../../api/submissionMutations";
import { invalidateSubmissionData } from "../../cache";

export function usePublishSubmissionGrade() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: publishSubmissionGrade,
    onSuccess: (submission) => invalidateSubmissionData(queryClient, submission),
  });
}
