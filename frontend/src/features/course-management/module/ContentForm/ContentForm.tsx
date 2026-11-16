import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { fromDateTimeLocal, toDateTimeLocal } from '../../../../shared/lib/dateFormat'
import type { Content, ContentFields, ContentType, NewQuizQuestion } from '../../../content/types'
import { checkContentVisibleFrom } from '../../../content/visibility'
import type { CourseModule } from '../../../modules/types'
import { useLeaveGuard } from '../../../../shared/ui/SaveBar'
import { combineExtensions, FILE_TYPE_PRESETS, formatExtensions, splitExtensions } from '../../fileTypes'
import VisibleFromField from '../VisibleFromField'
import { emptyQuestion, firstInvalidQuestion, questionsFromContent } from './quizQuestions'
import QuizQuestionsEditor from './QuizQuestionsEditor'
import '../../../../shared/styles/forms.css'
import './ContentForm.css'

type ContentFormProps = {
  type: ContentType
  module: CourseModule
  /** The saved item when editing; leave out to create a new one. */
  initial?: Content
  /** The submit button's text, e.g. "Create assignment" or "Save changes". */
  submitLabel: string
  pending: boolean
  /** Shown above the buttons when saving failed. */
  error?: string
  /** Shown beside the buttons after saving, e.g. "Saved." */
  success?: string
  /** Where Cancel goes. */
  cancelTo: string
  onSubmit: (fields: ContentFields) => void
  /** Called on every change, e.g. to clear a "Saved." message. */
  onChange?: () => void
}

/**
 * All fields of one type of content, for creating it and for editing it until it is
 * published. Only the fields of that type are shown.
 */
