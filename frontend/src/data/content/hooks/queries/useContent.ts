// Fetches and caches the full details of a content item. The type comes from the
// content summary, since the backend has one endpoint per content type.

import { useQuery } from "@tanstack/react-query";
import type { ContentId } from "../../../shared/types";
import { getContent } from "../../api/contentQueries";
import { contentKeys } from "../../queryKeys";
import type { ContentType } from "../../types";

export function useContent(type: ContentType | undefined, contentId: ContentId) {
  return useQuery({
    queryKey: contentKeys.content(contentId),
    queryFn: () => getContent(type!, contentId),
    // Wait until the type is known (from the module's content list).
    enabled: type !== undefined,
  });
}
