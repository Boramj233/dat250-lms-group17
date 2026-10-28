// Returns all courses the specified user is enrolled in.
import { useQuery } from "@tanstack/react-query";
import { getMyCourses } from "../../api/courseQueries";
import { courseKeys } from "../../queryKeys";
import type { UserId } from "../../types";

export function useMyCourses(userId: UserId) {
  return useQuery({
    queryKey: courseKeys.myCourses(userId),
    queryFn: () => getMyCourses(userId),
  });
}