export default function ContentForm(
  { type, module, initial, submitLabel, pending, error, success, cancelTo, onSubmit, onChange }: ContentFormProps,
) {
  const start = initialValues(initial)
  const [title, setTitle] = useState(start.title)
  // Visible to students from; empty inherits the module's start date.
  const [visibleFrom, setVisibleFrom] = useState(start.visibleFrom)
  const [body, setBody] = useState(start.body)
  const [deadline, setDeadline] = useState(start.deadline)
  const [allowLate, setAllowLate] = useState(start.allowLate)
  const [mandatory, setMandatory] = useState(start.mandatory)
  const [groupAssignment, setGroupAssignment] = useState(start.groupAssignment)
  const [fileTypePresets, setFileTypePresets] = useState<string[]>(start.fileTypePresets)
  const [otherFileTypes, setOtherFileTypes] = useState(start.otherFileTypes)
  const [starterUrl, setStarterUrl] = useState(start.starterUrl)
  const [starterCode, setStarterCode] = useState(start.starterCode)
  const [maxAttempts, setMaxAttempts] = useState(start.maxAttempts)
  // Correct answers needed to pass; empty for no pass requirement.
  const [requiredCorrect, setRequiredCorrect] = useState(start.requiredCorrect)
  const [questions, setQuestions] = useState<NewQuizQuestion[]>(start.questions)
  const [problem, setProblem] = useState<string | null>(null)
  // Changed since the form opened or was last sent: leaving (also Cancel) asks first.
  const [dirty, setDirty] = useState(false)
  const leaveDialog = useLeaveGuard(dirty)

  /** Wraps a setter so every change marks the form changed and tells the parent. */
  function changed<T>(set: (value: T) => void) {
    return (value: T) => { set(value); setDirty(true); onChange?.() }
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    setProblem(null)
    const invalid = type === 'QUIZ' ? firstInvalidQuestion(questions) : undefined
    if (invalid) {
      setProblem(`Question ${invalid} needs a text, text in every answer and at least one correct answer.`)
      return
    }
    const requiredCorrectAnswers = requiredCorrect.trim() === '' ? null : Number(requiredCorrect)
    if (type === 'QUIZ' && requiredCorrectAnswers !== null
      && (!Number.isInteger(requiredCorrectAnswers) || requiredCorrectAnswers < 1 || requiredCorrectAnswers > questions.length)) {
      setProblem(`Correct answers to pass must be a whole number from 1 to ${questions.length} (the number of questions), or empty.`)
      return
    }
    if (type === 'CODING_EXERCISE' && !starterUrl.trim() && !starterCode.trim()) {
      setProblem('Add a starter repository, starter code, or both.')
      return
    }
    const visibleFromIso = fromDateTimeLocal(visibleFrom)
    const visibleProblem = checkContentVisibleFrom(visibleFromIso, module.availableFrom)
    if (visibleProblem) {
      setProblem(visibleProblem)
      return
    }
    setDirty(false)
    const base = { title: title.trim(), visibleFrom: visibleFromIso }
    const deadlineIso = fromDateTimeLocal(deadline)
    onSubmit(
      type === 'STATIC_PAGE' ? { ...base, type, body }
        : type === 'ASSIGNMENT' ? {
          ...base, type, instructions: body, deadline: deadlineIso, allowLateSubmission: allowLate, mandatory,
          groupAssignment, acceptedFileTypes: combineExtensions(fileTypePresets, otherFileTypes),
        }
          : type === 'CODING_EXERCISE' ? {
            ...base, type, instructions: body, deadline: deadlineIso, allowLateSubmission: allowLate,
            starterRepositoryUrl: starterUrl.trim() || null,
            starterCode: starterCode.trim() ? starterCode : null,
          }
            : { ...base, type, maxAttempts, requiredCorrectAnswers, questions },
    )
  }

  return (
    <form className="form" onSubmit={submit}>
      <div className="form-field">
        <label htmlFor="content-title">Title</label>
        <input id="content-title" required maxLength={255} value={title}
          onChange={event => changed(setTitle)(event.target.value)} />
      </div>

      <VisibleFromField value={visibleFrom} onChange={changed(setVisibleFrom)} module={module}
        problem={checkContentVisibleFrom(fromDateTimeLocal(visibleFrom), module.availableFrom)} />

      {type !== 'QUIZ' && (
        <div className="form-field">
          <label htmlFor="content-body">
            {type === 'STATIC_PAGE' ? 'Text' : type === 'CODING_EXERCISE' ? 'Description' : 'Instructions'}
          </label>
          <textarea id="content-body" required maxLength={10000} value={body}
            onChange={event => changed(setBody)(event.target.value)} />
        </div>
      )}

      {type === 'CODING_EXERCISE' && (
        <>
          <div className="form-field">
            <label htmlFor="content-starter">Starter repository URL</label>
            <input id="content-starter" type="url" maxLength={2048} value={starterUrl}
              onChange={event => changed(setStarterUrl)(event.target.value)} placeholder="https://github.com/…" />
          </div>
          <div className="form-field">
            <label htmlFor="content-starter-code">Starter code</label>
            <textarea id="content-starter-code" className="code-input" spellCheck={false} value={starterCode}
              onChange={event => changed(setStarterCode)(event.target.value)} />
            <p className="form-hint">A repository, starter code, or both.</p>
          </div>
        </>
      )}

      {(type === 'ASSIGNMENT' || type === 'CODING_EXERCISE') && (
        <>
          <div className="form-field">
            <label htmlFor="content-deadline">Deadline</label>
            <input id="content-deadline" type="datetime-local" value={deadline}
              onChange={event => changed(setDeadline)(event.target.value)} />
            <p className="form-hint">Leave empty for no deadline. Your local time.</p>
          </div>
          <label className="form-check">
            <input type="checkbox" checked={allowLate} onChange={event => changed(setAllowLate)(event.target.checked)} />
            Allow late submissions
          </label>
          {type === 'ASSIGNMENT' && (
            <>
              <label className="form-check">
                <input type="checkbox" checked={mandatory} onChange={event => changed(setMandatory)(event.target.checked)} />
                Mandatory
              </label>
              <label className="form-check">
                <input type="checkbox" checked={groupAssignment}
                  onChange={event => changed(setGroupAssignment)(event.target.checked)} />
                Group assignment (one submission per group)
              </label>

              <fieldset className="form-field file-types">
                <legend className="form-label">Accepted file types</legend>
                <div className="form-row">
                  {FILE_TYPE_PRESETS.map(preset => (
                    <label key={preset.label} className="form-check" title={preset.extensions.join(', ')}>
                      <input
                        type="checkbox"
                        checked={fileTypePresets.includes(preset.label)}
                        onChange={event => changed(setFileTypePresets)(event.target.checked
                          ? [...fileTypePresets, preset.label]
                          : fileTypePresets.filter(label => label !== preset.label))}
                      />
                      {preset.label}
                    </label>
                  ))}
                </div>
                <label htmlFor="file-types-other">Other extensions</label>
                <input id="file-types-other" value={otherFileTypes} placeholder="e.g. .py, .java"
                  onChange={event => changed(setOtherFileTypes)(event.target.value)} />
                <p className="form-hint">
                  Accepted: {formatExtensions(combineExtensions(fileTypePresets, otherFileTypes))}.
                  Tick nothing to accept any file type.
                </p>
              </fieldset>
            </>
          )}
        </>
      )}

      {type === 'QUIZ' && (
        <>
          <div className="form-field">
            <label htmlFor="quiz-attempts">Attempts allowed</label>
            <input id="quiz-attempts" type="number" required min={1} value={maxAttempts}
              onChange={event => changed(setMaxAttempts)(Number(event.target.value))} />
          </div>
          <div className="form-field">
            <label htmlFor="quiz-required-correct">Correct answers to pass</label>
            <input id="quiz-required-correct" type="number" min={1} max={questions.length} step={1}
              inputMode="numeric" value={requiredCorrect} placeholder="No pass requirement"
              aria-describedby="quiz-required-correct-hint"
              onChange={event => changed(setRequiredCorrect)(event.target.value)} />
            <p id="quiz-required-correct-hint" className="form-hint">
              An attempt is passed automatically when at least this many of the {questions.length}
              {questions.length === 1 ? ' question is' : ' questions are'} correct. Leave it empty for no pass requirement.
            </p>
          </div>
          <QuizQuestionsEditor questions={questions} onChange={changed(setQuestions)} />
        </>
      )}

      {problem && <p className="form-error" role="alert">{problem}</p>}
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className="form-actions">
        <button type="submit" className="button-primary" disabled={pending}>
          {pending ? 'Saving…' : submitLabel}
        </button>
        <Link className="button-secondary" to={cancelTo}>Cancel</Link>
        {dirty && !pending && <span className="form-hint">Unsaved changes</span>}
        {success && !dirty && <span className="form-success" role="status">{success}</span>}
      </div>
      {leaveDialog}
    </form>
  )
}

