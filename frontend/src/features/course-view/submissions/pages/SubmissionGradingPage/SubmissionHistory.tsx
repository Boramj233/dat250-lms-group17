import { Link } from 'react-router'
import { formatDateTime } from '@shared/lib/dateFormat'
import { paths } from '@shared/paths'
import type { ContentId, CourseId } from '@shared/types'
import { submissionKindLabel } from '@domain/assignments/submissionStats'
import type { ReviewSubmission } from '@domain/assignments/types'
import { Card } from '../../../components/DetailLayout'

type SubmissionHistoryProps = {
  courseId: CourseId
  contentId: ContentId
  /** Newest first. */
  history: ReviewSubmission[]
  currentId: string
}

/** Every submission by the student (or group) for the item; the shown one is marked. */
export default function SubmissionHistory({ courseId, contentId, history, currentId }: SubmissionHistoryProps) {
  return (
    <Card id="history-title" title={`All submissions (${history.length})`}>
      <ol className="grading-history">
        {history.map((entry, i) => (
          <li key={entry.id} className={String(entry.id) === currentId ? 'is-current' : undefined}>
            {String(entry.id) === currentId ? (
              <strong>{formatDateTime(new Date(entry.submittedAt))}</strong>
            ) : (
              <Link to={paths.courseSubmission(courseId, contentId, entry.id)}>
                {formatDateTime(new Date(entry.submittedAt))}
              </Link>
            )}
            <span className="text-meta">
              {' · '}
              {submissionKindLabel(entry)}
              {entry.late && ' · late'}
              {i === 0 && ' · latest'}
            </span>
          </li>
        ))}
      </ol>
    </Card>
  )
}
