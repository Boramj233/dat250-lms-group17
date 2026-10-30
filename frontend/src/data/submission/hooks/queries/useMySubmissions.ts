// Fetches a user's individual and group submissions for an assignment or coding exercise.

import { useQuery } from "@tanstack/react-query";
import type { ContentId, UserId } from "../../../shared/types";
import { getMySubmissions } from "../../api/submissionQueries";
import { submissionKeys } from "../../queryKeys";

export function useMySubmissions(contentId: ContentId, userId: UserId) {
  return useQuery({
    queryKey: submissionKeys.mySubmissions(contentId, userId),
    queryFn: () => getMySubmissions(contentId, userId),
  });
}
