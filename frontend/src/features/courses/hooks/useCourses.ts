import { useQuery } from "@tanstack/react-query";
import { getMyCourses } from "../api/coursesApi";

export function useCourses() {
  return useQuery({
    queryKey: ["myCourses"],
    queryFn: getMyCourses,
  });
}