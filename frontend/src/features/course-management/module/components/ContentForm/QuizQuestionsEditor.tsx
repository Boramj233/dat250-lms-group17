import type { NewQuizQuestion, QuizQuestionType } from '@domain/content/types'
import { Field } from '@shared/ui/Form'
import RequiredMark from '@shared/ui/RequiredMark'
import { moveItem } from '@shared/lib/moveItem'
import { emptyQuestion, QUESTION_TYPES, withType } from './quizQuestions'

type QuizQuestionsEditorProps = {
  questions: NewQuizQuestion[]
  onChange: (questions: NewQuizQuestion[]) => void
}

/**
 * The questions of a quiz. Each has a type, a text, an optional hint and the answer key
 * of its type: answers with the correct ones ticked (multiple choice), True or False
 * (true or false), accepted answers (fill in the blank), items in the correct order
 * (ordering) or pairs that belong together (matching).
 */
export default function QuizQuestionsEditor({ questions, onChange }: QuizQuestionsEditorProps) {
  function update(index: number, change: Partial<NewQuizQuestion>) {
    onChange(questions.map((q, i) => (i === index ? { ...q, ...change } : q)))
  }

  return (
    <>
      {questions.map((question, index) => {
        const type = question.type ?? 'MULTIPLE_CHOICE'
        const editorProps = {
          question,
          number: index + 1,
          update: (change: Partial<NewQuizQuestion>) => update(index, change),
        }
        return (
          <fieldset key={index} className="quiz-question-editor">
            <legend className="form-label">Question {index + 1}</legend>
            <Field label="Question type" required>
              {props => (
                <select
                  {...props}
                  value={type}
                  onChange={event =>
                    onChange(
                      questions.map((q, i) => (i === index ? withType(q, event.target.value as QuizQuestionType) : q)),
                    )
                  }
                >
                  {QUESTION_TYPES.map(option => (
                    <option key={option.type} value={option.type}>
                      {option.label}
                    </option>
                  ))}
                </select>
              )}
            </Field>
            <Field
              label="Question"
              required
              hint={type === 'FILL_IN_BLANK' ? 'Write the sentence with ___ where the missing word goes.' : undefined}
            >
              {props => (
                <input
                  {...props}
                  // Checked when the form is sent, with a message naming the question.
                  required={undefined}
                  aria-required="true"
                  value={question.questionText}
                  onChange={event => update(index, { questionText: event.target.value })}
                />
              )}
            </Field>
            <Field label="Hint (optional)" hint="Shown to the student after an attempt where this question was wrong.">
              {props => (
                <input
                  {...props}
                  value={question.hint ?? ''}
                  onChange={event => update(index, { hint: event.target.value })}
                />
              )}
            </Field>
            {type === 'TRUE_FALSE' ? (
              <TrueFalseEditor {...editorProps} />
            ) : type === 'FILL_IN_BLANK' ? (
              <AcceptedAnswersEditor {...editorProps} />
            ) : type === 'ORDERING' ? (
              <OrderingEditor {...editorProps} />
            ) : type === 'MATCHING' ? (
              <MatchingEditor {...editorProps} />
            ) : (
              <ChoiceEditor {...editorProps} />
            )}
            <div className="form-actions">
              <button
                type="button"
                className="button-secondary"
                disabled={questions.length <= 1}
                onClick={() => onChange(questions.filter((_, i) => i !== index))}
              >
                Remove question {index + 1}
              </button>
            </div>
          </fieldset>
        )
      })}
      <div className="form-actions">
        <button type="button" className="button-secondary" onClick={() => onChange([...questions, emptyQuestion()])}>
          Add question
        </button>
      </div>
    </>
  )
}

type EditorProps = {
  question: NewQuizQuestion
  /** The question's number, from 1, for labels. */
  number: number
  update: (change: Partial<NewQuizQuestion>) => void
}

/** A label for the answer key, with the required mark, and a hint below it. */
function KeyHeading({ label, hint }: { label: string; hint: string }) {
  return (
    <>
      <p className="form-label">
        {label}
        <RequiredMark />
      </p>
      <p className="form-hint">{hint}</p>
    </>
  )
}

/** Multiple choice: two or more answers, the correct ones ticked. */
function ChoiceEditor({ question, update }: EditorProps) {
  const options = question.options
  const set = (optionIndex: number, change: Partial<(typeof options)[number]>) =>
    update({ options: options.map((o, i) => (i === optionIndex ? { ...o, ...change } : o)) })
  return (
    <>
      <KeyHeading label="Answers" hint="Every answer needs a text; tick the correct ones (at least one)." />
      {options.map((option, optionIndex) => (
        <div key={optionIndex} className="quiz-option-editor">
          <input
            type="checkbox"
            aria-label={`Answer ${optionIndex + 1} is correct`}
            checked={option.correct}
            onChange={event => set(optionIndex, { correct: event.target.checked })}
          />
          <input
            aria-label={`Answer ${optionIndex + 1}`}
            aria-required="true"
            value={option.optionText}
            placeholder={`Answer ${optionIndex + 1}`}
            onChange={event => set(optionIndex, { optionText: event.target.value })}
          />
          <button
            type="button"
            className="button-secondary"
            disabled={options.length <= 2}
            onClick={() => update({ options: options.filter((_, i) => i !== optionIndex) })}
          >
            Remove<span className="visually-hidden"> answer {optionIndex + 1}</span>
          </button>
        </div>
      ))}
      <div className="form-actions">
        <button
          type="button"
          className="button-secondary"
          onClick={() => update({ options: [...options, { optionText: '', correct: false }] })}
        >
          Add answer
        </button>
      </div>
    </>
  )
}

