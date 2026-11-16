import type { NewQuizQuestion, QuizContentQuestion } from '../../../content/types'

/** A new question with two empty answers, the first one marked correct. */
export const emptyQuestion = (): NewQuizQuestion => ({
  questionText: '',
  hint: '',
  options: [{ optionText: '', correct: true }, { optionText: '', correct: false }],
})

/** The number (from 1) of the first question that is not complete, or undefined if all are. */
export function firstInvalidQuestion(questions: NewQuizQuestion[]): number | undefined {
  const index = questions.findIndex(q =>
    !q.questionText.trim() || q.options.some(o => !o.optionText.trim()) || !q.options.some(o => o.correct))
  return index >= 0 ? index + 1 : undefined
}

/**
 * A saved quiz's questions as editable ones, in order. `correct` and `hint` are only
 * sent to teachers (proposed); without them no answer is ticked.
 */
export function questionsFromContent(questions: QuizContentQuestion[]): NewQuizQuestion[] {
  return [...questions]
    .sort((a, b) => a.position - b.position)
    .map(question => ({
      questionText: question.questionText,
      hint: question.hint ?? '',
      options: [...question.options]
        .sort((a, b) => a.position - b.position)
        .map(option => ({ optionText: option.optionText, correct: option.correct ?? false })),
    }))
}
