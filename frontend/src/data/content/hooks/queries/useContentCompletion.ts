// Fetches and caches a specific user's completion status for a content item.

import { useQuery } from "@tanstack/react-query";
import type { ContentId, UserId } from "../../../shared/types";
import { getContentCompletion } from "../../api/contentQueries";
import { contentKeys } from "../../queryKeys";

export function useContentCompletion(contentId: ContentId, userId: UserId) {
  return useQuery({
    queryKey: contentKeys.completion(contentId, userId),
    queryFn: () => getContentCompletion(contentId, userId),
  });
}
