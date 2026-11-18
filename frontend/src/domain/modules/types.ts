import type { CourseId, ModuleId } from '@shared/types'

// The backend's ModuleResponse, as api.ts maps it: `dependencyIds` becomes
// `prerequisiteModuleIds`, `availableToAllFrom` becomes `openToAllFrom`, and the dates
// (LocalDateTime, local time) become ISO instants.
export interface CourseModule {
  id: ModuleId
  courseId: CourseId
  title: string
  position: number
  /** The modules that must be completed first (ModuleResponse.dependencyIds). */
  prerequisiteModuleIds?: ModuleId[]
  /**
   * ISO 8601 instant: the module is closed to students before this; null or missing
   * for no start date. Changed with PUT /modules/{id}/availability.
   */
  availableFrom?: string | null
  /**
   * ISO 8601 instant: from this on the module is open to all students, also those
   * who have not completed its prerequisites; null or missing for never
   * (ModuleResponse.availableToAllFrom).
   */
  openToAllFrom?: string | null
}

export interface DeleteModuleInput {
  courseId: CourseId
  moduleId: ModuleId
}

export interface CreateModuleInput {
  courseId: CourseId
  title: string
  position: number
  prerequisiteModuleIds?: ModuleId[]
}

/**
 * The course's modules in their new order (rank), first to last. Every module of the
 * course, each once; they get positions 0, 1, 2, … from it.
 */
export interface ReorderModulesInput {
  courseId: CourseId
  moduleIds: ModuleId[]
}

export interface UpdateModuleInput {
  courseId: CourseId
  moduleId: ModuleId
  changes: Partial<
    Pick<CourseModule, 'title' | 'position' | 'prerequisiteModuleIds' | 'availableFrom' | 'openToAllFrom'>
  >
}
