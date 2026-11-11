// Fetches and caches the content summaries of a module, sorted by position.

import { useQuery } from "@tanstack/react-query";
import type { CourseId, ModuleId } from "../../../shared/types";
import { getModuleContent } from "../../api/contentQueries";
import { contentKeys } from "../../queryKeys";

/** Does nothing while `moduleId` is undefined (e.g. no module selected). */
export function useModuleContent(courseId: CourseId, moduleId: ModuleId | undefined) {
  return useQuery({
    queryKey: contentKeys.moduleContent(moduleId ?? ""),
    queryFn: () => getModuleContent(courseId, moduleId!),
    enabled: moduleId !== undefined,
  });
}
