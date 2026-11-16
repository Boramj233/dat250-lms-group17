import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { ApiError } from '../../../../shared/api/apiClient'
import { formatDateTime } from '../../../../shared/lib/dateFormat'
import { Breadcrumbs } from '../../../../shared/navigation/Trail'
import { paths } from '../../../../shared/paths'
import type { ContentId, CourseId } from '../../../../shared/types'
import PageHeader from '../../../../shared/ui/PageHeader'
import QueryState from '../../../../shared/ui/QueryState'
import { useGradeReview, useReleaseGrade, useReviewHistory, useReviewSubmission, useReviewSubmissions, useSaveGrade } from '../../../assignments/hooks'
import { formatScore, MAX_SCORE } from '../../../assignments/scoreFormat'
import type { GradeReview, ReviewSubmission } from '../../../assignments/types'
import { useContent, useCourseContent } from '../../../content/hooks'
import SubmissionAnswer from '../../components/SubmissionAnswer'
import { useCourseView } from '../../courseContext'
import { submissionKindLabel } from '../../submissionStats'
import { usePeopleNames } from '../../usePeopleNames'
import '../../../../shared/styles/forms.css'
import '../ContentView/SubmittableContent.css'
import './SubmissionGradingPage.css'

/**
 * Teachers: one submission with its earlier attempts and a form to grade it (a score
 * from 0 to 100 and feedback). Saving keeps the grade from the student; releasing shows
 * it to them, after which it cannot be changed. Only the latest submission can be graded.
 */
export default function SubmissionGradingPage() {
  const { contentId = '', submissionId = '' } = useParams()
  const { course, modules, isTeacher } = useCourseView()
  const content = useCourseContent(course.id, modules.map(module => module.id))
  const item = modules.flatMap(module => content.byModule.get(String(module.id)) ?? [])
    .find(entry => String(entry.id) === contentId)
  const details = useContent(item?.type, contentId)
  const submission = useReviewSubmission(submissionId)
  const history = useReviewHistory(submissionId)
  const grade = useGradeReview(submissionId)
  // The latest submissions of the item, for Previous / Next.
  const latest = useReviewSubmissions(contentId)
  const people = usePeopleNames(course.id)

  if (!isTeacher) return <p role="alert">Only teachers of {course.title} can grade its submissions.</p>

  const studentName = submission.data ? people.nameOf(submission.data.studentUserId) : '…'
  const crumbs = [
    { label: course.title, to: paths.course(course.id) },
    { label: 'Submissions', to: paths.courseSection(course.id, 'submissions') },
    { label: item?.title ?? '…', to: paths.courseSectionContent(course.id, 'submissions', contentId) },
    { label: studentName },
  ]
  const deadline = details.data && (details.data.type === 'ASSIGNMENT' || details.data.type === 'CODING_EXERCISE')
    ? details.data.deadline
    : undefined
  // The newest submission in the history is the one that can be graded.
  const newest = history.data?.[0]
  const isLatest = newest ? String(newest.id) === submissionId : undefined
  const list = latest.data ?? []
  const index = list.findIndex(s => String(s.id) === submissionId)

  return (
    <>
      <Breadcrumbs items={crumbs} />
      <QueryState query={submission} loading="Loading the submission…" error="Could not load the submission.">
        {data => (
          <>
            <PageHeader title={studentName} kicker={item?.title ?? 'Submission'} />
            <div className="submittable">
              <div className="submittable-main">
                <section className="submittable-card" aria-labelledby="answer-title">
                  <h2 id="answer-title" className="submittable-card-title">Submission</h2>
                  <SubmissionAnswer submission={data} />
                </section>

                {grade.isPending || isLatest === undefined
                  ? <p role="status">Loading the grade…</p>
                  : grade.error
                    ? <p role="alert">Could not load the grade.</p>
                    : (
                      <GradeCard
                        key={data.id}
                        submission={data}
                        contentId={contentId}
                        courseId={course.id}
                        grade={grade.data ?? null}
                        isLatest={isLatest}
                        newestId={newest?.id}
                      />
                    )}

                {history.data && history.data.length > 1 && (
                  <History courseId={course.id} contentId={contentId} history={history.data} currentId={submissionId} />
                )}

                <nav className="grading-nav" aria-label="Other submissions">
                  {index > 0
                    ? <Link to={paths.courseSubmission(course.id, contentId, list[index - 1].id)}>‹ Previous</Link>
                    : <span />}
                  <Link to={paths.courseSectionContent(course.id, 'submissions', contentId)}>All submissions</Link>
                  {index >= 0 && index < list.length - 1
                    ? <Link to={paths.courseSubmission(course.id, contentId, list[index + 1].id)}>Next ›</Link>
                    : <span />}
                </nav>
              </div>

              <aside className="submittable-card submittable-panel" aria-label="Submission details">
                <dl className="submittable-facts">
                  <dt>Student</dt>
                  <dd>
                    {studentName}
                    {people.members?.find(m => String(m.userId) === String(data.studentUserId))?.email && (
                      <>
                        <br />
                        <span className="submittable-muted">
                          {people.members.find(m => String(m.userId) === String(data.studentUserId))!.email}
                        </span>
                      </>
                    )}
                  </dd>
                  {data.groupId !== null && (
                    <>
                      <dt>Handed in for group</dt>
                      <dd>
                        <strong>{people.groupLabel(data.groupId)}</strong>
                        <ul className="submittable-members">
                          {data.members.map(member => <li key={member.userId}>{people.nameOf(member.userId)}</li>)}
                        </ul>
                      </dd>
                    </>
                  )}
                  <dt>Submitted</dt>
                  <dd>
                    {formatDateTime(new Date(data.submittedAt))}
                    {data.late && <span className="submittable-late"> (late)</span>}
                    {history.data && history.data.length > 1 && (
                      <span className="submittable-muted"> · {history.data.length} submissions in all</span>
                    )}
                  </dd>
                  <dt>Deadline</dt>
                  <dd>{deadline === undefined ? '…' : deadline ? formatDateTime(new Date(deadline)) : 'No deadline'}</dd>
                  <dt>Score</dt>
                  <dd>
                    {grade.data ? <strong>{formatScore(grade.data.score)}</strong> : 'Not graded yet'}
                    {grade.data && (
                      <span className="submittable-muted">
                        <br />
                        {grade.data.releasedAt
                          ? `Released ${formatDateTime(new Date(grade.data.releasedAt))}`
                          : 'Saved, not released to the student'}
                      </span>
                    )}
                  </dd>
                </dl>
              </aside>
            </div>
          </>
        )}
      </QueryState>
    </>
  )
}

