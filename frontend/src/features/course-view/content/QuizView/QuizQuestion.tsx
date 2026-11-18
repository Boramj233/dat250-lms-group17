import type { QuizContentItem, QuizContentQuestion, QuizQuestionType } from '@domain/content/types'
import { typeOf } from './questionType'
import { isAnswered, withMatch, withMovedItem, withOption, type AnswerDraft } from './quizAnswers'
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
  FILL_IN_BLANK: 'Write the missing word or words. Capital letters do not matter.',
  ORDERING: 'Put the items in the right order, first at the top.',
  MATCHING: 'Match every item on the left with one on the right. Each one on the right is used once.',
}

/** At most this long (SubmitQuizRequest.textAnswer). */
const MAX_TEXT_ANSWER = 255

type QuizQuestionProps = {
  question: QuizContentQuestion
  number: number
  total: number
  answer: AnswerDraft
  /** Leave out to show the question read-only (an earlier attempt). */
  onChange?: (answer: AnswerDraft) => void
  /**
   * An earlier attempt: whether the answer was correct, or null when no answer is stored
   * for the question (e.g. it was added later). The answer cannot be changed.
   */
  result?: boolean | null
  /**
   * Teachers: `answer` is the answer key. Shown read-only, marked "Correct answer", with
   * the question's hint (shown to students who answer it wrong) below.
   */
  answerKey?: { hint: string | null }
}

/**
 * One question in its own box. The type decides how it is answered: checkboxes
 * (multiple choice; every correct answer must be chosen), two large tiles (true or
 * false), a text field (fill in the blank), a list with Up/Down buttons (ordering) or a
 * menu per item (matching).
 */
export default function QuizQuestion({
  question,
  number,
  total,
  answer,
  onChange,
  result,
  answerKey,
}: QuizQuestionProps) {
  const type = typeOf(question)
  const reviewing = result !== undefined || answerKey !== undefined
  const resultClass = result === true || answerKey ? ' is-correct' : result === false ? ' is-wrong' : ''
  const answered = isAnswered(question, answer)
  const props = { question, answer, onChange: onChange ?? (() => {}) }
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
          {answerKey ? (
            <span className="quiz-question-result is-correct">
              <CheckIcon size={14} strokeWidth={3} />
              Correct answer · {TYPE_LABEL[type]}
            </span>
          ) : reviewing ? (
            <QuestionResult result={result} answered={answered} />
          ) : (
            <span className="quiz-question-type">{TYPE_LABEL[type]}</span>
          )}
        </span>
        <span className="quiz-question-text">{question.questionText}</span>
      </legend>
      {!reviewing && <p className="quiz-question-hint">{TYPE_HINT[type]}</p>}
      {!answerKey && reviewing && result !== null && !answered && (
        <p className="quiz-question-hint">You did not answer this question.</p>
      )}
      {type === 'FILL_IN_BLANK' ? (
        <TextAnswer {...props} />
      ) : type === 'ORDERING' ? (
        <OrderingAnswer {...props} />
      ) : type === 'MATCHING' ? (
        <MatchingAnswer {...props} />
      ) : (
        <ChoiceAnswer {...props} />
      )}
      {answerKey && (
        <p className="quiz-question-hint">
          {answerKey.hint ? <>Hint for a wrong answer: {answerKey.hint}</> : 'No hint for a wrong answer.'}
        </p>
      )}
    </fieldset>
  )
}

type AnswerProps = {
  question: QuizContentQuestion
  answer: AnswerDraft
  onChange: (answer: AnswerDraft) => void
}

/** Multiple choice (checkboxes) or true or false (two tiles, one of them chosen). */
function ChoiceAnswer({ question, answer, onChange }: AnswerProps) {
  const multiple = typeOf(question) === 'MULTIPLE_CHOICE'
  const options = [...question.options].sort((a, b) => a.position - b.position)
  return (
    <div className="quiz-options">
      {options.map(option => {
        const id = String(option.id)
        return (
          <label key={id} className="quiz-option">
            <input
              type={multiple ? 'checkbox' : 'radio'}
              name={`question-${question.id}`}
              value={id}
              checked={answer.optionIds.includes(id)}
              onChange={event => onChange(withOption(question, answer, id, event.target.checked))}
            />
            <span>{option.optionText}</span>
          </label>
        )
      })}
    </div>
  )
}

/** Fill in the blank: one text field. */
function TextAnswer({ question, answer, onChange }: AnswerProps) {
  const id = `question-${question.id}-answer`
  return (
    <div className="quiz-text-answer">
      <label htmlFor={id} className="visually-hidden">
        Your answer
      </label>
      <input
        id={id}
        type="text"
        autoComplete="off"
        maxLength={MAX_TEXT_ANSWER}
        placeholder="Your answer"
        value={answer.text}
        onChange={event => onChange({ ...answer, text: event.target.value })}
      />
    </div>
  )
}

/** Ordering: the items as a numbered list, each with Up and Down buttons. */
function OrderingAnswer({ question, answer, onChange }: AnswerProps) {
  const byId = new Map((question.items ?? []).map(item => [item.id, item]))
  const ordered = answer.order.map(id => byId.get(id)).filter((item): item is QuizContentItem => !!item)
  if (ordered.length === 0) return <p className="quiz-question-hint">No order stored.</p>
  return (
    <ol className="quiz-ordering">
      {ordered.map((item, index) => (
        <li key={item.id} className="quiz-ordering-item">
          <span className="quiz-ordering-position" aria-hidden="true">
            {index + 1}
          </span>
          <span className="quiz-ordering-text">{item.text}</span>
          <span className="quiz-ordering-buttons">
            <button
              type="button"
              className="button-secondary"
              disabled={index === 0}
              onClick={() => onChange(withMovedItem(answer, item.id, -1))}
            >
              <span aria-hidden="true">↑</span>
              <span className="visually-hidden">Move "{item.text}" up</span>
            </button>
            <button
              type="button"
              className="button-secondary"
              disabled={index === ordered.length - 1}
              onClick={() => onChange(withMovedItem(answer, item.id, 1))}
            >
              <span aria-hidden="true">↓</span>
              <span className="visually-hidden">Move "{item.text}" down</span>
            </button>
          </span>
        </li>
      ))}
    </ol>
  )
}

/** Matching: each item on the left with a menu of the items on the right. */
function MatchingAnswer({ question, answer, onChange }: AnswerProps) {
  const rightItems = question.rightItems ?? []
  return (
    <ul className="quiz-matching">
      {(question.leftItems ?? []).map(left => {
        const id = `question-${question.id}-match-${left.id}`
        const chosen = answer.matches[left.id] ?? ''
        return (
          <li key={left.id} className="quiz-matching-row">
            <label htmlFor={id} className="quiz-matching-left">
              {left.text}
            </label>
            <select
              id={id}
              className={chosen ? undefined : 'is-empty'}
              value={chosen}
              onChange={event => onChange(withMatch(answer, left.id, event.target.value))}
            >
              <option value="">Choose…</option>
              {rightItems.map(right => {
                const usedBy = Object.entries(answer.matches).find(
                  ([otherLeft, otherRight]) => otherRight === right.id && otherLeft !== left.id,
                )
                return (
                  <option key={right.id} value={right.id}>
                    {`${right.text}${usedBy ? ' (used)' : ''}`}
                  </option>
                )
              })}
            </select>
          </li>
        )
      })}
    </ul>
  )
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
