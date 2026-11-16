import type { ContentId, ModuleId } from '@shared/types'

export const contentKeys = {
  moduleContent: (moduleId: ModuleId) => ['content', 'module', String(moduleId)] as const,
  content: (contentId: ContentId) => ['content', 'detail', String(contentId)] as const,
  completion: (contentId: ContentId) => ['content', 'completion', String(contentId)] as const,
}
