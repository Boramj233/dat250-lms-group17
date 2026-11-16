import type { NewQuizQuestion } from '@domain/content/types'
import RequiredMark from '@shared/ui/RequiredMark'
import { emptyQuestion } from './quizQuestions'

type QuizQuestionsEditorProps = {
  questions: NewQuizQuestion[]
  onChange: (questions: NewQuizQuestion[]) => void
}

/** The questions of a new quiz: text, an optional hint, and two or more answers with the correct ones ticked. */
export default function QuizQuestionsEditor({ questions, onChange }: QuizQuestionsEditorProps) {
  function update(index: number, change: Partial<NewQuizQuestion>) {
    onChange(questions.map((q, i) => (i === index ? { ...q, ...change } : q)))
  }

  return (
    <>
      {questions.map((question, index) => (
        <fieldset key={index} className="quiz-question-editor">
          <legend className="form-label">Question {index + 1}</legend>
          <div className="form-field">
            <label htmlFor={`q-${index}-text`}>
              Question
              <RequiredMark />
            </label>
            <input
              id={`q-${index}-text`}
              value={question.questionText}
              aria-required="true"
              onChange={event => update(index, { questionText: event.target.value })}
            />
          </div>
          <div className="form-field">
            <label htmlFor={`q-${index}-hint`}>Hint (optional)</label>
            <input
              id={`q-${index}-hint`}
              value={question.hint ?? ''}
              onChange={event => update(index, { hint: event.target.value })}
            />
          </div>
          <p className="form-label">
            Answers
            <RequiredMark />
          </p>
          <p className="form-hint">Every answer needs a text; tick the correct ones (at least one).</p>
          {question.options.map((option, optionIndex) => (
            <div key={optionIndex} className="quiz-option-editor">
              <input
                type="checkbox"
                aria-label={`Answer ${optionIndex + 1} is correct`}
                checked={option.correct}
                onChange={event =>
                  update(index, {
                    options: question.options.map((o, i) =>
                      i === optionIndex ? { ...o, correct: event.target.checked } : o,
                    ),
                  })
                }
              />
              <input
                aria-label={`Answer ${optionIndex + 1}`}
                aria-required="true"
                value={option.optionText}
                placeholder={`Answer ${optionIndex + 1}`}
                onChange={event =>
                  update(index, {
                    options: question.options.map((o, i) =>
                      i === optionIndex ? { ...o, optionText: event.target.value } : o,
                    ),
                  })
                }
              />
              <button
                type="button"
                className="button-secondary"
                disabled={question.options.length <= 2}
                onClick={() => update(index, { options: question.options.filter((_, i) => i !== optionIndex) })}
              >
                Remove<span className="visually-hidden"> answer {optionIndex + 1}</span>
              </button>
            </div>
          ))}
          <div className="form-actions">
            <button
              type="button"
              className="button-secondary"
              onClick={() => update(index, { options: [...question.options, { optionText: '', correct: false }] })}
            >
              Add answer
            </button>
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
      ))}
      <div className="form-actions">
        <button type="button" className="button-secondary" onClick={() => onChange([...questions, emptyQuestion()])}>
          Add question
        </button>
      </div>
    </>
  )
}
