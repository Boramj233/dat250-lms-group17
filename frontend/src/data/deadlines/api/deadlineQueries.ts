// API calls for deadlines collected across all of a user's courses.

import { apiClient } from "../../shared/apiClient";
import type { UserId } from "../../shared/types";
import type { UpcomingDeadline } from "../types";

export function getUpcomingDeadlines(userId: UserId): Promise<UpcomingDeadline[]> {
  return apiClient<UpcomingDeadline[]>(
    `/api/users/${encodeURIComponent(String(userId))}/upcoming-deadlines`
  );
}