/** True or false: the answers are fixed; the teacher picks the correct one. */
function TrueFalseEditor({ question, number, update }: EditorProps) {
  return (
    <fieldset className="quiz-key-choice">
      <legend className="form-label">
        Correct answer
        <RequiredMark />
      </legend>
      {question.options.map((option, optionIndex) => (
        <label key={option.optionText} className="form-check">
          <input
            type="radio"
            name={`question-${number}-true-false`}
            checked={option.correct}
            onChange={() => update({ options: question.options.map((o, i) => ({ ...o, correct: i === optionIndex })) })}
          />
          {option.optionText}
        </label>
      ))}
    </fieldset>
  )
}

/**
 * A list of texts the teacher can add to and remove from, at least `min` long; with
 * `ordered`, also Up and Down buttons.
 */
function TextListEditor({
  items,
  itemLabel,
  min,
  ordered = false,
  onChange,
}: {
  items: string[]
  itemLabel: string
  min: number
  ordered?: boolean
  onChange: (items: string[]) => void
}) {
  return (
    <>
      {items.map((item, itemIndex) => {
        const label = `${itemLabel} ${itemIndex + 1}`
        return (
          <div key={itemIndex} className="quiz-option-editor">
            {ordered && (
              <span className="quiz-key-position" aria-hidden="true">
                {itemIndex + 1}
              </span>
            )}
            <input
              aria-label={label}
              aria-required="true"
              maxLength={255}
              value={item}
              placeholder={label}
              onChange={event => onChange(items.map((other, i) => (i === itemIndex ? event.target.value : other)))}
            />
            {ordered && (
              <>
                <button
                  type="button"
                  className="button-secondary"
                  disabled={itemIndex === 0}
                  onClick={() => onChange(moveItem(items, itemIndex, itemIndex - 1))}
                >
                  <span aria-hidden="true">↑</span>
                  <span className="visually-hidden">Move {label} up</span>
                </button>
                <button
                  type="button"
                  className="button-secondary"
                  disabled={itemIndex === items.length - 1}
                  onClick={() => onChange(moveItem(items, itemIndex, itemIndex + 1))}
                >
                  <span aria-hidden="true">↓</span>
                  <span className="visually-hidden">Move {label} down</span>
                </button>
              </>
            )}
            <button
              type="button"
              className="button-secondary"
              disabled={items.length <= min}
              onClick={() => onChange(items.filter((_, i) => i !== itemIndex))}
            >
              Remove<span className="visually-hidden"> {label}</span>
            </button>
          </div>
        )
      })}
      <div className="form-actions">
        <button
          type="button"
          className="button-secondary"
          disabled={items.length >= 20}
          onClick={() => onChange([...items, ''])}
        >
          Add {itemLabel.toLowerCase()}
        </button>
      </div>
    </>
  )
}

/** Fill in the blank: the answers that count as correct. */
function AcceptedAnswersEditor({ question, update }: EditorProps) {
  return (
    <>
      <KeyHeading
        label="Accepted answers"
        hint="An answer is correct when it is one of these. Capital letters and spaces at the ends do not matter."
      />
      <TextListEditor
        items={question.acceptedAnswers ?? []}
        itemLabel="Accepted answer"
        min={1}
        onChange={acceptedAnswers => update({ acceptedAnswers })}
      />
    </>
  )
}

/** Ordering: the items in the correct order; students get them shuffled. */
function OrderingEditor({ question, update }: EditorProps) {
  return (
    <>
      <KeyHeading
        label="Items in the correct order"
        hint="Put the items in the order that is correct, first at the top (2 to 20). Students get them shuffled."
      />
      <TextListEditor
        items={question.orderingItems ?? []}
        itemLabel="Item"
        min={2}
        ordered
        onChange={orderingItems => update({ orderingItems })}
      />
    </>
  )
}

/** Matching: pairs that belong together; students get the right side shuffled. */
function MatchingEditor({ question, update }: EditorProps) {
  const pairs = question.matchingPairs ?? []
  const set = (pairIndex: number, change: Partial<(typeof pairs)[number]>) =>
    update({ matchingPairs: pairs.map((p, i) => (i === pairIndex ? { ...p, ...change } : p)) })
  return (
    <>
      <KeyHeading
        label="Pairs"
        hint="Each row is a correct pair (2 to 20). Students match each left item with one on the right, which they get shuffled."
      />
      {pairs.map((pair, pairIndex) => (
        <div key={pairIndex} className="quiz-option-editor quiz-pair-editor">
          <input
            aria-label={`Pair ${pairIndex + 1}, left`}
            aria-required="true"
            maxLength={255}
            value={pair.left}
            placeholder="Left"
            onChange={event => set(pairIndex, { left: event.target.value })}
          />
          <span aria-hidden="true">↔</span>
          <input
            aria-label={`Pair ${pairIndex + 1}, right`}
            aria-required="true"
            maxLength={255}
            value={pair.right}
            placeholder="Right"
            onChange={event => set(pairIndex, { right: event.target.value })}
          />
          <button
            type="button"
            className="button-secondary"
            disabled={pairs.length <= 2}
            onClick={() => update({ matchingPairs: pairs.filter((_, i) => i !== pairIndex) })}
          >
            Remove<span className="visually-hidden"> pair {pairIndex + 1}</span>
          </button>
        </div>
      ))}
      <div className="form-actions">
        <button
          type="button"
          className="button-secondary"
          disabled={pairs.length >= 20}
          onClick={() => update({ matchingPairs: [...pairs, { left: '', right: '' }] })}
        >
          Add pair
        </button>
      </div>
    </>
  )
}
