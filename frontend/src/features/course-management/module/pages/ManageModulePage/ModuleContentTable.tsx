import { Link } from 'react-router'
import { formatDateTime } from '@shared/lib/dateFormat'
import { paths } from '@shared/paths'
import type { CourseId, ModuleId } from '@shared/types'
import ActionMenu, { type ActionMenuItem } from '@shared/ui/ActionMenu'
import { contentTypeLabel } from '@domain/content/contentTypes'
import type { ContentSummary } from '@domain/content/types'
import { isPublished } from '@domain/content/visibility'
import type { CourseModule } from '@domain/modules/types'
import { itemDraft, type ContentDraft } from './contentDraft'

type ModuleContentTableProps = {
  courseId: CourseId
  module: CourseModule
  content: ContentSummary[]
  draft: ContentDraft
  /** For "Published" / "Unpublished". */
  now: number
  /** While saving, nothing can be changed. */
  saving: boolean
  /** "Module 2: Basics", for "Moves to …". */
  moduleLabel: (id: ModuleId) => string
  onRequiredChange: (item: ContentSummary, required: boolean) => void
  /** The "⋯" menu of a row. */
  actionsFor: (item: ContentSummary) => ActionMenuItem[]
}

/**
 * The module's content: title, type, visible from, published or not, required for module
 * completion and a "⋯" menu. Unsaved changes are marked on their rows.
 */
export default function ModuleContentTable(props: ModuleContentTableProps) {
  const { courseId, module, content, draft, now, saving, moduleLabel, onRequiredChange, actionsFor } = props
  return (
    <div className="manage-content-table-wrap">
      <table className="manage-content-table">
        <thead>
          <tr>
            <th scope="col">Title</th>
            <th scope="col">Type</th>
            <th scope="col">Visible from</th>
            <th scope="col">Status</th>
            <th scope="col" className="is-center">
              Required for module completion
            </th>
            <th scope="col">
              <span className="visually-hidden">Actions</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {content.map(item => {
            const { deleted, movedTo, requiredChanged, required } = itemDraft(draft, item)
            const rowClass = [
              item.active === false ? 'is-inactive' : '',
              deleted ? 'is-deleted' : movedTo !== undefined || requiredChanged ? 'is-changed' : '',
            ]
              .filter(Boolean)
              .join(' ')
            return (
              <tr key={item.id} className={rowClass || undefined}>
                <th scope="row">
                  <Link className="manage-content-title" to={paths.manageContent(courseId, module.id, item.id)}>
                    {item.title}
                  </Link>
                  {deleted && <span className="tag tag-strong tag-danger manage-change">Will be deleted</span>}
                  {!deleted && movedTo !== undefined && (
                    <span className="tag tag-strong manage-change">Moves to {moduleLabel(movedTo)}</span>
                  )}
                </th>
                <td className="manage-content-type">{contentTypeLabel(item.type)}</td>
                <td className="manage-content-type">
                  {item.visibleFrom ? (
                    formatDateTime(new Date(item.visibleFrom))
                  ) : module.availableFrom ? (
                    <>With the module · {formatDateTime(new Date(module.availableFrom))}</>
                  ) : (
                    'With the module'
                  )}
                </td>
                <td>
                  <PublishStatus published={isPublished(item, module, now)} />
                </td>
                <td className={`is-center${requiredChanged ? ' is-changed-cell' : ''}`}>
                  <input
                    type="checkbox"
                    className="manage-content-check"
                    aria-label={`${item.title} is required for module completion${requiredChanged ? ' (not saved)' : ''}`}
                    checked={required}
                    disabled={saving || deleted}
                    onChange={event => onRequiredChange(item, event.target.checked)}
                  />
                </td>
                <td className="is-end">
                  <ActionMenu label={`Actions for ${item.title}`} items={actionsFor(item)} />
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

/** "Published" once the visible date has passed, "Unpublished" before. Only shown, not changed here. */
function PublishStatus({ published }: { published: boolean }) {
  return (
    <span className={`manage-publish-label ${published ? 'is-published' : 'is-unpublished'}`}>
      {published ? 'Published' : 'Unpublished'}
    </span>
  )
}
