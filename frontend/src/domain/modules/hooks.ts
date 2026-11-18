import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useInvalidatingMutation } from '@shared/api/useInvalidatingMutation'
import type { CourseId } from '@shared/types'
import { contentKeys } from '@domain/content/queryKeys'
import { createModule, deleteModule, getCourseModules, reorderModules, updateModule } from './api'
import { moduleKeys } from './queryKeys'

// Fetches and caches the modules belonging to a specific course, sorted by position.
export function useCourseModules(courseId: CourseId) {
  return useQuery({
    queryKey: moduleKeys.courseModules(courseId),
    queryFn: () => getCourseModules(courseId),
    select: modules => [...modules].sort((a, b) => a.position - b.position),
  })
}

// Creates a module in a course and marks the cached module list as outdated.
//for teachers, not for students
export function useCreateModule() {
  return useInvalidatingMutation(createModule, (_module, { courseId }) => [moduleKeys.courseModules(courseId)])
}

// Updates module details or prerequisites and refreshes the module list.
//for teachers, not for students
export function useUpdateModule() {
  return useInvalidatingMutation(updateModule, (_module, { courseId }) => [moduleKeys.courseModules(courseId)])
}

// Deletes a module, forgets its cached content list and refreshes the module list.
//for teachers, not for students
export function useDeleteModule() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deleteModule,
    onSuccess: async (_data, { courseId, moduleId }) => {
      queryClient.removeQueries({ queryKey: contentKeys.moduleContent(moduleId) })
      await queryClient.invalidateQueries({ queryKey: moduleKeys.courseModules(courseId) })
    },
  })
}

// Saves a new order (rank) of the course's modules; students see it at once.
//for teachers, not for students
export function useReorderModules() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: reorderModules,
    onSuccess: (modules, { courseId }) => queryClient.setQueryData(moduleKeys.courseModules(courseId), modules),
  })
}
