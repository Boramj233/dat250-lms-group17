// Fetches and caches the upcoming deadlines in all courses the user is enrolled in.

import { useQuery } from "@tanstack/react-query";
import { getUpcomingDeadlines } from "../../api/deadlineQueries";
import { deadlineKeys } from "../../queryKeys";
import type { UserId } from "../../../shared/types";

export function useUpcomingDeadlines(userId: UserId) {
  return useQuery({
    queryKey: deadlineKeys.upcoming(userId),
    queryFn: () => getUpcomingDeadlines(userId),
  });
}
