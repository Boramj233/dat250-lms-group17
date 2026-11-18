import { useState } from 'react'
import { errorMessage } from '@shared/api/errorMessage'
import { formatDateTime } from '@shared/lib/dateFormat'
import { paths } from '@shared/paths'
import PageHeader from '@shared/ui/PageHeader'
import QueryState from '@shared/ui/QueryState'
import { contentTypeLabel } from '@domain/content/contentTypes'
import { useContent, useEditContent } from '@domain/content/hooks'
import type { Content, ContentSummary } from '@domain/content/types'
import { effectiveVisibleFrom, isPublished } from '@domain/content/visibility'
import { useQuizAnswerKey } from '@domain/quizzes/hooks'
import { quizKeys } from '@domain/quizzes/queryKeys'
import { useQueryClient } from '@tanstack/react-query'
import { initialValues } from '../../components/ContentForm/contentFormValues'
import type { QuizAnswerKey } from '@domain/quizzes/types'
import { useManageCourse } from '../../../manageCourseContext'
import ContentForm from '../../components/ContentForm'
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
  // A quiz's correct answers are not in the quiz itself; teachers get them separately.
  const answerKey = useQuizAnswerKey(item?.id ?? '', { enabled: item?.type === 'QUIZ' })
  const [now] = useState(() => Date.now())

  if (!item) return <p role="alert">This content does not exist in {module.title}.</p>
  const published = isPublished(item, module, now)

  return (
    <>
      <PageHeader
        title={item.title}
        kicker={`${contentTypeLabel(item.type)} · Module ${number}${item.requiredForModuleCompletion ? ' · Required to complete the module' : ''}`}
      />
      <QueryState query={content} loading="Loading the content…" error="Could not load the content.">
        {data => (
          <>
            <p className="content-publish-status">
              {published ? (
                <>
                  <strong>Published.</strong> Students can see it{publishedSince(item, module)}.
                </>
              ) : (
                <>
                  <strong>Unpublished.</strong> {notPublishedReason(item, module)}
                </>
              )}
            </p>
            {data.type === 'QUIZ' && (
              <p className="form-hint">
                The questions can be changed until a student has taken the quiz. After that, only the title, the number
                of attempts and the passing percentage can be changed.
              </p>
            )}
            {data.type === 'QUIZ' && answerKey.error && (
              <p role="alert">Could not load the correct answers, so the questions are shown without them.</p>
            )}
            {/* The form starts from its values once, so a quiz waits for its answer key. */}
            {data.type === 'QUIZ' && answerKey.isPending ? (
              <p role="status">Loading the correct answers…</p>
            ) : (
              <EditForm
                key={data.id}
                content={data}
                answerKey={answerKey.data}
                required={item.requiredForModuleCompletion ?? false}
              />
            )}
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

/** All fields, saved together with the endpoints for the item's type (see editContent). */
function EditForm({
  content,
  answerKey,
  required,
}: {
  content: Content
  answerKey?: QuizAnswerKey
  required: boolean
}) {
  const { course } = useManageCourse()
  const { module } = useManageModule()
  const edit = useEditContent()
  const queryClient = useQueryClient()
  const [saved, setSaved] = useState(false)
  // A quiz's questions as last saved, so they are only sent when they changed.
  const [savedQuestions, setSavedQuestions] = useState(() =>
    JSON.stringify(initialValues(content, answerKey).questions),
  )

  return (
    <ContentForm
      type={content.type}
      module={module}
      initial={content}
      answerKey={answerKey}
      submitLabel="Save changes"
      pending={edit.isPending}
      error={edit.error ? editError(edit.error, content.type) : undefined}
      success={saved ? 'Saved.' : undefined}
      cancelTo={paths.manageModule(course.id, module.id)}
      onChange={() => setSaved(false)}
      onSubmit={fields => {
        setSaved(false)
        const questions = fields.type === 'QUIZ' ? JSON.stringify(fields.questions) : savedQuestions
        edit.mutate(
          {
            contentId: content.id,
            moduleId: module.id,
            requiredForModuleCompletion: required,
            questionsChanged: questions !== savedQuestions,
            fields,
          },
          {
            onSuccess: () => {
              setSaved(true)
              setSavedQuestions(questions)
              if (content.type === 'QUIZ')
                void queryClient.invalidateQueries({ queryKey: quizKeys.answerKey(content.id) })
            },
          },
        )
      }}
    />
  )
}

function editError(error: Error, type: Content['type']): string {
  return errorMessage(
    error,
    {
      400: 'Some of the values are not allowed, e.g. an empty text or a missing starter repository.',
      403: 'Only teachers of the course can change it.',
      409:
        type === 'QUIZ'
          ? 'Students have already taken the quiz, so its questions can no longer be changed. The other changes are saved; undo the changes to the questions and save again.'
          : 'The server refused the change because of a conflict. Reload the page and try again.',
    },
    'Could not save. Try again.',
  )
}
