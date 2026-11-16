import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { formatDateTime } from '../../../../shared/lib/dateFormat'
import { safeHttpUrl } from '../../../../shared/lib/safeUrl'
import { Breadcrumbs } from '../../../../shared/navigation/Trail'
import { paths } from '../../../../shared/paths'
import PageHeader from '../../../../shared/ui/PageHeader'
import QueryState from '../../../../shared/ui/QueryState'
import { useContentSubmissions, useGradeSubmission, usePublishSubmissionGrade } from '../../../assignments/hooks'
import type { ContentSubmission } from '../../../assignments/types'
import { useContent, useCourseContent } from '../../../content/hooks'
import type { ContentId } from '../../../../shared/types'
import { useCourseView } from '../../courseContext'
import '../../../../shared/styles/forms.css'
import '../ContentView/SubmittableContent.css'
import './SubmissionGradingPage.css'

/** Quick choices for an approve/not approve grade; any other grade can be typed. */
const QUICK_GRADES = ['Passed', 'Not passed']

/**
 * Teachers: one student's submission with a form to grade it and give feedback.
 * Saving keeps the grade from the student; publishing shows it to them.
 */
export default function SubmissionGradingPage() {
  const { contentId = '', submissionId = '' } = useParams()
  const { course, modules, isTeacher } = useCourseView()
  const content = useCourseContent(course.id, modules.map(module => module.id))
  const item = modules.flatMap(module => content.byModule.get(String(module.id)) ?? [])
    .find(entry => String(entry.id) === contentId)
  const details = useContent(item?.type, contentId)
  const submissions = useContentSubmissions(contentId)

  if (!isTeacher) return <p role="alert">Only teachers of {course.title} can grade its submissions.</p>

  const all = submissions.data ?? []
  const index = all.findIndex(s => String(s.id) === submissionId)
  const submission = index >= 0 ? all[index] : undefined
  const deadline = details.data && (details.data.type === 'ASSIGNMENT' || details.data.type === 'CODING_EXERCISE')
    ? details.data.deadline
    : undefined

  const crumbs = [
    { label: course.title, to: paths.course(course.id) },
    { label: 'Submissions', to: paths.courseSection(course.id, 'submissions') },
    { label: item?.title ?? '…', to: paths.courseSectionContent(course.id, 'submissions', contentId) },
    { label: submission ? `${submission.student.firstName} ${submission.student.lastName}` : '…' },
  ]

  return (
    <>
      <Breadcrumbs items={crumbs} />
      <QueryState query={submissions} loading="Loading the submission…" error="Could not load the submission.">
        {() => !submission
          ? <p role="alert">This submission does not exist.</p>
          : (
            <>
              <PageHeader
                title={`${submission.student.firstName} ${submission.student.lastName}`}
                kicker={item?.title ?? 'Submission'}
              />
              <div className="submittable">
                <div className="submittable-main">
                  <Answer submission={submission} />
                  <GradeForm key={submission.id} submission={submission} contentId={contentId} />
                  <nav className="grading-nav" aria-label="Other submissions">
                    {index > 0
                      ? <Link to={paths.courseSubmission(course.id, contentId, all[index - 1].id)}>‹ Previous</Link>
                      : <span />}
                    <Link to={paths.courseSectionContent(course.id, 'submissions', contentId)}>All submissions</Link>
                    {index < all.length - 1
                      ? <Link to={paths.courseSubmission(course.id, contentId, all[index + 1].id)}>Next ›</Link>
                      : <span />}
                  </nav>
                </div>
                <aside className="submittable-card submittable-panel" aria-label="Submission details">
                  <Facts submission={submission} deadline={deadline} attempts={all.filter(s =>
                    String(s.student.userId) === String(submission.student.userId)).length} />
                </aside>
              </div>
            </>
          )}
      </QueryState>
    </>
  )
}

/** What the student handed in: the text, or the repository as a link. */
function Answer({ submission }: { submission: ContentSubmission }) {
  const url = submission.type === 'GIT_URL' ? safeHttpUrl(submission.payload) : undefined
  return (
    <section className="submittable-card" aria-labelledby="answer-title">
      <h2 id="answer-title" className="submittable-card-title">Submission</h2>
      <div className="submittable-answer">
        <p className="submittable-label">
          {submission.type === 'GIT_URL' ? 'Repository' : 'Answer'}
          <span className="submittable-muted"> · {formatDateTime(new Date(submission.submittedAt))}</span>
        </p>
        {submission.type === 'GIT_URL'
          ? (url ? <a href={url} target="_blank" rel="noreferrer">{url}</a> : <span>{submission.payload}</span>)
          : <div className="submittable-text">{submission.payload}</div>}
      </div>
    </section>
  )
}

