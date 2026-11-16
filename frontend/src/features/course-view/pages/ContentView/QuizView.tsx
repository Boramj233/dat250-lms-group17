import { useState, type FormEvent, type ReactNode } from 'react'
import { formatDateTime, formatTimeLeft } from '../../../../shared/lib/dateFormat'
import type { QuizContent, QuizContentQuestion, QuizQuestionType } from '../../../content/types'
import { useMyQuizAttempts, useSubmitQuizAnswers } from '../../../quizzes/hooks'
import { attemptPassed, passRequirementText, quizPassed } from '../../../quizzes/passRule'
import type { QuizAttemptScore } from '../../../quizzes/types'
import PassedBadge from '../../components/PassedBadge'
import '../../../../shared/styles/forms.css'
import './SubmittableContent.css'
import './QuizView.css'

const TYPE_LABEL: Record<QuizQuestionType, string> = {
  SINGLE_CHOICE: 'Single choice',
  MULTIPLE_CHOICE: 'Multiple choice',
  TRUE_FALSE: 'True or false',
}

const TYPE_HINT: Record<QuizQuestionType, string> = {
  SINGLE_CHOICE: 'Choose one answer.',
  MULTIPLE_CHOICE: 'Choose every answer that is correct.',
  TRUE_FALSE: 'Is the statement true or false?',
}

const typeOf = (question: QuizContentQuestion): QuizQuestionType => question.questionType ?? 'SINGLE_CHOICE'

type QuizViewProps = {
  content: QuizContent
  /** Teachers can preview the questions, but not hand in. */
  isTeacher: boolean
}

type Mode = { kind: 'info' } | { kind: 'taking' } | { kind: 'result', score: QuizAttemptScore }

/**
 * A quiz: its explanation with a Start button, and a panel with the deadline, attempts
 * best score and, when the quiz has one, how many correct answers it takes to pass.
 * Starting shows one box per question, styled by its type; submitting grades it at once
 * and shows the score and whether the attempt passed.
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

  return (
    <div className="submittable">
      <div className="submittable-main">
        <section className="submittable-card" aria-labelledby="quiz-about-title">
          <h2 id="quiz-about-title" className="submittable-card-title">About this quiz</h2>
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
        </section>

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
          <section className="submittable-card quiz-result" aria-labelledby="quiz-result-title" role="status">
            <h2 id="quiz-result-title" className="submittable-card-title">Your result</h2>
            <p className="quiz-result-score">
              {mode.score.correctCount} of {mode.score.totalQuestions} correct
              {' '}<PassedBadge passed={attemptPassed(mode.score, required)} />
            </p>
            {attemptPassed(mode.score, required) === false && (
              <p className="submittable-muted">
                You needed {passRequirementText(required ?? 0, mode.score.totalQuestions)} to pass.
                {attemptsLeft > 0
                  ? ` You have ${attemptsLeft} ${attemptsLeft === 1 ? 'attempt' : 'attempts'} left.`
                  : ' You have no attempts left.'}
              </p>
            )}
            <div className="submittable-actions">
              <button type="button" className="button-secondary" onClick={() => setMode({ kind: 'info' })}>
                Back to the quiz
              </button>
            </div>
          </section>
        )}
      </div>

      <aside className="submittable-card submittable-panel" aria-label="Quiz details">
        <dl className="submittable-facts">
          <dt>Deadline</dt>
          <dd>
            {deadline
              ? <>
                  {formatDateTime(deadline)}
                  <span className="submittable-muted"> · {deadline < now ? 'passed' : formatTimeLeft(deadline, now)}</span>
                </>
              : 'No deadline'}
          </dd>
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
                              <span className="submittable-muted">
                                {' '}· {attemptsLeft} {attemptsLeft === 1 ? 'attempt' : 'attempts'} left
                              </span>
                            </>
                          : <><PassedBadge passed={false} /><span className="submittable-muted"> · no attempts left</span></>
                        : 'Not taken yet'}
                  </dd>
                </>
              )}
            </>
          )}
        </dl>
      </aside>
    </div>
  )
}

type QuizFormProps = {
  quizId: QuizContent['id']
  questions: QuizContentQuestion[]
  /** Teachers: try the questions without handing in. */
  preview: boolean
  onSubmitted: (score: QuizAttemptScore) => void
  onCancel: () => void
}

