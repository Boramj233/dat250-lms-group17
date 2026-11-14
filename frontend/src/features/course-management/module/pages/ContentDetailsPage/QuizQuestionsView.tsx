import type { QuizContentQuestion } from '../../../../content/types'

/**
 * A quiz's questions with their answers, read-only. Correct answers are marked and
 * hints shown when the server sends them (proposed teacher-only fields, see
 * data/content/README.md); otherwise a note says they are not available.
 */
export default function QuizQuestionsView({ questions }: { questions: QuizContentQuestion[] }) {
  if (questions.length === 0) return <p>This quiz has no questions yet.</p>

  const sorted = [...questions].sort((a, b) => a.position - b.position)
  const knowsCorrect = sorted.some(q => q.options.some(o => o.correct !== undefined))

  return (
    <>
      {!knowsCorrect && (
        <p className="form-hint">The server does not say which answers are correct yet, so they are not marked.</p>
      )}
      <ol className="quiz-questions">
        {sorted.map((question, index) => (
          <li key={question.id} className="quiz-question">
            <p className="quiz-question-text">
              <span className="quiz-question-number">Question {index + 1}</span>
              {question.questionText}
            </p>
            <ul className="quiz-answers">
              {[...question.options].sort((a, b) => a.position - b.position).map(option => (
                <li key={option.id} className={`quiz-answer${option.correct ? ' is-correct' : ''}`}>
                  <span className="quiz-answer-mark" aria-hidden="true">{option.correct ? '✓' : ''}</span>
                  <span className="quiz-answer-text">{option.optionText}</span>
                  {option.correct && <span className="visually-hidden"> (correct)</span>}
                </li>
              ))}
            </ul>
            {question.hint && <p className="quiz-question-hint"><strong>Hint:</strong> {question.hint}</p>}
          </li>
        ))}
      </ol>
    </>
  )
}
