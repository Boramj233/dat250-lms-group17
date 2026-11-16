import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { CourseId } from "../../shared/types";
import { createModule, getCourseModules, updateModule } from "./api";
import { moduleKeys } from "./queryKeys";

// Fetches and caches the modules belonging to a specific course.
export function useCourseModules(courseId: CourseId) {
  return useQuery({
    queryKey: moduleKeys.courseModules(courseId),
    queryFn: () => getCourseModules(courseId),
  });
}

// Creates a module in a course and marks the cached module list as outdated.
//for teachers, not for students
export function useCreateModule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createModule,
    onSuccess: (_module, { courseId }) =>
      queryClient.invalidateQueries({ queryKey: moduleKeys.courseModules(courseId) }),
  });
}

// Updates module details or prerequisites and refreshes the module list.
//for teachers, not for students
export function useUpdateModule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateModule,
    onSuccess: (_module, { courseId }) =>
      queryClient.invalidateQueries({ queryKey: moduleKeys.courseModules(courseId) }),
  });
}
