import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { fromDateTimeLocal } from '@shared/lib/dateFormat'
import RequiredMark from '@shared/ui/RequiredMark'
import { useLeaveGuard } from '@shared/ui/SaveBar'
import { isSubmittableType } from '@domain/content/contentTypes'
import type { Content, ContentFields, ContentType } from '@domain/content/types'
import { checkContentVisibleFrom } from '@domain/content/visibility'
import type { CourseModule } from '@domain/modules/types'
import VisibleFromField from '../VisibleFromField'
import { QuizFields, StarterFields, SubmissionFields } from './ContentFormFields'
import { formProblem, initialValues, toFields, type ContentFormValues, type SetValue } from './contentFormValues'
import '@shared/styles/forms.css'
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
 * published. Only the fields of that type are shown (ContentFormFields); the values,
 * checks and what is sent are in contentFormValues.
 */
export default function ContentForm({
  type,
  module,
  initial,
  submitLabel,
  pending,
  error,
  success,
  cancelTo,
  onSubmit,
  onChange,
}: ContentFormProps) {
  const [values, setValues] = useState<ContentFormValues>(() => initialValues(initial))
  const [problem, setProblem] = useState<string | null>(null)
  // Changed since the form opened or was last sent: leaving (also Cancel) asks first.
  const [dirty, setDirty] = useState(false)
  const leaveDialog = useLeaveGuard(dirty)

  /** Every change marks the form changed and tells the parent. */
  const set: SetValue = key => value => {
    setValues(previous => ({ ...previous, [key]: value }))
    setDirty(true)
    onChange?.()
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    const found = formProblem(type, values, module)
    setProblem(found)
    if (found) return
    setDirty(false)
    onSubmit(toFields(type, values))
  }

  return (
    <form className="form" onSubmit={submit}>
      <p className="form-hint">
        Fields marked <span className="form-required">*</span> are required.
      </p>

      <div className="form-field">
        <label htmlFor="content-title">
          Title
          <RequiredMark />
        </label>
        <input
          id="content-title"
          required
          maxLength={255}
          value={values.title}
          onChange={event => set('title')(event.target.value)}
        />
      </div>

      <VisibleFromField
        value={values.visibleFrom}
        onChange={set('visibleFrom')}
        module={module}
        problem={checkContentVisibleFrom(fromDateTimeLocal(values.visibleFrom), module.availableFrom)}
      />

      {type !== 'QUIZ' && (
        <div className="form-field">
          <label htmlFor="content-body">
            {type === 'STATIC_PAGE' ? 'Text' : type === 'CODING_EXERCISE' ? 'Description' : 'Instructions'}
            <RequiredMark />
          </label>
          <textarea
            id="content-body"
            required
            maxLength={10000}
            value={values.body}
            onChange={event => set('body')(event.target.value)}
          />
        </div>
      )}

      {type === 'CODING_EXERCISE' && <StarterFields values={values} set={set} />}
      {isSubmittableType(type) && <SubmissionFields type={type} values={values} set={set} />}
      {type === 'QUIZ' && <QuizFields values={values} set={set} />}

      {problem && (
        <p className="form-error" role="alert">
          {problem}
        </p>
      )}
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
      <div className="form-actions">
        <button type="submit" className="button-primary" disabled={pending}>
          {pending ? 'Saving…' : submitLabel}
        </button>
        <Link className="button-secondary" to={cancelTo}>
          Cancel
        </Link>
        {dirty && !pending && <span className="form-hint">Unsaved changes</span>}
        {success && !dirty && (
          <span className="form-success" role="status">
            {success}
          </span>
        )}
      </div>
      {leaveDialog}
    </form>
  )
}
