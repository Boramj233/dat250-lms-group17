// Fetches all submissions for an assignment or coding exercise for the teacher's overview.
// Backend must restrict access to teachers of the course.

import { useQuery } from "@tanstack/react-query";
import type { ContentId, UserId } from "../../../shared/types";
import { getAssignmentSubmissions } from "../../api/submissionQueries";
import { submissionKeys } from "../../queryKeys";

export function useAssignmentSubmissions(contentId: ContentId, viewerId: UserId) {
  return useQuery({
    queryKey: submissionKeys.assignmentSubmissions(contentId, viewerId),
    queryFn: () => getAssignmentSubmissions(contentId),
  });
}
