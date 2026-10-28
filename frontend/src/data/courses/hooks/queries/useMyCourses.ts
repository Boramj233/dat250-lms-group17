// Fetches and caches a user's enrolled courses, including their role in each course.

import { useQuery } from "@tanstack/react-query";
import { getMyCourses } from "../../api/courseQueries";
import { courseKeys } from "../../queryKeys";
import type { UserId } from "../../../shared/types";

export function useMyCourses(userId: UserId) {
  return useQuery({
    queryKey: courseKeys.myCourses(userId),
    queryFn: () => getMyCourses(userId),
  });
}
