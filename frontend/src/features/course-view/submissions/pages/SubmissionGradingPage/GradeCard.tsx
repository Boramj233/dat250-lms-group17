import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { errorMessage } from '@shared/api/errorMessage'
import { formatDateTime, fromDateTimeLocal, toDateTimeLocal } from '@shared/lib/dateFormat'
import { paths } from '@shared/paths'
import type { ContentId, CourseId } from '@shared/types'
import { Field, FormError, FormSuccess, SubmitButton } from '@shared/ui/Form'
import { useReleaseGrade, useSaveGrade } from '@domain/assignments/hooks'
import { formatScore, MAX_SCORE } from '@domain/assignments/scoreFormat'
import type { GradeReview, ReviewSubmission } from '@domain/assignments/types'
import { Card } from '../../../components/DetailLayout'
import PassedBadge from '../../../components/PassedBadge'

/** The passed / not passed choice in the grading form (proposed in the backend). */
const PASSED_CHOICES: { value: boolean | null; label: string }[] = [
  { value: true, label: 'Passed' },
  { value: false, label: 'Not passed' },
  { value: null, label: 'Not decided yet' },
]

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
 * The grade: a form while it can be changed, otherwise what was given. A published
 * grade cannot be changed, and an earlier submission cannot be graded. The grade is
 * visible to the student from its own visible date, or once it is published.
 */
export default function GradeCard({ submission, contentId, courseId, grade, isLatest, newestId }: GradeCardProps) {
  const save = useSaveGrade()
  const release = useReleaseGrade()
  const [score, setScore] = useState(grade ? String(grade.score) : '')
  const [feedback, setFeedback] = useState(grade?.feedback ?? '')
  const [passed, setPassed] = useState<boolean | null>(grade?.passed ?? null)
  // The grade's own visible date (datetime-local); empty: hidden until published.
  const [visibleDate, setVisibleDate] = useState(toDateTimeLocal(grade?.visibleFrom))
  const [now] = useState(() => Date.now())
  const visibleFrom = fromDateTimeLocal(visibleDate)
  const visibleInPast = visibleFrom !== null && Date.parse(visibleFrom) <= now
  const [done, setDone] = useState<'saved' | 'published' | null>(null)
  const busy = save.isPending || release.isPending
  const failed = save.error ?? release.error

  const number = Number(score.replace(',', '.'))
  const scoreValid =
    score.trim() !== '' &&
    Number.isFinite(number) &&
    number >= 0 &&
    number <= MAX_SCORE &&
    Math.round(number * 100) === number * 100

  if (grade?.releasedAt) {
    return (
      <Card id="grade-title" title="Grade and feedback">
        <p className="grading-score">
          {formatScore(grade.score)} <PassedBadge passed={grade.passed} />
        </p>
        <div className="submittable-text">{grade.feedback || <span className="text-meta">No feedback.</span>}</div>
        <p className="text-meta submittable-note">
          Visible to the student since {formatDateTime(new Date(grade.releasedAt))}. A published grade cannot be
          changed.
        </p>
      </Card>
    )
  }

  if (!isLatest) {
    return (
      <Card id="grade-title" title="Grade and feedback">
        <p className="text-meta submittable-note">
          A newer submission exists. Only the latest submission can be graded.
          {newestId !== undefined && (
            <>
              {' '}
              <Link to={paths.courseSubmission(courseId, contentId, newestId)}>Go to the latest</Link>
            </>
          )}
        </p>
      </Card>
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
        passed,
        // Publishing now makes a later date pointless.
        visibleFrom: andRelease ? null : visibleFrom,
        version: grade?.version ?? null,
      })
      if (andRelease) await release.mutateAsync({ submissionId: submission.id, contentId, version: saved.version })
      setDone(andRelease ? 'published' : 'saved')
    } catch {
      // Shown below from the mutations' errors.
    }
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    void submit(false)
  }

  return (
    <Card id="grade-title" title="Grade and feedback">
      <form className="form grading-form" onSubmit={handleSubmit}>
        <Field
          label={`Score (0–${MAX_SCORE})`}
          required
          className="grading-score-field"
          error={score.trim() !== '' && !scoreValid && `A number from 0 to ${MAX_SCORE}, with at most two decimals.`}
        >
          {props => (
            <div className="grading-score-input">
              <input
                {...props}
                type="number"
                inputMode="decimal"
                min={0}
                max={MAX_SCORE}
                step={0.01}
                value={score}
                onChange={event => setScore(event.target.value)}
              />
              <span className="text-meta">/ {MAX_SCORE}</span>
            </div>
          )}
        </Field>

        <fieldset className="form-field grading-passed">
          <legend className="form-label">Passed</legend>
          {PASSED_CHOICES.map(choice => (
            <label key={String(choice.value)} className="form-check">
              <input
                type="radio"
                name="grading-passed"
                checked={passed === choice.value}
                onChange={() => setPassed(choice.value)}
              />
              {choice.label}
            </label>
          ))}
        </fieldset>

        <Field label="Feedback to the student">
          {props => (
            <textarea
              {...props}
              rows={6}
              maxLength={10000}
              value={feedback}
              onChange={event => setFeedback(event.target.value)}
            />
          )}
        </Field>

        <Field
          label="Visible date"
          hint="When the student sees this grade. Leave it empty to keep it hidden until you publish it."
          error={visibleInPast && 'Choose a time in the future, or use Save and publish now.'}
        >
          {props => (
            <input
              {...props}
              type="datetime-local"
              className="grading-visible-input"
              value={visibleDate}
              onChange={event => setVisibleDate(event.target.value)}
            />
          )}
        </Field>

        <FormError>{failed && gradeError(failed)}</FormError>
        <FormSuccess>
          {done &&
            (done === 'published'
              ? 'Saved and published to the student.'
              : visibleFrom
                ? `Saved. The student sees it from ${formatDateTime(new Date(visibleFrom))}, or when you publish it.`
                : 'Saved. The student cannot see it until you publish it.')}
        </FormSuccess>

        <div className="form-actions">
          <SubmitButton pending={save.isPending && !release.isPending} disabled={busy || !scoreValid || visibleInPast}>
            Save
          </SubmitButton>
          <button
            type="button"
            className="button-secondary"
            disabled={busy || !scoreValid}
            onClick={() => void submit(true)}
          >
            {release.isPending ? 'Publishing…' : 'Save and publish now'}
          </button>
        </div>
        <p className="text-meta submittable-note">
          Save and publish now makes the grade visible right away. A published grade cannot be changed afterwards.
        </p>
      </form>
    </Card>
  )
}

/** A message for a failed save or release, from the backend's status code. */
function gradeError(error: unknown): string {
  return errorMessage(
    error,
    {
      409: 'The grade was changed in the meantime, it is already published, or a newer submission exists. Reload the page and try again.',
      400: `The score must be a number from 0 to ${MAX_SCORE}, with at most two decimals, and the visible date in the future.`,
      403: 'Only active teachers of the course can grade.',
    },
    'Could not save the grade. Try again.',
  )
}
