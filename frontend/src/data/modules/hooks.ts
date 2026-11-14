import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { CourseId, UserId } from "../../shared/types";
import { contentKeys } from "../content/queryKeys";
import { createModule, deleteModule, getCourseModules, getUserModuleProgress, updateModule } from "./api";
import { moduleKeys } from "./queryKeys";

// Fetches and caches the modules belonging to a specific course.
export function useCourseModules(courseId: CourseId) {
  return useQuery({
    queryKey: moduleKeys.courseModules(courseId),
    queryFn: () => getCourseModules(courseId),
  });
}

// Fetches a user's module completion and access status as calculated by the backend.
export function useUserModuleProgress(courseId: CourseId, userId: UserId) {
  return useQuery({
    queryKey: moduleKeys.userModuleProgress(courseId, userId),
    queryFn: () => getUserModuleProgress(courseId, userId),
  });
}

// Creates a module and marks the course's module list and user progress as outdated.
//for teachers, not for students
// Creates a module in a course and marks the cached module list as outdated.
export function useCreateModule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createModule,
    onSuccess: async (_module, { courseId }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: moduleKeys.courseModules(courseId) }),
        queryClient.invalidateQueries({ queryKey: moduleKeys.courseProgress(courseId) }),
      ]);
    },
  });
}

// Deletes a module and its content and refreshes related lists and user progress.
//for teachers, not for students
// Deletes a module and its content, clears its cached content list, and marks related data as outdated.
export function useDeleteModule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deleteModule,
    onSuccess: async (_data, { courseId, moduleId }) => {
      queryClient.removeQueries({ queryKey: contentKeys.moduleContent(moduleId) });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: moduleKeys.courseModules(courseId) }),
        queryClient.invalidateQueries({ queryKey: moduleKeys.courseProgress(courseId) }),
        // A module deletion also deletes its content. Refresh cached details and
        // progress, including content not currently present in the module list.
        queryClient.invalidateQueries({ queryKey: contentKeys.all }),
      ]);
    },
  });
}

// Updates module details or prerequisites and refreshes the module list and progress.
//for teachers, not for students
export function useUpdateModule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateModule,
    onSuccess: async (_module, { courseId }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: moduleKeys.courseModules(courseId) }),
        queryClient.invalidateQueries({ queryKey: moduleKeys.courseProgress(courseId) }),
      ]);
    },
  });
}
