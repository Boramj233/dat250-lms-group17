import { useNavigate, useParams } from 'react-router'
import { paths } from '@shared/paths'
import NotFoundPage from '@shared/ui/NotFoundPage'
import PageHeader from '@shared/ui/PageHeader'
import { contentTypeBySlug } from '@domain/content/contentTypes'
import { useCreateContent } from '@domain/content/hooks'
import type { CreateContentInput } from '@domain/content/types'
import { useManageCourse } from '../../../manageCourseContext'
import ContentForm from '../../components/ContentForm'
import { useManageModule } from '../../manageModuleContext'

/**
 * Create one type of content at the end of the module: /new/assignment, /new/quiz,
 * /new/coding-exercise or /new/page. Only the fields of that type are shown.
 */
export default function NewContentPage() {
  const info = contentTypeBySlug(useParams().type)
  const navigate = useNavigate()
  const { course } = useManageCourse()
  const { module, number, content } = useManageModule()
  const createContent = useCreateContent()

  if (!info) return <NotFoundPage />
  const backTo = paths.manageModule(course.id, module.id)

  return (
    <>
      <PageHeader title={`New ${info.label.toLowerCase()}`} kicker={`Module ${number}: ${module.title}`} />
      <ContentForm
        type={info.type}
        module={module}
        submitLabel={`Create ${info.label.toLowerCase()}`}
        pending={createContent.isPending}
        error={createContent.error ? 'Could not save. Try again.' : undefined}
        cancelTo={backTo}
        onSubmit={fields => {
          // At the end of the module.
          const input = {
            ...fields,
            courseId: course.id,
            moduleId: module.id,
            position: content.length,
          } as CreateContentInput
          createContent.mutate(input, { onSuccess: () => navigate(backTo) })
        }}
      />
    </>
  )
}
