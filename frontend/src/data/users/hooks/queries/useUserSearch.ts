// Accounts matching a search text, for adding members to a course. Waits until the
// text has at least two characters.

import { useQuery } from "@tanstack/react-query";
import { searchUsers } from "../../api/userQueries";

export function useUserSearch(search: string) {
  const text = search.trim();
  return useQuery({
    queryKey: ["users", "search", text] as const,
    queryFn: () => searchUsers(text),
    enabled: text.length >= 2,
    staleTime: 30_000,
  });
}
