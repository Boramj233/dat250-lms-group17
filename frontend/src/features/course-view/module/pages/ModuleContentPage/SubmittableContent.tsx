import { useState, type FormEvent, type ReactNode } from 'react'
import { useCurrentUser } from '../../../../../shared/auth/useAuth'
import { formatDateTime, formatTimeLeft } from '../../../../../shared/lib/dateFormat'
import { safeHttpUrl } from '../../../../../shared/lib/safeUrl'
import type { UserId } from '../../../../../shared/types'
import { useMySubmissionHistory, useSubmissionResult, useSubmitMySubmission } from '../../../../assignments/hooks'
import type { MySubmission, SubmissionGroup } from '../../../../assignments/types'
import type { AssignmentContent, CodingExerciseContent } from '../../../../content/types'
import '../../../../../shared/styles/forms.css'
import './SubmittableContent.css'

type Submittable = AssignmentContent | CodingExerciseContent

type SubmittableContentProps = {
  content: Submittable
  /** Teachers see the task and the deadline, but do not hand in. */
  isTeacher: boolean
}

/**
 * An assignment or coding exercise: the task and the student's submission as cards on
 * the left, a details panel (deadline, submitted, group, grade, feedback) on the right.
 */
export default function SubmittableContent({ content, isTeacher }: SubmittableContentProps) {
  const [now] = useState(() => new Date())
  const deadline = content.deadline ? new Date(content.deadline) : null

  if (!isTeacher) return <StudentSubmission content={content} deadline={deadline} now={now} />
  return (
    <Layout
      main={
        <>
          <Task content={content} />
          <p className="submittable-note">
            Students hand in their answer here. You see and grade submissions under Manage.
          </p>
        </>
      }
      panel={<DeadlineFact deadline={deadline} allowLate={content.allowLateSubmission} now={now} />}
    />
  )
}

/** The cards on the left, the details panel on the right (on top on narrow screens). */
function Layout({ main, panel }: { main: ReactNode, panel: ReactNode }) {
  return (
    <div className="submittable">
      <div className="submittable-main">{main}</div>
      <aside className="submittable-card submittable-panel" aria-label="Submission details">{panel}</aside>
    </div>
  )
}

/** A bordered box with a heading, for the task and the submission. */
function Card({ id, title, children }: { id: string, title: string, children: ReactNode }) {
  return (
    <section className="submittable-card" aria-labelledby={id}>
      <h2 id={id} className="submittable-card-title">{title}</h2>
      {children}
    </section>
  )
}

/** The description of the task, and for a coding exercise where to start from. */
function Task({ content }: { content: Submittable }) {
  const repository = content.type === 'CODING_EXERCISE' ? safeHttpUrl(content.starterRepositoryUrl) : undefined
  return (
    <Card id="task-title" title="Description">
      <div className="submittable-text">{content.instructions || 'No description.'}</div>
      {repository && (
        <p>Starter repository: <a href={repository} target="_blank" rel="noreferrer">{repository}</a></p>
      )}
      {content.type === 'CODING_EXERCISE' && content.starterCode && (
        <div className="submittable-starter">
          <p className="submittable-label">Starter code</p>
          <pre className="submittable-code"><code>{content.starterCode}</code></pre>
        </div>
      )}
    </Card>
  )
}

type StudentSubmissionProps = {
  content: Submittable
  deadline: Date | null
  now: Date
}

/** The student's view: the task, their submission with the button below, and the panel. */
function StudentSubmission({ content, deadline, now }: StudentSubmissionProps) {
  const user = useCurrentUser().data!
  const history = useMySubmissionHistory(content.id)
  const latest = history.data?.[0]
  // Grade and feedback for the latest submission. Proposed endpoint: until the
  // backend has it, this fails and the panel says "Not graded yet".
  const result = useSubmissionResult(latest?.id, user.userId)
  const [writing, setWriting] = useState(false)
  const closed = deadline !== null && deadline < now && !content.allowLateSubmission
  const isGroupAssignment = content.type === 'ASSIGNMENT' && content.groupAssignment === true

  return (
    <Layout
      main={
        <>
          <Task content={content} />
          <Card id="submission-title" title="Your submission">
            {isGroupAssignment && (
              <p className="submittable-note">
                Group assignment: one submission counts for everyone in your group.
              </p>
            )}
            {history.isPending && <p role="status">Loading your submissions…</p>}
            {history.error && <p role="alert">Could not load your submissions.</p>}

            {writing
              ? <SubmissionForm content={content} onDone={() => setWriting(false)} onCancel={() => setWriting(false)} />
              : history.data && (
                <>
                  {latest
                    ? <SubmittedAnswer submission={latest} />
                    : <p className="submittable-empty">Nothing handed in yet.</p>}
                  <div className="submittable-actions">
                    {closed
                      ? <p>The deadline has passed. Late submissions are not accepted.</p>
                      : (
                        <button type="button" className="button-primary" onClick={() => setWriting(true)}>
                          {latest ? 'Submit again' : 'Start submission'}
                        </button>
                      )}
                  </div>
                </>
              )}
          </Card>
        </>
      }
      panel={
        <>
          <DeadlineFact deadline={deadline} allowLate={content.allowLateSubmission} now={now} />
          <dl className="submittable-facts">
            <dt>Submitted</dt>
            <dd>
              {latest
                ? <>
                    {formatDateTime(new Date(latest.submittedAt))}
                    {latest.late && <span className="submittable-late"> (late)</span>}
                  </>
                : 'Not submitted yet'}
              {history.data && history.data.length > 1 && (
                <span className="submittable-muted"> · {history.data.length} submissions</span>
              )}
            </dd>
            {latest?.group && (
              <>
                <dt>Submitted by group</dt>
                <dd><GroupFact group={latest.group} userId={user.userId} /></dd>
              </>
            )}
            <dt>Grade</dt>
            <dd>{result.data?.grade ? <strong>{result.data.grade.value}</strong> : latest ? 'Not graded yet' : '–'}</dd>
            {result.data?.grade?.feedback && (
              <>
                <dt>Feedback from the teacher</dt>
                <dd className="submittable-text">{result.data.grade.feedback}</dd>
              </>
            )}
          </dl>
        </>
      }
    />
  )
}

