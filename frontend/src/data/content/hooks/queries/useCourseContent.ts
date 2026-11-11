// The content of several modules of a course at once, e.g. "all assignments in the
// course". Shares the cache with useModuleContent (same query keys).

import { useQueries } from "@tanstack/react-query";
import type { CourseId, ModuleId } from "../../../shared/types";
import { getModuleContent } from "../../api/contentQueries";
import { contentKeys } from "../../queryKeys";
import type { ContentSummary } from "../../types";

export function useCourseContent(courseId: CourseId, moduleIds: ModuleId[]) {
  return useQueries({
    queries: moduleIds.map(moduleId => ({
      queryKey: contentKeys.moduleContent(moduleId),
      queryFn: () => getModuleContent(courseId, moduleId),
    })),
    combine: results => ({
      /** Content per module id (as string); missing while that module is loading. */
      byModule: new Map<string, ContentSummary[]>(
        results.flatMap((result, index) =>
          result.data ? [[String(moduleIds[index]), result.data] as const] : [])
      ),
      isPending: results.some(result => result.isPending),
      isError: results.some(result => result.isError),
    }),
  });
}
