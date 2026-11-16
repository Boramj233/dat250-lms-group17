import { useState } from 'react'
import { ApiError } from '@shared/api/apiClient'
import { formatDateTime } from '@shared/lib/dateFormat'
import { paths } from '@shared/paths'
import PageHeader from '@shared/ui/PageHeader'
import QueryState from '@shared/ui/QueryState'
import { contentTypeLabel } from '@domain/content/contentTypes'
import { useContent, useEditContent } from '@domain/content/hooks'
import type { Content, ContentSummary } from '@domain/content/types'
import { effectiveVisibleFrom, isPublished } from '@domain/content/visibility'
import { useManageCourse } from '../../../manageCourseContext'
import ContentForm from '../../ContentForm'
import { useManageModule } from '../../manageModuleContext'
import { useManagedContent } from '../../useManagedContent'
import './ContentDetailsPage.css'

/**
 * One content item's setup: every field in the same form it was created with, and whether
 * it is published (its visible date has passed). When it may no longer be changed is up
 * to the backend; the form shows its answer (409) if it refuses.
 */
export default function ContentDetailsPage() {
  const { module, number } = useManageModule()
  const item = useManagedContent()
  // The type comes from the module's content list; the backend has one endpoint per type.
  const content = useContent(item?.type, item?.id ?? '')
  const [now] = useState(() => Date.now())

  if (!item) return <p role="alert">This content does not exist in {module.title}.</p>
  const published = isPublished(item, module, now)

  return (
    <>
      <PageHeader
        title={item.title}
        kicker={`${contentTypeLabel(item.type)} · Module ${number}${item.requiredForCompletion ? ' · Required to complete the module' : ''}`}
      />
      <QueryState query={content} loading="Loading the content…" error="Could not load the content.">
        {data => (
          <>
            <p className="content-publish-status">
              {published
                ? <><strong>Published.</strong> Students can see it{publishedSince(item, module)}.</>
                : <><strong>Unpublished.</strong> {notPublishedReason(item, module)}</>}
            </p>
            <EditForm key={data.id} content={data} />
          </>
        )}
      </QueryState>
    </>
  )
}

/** " since 3 Nov 08:00", or nothing when it has been visible since it was made. */
function publishedSince(item: ContentSummary, module: { availableFrom?: string | null }): string {
  const from = effectiveVisibleFrom(item, module)
  return from ? ` since ${formatDateTime(new Date(from))}` : ''
}

function notPublishedReason(item: ContentSummary, module: { availableFrom?: string | null }): string {
  const from = effectiveVisibleFrom(item, module)
  return from
    ? `Students see it from ${formatDateTime(new Date(from))}${item.visibleFrom ? '' : ', when the module opens'}.`
    : 'Students cannot see it yet.'
}

/** All fields, saved together (proposed PATCH; a quiz's questions are replaced). */
function EditForm({ content }: { content: Content }) {
  const { course } = useManageCourse()
  const { module } = useManageModule()
  const edit = useEditContent()
  const [saved, setSaved] = useState(false)

  return (
    <ContentForm
      type={content.type}
      module={module}
      initial={content}
      submitLabel="Save changes"
      pending={edit.isPending}
      error={edit.error ? editError(edit.error) : undefined}
      success={saved ? 'Saved.' : undefined}
      cancelTo={paths.manageModule(course.id, module.id)}
      onChange={() => setSaved(false)}
      onSubmit={fields => {
        setSaved(false)
        edit.mutate({ contentId: content.id, moduleId: module.id, fields }, { onSuccess: () => setSaved(true) })
      }}
    />
  )
}

function editError(error: Error): string {
  if (error instanceof ApiError) {
    if (error.status === 409) return 'The server does not allow changing it any more (e.g. because it is published).'
    if (error.status === 400) return 'Some of the values are not allowed, e.g. a visible date before the module opens.'
  }
  return 'Could not save. Try again.'
}
