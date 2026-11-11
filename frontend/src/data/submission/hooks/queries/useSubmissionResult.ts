// Fetches grading status, released feedback, and permitted coding test results.

import { useQuery } from "@tanstack/react-query";
import type { UserId } from "../../../shared/types";
import { getSubmissionResult } from "../../api/submissionQueries";
import { submissionKeys } from "../../queryKeys";
import type { SubmissionId } from "../../types";

/** Does nothing while `submissionId` is undefined (e.g. nothing submitted yet). */
export function useSubmissionResult(submissionId: SubmissionId | undefined, viewerId: UserId) {
  return useQuery({
    queryKey: submissionKeys.result(submissionId ?? "", viewerId),
    queryFn: () => getSubmissionResult(submissionId!),
    enabled: submissionId !== undefined,
  });
}
