import { useParams } from 'react-router'
import type { ContentSummary } from '../../content/types'
import { useManageModule } from './manageModuleContext'

/** The content item in the URL (:contentId), from the module's content list; undefined if not there. */
export function useManagedContent(): ContentSummary | undefined {
  const { contentId } = useParams()
  return useManageModule().content.find(item => String(item.id) === contentId)
}
