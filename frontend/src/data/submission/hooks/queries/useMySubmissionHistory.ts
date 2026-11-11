// The logged-in user's submissions for an assignment or coding exercise, newest first.

import { useQuery } from "@tanstack/react-query";
import type { ContentId } from "../../../shared/types";
import { getMySubmissionHistory } from "../../api/submissionQueries";
import { submissionKeys } from "../../queryKeys";

export function useMySubmissionHistory(contentId: ContentId) {
  return useQuery({
    queryKey: submissionKeys.myHistory(contentId),
    queryFn: () => getMySubmissionHistory(contentId),
    select: submissions =>
      [...submissions].sort((a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt)),
  });
}
