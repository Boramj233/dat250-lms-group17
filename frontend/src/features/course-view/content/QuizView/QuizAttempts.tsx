import { formatDateTime } from '@shared/lib/dateFormat'
import type { QuizContent, QuizContentQuestion } from '@domain/content/types'
import { useMyQuizAttempt } from '@domain/quizzes/hooks'
import { attemptPassed } from '@domain/quizzes/passRule'
import type { QuizAttemptScore } from '@domain/quizzes/types'
import { Card } from '../../components/DetailLayout'
import PassedBadge from '../../components/PassedBadge'
import QuizQuestion from './QuizQuestion'

type AttemptListProps = {
  /** Newest first. */
  attempts: QuizAttemptScore[]
  required: number | null
  onView: (attemptId: QuizAttemptScore['id']) => void
}

/** The student's attempts, newest first, each with its score and a button to see the answers. */
export function AttemptList({ attempts, required, onView }: AttemptListProps) {
  return (
    <Card id="quiz-attempts-title" title="Your attempts">
      <ol className="quiz-attempts" reversed>
        {attempts.map((attempt, index) => (
          <li key={attempt.id} className="quiz-attempt">
            <span className="quiz-attempt-name">Attempt {attempts.length - index}</span>
            <span className="submittable-muted">{formatDateTime(new Date(attempt.submittedAt))}</span>
            <span className="quiz-attempt-score">{attempt.correctCount} of {attempt.totalQuestions} correct</span>
            <PassedBadge passed={attemptPassed(attempt, required)} />
            <button type="button" className="button-secondary quiz-attempt-view" onClick={() => onView(attempt.id)}>
              View answers<span className="visually-hidden"> for attempt {attempts.length - index}</span>
            </button>
          </li>
        ))}
      </ol>
    </Card>
  )
}

type AttemptReviewProps = {
  quizId: QuizContent['id']
  /** The quiz's questions, in order. */
  questions: QuizContentQuestion[]
  /** All the student's attempts, newest first (for the attempt's number). */
  attempts: QuizAttemptScore[]
  attemptId: QuizAttemptScore['id']
  required: number | null
  onBack: () => void
}

/**
 * One earlier attempt: the score, then every question with what the student chose
 * (read-only) and whether it was correct. The correct answers are not shown, so the
 * review does not give them away while attempts are left.
 */
export function AttemptReview({ quizId, questions, attempts, attemptId, required, onBack }: AttemptReviewProps) {
  const detail = useMyQuizAttempt(quizId, attemptId)
  const index = attempts.findIndex(attempt => String(attempt.id) === String(attemptId))
  const number = index >= 0 ? attempts.length - index : undefined

  const back = (
    <div className="submittable-actions">
      <button type="button" className="button-secondary" onClick={onBack}>Back to the quiz</button>
    </div>
  )

  if (detail.isPending) return <p role="status">Loading your answers…</p>
  if (detail.error) {
    return (
      <Card>
        <p role="alert">Could not load the answers of this attempt.</p>
        {back}
      </Card>
    )
  }

  const attempt = detail.data
  const answerOf = (question: QuizContentQuestion) =>
    attempt.answers.find(answer => String(answer.questionId) === String(question.id))
  const wrong = attempt.totalQuestions - attempt.correctCount

  return (
    <>
      <Card id="quiz-review-title" title={<>Your answers{number !== undefined && ` · attempt ${number}`}</>}>
        <p className="quiz-result-score">
          {attempt.correctCount} of {attempt.totalQuestions} correct
          {' '}<PassedBadge passed={attemptPassed(attempt, required)} />
        </p>
        <p className="submittable-muted">
          Handed in {formatDateTime(new Date(attempt.submittedAt))}
          {' · '}{attempt.correctCount} correct, {wrong} wrong.
          {' '}Your answers are marked; the correct answers are not shown.
        </p>
        {back}
      </Card>

      <div className="quiz-form" aria-label="Your answers">
        {questions.map((question, i) => {
          const answer = answerOf(question)
          return (
            <QuizQuestion
              key={question.id}
              question={question}
              number={i + 1}
              total={questions.length}
              chosen={(answer?.selectedOptionIds ?? []).map(String)}
              result={answer ? answer.correct : null}
            />
          )
        })}
      </div>

      <Card>{back}</Card>
    </>
  )
}
