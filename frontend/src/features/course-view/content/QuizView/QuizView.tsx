import { useState } from 'react'
import type { QuizContent } from '@domain/content/types'
import { useMyQuizAttempts } from '@domain/quizzes/hooks'
import { attemptPassed, passRequirementText, quizPassed } from '@domain/quizzes/passRule'
import type { QuizAttemptScore } from '@domain/quizzes/types'
import DetailLayout, { Card, DeadlineFact } from '../../components/DetailLayout'
import PassedBadge from '../../components/PassedBadge'
import { AttemptList, AttemptReview } from './QuizAttempts'
import QuizForm from './QuizForm'
import '@shared/styles/forms.css'
import './QuizView.css'

type QuizViewProps = {
  content: QuizContent
  /** Teachers can preview the questions, but not hand in. */
  isTeacher: boolean
}

type Mode =
  | { kind: 'info' }
  | { kind: 'taking' }
  | { kind: 'result', score: QuizAttemptScore }
  /** Looking at an earlier attempt's answers. */
  | { kind: 'review', attemptId: QuizAttemptScore['id'] }

/**
 * A quiz: its explanation with a Start button, and a panel with the deadline, attempts
 * best score and, when the quiz has one, how many correct answers it takes to pass.
 * Starting shows one box per question, styled by its type; submitting grades it at once
 * and shows the score and whether the attempt passed. Students can look at each earlier
 * attempt's answers, with every question marked correct or wrong.
 */
export default function QuizView({ content, isTeacher }: QuizViewProps) {
  const [now] = useState(() => new Date())
  const attempts = useMyQuizAttempts(content.id)
  const [mode, setMode] = useState<Mode>({ kind: 'info' })

  const questions = [...(content.questions ?? [])].sort((a, b) => a.position - b.position)
  const deadline = content.deadline ? new Date(content.deadline) : null
  const used = attempts.data?.length ?? 0
  const best = attempts.data?.reduce<QuizAttemptScore | undefined>(
    (top, attempt) => (!top || attempt.correctCount > top.correctCount ? attempt : top), undefined)
  const required = content.requiredCorrectAnswers ?? null
  const passed = attempts.data ? quizPassed(attempts.data, required) : null
  const attemptsLeft = Math.max(0, content.maxAttempts - used)

  // Why the quiz cannot be started, if it cannot.
  const blocked = deadline && deadline < now ? 'The deadline has passed.'
    : attempts.data && used >= content.maxAttempts ? 'You have used all your attempts.'
    : questions.length === 0 ? 'This quiz has no questions yet.'
    : null

  const panel = (
    <dl className="submittable-facts">
      <DeadlineFact deadline={deadline} now={now} />
      <dt>Attempts allowed</dt>
      <dd>{content.maxAttempts}</dd>
      {!isTeacher && (
        <>
          <dt>Attempts used</dt>
          <dd>{attempts.isPending ? '…' : attempts.error ? '–' : `${used} of ${content.maxAttempts}`}</dd>
        </>
      )}
      <dt>Questions</dt>
      <dd>{questions.length}</dd>
      {required !== null && (
        <>
          <dt>To pass</dt>
          <dd>{passRequirementText(required, questions.length)}</dd>
        </>
      )}
      {!isTeacher && (
        <>
          <dt>Best score</dt>
          <dd>{best ? <strong>{best.correctCount} / {best.totalQuestions}</strong> : '–'}</dd>
          {required !== null && (
            <>
              <dt>Result</dt>
              <dd>
                {attempts.isPending ? '…'
                  : passed === true ? <PassedBadge passed />
                  : passed === false
                    ? attemptsLeft > 0
                      ? <>
                          <strong>Not passed yet</strong>
                          <span className="text-meta">
                            {' '}· {attemptsLeft} {attemptsLeft === 1 ? 'attempt' : 'attempts'} left
                          </span>
                        </>
                      : <><PassedBadge passed={false} /><span className="text-meta"> · no attempts left</span></>
                    : 'Not taken yet'}
              </dd>
            </>
          )}
        </>
      )}
    </dl>
  )

  return (
    <DetailLayout panelLabel="Quiz details" panel={panel}>
      <Card id="quiz-about-title" title="About this quiz">
        <div className="submittable-text">{content.description || 'No description.'}</div>
        {required !== null && (
          <p className="quiz-pass-rule">
            To pass, you need at least <strong>{passRequirementText(required, questions.length)}</strong> answers
            in one attempt. It is checked automatically when you submit.
          </p>
        )}
        {mode.kind === 'info' && (
          <div className="submittable-actions">
            {isTeacher
              ? (
                <button type="button" className="button-secondary" onClick={() => setMode({ kind: 'taking' })}
                  disabled={questions.length === 0}>
                  Preview quiz
                </button>
              )
              : blocked
                ? <p>{blocked}</p>
                : (
                  <button type="button" className="button-primary" onClick={() => setMode({ kind: 'taking' })}
                    disabled={attempts.isPending}>
                    {used > 0 ? 'Start new attempt' : 'Start quiz'}
                  </button>
                )}
          </div>
        )}
      </Card>

      {mode.kind === 'info' && !isTeacher && attempts.data && attempts.data.length > 0 && (
        <AttemptList
          attempts={attempts.data}
          required={required}
          onView={attemptId => setMode({ kind: 'review', attemptId })}
        />
      )}

      {mode.kind === 'review' && attempts.data && (
        <AttemptReview
          quizId={content.id}
          questions={questions}
          attempts={attempts.data}
          attemptId={mode.attemptId}
          required={required}
          onBack={() => setMode({ kind: 'info' })}
        />
      )}

      {mode.kind === 'taking' && (
        <QuizForm
          quizId={content.id}
          questions={questions}
          preview={isTeacher}
          onSubmitted={score => setMode({ kind: 'result', score })}
          onCancel={() => setMode({ kind: 'info' })}
        />
      )}

      {mode.kind === 'result' && (
        <Card id="quiz-result-title" title="Your result" className="quiz-result" role="status">
          <p className="quiz-result-score">
            {mode.score.correctCount} of {mode.score.totalQuestions} correct
            {' '}<PassedBadge passed={attemptPassed(mode.score, required)} />
          </p>
          {attemptPassed(mode.score, required) === false && (
            <p className="text-meta">
              You needed {passRequirementText(required ?? 0, mode.score.totalQuestions)} to pass.
              {attemptsLeft > 0
                ? ` You have ${attemptsLeft} ${attemptsLeft === 1 ? 'attempt' : 'attempts'} left.`
                : ' You have no attempts left.'}
            </p>
          )}
          <div className="submittable-actions">
            <button type="button" className="button-primary"
              onClick={() => setMode({ kind: 'review', attemptId: mode.score.id })}>
              See your answers
            </button>
            <button type="button" className="button-secondary" onClick={() => setMode({ kind: 'info' })}>
              Back to the quiz
            </button>
          </div>
        </Card>
      )}
    </DetailLayout>
  )
}
