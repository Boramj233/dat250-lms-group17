import { useState } from 'react'
import { paths } from '@shared/paths'
import SaveBar from '@shared/ui/SaveBar'
import { useCourseContent } from '@domain/content/hooks'
import { useReorderModules } from '@domain/modules/hooks'
import { availabilityText } from '@domain/modules/moduleFormat'
import { useManageCourse } from '../../manageCourseContext'
import AddModuleForm from './AddModuleForm'
import ModuleRow from './ModuleRow'
import { useModuleOrder } from './useModuleOrder'
import './ModuleListPage.css'

/**
 * The Content tab: the course's modules in their order (rank), which is the order every
 * student sees them in, each with a button to manage it, and "Add a module". The order is
 * changed by dragging a row, or with the ↑ and ↓ buttons (keyboard, touch); it is saved
 * for all modules at once with Save changes.
 */
export default function ModuleListPage() {
  const { course, modules } = useManageCourse()
  // All modules' content at once, for the counts; the same cache as the module pages.
  const content = useCourseContent(
    course.id,
    modules.map(m => m.id),
  )
  const reorder = useReorderModules()
  const [saved, setSaved] = useState<string | null>(null)
  const order = useModuleOrder(modules, () => {
    setSaved(null)
    reorder.reset()
  })

  function save() {
    if (!order.order) return
    reorder.mutate(
      { courseId: course.id, moduleIds: order.order },
      {
        onSuccess: () => {
          order.reset()
          setSaved('The new order is saved. Students see the modules in this order.')
        },
      },
    )
  }

  return (
    <>
      {modules.length === 0 ? (
        <p>This course has no modules yet. Add the first one below.</p>
      ) : (
        <>
          <p className="form-hint manage-module-help">
            The order here is the order students see the modules in. Drag a module by its handle, or use ↑ and ↓, then
            save.
          </p>
          <ol className="manage-module-list">
            {order.shown.map((module, index) => {
              const id = String(module.id)
              const savedIndex = order.savedIds.indexOf(id)
              return (
                <ModuleRow
                  key={module.id}
                  module={module}
                  index={index}
                  savedIndex={savedIndex}
                  moved={order.order !== null && savedIndex !== index}
                  dragging={order.dragId === id}
                  itemCount={content.byModule.get(id)?.length}
                  availability={availabilityText(module, modules)}
                  last={index === order.shown.length - 1}
                  busy={reorder.isPending}
                  manageTo={paths.manageModule(course.id, module.id)}
                  onMove={by => order.move(module, by)}
                  onDragStart={event => order.dragStart(event, id)}
                  onDragOver={event => order.dragOver(event, id)}
                  onDragEnd={order.dragEnd}
                />
              )
            })}
          </ol>
          <p className="visually-hidden" aria-live="polite">
            {order.announcement}
          </p>
          <SaveBar
            changes={order.changes}
            saving={reorder.isPending}
            error={reorder.error ? 'Could not save the new order. Try again.' : null}
            saved={saved}
            saveLabel="Save order"
            onSave={save}
            onCancel={() => order.reset('The order is back to the saved one.')}
          />
        </>
      )}
      <AddModuleForm courseId={course.id} position={modules.length} />
    </>
  )
}
