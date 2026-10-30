// Fetches grading status, released feedback, and permitted coding test results.

import { useQuery } from "@tanstack/react-query";
import type { UserId } from "../../../shared/types";
import { getSubmissionResult } from "../../api/submissionQueries";
import { submissionKeys } from "../../queryKeys";
import type { SubmissionId } from "../../types";

export function useSubmissionResult(submissionId: SubmissionId, viewerId: UserId) {
  return useQuery({
    queryKey: submissionKeys.result(submissionId, viewerId),
    queryFn: () => getSubmissionResult(submissionId),
  });
}