/** The form's starting values: empty for a new item, otherwise the saved item's. */
function initialValues(content: Content | undefined) {
  const files = content?.type === 'ASSIGNMENT' ? splitExtensions(content.acceptedFileTypes ?? []) : undefined
  const submittable = content?.type === 'ASSIGNMENT' || content?.type === 'CODING_EXERCISE' ? content : undefined
  return {
    title: content?.title ?? '',
    visibleFrom: toDateTimeLocal(content?.visibleFrom),
    body: content?.type === 'STATIC_PAGE' ? content.body : submittable?.instructions ?? '',
    deadline: toDateTimeLocal(submittable?.deadline),
    allowLate: submittable?.allowLateSubmission ?? false,
    mandatory: content?.type === 'ASSIGNMENT' ? content.mandatory ?? false : false,
    groupAssignment: content?.type === 'ASSIGNMENT' ? content.groupAssignment ?? false : false,
    fileTypePresets: files?.presetLabels ?? [],
    otherFileTypes: files?.other ?? '',
    starterUrl: content?.type === 'CODING_EXERCISE' ? content.starterRepositoryUrl ?? '' : '',
    starterCode: content?.type === 'CODING_EXERCISE' ? content.starterCode ?? '' : '',
    maxAttempts: content?.type === 'QUIZ' ? content.maxAttempts : 1,
    requiredCorrect: content?.type === 'QUIZ' && content.requiredCorrectAnswers != null
      ? String(content.requiredCorrectAnswers) : '',
    questions: content?.type === 'QUIZ' && content.questions?.length
      ? questionsFromContent(content.questions) : [emptyQuestion()],
  }
}
