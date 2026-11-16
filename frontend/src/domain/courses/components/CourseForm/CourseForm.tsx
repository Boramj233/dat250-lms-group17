import type { FormEvent, ReactNode } from 'react'
import type { CreateCourseInput, Term } from '../../types'
import { Field, FormError, RequiredFieldsNote, SubmitButton, useFormValues } from '@shared/ui/Form'

type CourseFormProps = {
  /** Values to start with, e.g. the course being edited. */
  initial: CreateCourseInput
  /** Called with the trimmed values when the form is sent. */
  onSubmit: (values: CreateCourseInput) => void
  /** Text on the submit button, e.g. "Create course". */
  submitLabel: string
  /** Disables the submit button and shows "Saving…" while the course is saved. */
  saving?: boolean
  /** Error message to show above the buttons. */
  error?: ReactNode
  /** More buttons next to the submit button, e.g. a cancel link. */
  actions?: ReactNode
  /** Called with the current values on every change, e.g. to know if there are unsaved changes. */
  onChange?: (values: CreateCourseInput) => void
}

/** The fields of a course: title, description, term and year. Used to create and edit courses. */
export default function CourseForm({
  initial,
  onSubmit,
  submitLabel,
  saving = false,
  error,
  actions,
  onChange,
}: CourseFormProps) {
  const { values, set } = useFormValues(initial, onChange)

  function submit(event: FormEvent) {
    event.preventDefault()
    onSubmit({ ...values, title: values.title.trim(), description: values.description.trim() })
  }

  return (
    <form className="form" onSubmit={submit}>
      <RequiredFieldsNote />
      <Field label="Title" required>
        {props => (
          <input
            {...props}
            maxLength={255}
            value={values.title}
            onChange={event => set('title')(event.target.value)}
            placeholder="DAT250 Advanced Software Technologies"
          />
        )}
      </Field>
      <Field label="Description" required>
        {props => (
          <textarea {...props} value={values.description} onChange={event => set('description')(event.target.value)} />
        )}
      </Field>
      <div className="form-row">
        <Field label="Term" required>
          {props => (
            <select {...props} value={values.term} onChange={event => set('term')(event.target.value as Term)}>
              <option value="SPRING">Spring</option>
              <option value="AUTUMN">Autumn</option>
            </select>
          )}
        </Field>
        <Field label="Year" required>
          {props => (
            <input
              {...props}
              type="number"
              min={1}
              value={values.year}
              onChange={event => set('year')(Number(event.target.value))}
            />
          )}
        </Field>
      </div>
      <FormError>{error}</FormError>
      <div className="form-actions">
        <SubmitButton pending={saving}>{submitLabel}</SubmitButton>
        {actions}
      </div>
    </form>
  )
}
