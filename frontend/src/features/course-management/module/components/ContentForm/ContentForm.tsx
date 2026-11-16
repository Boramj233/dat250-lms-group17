import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { fromDateTimeLocal } from '@shared/lib/dateFormat'
import { Field, FormError, FormSuccess, RequiredFieldsNote, SubmitButton, useFormValues } from '@shared/ui/Form'
import { useLeaveGuard } from '@shared/ui/SaveBar'
import { isSubmittableType } from '@domain/content/contentTypes'
import type { Content, ContentFields, ContentType } from '@domain/content/types'
import { checkContentVisibleFrom } from '@domain/content/visibility'
import type { CourseModule } from '@domain/modules/types'
import VisibleFromField from '../VisibleFromField'
import { QuizFields, StarterFields, SubmissionFields } from './ContentFormFields'
import { formProblem, initialValues, toFields } from './contentFormValues'
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
  // `dirty`: changed since the form opened or was last sent; leaving (also Cancel) asks first.
  const { values, set, dirty, setDirty } = useFormValues(
    () => initialValues(initial),
    () => onChange?.(),
  )
  const [problem, setProblem] = useState<string | null>(null)
  const leaveDialog = useLeaveGuard(dirty)

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
      <RequiredFieldsNote />

      <Field label="Title" required>
        {props => (
          <input {...props} maxLength={255} value={values.title} onChange={event => set('title')(event.target.value)} />
        )}
      </Field>

      <VisibleFromField
        value={values.visibleFrom}
        onChange={set('visibleFrom')}
        module={module}
        problem={checkContentVisibleFrom(fromDateTimeLocal(values.visibleFrom), module.availableFrom)}
      />

      {type !== 'QUIZ' && (
        <Field
          label={type === 'STATIC_PAGE' ? 'Text' : type === 'CODING_EXERCISE' ? 'Description' : 'Instructions'}
          required
        >
          {props => (
            <textarea
              {...props}
              maxLength={10000}
              value={values.body}
              onChange={event => set('body')(event.target.value)}
            />
          )}
        </Field>
      )}

      {type === 'CODING_EXERCISE' && <StarterFields values={values} set={set} />}
      {isSubmittableType(type) && <SubmissionFields type={type} values={values} set={set} />}
      {type === 'QUIZ' && <QuizFields values={values} set={set} />}

      <FormError>{problem}</FormError>
      <FormError>{error}</FormError>
      <div className="form-actions">
        <SubmitButton pending={pending}>{submitLabel}</SubmitButton>
        <Link className="button-secondary" to={cancelTo}>
          Cancel
        </Link>
        {dirty && !pending && <span className="form-hint">Unsaved changes</span>}
        <FormSuccess inline>{!dirty && success}</FormSuccess>
      </div>
      {leaveDialog}
    </form>
  )
}
