// Fetches a submission's files or repository and the result visible to the current viewer.

import { useQuery } from "@tanstack/react-query";
import type { UserId } from "../../../shared/types";
import { getSubmission } from "../../api/submissionQueries";
import { submissionKeys } from "../../queryKeys";
import type { SubmissionId } from "../../types";

export function useSubmission(submissionId: SubmissionId, viewerId: UserId) {
  return useQuery({
    queryKey: submissionKeys.submission(submissionId, viewerId),
    queryFn: () => getSubmission(submissionId),
  });
}
