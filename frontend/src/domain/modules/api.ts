import { apiClient, apiPath, jsonBody } from '@shared/api/apiClient'
import type { CourseId, ModuleId } from '@shared/types'
import type {
  CourseModule,
  CreateModuleInput,
  DeleteModuleInput,
  ReorderModulesInput,
  UpdateModuleInput,
} from './types'

/** The backend's ModuleResponse. Its dates are LocalDateTime: local time, no offset. */
interface ModuleResponse {
  id: ModuleId
  courseId: CourseId
  title: string
  position: number
  dependencyIds: ModuleId[] | null
  availableFrom: string | null
  availableToAllFrom: string | null
}

/** A LocalDateTime from the backend ("2026-11-18T08:00:00") as an ISO instant. */
function fromLocalDateTime(value: string | null | undefined): string | null {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date.toISOString()
}

/**
 * An ISO instant as the backend's LocalDateTime: the browser's local time without an
 * offset (the backend compares it with its own local time).
 */
function toLocalDateTime(iso: string | null | undefined): string | null {
  if (!iso) return null
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return null
  const pad = (n: number) => String(n).padStart(2, '0')
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  )
}

function toCourseModule(response: ModuleResponse): CourseModule {
  return {
    id: response.id,
    courseId: response.courseId,
    title: response.title,
    position: response.position,
    prerequisiteModuleIds: response.dependencyIds ?? [],
    availableFrom: fromLocalDateTime(response.availableFrom),
    openToAllFrom: fromLocalDateTime(response.availableToAllFrom),
  }
}

export async function getCourseModules(courseId: CourseId): Promise<CourseModule[]> {
  const modules = await apiClient<ModuleResponse[]>(apiPath`/api/courses/${courseId}/modules`)
  return modules.map(toCourseModule)
}

export async function getModule(courseId: CourseId, moduleId: ModuleId): Promise<CourseModule> {
  return toCourseModule(await apiClient<ModuleResponse>(apiPath`/api/courses/${courseId}/modules/${moduleId}`))
}

/** POST /api/courses/{id}/modules (title, position), then one POST per prerequisite. */
export async function createModule({
  courseId,
  title,
  position,
  prerequisiteModuleIds = [],
}: CreateModuleInput): Promise<CourseModule> {
  let module = await apiClient<ModuleResponse>(
    apiPath`/api/courses/${courseId}/modules`,
    jsonBody('POST', { title, position }),
  )
  for (const requiredId of prerequisiteModuleIds) {
    module = await addDependency(courseId, module.id, requiredId)
  }
  return toCourseModule(module)
}

function addDependency(courseId: CourseId, moduleId: ModuleId, requiredId: ModuleId): Promise<ModuleResponse> {
  return apiClient<ModuleResponse>(apiPath`/api/courses/${courseId}/modules/${moduleId}/dependencies/${requiredId}`, {
    method: 'POST',
  })
}

function removeDependency(courseId: CourseId, moduleId: ModuleId, requiredId: ModuleId): Promise<void> {
  return apiClient<void>(apiPath`/api/courses/${courseId}/modules/${moduleId}/dependencies/${requiredId}`, {
    method: 'DELETE',
  })
}

/**
 * Saves the changed fields with the backend's separate endpoints:
 * - title, position: PUT /modules/{id} (needs both; the other is taken from the module),
 * - availableFrom, openToAllFrom: PUT /modules/{id}/availability (sets both; a missing one
 *   is kept),
 * - prerequisiteModuleIds: POST/DELETE /modules/{id}/dependencies/{requiredId} for the
 *   difference to what is saved.
 * Resolves to the module as saved.
 */
export async function updateModule({ courseId, moduleId, changes }: UpdateModuleInput): Promise<CourseModule> {
  let current = await getModule(courseId, moduleId)

  if (changes.title !== undefined || changes.position !== undefined) {
    current = toCourseModule(
      await apiClient<ModuleResponse>(
        apiPath`/api/courses/${courseId}/modules/${moduleId}`,
        jsonBody('PUT', { title: changes.title ?? current.title, position: changes.position ?? current.position }),
      ),
    )
  }

  if (changes.availableFrom !== undefined || changes.openToAllFrom !== undefined) {
    const availableFrom = changes.availableFrom !== undefined ? changes.availableFrom : current.availableFrom
    const openToAllFrom = changes.openToAllFrom !== undefined ? changes.openToAllFrom : current.openToAllFrom
    current = toCourseModule(
      await apiClient<ModuleResponse>(
        apiPath`/api/courses/${courseId}/modules/${moduleId}/availability`,
        jsonBody('PUT', {
          availableFrom: toLocalDateTime(availableFrom),
          availableToAllFrom: toLocalDateTime(openToAllFrom),
        }),
      ),
    )
  }

  if (changes.prerequisiteModuleIds !== undefined) {
    const saved = (current.prerequisiteModuleIds ?? []).map(String)
    const wanted = changes.prerequisiteModuleIds.map(String)
    for (const id of saved.filter(id => !wanted.includes(id))) {
      await removeDependency(courseId, moduleId, id)
    }
    for (const id of wanted.filter(id => !saved.includes(id))) {
      current = toCourseModule(await addDependency(courseId, moduleId, id))
    }
    if (saved.some(id => !wanted.includes(id))) {
      current = { ...current, prerequisiteModuleIds: changes.prerequisiteModuleIds }
    }
  }

  return current
}

/** DELETE /api/courses/{courseId}/modules/{moduleId} (204). */
export function deleteModule({ courseId, moduleId }: DeleteModuleInput): Promise<void> {
  return apiClient<void>(apiPath`/api/courses/${courseId}/modules/${moduleId}`, { method: 'DELETE' })
}

/**
 * Saves the order (rank) of the course's modules. The backend has no endpoint for the
 * whole order, so every module whose position changes gets PUT /modules/{id} with its
 * title and new position (0, 1, 2, …). Answers with the modules in their new order.
 */
export async function reorderModules({ courseId, moduleIds }: ReorderModulesInput): Promise<CourseModule[]> {
  const modules = await getCourseModules(courseId)
  const byId = new Map(modules.map(m => [String(m.id), m]))
  const result: CourseModule[] = []
  for (const [position, id] of moduleIds.entries()) {
    const module = byId.get(String(id))
    if (!module) throw new Error(`Module ${id} is not in the course`)
    if (module.position === position) {
      result.push(module)
      continue
    }
    result.push(
      toCourseModule(
        await apiClient<ModuleResponse>(
          apiPath`/api/courses/${courseId}/modules/${id}`,
          jsonBody('PUT', { title: module.title, position }),
        ),
      ),
    )
  }
  return result
}
