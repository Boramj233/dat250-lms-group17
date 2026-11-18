import { useState } from 'react'
import type { ContentId } from '@shared/types'
import { useConfirm } from '@shared/ui/ConfirmDialog'
import { useReleaseAllGrades } from '@domain/assignments/hooks'
import { FormError, FormSuccess } from '@shared/ui/Form'
import { formatCount } from '@shared/lib/formatCount'

type GradePublishingProps = {
  contentId: ContentId
  /** Saved grades the students cannot see yet; undefined while the grades load. */
  unpublished: number | undefined
}

/**
 * Teachers: "Publish all" makes every saved grade in the list visible now. Each grade
 * also has its own visible date, set when grading it.
 */
export default function GradePublishing({ contentId, unpublished }: GradePublishingProps) {
  const releaseAll = useReleaseAllGrades(contentId)
  const [confirm, confirmDialog] = useConfirm()
  const [published, setPublished] = useState<string | null>(null)

  async function publishAll() {
    if (!unpublished) return
    const confirmed = await confirm({
      title: `Publish ${formatCount(unpublished, 'grade')}?`,
      message:
        'The students see their score and feedback right away, also for grades with a later visible date. Grades without a passed / not passed decision are not published. A published grade cannot be changed.',
      confirmLabel: 'Publish all',
    })
    if (!confirmed) return
    setPublished(null)
    releaseAll.mutate(undefined, {
      onSuccess: result =>
        setPublished(
          `${result.released} ${result.released === 1 ? 'grade is' : 'grades are'} now visible to the students.` +
            (result.undecided > 0
              ? ` ${formatCount(result.undecided, 'grade')} without a passed / not passed decision ${result.undecided === 1 ? 'was' : 'were'} not published.`
              : ''),
        ),
    })
  }

  return (
    <section className="teacher-panel grade-publishing" aria-labelledby="grade-publishing-title">
      <h2 id="grade-publishing-title" className="teacher-panel-title">
        Publishing grades
      </h2>
      <p className="grade-publishing-status">
        A grade is visible to the student from its visible date, which you set when you grade it, or from when you
        publish it.
      </p>
      <div className="form-actions">
        <button
          type="button"
          className="button-primary"
          onClick={() => void publishAll()}
          disabled={!unpublished || releaseAll.isPending}
        >
          {releaseAll.isPending ? 'Publishing…' : `Publish all${unpublished ? ` (${unpublished})` : ''}`}
        </button>
        <span className="form-hint">
          {unpublished === undefined
            ? '…'
            : unpublished === 0
              ? 'No saved grades are waiting to be published.'
              : `${unpublished} saved ${unpublished === 1 ? 'grade is' : 'grades are'} not visible to the students yet.`}
        </span>
      </div>
      <FormError>{releaseAll.error && 'Could not publish the grades. Try again.'}</FormError>
      <FormSuccess>{published}</FormSuccess>
      {confirmDialog}
    </section>
  )
}
