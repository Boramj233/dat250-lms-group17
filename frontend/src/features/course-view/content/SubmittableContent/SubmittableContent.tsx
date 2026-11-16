import { useState } from 'react'
import { useCurrentUser } from '@shared/auth/useAuth'
import { formatDateTime } from '@shared/lib/dateFormat'
import { safeHttpUrl } from '@shared/lib/safeUrl'
import { useMyReleasedGrade, useMySubmissionHistory } from '@domain/assignments/hooks'
import { formatScore } from '@domain/assignments/scoreFormat'
import type { MySubmission } from '@domain/assignments/types'
import type { AssignmentContent, CodingExerciseContent } from '@domain/content/types'
import DetailLayout, { Card, DeadlineFact } from '../../components/DetailLayout'
import PassedBadge from '../../components/PassedBadge'
import SubmissionAnswer from '../../components/SubmissionAnswer'
import { useCourseView } from '../../courseContext'
import { usePeopleNames } from '../../usePeopleNames'
import SubmissionForm from './SubmissionForm'
import '@shared/styles/forms.css'
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
    <DetailLayout
      panelLabel="Submission details"
      panel={
        <>
          <MandatoryFact content={content} />
          <dl className="submittable-facts">
            <DeadlineFact deadline={deadline} allowLate={content.allowLateSubmission} now={now} />
          </dl>
        </>
      }
    >
      <Task content={content} />
      <p className="text-meta submittable-note">
        Students hand in their answer here. You see and grade submissions under Submissions.
      </p>
    </DetailLayout>
  )
}

/** The description of the task, and for a coding exercise where to start from. */
function Task({ content }: { content: Submittable }) {
  const repository = content.type === 'CODING_EXERCISE' ? safeHttpUrl(content.starterRepositoryUrl) : undefined
  return (
    <Card id="task-title" title="Description">
      <div className="submittable-text">{content.instructions || 'No description.'}</div>
      {repository && (
        <p>
          Starter repository:{' '}
          <a href={repository} target="_blank" rel="noreferrer">
            {repository}
          </a>
        </p>
      )}
      {content.type === 'CODING_EXERCISE' && content.starterCode && (
        <div className="submittable-starter">
          <p className="submittable-label">Starter code</p>
          <pre className="submittable-code">
            <code>{content.starterCode}</code>
          </pre>
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
    <DetailLayout
      panelLabel="Submission details"
      panel={
        <>
          <MandatoryFact content={content} />
          <dl className="submittable-facts">
            <DeadlineFact deadline={deadline} allowLate={content.allowLateSubmission} now={now} />
          </dl>
          <dl className="submittable-facts">
            <dt>Submitted</dt>
            <dd>
              {latest ? (
                <>
                  {formatDateTime(new Date(latest.submittedAt))}
                  {latest.late && <span className="submittable-late"> (late)</span>}
                </>
              ) : (
                'Not submitted yet'
              )}
              {history.data && history.data.length > 1 && (
                <span className="text-meta"> · {history.data.length} submissions</span>
              )}
            </dd>
            {latest && latest.groupId !== null && (
              <>
                <dt>Submitted by group</dt>
                <dd>
                  <GroupFact submission={latest} people={people} userId={user.userId} />
                </dd>
              </>
            )}
            <dt>Score</dt>
            <dd>
              {grade.data ? (
                <>
                  <strong>{formatScore(grade.data.score)}</strong> <PassedBadge passed={grade.data.passed} />
                </>
              ) : latest ? (
                'Not graded yet'
              ) : (
                '–'
              )}
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
    >
      <Task content={content} />
      <Card id="submission-title" title="Your submission">
        {isGroupAssignment && (
          <p className="text-meta submittable-note">
            {myGroup ? (
              <>
                Group assignment: you hand in for <strong>{people.groupLabel(myGroup.id)}</strong>, and it counts for
                everyone in it.
              </>
            ) : (
              'Group assignment: you are not in a group in this course, so you hand in for yourself.'
            )}
          </p>
        )}
        {history.isPending && <p role="status">Loading your submissions…</p>}
        {history.error && <p role="alert">Could not load your submissions.</p>}

        {writing ? (
          <SubmissionForm
            content={content}
            groupId={myGroup?.id}
            onDone={() => setWriting(false)}
            onCancel={() => setWriting(false)}
          />
        ) : (
          history.data && (
            <>
              {latest ? (
                <SubmissionAnswer submission={latest} />
              ) : (
                <p className="submittable-empty">Nothing handed in yet.</p>
              )}
              <div className="submittable-actions">
                {closed ? (
                  <p>The deadline has passed. Late submissions are not accepted.</p>
                ) : (
                  <button type="button" className="button-primary" onClick={() => setWriting(true)}>
                    {latest ? 'Submit again' : 'Start submission'}
                  </button>
                )}
              </div>
            </>
          )
        )}
      </Card>
    </DetailLayout>
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
        {content.mandatory ? (
          <>
            <span className="tag tag-primary">Mandatory</span>
            <br />
            <span className="text-meta">Must be approved to complete the course</span>
          </>
        ) : (
          <>
            <strong>Optional</strong>
            <br />
            <span className="text-meta">Not required to complete the course</span>
          </>
        )}
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
            {String(member.userId) === String(userId) && <span className="text-meta"> (you)</span>}
          </li>
        ))}
      </ul>
    </>
  )
}
