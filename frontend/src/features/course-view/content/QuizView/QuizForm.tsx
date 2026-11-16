import { useState, type FormEvent } from 'react'
import type { QuizContent, QuizContentQuestion } from '@domain/content/types'
import { useSubmitQuizAnswers } from '@domain/quizzes/hooks'
import type { QuizAttemptScore } from '@domain/quizzes/types'
import { typeOf } from './questionType'
import { FormError, SubmitButton } from '@shared/ui/Form'
import QuizQuestion from './QuizQuestion'

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
  // Chosen option ids per question id (as strings).
  const [chosen, setChosen] = useState<Record<string, string[]>>({})
  const answered = questions.filter(q => (chosen[String(q.id)] ?? []).length > 0).length

  function choose(question: QuizContentQuestion, optionId: string, checked: boolean) {
    const key = String(question.id)
    setChosen(previous => {
      const current = previous[key] ?? []
      const next =
        typeOf(question) === 'MULTIPLE_CHOICE'
          ? checked
            ? [...current, optionId]
            : current.filter(id => id !== optionId)
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
      {preview && (
        <p className="text-meta submittable-note">Preview: this is how students see the quiz. Nothing is saved.</p>
      )}
      {questions.map((question, index) => (
        <QuizQuestion
          key={question.id}
          question={question}
          number={index + 1}
          total={questions.length}
          chosen={chosen[String(question.id)] ?? []}
          onChoose={(optionId, checked) => choose(question, optionId, checked)}
        />
      ))}

      <div className="submittable-card quiz-form-footer">
        <p className="text-meta">
          Answered {answered} of {questions.length}
          {!preview && answered < questions.length && ' · unanswered questions count as wrong'}
        </p>
        <FormError>{submit.error && 'Could not submit the quiz. Try again.'}</FormError>
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
