import { useState, type FormEvent } from 'react'
import type { QuizContent, QuizContentQuestion } from '@domain/content/types'
import { useSubmitQuizAnswers } from '@domain/quizzes/hooks'
import type { QuizAttemptScore } from '@domain/quizzes/types'
import { FormError, SubmitButton } from '@shared/ui/Form'
import QuizQuestion from './QuizQuestion'
import { emptyAnswer, isAnswered, isSendable, toAnswerInput, type AnswerDraft } from './quizAnswers'

type QuizFormProps = {
  quizId: QuizContent['id']
  questions: QuizContentQuestion[]
  /** Teachers: try the questions without handing in. */
  preview: boolean
  onSubmitted: (score: QuizAttemptScore) => void
  onCancel: () => void
}

/** One box per question; all answers are handed in together. */
export default function QuizForm({ quizId, questions, preview, onSubmitted, onCancel }: QuizFormProps) {
  const submit = useSubmitQuizAnswers()
  // The answer per question id (as a string); a question without one has its empty answer.
  const [answers, setAnswers] = useState<Record<string, AnswerDraft>>({})
  const answerOf = (question: QuizContentQuestion) => answers[String(question.id)] ?? emptyAnswer(question)

  const answered = questions.filter(q => isAnswered(q, answerOf(q))).length
  // Matching questions must be complete, or the backend refuses the whole attempt.
  const incomplete = questions.flatMap((q, index) => (isSendable(q, answerOf(q)) ? [] : [index + 1]))
  const [triedToSubmit, setTriedToSubmit] = useState(false)

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (preview) return
    if (incomplete.length > 0) {
      setTriedToSubmit(true)
      return
    }
    submit.mutate({ quizId, answers: questions.map(q => toAnswerInput(q, answerOf(q))) }, { onSuccess: onSubmitted })
  }

  return (
    <form className="quiz-form" onSubmit={handleSubmit} aria-label={preview ? 'Quiz preview' : 'Quiz'}>
      {preview && (
        <p className="text-meta submittable-note">Preview: this is how students see the quiz. Nothing is saved.</p>
      )}
      {questions.map((question, index) => (
        <QuizQuestion
          key={question.id}
          question={question}
          number={index + 1}
          total={questions.length}
          answer={answerOf(question)}
          onChange={answer => setAnswers(previous => ({ ...previous, [String(question.id)]: answer }))}
        />
      ))}

      <div className="submittable-card quiz-form-footer">
        <p className="text-meta">
          Answered {answered} of {questions.length}
          {!preview && answered < questions.length && ' · unanswered questions count as wrong'}
        </p>
        <FormError>
          {triedToSubmit && incomplete.length > 0
            ? `Match every item in ${incomplete.length === 1 ? 'question' : 'questions'} ${incomplete.join(', ')} before you submit.`
            : submit.error && 'Could not submit the quiz. Try again.'}
        </FormError>
        <div className="quiz-form-actions">
          {!preview && (
            <SubmitButton pending={submit.isPending} pendingLabel="Submitting…">
              Submit quiz
            </SubmitButton>
          )}
          <button type="button" className="button-secondary" onClick={onCancel} disabled={submit.isPending}>
            {preview ? 'Close preview' : 'Cancel'}
          </button>
        </div>
      </div>
    </form>
  )
}
