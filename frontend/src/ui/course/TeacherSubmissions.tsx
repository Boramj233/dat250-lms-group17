import { useState, type FormEvent } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { gradeKeys } from '../../data/grades/queryKeys'
import type { ContentId } from '../../data/shared/types'
import { useGradeSubmission } from '../../data/submission/hooks/mutations/useGradeSubmission'
import { usePublishSubmissionGrade } from '../../data/submission/hooks/mutations/usePublishSubmissionGrade'
import { useContentSubmissions } from '../../data/submission/hooks/queries/useContentSubmissions'
import { submissionKeys } from '../../data/submission/queryKeys'
import type { ContentSubmission } from '../../data/submission/types'
import { formatDateTime } from '../components/dateFormat'
import { safeHttpUrl } from '../components/safeUrl'
import '../components/forms.css'
import './TeacherSubmissions.css'

/**
 * Teachers: every student's latest submission for an assignment or coding exercise,
 * with a form to set the grade and comment and publish them to the student.
 */
export default function TeacherSubmissions({ contentId }: { contentId: ContentId }) {
  const submissions = useContentSubmissions(contentId)
  const sorted = [...(submissions.data ?? [])].sort((a, b) =>
    Number(Boolean(a.grade?.publishedAt)) - Number(Boolean(b.grade?.publishedAt))
    || a.student.lastName.localeCompare(b.student.lastName))
  const awaiting = sorted.filter(s => !s.grade?.publishedAt).length

  return (
    <section className="teacher-panel" aria-labelledby="teacher-submissions-title">
      <h2 id="teacher-submissions-title" className="teacher-panel-title">
        Submissions <span className="teacher-tag">Teacher</span>
      </h2>
      {submissions.isPending && <p role="status">Loading submissions…</p>}
      {submissions.error && <p role="alert">Could not load the submissions.</p>}
      {submissions.data?.length === 0 && <p>No student has submitted yet.</p>}
      {sorted.length > 0 && (
        <>
          <p className="form-hint">{sorted.length} submitted, {awaiting} awaiting a published grade.</p>
          <ul className="teacher-submission-list">
            {sorted.map(submission => (
              <li key={submission.id}><SubmissionRow contentId={contentId} submission={submission} /></li>
            ))}
          </ul>
        </>
      )}
    </section>
  )
}

function SubmissionRow({ contentId, submission }: { contentId: ContentId, submission: ContentSubmission }) {
  const queryClient = useQueryClient()
  const grade = useGradeSubmission()
  const publish = usePublishSubmissionGrade()
  const [value, setValue] = useState(submission.grade?.value ?? '')
  const [feedback, setFeedback] = useState(submission.grade?.feedback ?? '')
  const [saved, setSaved] = useState(false)
  const busy = grade.isPending || publish.isPending
  const { student } = submission
  const url = submission.type === 'GIT_URL' ? safeHttpUrl(submission.payload) : undefined

  async function submit(event: FormEvent) {
    event.preventDefault()
    setSaved(false)
    try {
      await grade.mutateAsync({ submissionId: submission.id, grade: value.trim(), feedback: feedback.trim() || undefined })
      await publish.mutateAsync(submission.id)
      setSaved(true)
    } catch {
      // Shown through grade.error / publish.error.
    } finally {
      // The list and everyone's grade pages show the new grade.
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: submissionKeys.contentSubmissions(contentId) }),
        queryClient.invalidateQueries({ queryKey: gradeKeys.all }),
      ])
    }
  }

  return (
    <article className="teacher-submission">
      <header className="teacher-submission-header">
        <div className="teacher-submission-student">
          <span className="teacher-submission-name">{student.firstName} {student.lastName}</span>
          <span className="teacher-submission-meta">
            {student.email} · Submitted{' '}
            <time dateTime={submission.submittedAt}>{formatDateTime(new Date(submission.submittedAt))}</time>
            {submission.late && <strong> (late)</strong>}
          </span>
        </div>
        <span className="teacher-submission-status">
          {submission.grade?.publishedAt ? `Published: ${submission.grade.value}` : 'Awaiting grade'}
        </span>
      </header>

      <div className="teacher-submission-payload">
        {url
          ? <a href={url} target="_blank" rel="noreferrer">{url}</a>
          : <span className="content-text">{submission.payload}</span>}
      </div>

      <form className="form teacher-grade-form" onSubmit={submit}>
        <div className="form-row">
          <div className="form-field teacher-grade-value">
            <label htmlFor={`grade-${submission.id}`}>Grade</label>
            <input id={`grade-${submission.id}`} required value={value}
              onChange={event => { setValue(event.target.value); setSaved(false) }}
              placeholder="e.g. Passed or A" />
          </div>
          <div className="form-field">
            <label htmlFor={`feedback-${submission.id}`}>Comment to the student</label>
            <textarea id={`feedback-${submission.id}`} value={feedback} rows={3}
              onChange={event => { setFeedback(event.target.value); setSaved(false) }} />
          </div>
        </div>
        {(grade.error || publish.error) && (
          <p className="form-error" role="alert">The grade could not be saved. Try again.</p>
        )}
        <div className="form-actions">
          <button type="submit" className="button-primary" disabled={busy}>
            {busy ? 'Saving…' : submission.grade?.publishedAt ? 'Update and publish' : 'Save and publish'}
          </button>
          {saved && <span className="form-success" role="status">Published to the student.</span>}
        </div>
      </form>
    </article>
  )
}
