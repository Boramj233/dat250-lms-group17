import type { ReactNode } from 'react'
import type { QuizContentQuestion, QuizQuestionType } from '@domain/content/types'
import { canAnswer, typeOf } from './questionType'
import { CheckIcon, CloseIcon } from '@shared/ui/icons'

const TYPE_LABEL: Record<QuizQuestionType, string> = {
  MULTIPLE_CHOICE: 'Multiple choice',
  TRUE_FALSE: 'True or false',
  FILL_IN_BLANK: 'Fill in the blank',
  ORDERING: 'Ordering',
  MATCHING: 'Matching',
}

const TYPE_HINT: Record<QuizQuestionType, string> = {
  MULTIPLE_CHOICE: 'Choose every answer that is correct.',
  TRUE_FALSE: 'Is the statement true or false?',
  FILL_IN_BLANK: 'Write the missing word or words.',
  ORDERING: 'Put the items in the right order.',
  MATCHING: 'Match each item on the left with one on the right.',
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
 * One question in its own box. The type decides the look: checkboxes (multiple choice;
 * every correct answer must be chosen) or two large tiles (true or false). Fill-in,
 * ordering and matching questions cannot be answered here yet and say so.
 */
export default function QuizQuestion({ question, number, total, chosen, onChoose, result }: QuizQuestionProps) {
  const type = typeOf(question)
  const options = [...question.options].sort((a, b) => a.position - b.position)
  const name = `question-${question.id}`
  const reviewing = result !== undefined
  const resultClass = result === true ? ' is-correct' : result === false ? ' is-wrong' : ''
  return (
    <fieldset
      className={`quiz-question is-${type.toLowerCase().replaceAll('_', '-')}${reviewing ? ' is-review' : ''}${resultClass}`}
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
      {!canAnswer(question) ? (
        <p className="quiz-question-hint" role="note">
          {TYPE_LABEL[type]} questions cannot be answered here yet.
        </p>
      ) : (
        !reviewing && <p className="quiz-question-hint">{TYPE_HINT[type]}</p>
      )}
      {reviewing && canAnswer(question) && chosen.length === 0 && (
        <p className="quiz-question-hint">You did not answer this question.</p>
      )}
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
      {result ? <CheckIcon size={14} strokeWidth={3} /> : <CloseIcon size={14} strokeWidth={3} />}
      {result ? 'Correct' : answered ? 'Wrong' : 'Not answered'}
    </span>
  )
}
