import { fromDateTimeLocal, toDateTimeLocal } from '@shared/lib/dateFormat'
import { isSubmittable } from '@domain/content/contentTypes'
import type { Content, ContentFields, ContentType, NewQuizQuestion } from '@domain/content/types'
import { checkContentVisibleFrom } from '@domain/content/visibility'
import type { CourseModule } from '@domain/modules/types'
import type { QuizAnswerKey } from '@domain/quizzes/types'
import { combineExtensions, splitExtensions } from './fileTypes'
import { emptyQuestion, firstInvalidQuestion, questionsFromContent } from './quizQuestions'

/** Everything the form holds, as the fields show it (dates as datetime-local text). */
export type ContentFormValues = {
  title: string
  /** Visible to students from; empty inherits the module's start date. */
  visibleFrom: string
  /** A page's text, or the instructions of an assignment or coding exercise. */
  body: string
  deadline: string
  allowLate: boolean
  mandatoryForExam: boolean
  groupAssignment: boolean
  fileTypePresets: string[]
  otherFileTypes: string
  starterUrl: string
  starterCode: string
  maxAttempts: number
  /** Percentage of the questions that must be correct to pass, 1–100. */
  passingPercentage: number
  questions: NewQuizQuestion[]
}

/** The backend's default (Quiz.passingPercentage). */
export const DEFAULT_PASSING_PERCENTAGE = 60

/**
 * The form's starting values: empty for a new item, otherwise the saved item's. For a
 * quiz, `answerKey` fills in the correct answers of its questions.
 */
export function initialValues(content: Content | undefined, answerKey?: QuizAnswerKey): ContentFormValues {
  const files = content?.type === 'ASSIGNMENT' ? splitExtensions(content.acceptedFileTypes ?? []) : undefined
  const submittable = isSubmittable(content) ? content : undefined
  return {
    title: content?.title ?? '',
    visibleFrom: toDateTimeLocal(content?.visibleFrom),
    body: content?.type === 'STATIC_PAGE' ? content.body : (submittable?.instructions ?? ''),
    deadline: toDateTimeLocal(submittable?.deadline),
    allowLate: submittable?.allowLateSubmission ?? false,
    mandatoryForExam: content?.type === 'ASSIGNMENT' ? content.mandatoryForExam : false,
    groupAssignment: content?.type === 'ASSIGNMENT' ? (content.groupAssignment ?? false) : false,
    fileTypePresets: files?.presetLabels ?? [],
    otherFileTypes: files?.other ?? '',
    starterUrl: content?.type === 'CODING_EXERCISE' ? (content.starterRepositoryUrl ?? '') : '',
    starterCode: content?.type === 'CODING_EXERCISE' ? (content.starterCode ?? '') : '',
    maxAttempts: content?.type === 'QUIZ' ? content.maxAttempts : 1,
    passingPercentage: content?.type === 'QUIZ' ? content.passingPercentage : DEFAULT_PASSING_PERCENTAGE,
    questions:
      content?.type === 'QUIZ' && content.questions?.length
        ? questionsFromContent(content.questions, answerKey)
        : [emptyQuestion()],
  }
}

/** Why the form cannot be sent yet, or null when it can. The browser checks required fields itself. */
export function formProblem(type: ContentType, values: ContentFormValues, module: CourseModule): string | null {
  const invalid = type === 'QUIZ' ? firstInvalidQuestion(values.questions) : undefined
  if (invalid) return invalid
  const percentage = values.passingPercentage
  if (type === 'QUIZ' && (!Number.isInteger(percentage) || percentage < 1 || percentage > 100)) {
    return 'The passing percentage must be a whole number from 1 to 100.'
  }
  if (type === 'CODING_EXERCISE' && !values.starterUrl.trim() && !values.starterCode.trim()) {
    return 'Add a starter repository, starter code, or both.'
  }
  return checkContentVisibleFrom(fromDateTimeLocal(values.visibleFrom), module.availableFrom) ?? null
}

/** What is sent to the backend for this type; only that type's fields. */
export function toFields(type: ContentType, values: ContentFormValues): ContentFields {
  const base = { title: values.title.trim(), visibleFrom: fromDateTimeLocal(values.visibleFrom) }
  const deadline = fromDateTimeLocal(values.deadline)
  switch (type) {
    case 'STATIC_PAGE':
      return { ...base, type, body: values.body }
    case 'ASSIGNMENT':
      return {
        ...base,
        type,
        instructions: values.body,
        deadline,
        allowLateSubmission: values.allowLate,
        mandatoryForExam: values.mandatoryForExam,
        groupAssignment: values.groupAssignment,
        acceptedFileTypes: combineExtensions(values.fileTypePresets, values.otherFileTypes),
      }
    case 'CODING_EXERCISE':
      return {
        ...base,
        type,
        instructions: values.body,
        deadline,
        allowLateSubmission: values.allowLate,
        starterRepositoryUrl: values.starterUrl.trim() || null,
        starterCode: values.starterCode.trim() ? values.starterCode : null,
      }
    case 'QUIZ':
      return {
        ...base,
        type,
        maxAttempts: values.maxAttempts,
        passingPercentage: values.passingPercentage,
        questions: values.questions,
      }
  }
}
