// Returns all courses regardless of enrollment.
import { useQuery } from "@tanstack/react-query";
import { getAllCourses } from "../../api/courseQueries";
import { courseKeys } from "../../queryKeys";

export function useAllCourses() {
  return useQuery({
    queryKey: courseKeys.allCourses,
    queryFn: getAllCourses,
  });
}
