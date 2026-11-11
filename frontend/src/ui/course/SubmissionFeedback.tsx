import { useCurrentUser } from '../../data/auth/useAuth'
import type { ContentId } from '../../data/shared/types'
import { useMySubmissionHistory } from '../../data/submission/hooks/queries/useMySubmissionHistory'
import { useSubmissionResult } from '../../data/submission/hooks/queries/useSubmissionResult'
import { formatDateTime } from '../components/dateFormat'
import { safeHttpUrl } from '../components/safeUrl'
import './SubmissionFeedback.css'

/**
 * For an assignment or coding exercise: the user's latest submission, and the grade
 * and the teacher's comment on it once they are published.
 */
export default function SubmissionFeedback({ contentId }: { contentId: ContentId }) {
  const user = useCurrentUser().data!
  const history = useMySubmissionHistory(contentId)
  const latest = history.data?.[0]
  const result = useSubmissionResult(latest?.id, user.userId)
  const grade = result.data?.grade

  return (
    <section className="submission-feedback" aria-labelledby="submission-feedback-title">
      <h2 id="submission-feedback-title" className="course-section-title">Your submission</h2>

      {history.isPending && <p role="status">Loading your submission…</p>}
      {history.error && <p role="alert">Could not load your submission.</p>}
      {history.data?.length === 0 && <p>You have not submitted anything yet.</p>}

      {latest && (
        <>
          <dl className="content-facts">
            <div>
              <dt>Submitted</dt>
              <dd>
                <time dateTime={latest.submittedAt}>{formatDateTime(new Date(latest.submittedAt))}</time>
                {latest.late && <span className="submission-late"> (late)</span>}
              </dd>
            </div>
            {history.data!.length > 1 && (
              <div>
                <dt>Submissions</dt>
                <dd>{history.data!.length}, the latest is shown</dd>
              </div>
            )}
            <div className="submission-payload">
              <dt>{latest.type === 'GIT_URL' ? 'Repository' : 'Answer'}</dt>
              <dd><SubmittedPayload type={latest.type} payload={latest.payload} /></dd>
            </div>
          </dl>

          <div className="grade-box">
            <div className="grade-box-row">
              <span className="grade-box-label">Grade</span>
              {/* A missing result (no grading endpoint yet) reads the same as "not graded". */}
              <span className="grade-box-value">
                {result.isPending ? '…' : grade ? grade.value : 'Not graded yet'}
              </span>
            </div>
            <div className="grade-box-comment">
              <span className="grade-box-label">Comment from the teacher</span>
              {grade?.feedback
                ? <p className="content-text">{grade.feedback}</p>
                : <p className="grade-box-empty">{grade ? 'No comment.' : 'No comment yet.'}</p>}
            </div>
            {grade?.publishedAt && (
              <p className="grade-box-published">
                Published <time dateTime={grade.publishedAt}>{formatDateTime(new Date(grade.publishedAt))}</time>
              </p>
            )}
          </div>
        </>
      )}
    </section>
  )
}

function SubmittedPayload({ type, payload }: { type: 'TEXT' | 'GIT_URL', payload: string }) {
  if (type === 'GIT_URL') {
    const url = safeHttpUrl(payload)
    return url ? <a href={url} target="_blank" rel="noreferrer">{url}</a> : <>{payload}</>
  }
  // Plain text: React escapes it.
  return <span className="content-text">{payload}</span>
}
