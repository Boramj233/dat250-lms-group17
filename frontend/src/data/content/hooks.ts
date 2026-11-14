import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ContentId, CourseId, ModuleId, UserId } from "../../shared/types";
import { moduleKeys } from "../modules/queryKeys";
import { quizKeys } from "../quizzes/queryKeys";
import { createContent, deleteContent, getContent, getContentCompletion, getModuleContent, getMyCourseCompletions, moveContent, setContentCompletion, updateContent } from "./api";
import { contentKeys } from "./queryKeys";
import type { ContentSummary, ContentType } from "./types";

// Fetches and caches the full details of a content item. The type comes from the
// content summary, since the backend has one endpoint per content type.
export function useContent(type: ContentType | undefined, contentId: ContentId) {
  return useQuery({
    queryKey: contentKeys.content(contentId),
    queryFn: () => getContent(type!, contentId),
    // Wait until the type is known (from the module's content list).
    enabled: type !== undefined,
  });
}

// Fetches and caches a specific user's completion status for a content item.
export function useContentCompletion(contentId: ContentId, userId: UserId) {
  return useQuery({
    queryKey: contentKeys.completion(contentId, userId),
    queryFn: () => getContentCompletion(contentId, userId),
  });
}

// Which assignments, coding exercises and quizzes in a course the logged-in user has
// completed, as a set of content ids (strings) for quick lookups.
export function useCourseCompletions(courseId: CourseId) {
  return useQuery({
    queryKey: contentKeys.courseCompletions(courseId),
    queryFn: () => getMyCourseCompletions(courseId),
    select: completions => new Set(completions.map(item => String(item.contentId))),
  });
}

// The content of several modules of a course at once, e.g. "all assignments in the
// course". Shares the cache with useModuleContent (same query keys).
export function useCourseContent(courseId: CourseId, moduleIds: ModuleId[]) {
  return useQueries({
    queries: moduleIds.map(moduleId => ({
      queryKey: contentKeys.moduleContent(moduleId),
      queryFn: () => getModuleContent(courseId, moduleId),
    })),
    combine: results => ({
      /** Content per module id (as string); missing while that module is loading. */
      byModule: new Map<string, ContentSummary[]>(
        results.flatMap((result, index) =>
          result.data ? [[String(moduleIds[index]), result.data] as const] : [])
      ),
      isPending: results.some(result => result.isPending),
      isError: results.some(result => result.isError),
    }),
  });
}

// Fetches and caches the content summaries of a module, sorted by position.
/** Does nothing while `moduleId` is undefined (e.g. no module selected). */
export function useModuleContent(courseId: CourseId, moduleId: ModuleId | undefined) {
  return useQuery({
    queryKey: contentKeys.moduleContent(moduleId ?? ""),
    queryFn: () => getModuleContent(courseId, moduleId!),
    enabled: moduleId !== undefined,
  });
}

// Creates a page, assignment, coding exercise or quiz (with questions) and refreshes
// the module's content list and user progress.
//only for teachers, not for students
export function useCreateContent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createContent,
    onSuccess: async (_content, { moduleId }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: contentKeys.moduleContent(moduleId) }),
        // Parent course IDs are not part of this mutation's input.
        queryClient.invalidateQueries({ queryKey: moduleKeys.progress }),
      ]);
    },
  });
}

// Deletes content, clears its cached data, and refreshes the module list and progress.
//only for teachers, not for students
export function useDeleteContent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deleteContent,
    onSuccess: async (_data, { contentId, moduleId }) => {
      queryClient.removeQueries({ queryKey: contentKeys.content(contentId) });
      queryClient.removeQueries({ queryKey: quizKeys.quiz(contentId) });
      queryClient.removeQueries({ queryKey: quizKeys.definition(contentId) });
      queryClient.removeQueries({ queryKey: quizKeys.quizAttempts(contentId) });
      queryClient.removeQueries({ queryKey: contentKeys.completions(contentId) });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: contentKeys.moduleContent(moduleId) }),
        queryClient.invalidateQueries({ queryKey: moduleKeys.progress }),
      ]);
    },
  });
}

// Moves or reorders content and refreshes its details, module lists, and user progress.
//only for teachers, not for students
export function useMoveContent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: moveContent,
    onSuccess: async (_content, { contentId, sourceModuleId, targetModuleId }) => {
      const affectedKeys = [
        contentKeys.content(contentId),
        contentKeys.moduleContent(sourceModuleId),
      ];
      if (String(sourceModuleId) !== String(targetModuleId)) {
        affectedKeys.push(contentKeys.moduleContent(targetModuleId));
      }
      await Promise.all([
        ...affectedKeys.map((queryKey) => queryClient.invalidateQueries({ queryKey })),
        queryClient.invalidateQueries({ queryKey: moduleKeys.progress }),
      ]);
    },
  });
}

// Records content completion and refreshes the user's module progress and access.
//for studentinteraction
export function useSetContentCompletion() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: setContentCompletion,
    onSuccess: async (_completion, { courseId, contentId, userId }) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: contentKeys.completion(contentId, userId),
        }),
        queryClient.invalidateQueries({
          queryKey: moduleKeys.userModuleProgress(courseId, userId),
        }),
      ]);
    },
  });
}

// Updates content and refreshes its details, module content list, and user progress.
//only for teachers, not for students
export function useUpdateContent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateContent,
    onSuccess: async (_content, { contentId, moduleId }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: contentKeys.content(contentId) }),
        queryClient.invalidateQueries({ queryKey: quizKeys.quiz(contentId) }),
        queryClient.invalidateQueries({ queryKey: quizKeys.definition(contentId) }),
        queryClient.invalidateQueries({ queryKey: contentKeys.moduleContent(moduleId) }),
        queryClient.invalidateQueries({ queryKey: moduleKeys.progress }),
      ]);
    },
  });
}
