// Fetches a user's module completion and access status as calculated by the backend.

import { useQuery } from "@tanstack/react-query";
import type { CourseId, UserId } from "../../../shared/types";
import { getUserModuleProgress } from "../../api/moduleQueries";
import { moduleKeys } from "../../queryKeys";

export function useUserModuleProgress(courseId: CourseId, userId: UserId) {
  return useQuery({
    queryKey: moduleKeys.userModuleProgress(courseId, userId),
    queryFn: () => getUserModuleProgress(courseId, userId),
  });
}
