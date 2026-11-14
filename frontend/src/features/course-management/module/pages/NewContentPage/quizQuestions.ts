import type { NewQuizQuestion } from '../../../../content/types'

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
