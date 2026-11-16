import type { ReactNode } from 'react'
import type { QuizContentQuestion, QuizQuestionType } from '@domain/content/types'
import { typeOf } from './questionType'

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

type QuizQuestionProps = {
  question: QuizContentQuestion
  number: number
  total: number
  chosen: string[]
  /** Leave out to show the question read-only (an earlier attempt). */
  onChoose?: (optionId: string, checked: boolean) => void
  /**
   * An earlier attempt: whether the answer was correct, or null when no answer is stored
   * for the question (e.g. it was added later). The options cannot be changed.
   */
  result?: boolean | null
}

/**
 * One question in its own box. The type decides the look: round buttons in a list
 * (single choice), checkboxes (multiple choice) or two large tiles (true or false).
 */
export default function QuizQuestion({ question, number, total, chosen, onChoose, result }: QuizQuestionProps) {
  const type = typeOf(question)
  const options = [...question.options].sort((a, b) => a.position - b.position)
  const name = `question-${question.id}`
  const reviewing = result !== undefined
  const resultClass = result === true ? ' is-correct' : result === false ? ' is-wrong' : ''
  return (
    <fieldset
      className={`quiz-question is-${type.toLowerCase().replace('_', '-')}${reviewing ? ' is-review' : ''}${resultClass}`}
      disabled={reviewing}
    >
      <legend className="quiz-question-legend">
        <span className="quiz-question-header">
          <span className="quiz-question-number">
            Question {number} of {total}
          </span>
          {reviewing ? (
            <QuestionResult result={result} answered={chosen.length > 0} />
          ) : (
            <span className="quiz-question-type">{TYPE_LABEL[type]}</span>
          )}
        </span>
        <span className="quiz-question-text">{question.questionText}</span>
      </legend>
      {!reviewing && <p className="quiz-question-hint">{TYPE_HINT[type]}</p>}
      {reviewing && chosen.length === 0 && <p className="quiz-question-hint">You did not answer this question.</p>}
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
                readOnly={!onChoose}
                onChange={event => onChoose?.(id, event.target.checked)}
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

/** "✓ Correct", "✗ Wrong" (or "✗ Not answered"), or nothing stored for the question. */
function QuestionResult({ result, answered }: { result: boolean | null | undefined; answered: boolean }) {
  if (result === null || result === undefined) return <span className="quiz-question-result">No answer stored</span>
  return (
    <span className={`quiz-question-result ${result ? 'is-correct' : 'is-wrong'}`}>
      <svg
        width="14"
        height="14"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="3"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
        focusable="false"
      >
        {result ? <path d="M5 12.5l4.5 4.5L19 7.5" /> : <path d="M6 6l12 12M18 6L6 18" />}
      </svg>
      {result ? 'Correct' : answered ? 'Wrong' : 'Not answered'}
    </span>
  )
}