/** A message for a failed save or release, from the backend's status code. */
function gradeError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 409) return 'The grade was changed in the meantime, it is already released, or a newer submission exists. Reload the page and try again.'
    if (error.status === 400) return `The score must be a number from 0 to ${MAX_SCORE}, with at most two decimals.`
    if (error.status === 403) return 'Only active teachers of the course can grade.'
  }
  return 'Could not save the grade. Try again.'
}

type GradeCardProps = {
  submission: ReviewSubmission
  contentId: ContentId
  courseId: CourseId
  /** The current grade; null when not graded yet. */
  grade: GradeReview | null
  /** Only the latest submission of the student or group can be graded. */
  isLatest: boolean
  newestId?: ReviewSubmission['id']
}

/**
 * The grade: a form while it can be changed, otherwise what was given. A released
 * grade cannot be changed, and an earlier submission cannot be graded.
 */
function GradeCard({ submission, contentId, courseId, grade, isLatest, newestId }: GradeCardProps) {
  const save = useSaveGrade()
  const release = useReleaseGrade()
  const [score, setScore] = useState(grade ? String(grade.score) : '')
  const [feedback, setFeedback] = useState(grade?.feedback ?? '')
  const [done, setDone] = useState<'saved' | 'released' | null>(null)
  const busy = save.isPending || release.isPending
  const failed = save.error ?? release.error

  const number = Number(score.replace(',', '.'))
  const scoreValid = score.trim() !== '' && Number.isFinite(number) && number >= 0 && number <= MAX_SCORE
    && Math.round(number * 100) === number * 100

  if (grade?.releasedAt) {
    return (
      <section className="submittable-card" aria-labelledby="grade-title">
        <h2 id="grade-title" className="submittable-card-title">Grade and feedback</h2>
        <p className="grading-score">{formatScore(grade.score)}</p>
        <div className="submittable-text">{grade.feedback || <span className="submittable-muted">No feedback.</span>}</div>
        <p className="submittable-note">Released to the student {formatDateTime(new Date(grade.releasedAt))}. A released grade cannot be changed.</p>
      </section>
    )
  }

  if (!isLatest) {
    return (
      <section className="submittable-card" aria-labelledby="grade-title">
        <h2 id="grade-title" className="submittable-card-title">Grade and feedback</h2>
        <p className="submittable-note">
          A newer submission exists. Only the latest submission can be graded.
          {newestId !== undefined && <> <Link to={paths.courseSubmission(courseId, contentId, newestId)}>Go to the latest</Link></>}
        </p>
      </section>
    )
  }

  async function submit(andRelease: boolean) {
    setDone(null)
    try {
      const saved = await save.mutateAsync({
        submissionId: submission.id,
        contentId,
        score: number,
        feedback: feedback.trim(),
        version: grade?.version ?? null,
      })
      if (andRelease) await release.mutateAsync({ submissionId: submission.id, contentId, version: saved.version })
      setDone(andRelease ? 'released' : 'saved')
    } catch {
      // Shown below from the mutations' errors.
    }
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    void submit(false)
  }

  return (
    <section className="submittable-card" aria-labelledby="grade-title">
      <h2 id="grade-title" className="submittable-card-title">Grade and feedback</h2>
      <form className="form grading-form" onSubmit={handleSubmit}>
        <div className="form-field grading-score-field">
          <label htmlFor="grading-score">Score (0–{MAX_SCORE})</label>
          <div className="grading-score-input">
            <input id="grading-score" type="number" inputMode="decimal" min={0} max={MAX_SCORE} step={0.01}
              value={score} onChange={event => setScore(event.target.value)} required />
            <span className="submittable-muted">/ {MAX_SCORE}</span>
          </div>
          {score.trim() !== '' && !scoreValid && (
            <p className="form-error" role="alert">A number from 0 to {MAX_SCORE}, with at most two decimals.</p>
          )}
        </div>

        <div className="form-field">
          <label htmlFor="grading-feedback">Feedback to the student</label>
          <textarea id="grading-feedback" rows={6} maxLength={10000} value={feedback}
            onChange={event => setFeedback(event.target.value)} />
        </div>

        {failed && <p className="form-error" role="alert">{gradeError(failed)}</p>}
        {done && (
          <p className="form-success" role="status">
            {done === 'released' ? 'Saved and released to the student.' : 'Saved. The student cannot see it until you release it.'}
          </p>
        )}

        <div className="form-actions">
          <button type="submit" className="button-primary" disabled={busy || !scoreValid}>
            {save.isPending && !release.isPending ? 'Saving…' : 'Save'}
          </button>
          <button type="button" className="button-secondary" disabled={busy || !scoreValid} onClick={() => void submit(true)}>
            {release.isPending ? 'Releasing…' : 'Save and release to the student'}
          </button>
        </div>
        <p className="submittable-note">A released grade cannot be changed afterwards.</p>
      </form>
    </section>
  )
}

type HistoryProps = {
  courseId: CourseId
  contentId: ContentId
  /** Newest first. */
  history: ReviewSubmission[]
  currentId: string
}

/** Every submission by the student (or group) for the item; the shown one is marked. */
function History({ courseId, contentId, history, currentId }: HistoryProps) {
  return (
    <section className="submittable-card" aria-labelledby="history-title">
      <h2 id="history-title" className="submittable-card-title">All submissions ({history.length})</h2>
      <ol className="grading-history">
        {history.map((entry, i) => (
          <li key={entry.id} className={String(entry.id) === currentId ? 'is-current' : undefined}>
            {String(entry.id) === currentId
              ? <strong>{formatDateTime(new Date(entry.submittedAt))}</strong>
              : <Link to={paths.courseSubmission(courseId, contentId, entry.id)}>{formatDateTime(new Date(entry.submittedAt))}</Link>}
            <span className="submittable-muted">
              {' · '}{submissionKindLabel(entry)}{entry.late && ' · late'}{i === 0 && ' · latest'}
            </span>
          </li>
        ))}
      </ol>
    </section>
  )
}