function DeadlineFact({ deadline, allowLate, now }: { deadline: Date | null, allowLate: boolean, now: Date }) {
  return (
    <dl className="submittable-facts">
      <dt>Deadline</dt>
      <dd>
        {deadline
          ? <>
              {formatDateTime(deadline)}
              <span className="submittable-muted">
                {' · '}{deadline < now ? 'passed' : formatTimeLeft(deadline, now)}
              </span>
              <br />
              <span className="submittable-muted">
                {allowLate ? 'Late submissions are accepted' : 'Late submissions are not accepted'}
              </span>
            </>
          : 'No deadline'}
      </dd>
    </dl>
  )
}

/** The group's name and its members, with the logged-in user marked. */
function GroupFact({ group, userId }: { group: SubmissionGroup, userId: UserId }) {
  return (
    <>
      <strong>{group.name}</strong>
      <ul className="submittable-members" aria-label={`Members of ${group.name}`}>
        {group.members.map(member => (
          <li key={member.userId}>
            {member.firstName} {member.lastName}
            {String(member.userId) === String(userId) && <span className="submittable-muted"> (you)</span>}
          </li>
        ))}
      </ul>
    </>
  )
}

/** What was handed in last, in its own box: the text, or the repository as a link. */
function SubmittedAnswer({ submission }: { submission: MySubmission }) {
  const url = submission.type === 'GIT_URL' ? safeHttpUrl(submission.payload) : undefined
  return (
    <div className="submittable-answer">
      <p className="submittable-label">
        {submission.type === 'GIT_URL' ? 'Repository' : 'Answer'}
        <span className="submittable-muted"> · {formatDateTime(new Date(submission.submittedAt))}</span>
      </p>
      {submission.type === 'GIT_URL'
        ? (url ? <a href={url} target="_blank" rel="noreferrer">{url}</a> : <span>{submission.payload}</span>)
        : <div className="submittable-text">{submission.payload}</div>}
    </div>
  )
}

type SubmissionFormProps = {
  content: Submittable
  onDone: () => void
  onCancel: () => void
}

/**
 * The answer in the format the backend accepts: a text for an assignment, a link to a
 * Git repository for a coding exercise.
 */
function SubmissionForm({ content, onDone, onCancel }: SubmissionFormProps) {
  const submit = useSubmitMySubmission()
  const [value, setValue] = useState('')
  const isCode = content.type === 'CODING_EXERCISE'
  const answer = value.trim()
  const valid = isCode ? safeHttpUrl(answer) !== undefined : answer !== ''

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!valid) return
    submit.mutate(
      { contentId: content.id, type: isCode ? 'GIT_URL' : 'TEXT', payload: answer },
      { onSuccess: onDone },
    )
  }

  return (
    <form className="form submittable-form" onSubmit={handleSubmit}>
      <div className="form-field">
        <label htmlFor="submission-answer">{isCode ? 'Repository URL' : 'Your answer'}</label>
        <p id="submission-hint" className="form-hint">
          {isCode
            ? 'A link to your Git repository, starting with https://, e.g. https://github.com/you/project.'
            : 'Write your answer as text (at most 10 000 characters).'}
        </p>
        {isCode
          ? <input id="submission-answer" type="url" value={value} aria-describedby="submission-hint"
              onChange={event => setValue(event.target.value)} autoFocus />
          : <textarea id="submission-answer" rows={10} maxLength={10000} value={value}
              aria-describedby="submission-hint" onChange={event => setValue(event.target.value)} autoFocus />}
      </div>
      {submit.error && <p className="form-error" role="alert">Could not submit your answer. Try again.</p>}
      <div className="form-actions">
        <button type="submit" className="button-primary" disabled={!valid || submit.isPending}>
          {submit.isPending ? 'Submitting…' : 'Submit'}
        </button>
        <button type="button" className="button-secondary" onClick={onCancel} disabled={submit.isPending}>
          Cancel
        </button>
      </div>
    </form>
  )
}
