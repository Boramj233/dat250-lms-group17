import { errorMessage } from '@shared/api/errorMessage'
import { CheckCircleIcon } from '@shared/ui/icons'
import { useContentCompletion, useSetPageCompletion } from '@domain/content/hooks'
import type { ContentId } from '@shared/types'
import '@shared/styles/forms.css'

/**
 * Students: whether they have read a page, and a button to mark it as done (or undo
 * that). Uses GET/PUT /api/contents/{id}/completion.
 */
export default function PageCompletion({ contentId }: { contentId: ContentId }) {
  const status = useContentCompletion(contentId)
  const mark = useSetPageCompletion()

  if (status.isPending) return <p className="text-meta">Loading your progress…</p>
  if (status.error) return null
  const completed = status.data.completed

  return (
    <div className="page-completion">
      {completed ? (
        <p className="page-completion-done">
          <CheckCircleIcon size={18} strokeWidth={2.5} />
          You have marked this page as done.
        </p>
      ) : (
        status.data.requiredForModuleCompletion && <p className="text-meta">Required to complete the module.</p>
      )}
      <button
        type="button"
        className={completed ? 'button-secondary' : 'button-primary'}
        disabled={mark.isPending}
        onClick={() => mark.mutate({ contentId, completed: !completed })}
      >
        {mark.isPending ? 'Saving…' : completed ? 'Mark as not done' : 'Mark as done'}
      </button>
      {mark.error && (
        <p className="form-error" role="alert">
          {errorMessage(
            mark.error,
            { 403: 'This page is not open to you right now.' },
            'Could not save your progress. Try again.',
          )}
        </p>
      )}
    </div>
  )
}
