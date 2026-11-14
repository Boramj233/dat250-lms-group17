import type { CourseId, ModuleId } from "../../shared/types";

// Matches the backend's ModuleResponse; prerequisites are not in the backend yet.
export interface CourseModule {
  id: ModuleId;
  courseId: CourseId;
  title: string;
  position: number;
  prerequisiteModuleIds?: ModuleId[];
  /**
   * ISO 8601 instant: the module is closed to students before this; null or missing
   * for no start date. Proposed, not in the backend yet.
   */
  availableFrom?: string | null;
  /**
   * ISO 8601 instant: from this on the module is open to all students, also those
   * who have not completed its prerequisites; null or missing for never. Proposed.
   */
  openToAllFrom?: string | null;
}

// Computed by the backend for one user's progress and access in a course.
export interface UserModuleProgress {
  moduleId: ModuleId;
  completed: boolean;
  accessible: boolean;
}

export interface CreateModuleInput {
  courseId: CourseId;
  title: string;
  position: number;
  prerequisiteModuleIds?: ModuleId[];
}

export interface UpdateModuleInput {
  courseId: CourseId;
  moduleId: ModuleId;
  changes: Partial<Pick<CourseModule, "title" | "position" | "prerequisiteModuleIds" | "availableFrom" | "openToAllFrom">>;
}

export interface DeleteModuleInput {
  courseId: CourseId;
  moduleId: ModuleId;
}