/** One box per question; all answers are handed in together. */
function QuizForm({ quizId, questions, preview, onSubmitted, onCancel }: QuizFormProps) {
  const submit = useSubmitQuizAnswers()
  // Chosen option ids per question id (as strings).
  const [chosen, setChosen] = useState<Record<string, string[]>>({})
  const answered = questions.filter(q => (chosen[String(q.id)] ?? []).length > 0).length

  function choose(question: QuizContentQuestion, optionId: string, checked: boolean) {
    const key = String(question.id)
    setChosen(previous => {
      const current = previous[key] ?? []
      const next = typeOf(question) === 'MULTIPLE_CHOICE'
        ? (checked ? [...current, optionId] : current.filter(id => id !== optionId))
        : [optionId]
      return { ...previous, [key]: next }
    })
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (preview) return
    submit.mutate(
      {
        quizId,
        answers: questions.map(q => ({ questionId: q.id, selectedOptionIds: chosen[String(q.id)] ?? [] })),
      },
      { onSuccess: onSubmitted },
    )
  }

  return (
    <form className="quiz-form" onSubmit={handleSubmit} aria-label={preview ? 'Quiz preview' : 'Quiz'}>
      {preview && <p className="submittable-note">Preview: this is how students see the quiz. Nothing is saved.</p>}
      {questions.map((question, index) => (
        <QuestionBox
          key={question.id}
          question={question}
          number={index + 1}
          total={questions.length}
          chosen={chosen[String(question.id)] ?? []}
          onChoose={(optionId, checked) => choose(question, optionId, checked)}
        />
      ))}

      <div className="submittable-card quiz-form-footer">
        <p className="submittable-muted">
          Answered {answered} of {questions.length}
          {!preview && answered < questions.length && ' · unanswered questions count as wrong'}
        </p>
        {submit.error && <p className="form-error" role="alert">Could not submit the quiz. Try again.</p>}
        <div className="quiz-form-actions">
          {!preview && (
            <button type="submit" className="button-primary" disabled={submit.isPending}>
              {submit.isPending ? 'Submitting…' : 'Submit quiz'}
            </button>
          )}
          <button type="button" className="button-secondary" onClick={onCancel} disabled={submit.isPending}>
            {preview ? 'Close preview' : 'Cancel'}
          </button>
        </div>
      </div>
    </form>
  )
}

type QuestionBoxProps = {
  question: QuizContentQuestion
  number: number
  total: number
  chosen: string[]
  onChoose: (optionId: string, checked: boolean) => void
}

/**
 * One question in its own box. The type decides the look: round buttons in a list
 * (single choice), checkboxes (multiple choice) or two large tiles (true or false).
 */
function QuestionBox({ question, number, total, chosen, onChoose }: QuestionBoxProps) {
  const type = typeOf(question)
  const options = [...question.options].sort((a, b) => a.position - b.position)
  const name = `question-${question.id}`
  return (
    <fieldset className={`quiz-question is-${type.toLowerCase().replace('_', '-')}`}>
      <legend className="quiz-question-legend">
        <span className="quiz-question-header">
          <span className="quiz-question-number">Question {number} of {total}</span>
          <span className="quiz-question-type">{TYPE_LABEL[type]}</span>
        </span>
        <span className="quiz-question-text">{question.questionText}</span>
      </legend>
      <p className="quiz-question-hint">{TYPE_HINT[type]}</p>
      <div className="quiz-options">
        {options.map(option => {
          const id = String(option.id)
          return (
            <Option key={id}>
              <input
                type={type === 'MULTIPLE_CHOICE' ? 'checkbox' : 'radio'}
                name={name}
                value={id}
                checked={chosen.includes(id)}
                onChange={event => onChoose(id, event.target.checked)}
              />
              <span>{option.optionText}</span>
            </Option>
          )
        })}
      </div>
    </fieldset>
  )
}

function Option({ children }: { children: ReactNode }) {
  return <label className="quiz-option">{children}</label>
}
