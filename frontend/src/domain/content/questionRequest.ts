import type { NewQuizQuestion } from './types'

// The backend's CreateQuizQuestionRequest. Ordering and matching items get keys that are
// only used inside the request, to say which order or pairs are correct; the backend
// gives the stored items their own ids (UUIDs).
export interface CreateQuizQuestionRequest {
  questionText: string
  position: number
  hint: string | null
  type: NonNullable<NewQuizQuestion['type']>
  options: { optionText: string; position: number; correct: boolean }[]
  acceptedAnswers?: string[]
  ordering?: { items: { key: string; text: string }[]; correctOrder: string[] }
  matching?: {
    leftItems: { key: string; text: string }[]
    rightItems: { key: string; text: string }[]
    correctMatches: { leftKey: string; rightKey: string }[]
  }
}

/** CreateQuizQuestionRequest for a question at `position`, with only its type's fields. */
export function questionRequest(question: NewQuizQuestion, position: number): CreateQuizQuestionRequest {
  const type = question.type ?? 'MULTIPLE_CHOICE'
  const base = { questionText: question.questionText.trim(), position, hint: question.hint?.trim() || null, type }
  switch (type) {
    case 'MULTIPLE_CHOICE':
    case 'TRUE_FALSE':
      return {
        ...base,
        options: question.options.map((option, index) => ({
          optionText: option.optionText.trim(),
          correct: option.correct,
          position: index,
        })),
      }
    case 'FILL_IN_BLANK':
      return {
        ...base,
        options: [],
        acceptedAnswers: (question.acceptedAnswers ?? []).map(answer => answer.trim()).filter(Boolean),
      }
    case 'ORDERING': {
      const items = (question.orderingItems ?? []).map((text, index) => ({ key: `item-${index}`, text: text.trim() }))
      return { ...base, options: [], ordering: { items, correctOrder: items.map(item => item.key) } }
    }
    case 'MATCHING': {
      const pairs = question.matchingPairs ?? []
      return {
        ...base,
        options: [],
        matching: {
          leftItems: pairs.map((pair, index) => ({ key: `left-${index}`, text: pair.left.trim() })),
          rightItems: pairs.map((pair, index) => ({ key: `right-${index}`, text: pair.right.trim() })),
          correctMatches: pairs.map((_, index) => ({ leftKey: `left-${index}`, rightKey: `right-${index}` })),
        },
      }
    }
  }
}
