// Fetches and caches the full details of a content item by its ID.

import { useQuery } from "@tanstack/react-query";
import type { ContentId } from "../../../shared/types";
import { getContent } from "../../api/contentQueries";
import { contentKeys } from "../../queryKeys";

export function useContent(contentId: ContentId) {
  return useQuery({
    queryKey: contentKeys.content(contentId),
    queryFn: () => getContent(contentId),
  });
}

