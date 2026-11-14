import { useQuery } from "@tanstack/react-query";
import { getMyGrades } from "./api";
import { gradeKeys } from "./queryKeys";

// The logged-in user's graded and awaiting-grade submissions, newest first. The course
// page filters this list by course, so both pages share one request and cache entry.
export function useMyGrades() {
  return useQuery({
    queryKey: gradeKeys.mine,
    queryFn: getMyGrades,
    select: entries =>
      [...entries].sort((a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt)),
  });
}
