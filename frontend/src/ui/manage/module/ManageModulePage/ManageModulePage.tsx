import { Link, useNavigate } from 'react-router'
import { paths } from '../../../../app/paths'
import { useCourseContent, useDeleteContent, useMoveContent, useUpdateContent } from '../../../../data/content/hooks'
import type { ContentSummary } from '../../../../data/content/types'
import ActionMenu, { type ActionMenuItem } from '../../../../shared/ui/ActionMenu'
import PageHeader from '../../../../shared/ui/PageHeader'
import { CONTENT_TYPES, contentTypeLabel } from '../../contentTypes'
import { useManageCourse } from '../../manageCourseContext'
import { useManageModule } from '../manageModuleContext'
import ModulePrerequisites from './ModulePrerequisites'
import './ManageModulePage.css'

/**
 * One module: a button per content type to create new content, and a table of the
 * module's content: whether each item is required for module completion (a checkbox)
 * and a "⋯" menu to edit, move or delete it. Below, which modules must be completed
 * before this one opens.
 */
export default function ManageModulePage() {
  const { course, modules } = useManageCourse()
  const { module, number, content } = useManageModule()
  const navigate = useNavigate()
  const moveContent = useMoveContent()
  const deleteContent = useDeleteContent()
  const updateContent = useUpdateContent()
  // Content counts of all modules, so moved content goes to the end of its new module.
  const allContent = useCourseContent(course.id, modules.map(m => m.id))

  function actionsFor(item: ContentSummary): ActionMenuItem[] {
    const otherModules = modules
      .map((m, index) => ({ module: m, number: index + 1 }))
      .filter(({ module: m }) => m.id !== module.id)
    return [
      { label: 'Edit', onSelect: () => navigate(paths.manageContent(course.id, module.id, item.id)) },
      {
        label: 'Move to',
        disabled: otherModules.length === 0,
        items: otherModules.map(({ module: target, number: targetNumber }) => ({
          label: `${targetNumber}. ${target.title}`,
          onSelect: () => moveContent.mutate({
            contentId: item.id,
            sourceModuleId: module.id,
            targetModuleId: target.id,
            position: allContent.byModule.get(String(target.id))?.length ?? 0,
          }),
        })),
      },
      {
        label: 'Delete',
        danger: true,
        onSelect: () => {
          if (!window.confirm(`Delete "${item.title}"? This cannot be undone.`)) return
          deleteContent.mutate({ contentId: item.id, moduleId: module.id })
        },
      },
    ]
  }

  return (
    <>
      <PageHeader title={module.title} kicker={`Module ${number} of ${modules.length}`} />

      <section aria-labelledby="new-content-title">
        <h2 id="new-content-title" className="section-title">Create</h2>
        <ul className="new-content-types">
          {CONTENT_TYPES.map(info => (
            <li key={info.type}>
              <Link className="new-content-type" to={paths.manageNewContent(course.id, module.id, info.slug)}>
                <span className="new-content-type-label">+ {info.label}</span>
                <span className="new-content-type-description">{info.description}</span>
              </Link>
            </li>
          ))}
        </ul>
      </section>

      <section aria-labelledby="module-content-title">
        <h2 id="module-content-title" className="section-title">Content in this module</h2>
        {content.length === 0
          ? <p>No content yet. Create the first one above.</p>
          : (
            <div className="manage-content-table-wrap">
              <table className="manage-content-table">
                <thead>
                  <tr>
                    <th scope="col">Title</th>
                    <th scope="col">Type</th>
                    <th scope="col" className="is-center">Active</th>
                    <th scope="col" className="is-center">Required for module completion</th>
                    <th scope="col"><span className="visually-hidden">Actions</span></th>
                  </tr>
                </thead>
                <tbody>
                  {content.map(item => (
                    <tr key={item.id} className={item.active === false ? 'is-inactive' : undefined}>
                      <th scope="row">
                        <Link className="manage-content-title" to={paths.manageContent(course.id, module.id, item.id)}>
                          {item.title}
                        </Link>
                      </th>
                      <td className="manage-content-type">{contentTypeLabel(item.type)}</td>
                      <td className="is-center">
                        <input
                          type="checkbox"
                          className="manage-content-check"
                          aria-label={`${item.title} is active (visible to students)`}
                          title="Students can see active content"
                          checked={item.active ?? true}
                          disabled={updateContent.isPending}
                          onChange={event => updateContent.mutate({
                            contentId: item.id,
                            moduleId: module.id,
                            changes: { active: event.target.checked },
                          })}
                        />
                      </td>
                      <td className="is-center">
                        <input
                          type="checkbox"
                          className="manage-content-check"
                          aria-label={`${item.title} is required for module completion`}
                          checked={item.requiredForCompletion ?? false}
                          disabled={updateContent.isPending}
                          onChange={event => updateContent.mutate({
                            contentId: item.id,
                            moduleId: module.id,
                            changes: { requiredForCompletion: event.target.checked },
                          })}
                        />
                      </td>
                      <td className="is-end">
                        <ActionMenu label={`Actions for ${item.title}`} items={actionsFor(item)} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        {moveContent.error && <p className="form-error" role="alert">Could not move the content. Try again.</p>}
        {deleteContent.error && <p className="form-error" role="alert">Could not delete the content. Try again.</p>}
        {updateContent.error && <p className="form-error" role="alert">Could not change the content. Try again.</p>}
      </section>

      <ModulePrerequisites courseId={course.id} module={module} modules={modules} />
    </>
  )
}
