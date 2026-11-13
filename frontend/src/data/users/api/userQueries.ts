import { apiClient } from "../../shared/apiClient";
import type { UserLookupPage } from "../types";

/** Finds accounts by name or email (backend: COURSE_MANAGER and ADMIN only). */
export function searchUsers(search: string, size = 10): Promise<UserLookupPage> {
  const query = new URLSearchParams({ search, size: String(size) });
  return apiClient<UserLookupPage>(`/api/users/search?${query}`);
}
