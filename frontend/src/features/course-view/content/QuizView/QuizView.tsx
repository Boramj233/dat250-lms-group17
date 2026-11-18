import { useState } from 'react'
import type { QuizContent } from '@domain/content/types'
import { useMyQuizAttemptCount, useMyQuizAttempts } from '@domain/quizzes/hooks'
import { attemptPassed, bestAttempt, passRequirementText, quizPassed } from '@domain/quizzes/passRule'
import type { QuizAttemptScore } from '@domain/quizzes/types'
import DetailLayout, { Card, DeadlineFact } from '../../components/DetailLayout'
import PassedBadge from '../../components/PassedBadge'
import { AttemptList, AttemptReview } from './QuizAttempts'
import QuizForm from './QuizForm'
import { AnswerKey, StudentAttempts } from './TeacherQuizPanels'
import { formatCount } from '@shared/lib/formatCount'
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
  | { kind: 'result'; score: QuizAttemptScore }
  /** Looking at an earlier attempt's answers. */
  | { kind: 'review'; attemptId: QuizAttemptScore['id'] }
  /** Teachers: the correct answers. */
  | { kind: 'key' }

/**
 * A quiz: its explanation with a Start button, and a panel with the deadline, attempts
 * best score and the passing percentage (with how many correct answers that is).
 * Starting shows one box per question, styled by its type; submitting grades it at once
 * and shows the score and whether the attempt passed. Students can look at each earlier
 * attempt's answers, with every question marked correct or wrong. Teachers can preview
 * it, see the answer key, and look at each student's attempts and answers.
 */
export default function QuizView({ content, isTeacher }: QuizViewProps) {
  const [now] = useState(() => new Date())
  // Teachers do not take the quiz, so their own attempts are not loaded.
  const attempts = useMyQuizAttempts(content.id, { enabled: !isTeacher })
  // The number of attempts used, as the backend counts them when it refuses one more.
  const usedCount = useMyQuizAttemptCount(content.id, { enabled: !isTeacher })
  const [mode, setMode] = useState<Mode>({ kind: 'info' })

  const questions = [...(content.questions ?? [])].sort((a, b) => a.position - b.position)
  const deadline = content.deadline ? new Date(content.deadline) : null
  const used = usedCount.data ?? attempts.data?.length ?? 0
  const best = attempts.data && bestAttempt(attempts.data)
  const required = content.passingPercentage
  const passed = attempts.data ? quizPassed(attempts.data, required) : null
  const attemptsLeft = Math.max(0, content.maxAttempts - used)

  // Why the quiz cannot be started, if it cannot.
  const blocked =
    deadline && deadline < now
      ? 'The deadline has passed.'
      : (usedCount.data !== undefined || attempts.data) && used >= content.maxAttempts
        ? 'You have used all your attempts.'
        : questions.length === 0
          ? 'This quiz has no questions yet.'
          : null

  const panel = (
    <dl className="submittable-facts">
      <DeadlineFact deadline={deadline} now={now} />
      <dt>Attempts allowed</dt>
      <dd>{content.maxAttempts}</dd>
      {!isTeacher && (
        <>
          <dt>Attempts used</dt>
          <dd>
            {usedCount.isPending && attempts.isPending
              ? '…'
              : usedCount.error && attempts.error
                ? '–'
                : `${used} of ${content.maxAttempts}`}
          </dd>
        </>
      )}
      <dt>Questions</dt>
      <dd>{questions.length}</dd>
      <dt>To pass</dt>
      <dd>{passRequirementText(required, questions.length)}</dd>
      {!isTeacher && (
        <>
          <dt>Best score</dt>
          <dd>
            {best ? (
              <strong>
                {best.correctCount} / {best.totalQuestions}
              </strong>
            ) : (
              '–'
            )}
          </dd>
          <dt>Result</dt>
          <dd>
            {attempts.isPending ? (
              '…'
            ) : passed === true ? (
              <PassedBadge passed />
            ) : passed === false ? (
              attemptsLeft > 0 ? (
                <>
                  <strong>Not passed yet</strong>
                  <span className="text-meta"> · {formatCount(attemptsLeft, 'attempt')} left</span>
                </>
              ) : (
                <>
                  <PassedBadge passed={false} />
                  <span className="text-meta"> · no attempts left</span>
                </>
              )
            ) : (
              'Not taken yet'
            )}
          </dd>
        </>
      )}
    </dl>
  )

  return (
    <DetailLayout panelLabel="Quiz details" panel={panel}>
      <Card id="quiz-about-title" title="About this quiz">
        <div className="submittable-text">{content.description || 'No description.'}</div>
        <p className="quiz-pass-rule">
          To pass, at least <strong>{passRequirementText(required, questions.length)}</strong> of your answers in one
          attempt must be correct. It is checked automatically when you submit.
        </p>
        {mode.kind === 'info' && (
          <div className="submittable-actions">
            {isTeacher ? (
              <>
                <button
                  type="button"
                  className="button-secondary"
                  onClick={() => setMode({ kind: 'taking' })}
                  disabled={questions.length === 0}
                >
                  Preview quiz
                </button>
                <button
                  type="button"
                  className="button-secondary"
                  onClick={() => setMode({ kind: 'key' })}
                  disabled={questions.length === 0}
                >
                  Show answer key
                </button>
              </>
            ) : blocked ? (
              <p>{blocked}</p>
            ) : (
              <button
                type="button"
                className="button-primary"
                onClick={() => setMode({ kind: 'taking' })}
                disabled={attempts.isPending}
              >
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

      {mode.kind === 'info' && isTeacher && <StudentAttempts quiz={content} questions={questions} />}

      {mode.kind === 'key' && (
        <AnswerKey quizId={content.id} questions={questions} onBack={() => setMode({ kind: 'info' })} />
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
            {mode.score.correctCount} of {mode.score.totalQuestions} correct{' '}
            <PassedBadge passed={attemptPassed(mode.score, required)} />
          </p>
          {attemptPassed(mode.score, required) === false && (
            <p className="text-meta">
              You needed {passRequirementText(required, mode.score.totalQuestions)} to pass.
              {attemptsLeft > 0
                ? ` You have ${formatCount(attemptsLeft, 'attempt')} left.`
                : ' You have no attempts left.'}
            </p>
          )}
          {mode.score.hints && mode.score.hints.length > 0 && (
            <div className="quiz-hints">
              <p className="form-label">Hints for the questions you got wrong</p>
              <ul>
                {mode.score.hints.map(hint => {
                  const index = questions.findIndex(q => String(q.id) === String(hint.questionId))
                  return (
                    <li key={hint.questionId}>
                      {index >= 0 && <strong>Question {index + 1}: </strong>}
                      {hint.hint}
                    </li>
                  )
                })}
              </ul>
            </div>
          )}
          <div className="submittable-actions">
            <button
              type="button"
              className="button-primary"
              onClick={() => setMode({ kind: 'review', attemptId: mode.score.id })}
            >
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
