// Returns all courses the specified user is enrolled in.
import { useQuery } from "@tanstack/react-query";
import { getMyCourses } from "../../api/courseQueries";
import { courseKeys } from "../../queryKeys";

export function useMyCourses(userId: string | number) {
  return useQuery({
    queryKey: courseKeys.myCourses(userId),
    queryFn: () => getMyCourses(userId),
  });
}