type FactsProps = {
  submission: ContentSubmission
  /** undefined while loading; null for no deadline. */
  deadline: string | null | undefined
  /** How many times this student has handed in this item. */
  attempts: number
}

function Facts({ submission, deadline, attempts }: FactsProps) {
  const grade = submission.grade
  return (
    <dl className="submittable-facts">
      <dt>Student</dt>
      <dd>
        {submission.student.firstName} {submission.student.lastName}
        <br />
        <a className="submittable-muted" href={`mailto:${submission.student.email}`}>{submission.student.email}</a>
      </dd>
      {submission.group && (
        <>
          <dt>Handed in for group</dt>
          <dd>
            <strong>{submission.group.name}</strong>
            <ul className="submittable-members">
              {submission.group.members.map(member => (
                <li key={member.userId}>{member.firstName} {member.lastName}</li>
              ))}
            </ul>
          </dd>
        </>
      )}
      <dt>Submitted</dt>
      <dd>
        {formatDateTime(new Date(submission.submittedAt))}
        {submission.late && <span className="submittable-late"> (late)</span>}
        {attempts > 1 && <span className="submittable-muted"> · {attempts} submissions by this student</span>}
      </dd>
      <dt>Deadline</dt>
      <dd>{deadline === undefined ? '…' : deadline ? formatDateTime(new Date(deadline)) : 'No deadline'}</dd>
      <dt>Grade</dt>
      <dd>
        {grade ? <strong>{grade.value}</strong> : 'Not graded yet'}
        {grade && (
          <span className="submittable-muted">
            <br />
            {grade.publishedAt
              ? `Published ${formatDateTime(new Date(grade.publishedAt))}`
              : 'Saved, not published to the student'}
          </span>
        )}
      </dd>
    </dl>
  )
}

/** The grade (a quick choice or typed) and feedback; save, or save and publish. */
function GradeForm({ submission, contentId }: { submission: ContentSubmission, contentId: ContentId }) {
  const save = useGradeSubmission()
  const publish = usePublishSubmissionGrade()
  const [grade, setGrade] = useState(submission.grade?.value ?? '')
  const [feedback, setFeedback] = useState(submission.grade?.feedback ?? '')
  const [done, setDone] = useState<'saved' | 'published' | null>(null)
  const busy = save.isPending || publish.isPending
  const failed = save.error || publish.error

  async function submit(andPublish: boolean) {
    setDone(null)
    try {
      await save.mutateAsync({ submissionId: submission.id, contentId, grade: grade.trim(), feedback: feedback.trim() })
      if (andPublish) await publish.mutateAsync({ submissionId: submission.id, contentId })
      setDone(andPublish ? 'published' : 'saved')
    } catch {
      // Shown below from the mutations' errors.
    }
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    void submit(true)
  }

  return (
    <section className="submittable-card" aria-labelledby="grade-title">
      <h2 id="grade-title" className="submittable-card-title">Grade and feedback</h2>
      <form className="form grading-form" onSubmit={handleSubmit}>
        <fieldset className="grading-choices">
          <legend className="form-label">Grade</legend>
          <div className="grading-quick">
            {QUICK_GRADES.map(choice => (
              <button
                key={choice}
                type="button"
                className={`grading-choice${grade === choice ? ' is-selected' : ''}`}
                aria-pressed={grade === choice}
                onClick={() => setGrade(choice)}
              >
                {choice}
              </button>
            ))}
          </div>
          <label className="grading-other">
            <span className="form-hint">Or another grade, e.g. A–F</span>
            <input type="text" value={grade} onChange={event => setGrade(event.target.value)} maxLength={50} />
          </label>
        </fieldset>

        <div className="form-field">
          <label htmlFor="grading-feedback">Feedback to the student</label>
          <textarea id="grading-feedback" rows={6} value={feedback} onChange={event => setFeedback(event.target.value)} />
        </div>

        {failed && <p className="form-error" role="alert">Could not save the grade. Try again.</p>}
        {done && (
          <p className="form-success" role="status">
            {done === 'published' ? 'Saved and published to the student.' : 'Saved. The student cannot see it until you publish it.'}
          </p>
        )}

        <div className="form-actions">
          <button type="submit" className="button-primary" disabled={busy || grade.trim() === ''}>
            {publish.isPending ? 'Publishing…' : 'Save and publish'}
          </button>
          <button type="button" className="button-secondary" disabled={busy || grade.trim() === ''} onClick={() => void submit(false)}>
            {save.isPending && !publish.isPending ? 'Saving…' : 'Save without publishing'}
          </button>
        </div>
      </form>
    </section>
  )
}
