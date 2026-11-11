import type { CourseId, ModuleId } from "../shared/types";

// Matches the backend's ModuleResponse; prerequisites are not in the backend yet.
export interface CourseModule {
  id: ModuleId;
  courseId: CourseId;
  title: string;
  position: number;
  prerequisiteModuleIds?: ModuleId[];
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
  changes: Partial<Pick<CourseModule, "title" | "position" | "prerequisiteModuleIds">>;
}

export interface DeleteModuleInput {
  courseId: CourseId;
  moduleId: ModuleId;
}
