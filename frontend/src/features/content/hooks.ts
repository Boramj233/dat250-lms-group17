import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ContentId, CourseId, ModuleId } from "../../shared/types";
import { quizKeys } from "../quizzes/queryKeys";
import { createContent, deleteContent, getContent, getModuleContent, moveContent, updateContent } from "./api";
import { contentKeys } from "./queryKeys";
import type { Content, ContentSummary, ContentType } from "./types";

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

// The full details of several content items at once, e.g. the deadlines in a course's
// table of assignments. Shares the cache with useContent (same query keys).
export function useContents(items: { type: ContentType; id: ContentId }[]) {
  return useQueries({
    queries: items.map(item => ({
      queryKey: contentKeys.content(item.id),
      queryFn: () => getContent(item.type, item.id),
    })),
    combine: results => ({
      /** Details per content id (as string); missing while that item is loading. */
      byId: new Map<string, Content>(
        results.flatMap((result, index) =>
          result.data ? [[String(items[index].id), result.data] as const] : [])
      ),
      isPending: results.some(result => result.isPending),
    }),
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
// the module's content list.
//only for teachers, not for students
export function useCreateContent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createContent,
    onSuccess: (_content, { moduleId }) =>
      queryClient.invalidateQueries({ queryKey: contentKeys.moduleContent(moduleId) }),
  });
}

// Deletes content, clears its cached data (also the user's quiz attempts), and
// refreshes the module list.
//only for teachers, not for students
export function useDeleteContent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deleteContent,
    onSuccess: async (_data, { contentId, moduleId }) => {
      queryClient.removeQueries({ queryKey: contentKeys.content(contentId) });
      queryClient.removeQueries({ queryKey: quizKeys.quizAttempts(contentId) });
      await queryClient.invalidateQueries({ queryKey: contentKeys.moduleContent(moduleId) });
    },
  });
}

// Moves or reorders content and refreshes its details and module lists.
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
      await Promise.all(affectedKeys.map((queryKey) => queryClient.invalidateQueries({ queryKey })));
    },
  });
}

// Updates content and refreshes its details and module content list.
//only for teachers, not for students
export function useUpdateContent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateContent,
    onSuccess: async (_content, { contentId, moduleId }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: contentKeys.content(contentId) }),
        queryClient.invalidateQueries({ queryKey: contentKeys.moduleContent(moduleId) }),
      ]);
    },
  });
}
