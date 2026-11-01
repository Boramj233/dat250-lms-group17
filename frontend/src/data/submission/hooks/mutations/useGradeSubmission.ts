// Saves a teacher's grade and feedback without publishing them to students.
// A group submission has one shared grade for all submitting members.

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { gradeSubmission } from "../../api/submissionMutations";
import { invalidateSubmissionData } from "../../cache";

export function useGradeSubmission() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: gradeSubmission,
    onSuccess: (submission) => invalidateSubmissionData(queryClient, submission),
  });
}
