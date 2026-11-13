// Teachers: all students' latest submissions for an assignment or coding exercise.

import { useQuery } from "@tanstack/react-query";
import type { ContentId } from "../../../shared/types";
import { getContentSubmissions } from "../../api/submissionQueries";
import { submissionKeys } from "../../queryKeys";

export function useContentSubmissions(contentId: ContentId, enabled = true) {
  return useQuery({
    queryKey: submissionKeys.contentSubmissions(contentId),
    queryFn: () => getContentSubmissions(contentId),
    enabled,
  });
}
