// Fetches and caches content summaries for a specific module.

import { useQuery } from "@tanstack/react-query";
import type { ModuleId } from "../../../shared/types";
import { getModuleContent } from "../../api/contentQueries";
import { contentKeys } from "../../queryKeys";

export function useModuleContent(moduleId: ModuleId) {
  return useQuery({
    queryKey: contentKeys.moduleContent(moduleId),
    queryFn: () => getModuleContent(moduleId),
  });
}
