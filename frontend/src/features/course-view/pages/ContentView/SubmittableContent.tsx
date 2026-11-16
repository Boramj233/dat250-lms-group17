import { useState, type FormEvent, type ReactNode } from 'react'
import { errorMessage } from '@shared/api/errorMessage'
import { useCurrentUser } from '@shared/auth/useAuth'
import { formatDateTime, formatTimeLeft } from '@shared/lib/dateFormat'
import { safeHttpUrl } from '@shared/lib/safeUrl'
import type { GroupId } from '@shared/types'
import { useMyReleasedGrade, useMySubmissionHistory, useSubmitFile, useSubmitMySubmission } from '@domain/assignments/hooks'
import { formatScore } from '@domain/assignments/scoreFormat'
import type { MySubmission } from '@domain/assignments/types'
import type { AssignmentContent, CodingExerciseContent } from '@domain/content/types'
import PassedBadge from '../../components/PassedBadge'
import SubmissionAnswer from '../../components/SubmissionAnswer'
import { useCourseView } from '../../courseContext'
import { usePeopleNames } from '../../usePeopleNames'
import '@shared/styles/forms.css'
import './SubmittableContent.css'

type Submittable = AssignmentContent | CodingExerciseContent

/** The backend accepts PDF files up to this size (lms.submission.files.max-size). */
const MAX_FILE_BYTES = 10 * 1024 * 1024

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
            Students hand in their answer here. You see and grade submissions under Submissions.
          </p>
        </>
      }
      panel={
        <>
          <MandatoryFact content={content} />
          <DeadlineFact deadline={deadline} allowLate={content.allowLateSubmission} now={now} />
        </>
      }
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
  const { course } = useCourseView()
  const people = usePeopleNames(course.id)
  const history = useMySubmissionHistory(content.id)
  const latest = history.data?.[0]
  // The released grade of the latest submission; null until the teacher releases it.
  const grade = useMyReleasedGrade(latest?.id)
  const [writing, setWriting] = useState(false)
  const closed = deadline !== null && deadline < now && !content.allowLateSubmission
  const isGroupAssignment = content.type === 'ASSIGNMENT' && content.groupAssignment === true
  // A group assignment is handed in for the user's group in the course, if they have one.
  const myGroup = isGroupAssignment
    ? people.groups?.find(group => group.memberIds.some(id => String(id) === String(user.userId)))
    : undefined

  return (
    <Layout
      main={
        <>
          <Task content={content} />
          <Card id="submission-title" title="Your submission">
            {isGroupAssignment && (
              <p className="submittable-note">
                {myGroup
                  ? <>Group assignment: you hand in for <strong>{people.groupLabel(myGroup.id)}</strong>, and it counts for everyone in it.</>
                  : 'Group assignment: you are not in a group in this course, so you hand in for yourself.'}
              </p>
            )}
            {history.isPending && <p role="status">Loading your submissions…</p>}
            {history.error && <p role="alert">Could not load your submissions.</p>}

            {writing
              ? (
                <SubmissionForm
                  content={content}
                  groupId={myGroup?.id}
                  onDone={() => setWriting(false)}
                  onCancel={() => setWriting(false)}
                />
              )
              : history.data && (
                <>
                  {latest
                    ? <SubmissionAnswer submission={latest} />
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
          <MandatoryFact content={content} />
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
            {latest && latest.groupId !== null && (
              <>
                <dt>Submitted by group</dt>
                <dd><GroupFact submission={latest} people={people} userId={user.userId} /></dd>
              </>
            )}
            <dt>Score</dt>
            <dd>
              {grade.data
                ? <><strong>{formatScore(grade.data.score)}</strong> <PassedBadge passed={grade.data.passed} /></>
                : latest ? 'Not graded yet' : '–'}
            </dd>
            {grade.data?.feedback && (
              <>
                <dt>Feedback from the teacher</dt>
                <dd className="submittable-text">{grade.data.feedback}</dd>
              </>
            )}
          </dl>
        </>
      }
    />
  )
}

/**
 * Whether an assignment is mandatory or optional, at the top of the panel. Nothing for
 * coding exercises, or while the backend does not send `mandatory` (proposed).
 */
function MandatoryFact({ content }: { content: Submittable }) {
  if (content.type !== 'ASSIGNMENT' || content.mandatory === undefined) return null
  return (
    <dl className="submittable-facts">
      <dt>Requirement</dt>
      <dd>
        {content.mandatory
          ? <>
              <span className="submittable-mandatory">Mandatory</span>
              <br />
              <span className="submittable-muted">Must be approved to complete the course</span>
            </>
          : <>
              <strong>Optional</strong>
              <br />
              <span className="submittable-muted">Not required to complete the course</span>
            </>}
      </dd>
    </dl>
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

type GroupFactProps = {
  submission: MySubmission
  people: ReturnType<typeof usePeopleNames>
  userId: string | number
}

/** The group that handed in and its members then, with the logged-in user marked. */
function GroupFact({ submission, people, userId }: GroupFactProps) {
  return (
    <>
      <strong>{people.groupLabel(submission.groupId!)}</strong>
      <ul className="submittable-members">
        {submission.members.map(member => (
          <li key={member.userId}>
            {people.nameOf(member.userId)}
            {String(member.userId) === String(userId) && <span className="submittable-muted"> (you)</span>}
          </li>
        ))}
      </ul>
    </>
  )
}

/** A message for a failed hand-in, from the backend's status code. */
function submitError(error: unknown): string {
  return errorMessage(error, {
    409: 'The deadline has passed, so it could not be handed in.',
    415: 'Only PDF files are accepted.',
    413: 'The file is too large (at most 10 MB).',
    403: 'You cannot hand in here (or for this group).',
    503: 'Handing in files is not available right now. Try again later.',
  }, 'Could not submit your answer. Try again.')
}

type SubmissionFormProps = {
  content: Submittable
  /** Hand in for this group (a group assignment the user has a group for). */
  groupId?: GroupId
  onDone: () => void
  onCancel: () => void
}

/**
 * The answer in a format the backend accepts: for an assignment a text or a PDF file,
 * for a coding exercise a link to a Git repository.
 */
function SubmissionForm({ content, groupId, onDone, onCancel }: SubmissionFormProps) {
  const submitText = useSubmitMySubmission()
  const submitPdf = useSubmitFile()
  const isCode = content.type === 'CODING_EXERCISE'
  const [kind, setKind] = useState<'TEXT' | 'FILE'>('TEXT')
  const [value, setValue] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const pending = submitText.isPending || submitPdf.isPending
  const error = submitText.error ?? submitPdf.error

  const answer = value.trim()
  const fileProblem = file && (
    !file.name.toLowerCase().endsWith('.pdf') ? 'Choose a PDF file.'
      : file.size > MAX_FILE_BYTES ? 'The file is too large (at most 10 MB).'
      : null)
  const valid = isCode
    ? safeHttpUrl(answer) !== undefined
    : kind === 'FILE' ? file !== null && !fileProblem : answer !== ''

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!valid) return
    if (!isCode && kind === 'FILE') {
      submitPdf.mutate({ contentId: content.id, file: file!, groupId }, { onSuccess: onDone })
    } else {
      submitText.mutate(
        { contentId: content.id, type: isCode ? 'GIT_URL' : 'TEXT', payload: answer, groupId },
        { onSuccess: onDone },
      )
    }
  }

  return (
    <form className="form submittable-form" onSubmit={handleSubmit}>
      {!isCode && (
        <fieldset className="submittable-kind">
          <legend className="visually-hidden">How do you hand in?</legend>
          <label>
            <input type="radio" name="submission-kind" checked={kind === 'TEXT'} onChange={() => setKind('TEXT')} />
            Write an answer
          </label>
          <label>
            <input type="radio" name="submission-kind" checked={kind === 'FILE'} onChange={() => setKind('FILE')} />
            Upload a PDF file
          </label>
        </fieldset>
      )}

      {!isCode && kind === 'FILE'
        ? (
          <div className="form-field">
            <label htmlFor="submission-file">PDF file</label>
            <p id="submission-file-hint" className="form-hint">One PDF file, at most 10 MB.</p>
            <input id="submission-file" type="file" accept="application/pdf,.pdf" aria-describedby="submission-file-hint"
              onChange={event => setFile(event.target.files?.[0] ?? null)} />
            {fileProblem && <p className="form-error" role="alert">{fileProblem}</p>}
          </div>
        )
        : (
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
        )}

      {error && <p className="form-error" role="alert">{submitError(error)}</p>}
      <div className="form-actions">
        <button type="submit" className="button-primary" disabled={!valid || pending}>
          {pending ? 'Submitting…' : 'Submit'}
        </button>
        <button type="button" className="button-secondary" onClick={onCancel} disabled={pending}>
          Cancel
        </button>
      </div>
    </form>
  )
}
