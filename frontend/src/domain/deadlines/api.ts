import { apiClient, apiPath } from "@shared/api/apiClient";
import type { UserId } from "@shared/types";
import type { UpcomingDeadline } from "./types";

// API calls for deadlines collected across all of a user's courses.
export function getUpcomingDeadlines(userId: UserId): Promise<UpcomingDeadline[]> {
  return apiClient<UpcomingDeadline[]>(apiPath`/api/users/${userId}/upcoming-deadlines`);
}
