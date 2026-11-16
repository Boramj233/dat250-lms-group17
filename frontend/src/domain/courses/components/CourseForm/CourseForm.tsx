import { useState, type FormEvent, type ReactNode } from 'react'
import type { CreateCourseInput, Term } from '../../types'
import RequiredMark from '@shared/ui/RequiredMark'
import '@shared/styles/forms.css'

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
  const [title, setTitle] = useState(initial.title)
  const [description, setDescription] = useState(initial.description)
  const [term, setTerm] = useState<Term>(initial.term)
  const [year, setYear] = useState(initial.year)

  // Tells the parent the values after a change (the setters run first, so pass the new value).
  function report(change: Partial<CreateCourseInput>) {
    onChange?.({ title, description, term, year, ...change })
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    onSubmit({ title: title.trim(), description: description.trim(), term, year })
  }

  return (
    <form className="form" onSubmit={submit}>
      <p className="form-hint">
        Fields marked <span className="form-required">*</span> are required.
      </p>
      <div className="form-field">
        <label htmlFor="course-title">
          Title
          <RequiredMark />
        </label>
        <input
          id="course-title"
          required
          maxLength={255}
          value={title}
          onChange={event => {
            setTitle(event.target.value)
            report({ title: event.target.value })
          }}
          placeholder="DAT250 Advanced Software Technologies"
        />
      </div>
      <div className="form-field">
        <label htmlFor="course-description">
          Description
          <RequiredMark />
        </label>
        <textarea
          id="course-description"
          required
          value={description}
          onChange={event => {
            setDescription(event.target.value)
            report({ description: event.target.value })
          }}
        />
      </div>
      <div className="form-row">
        <div className="form-field">
          <label htmlFor="course-term">
            Term
            <RequiredMark />
          </label>
          <select
            id="course-term"
            required
            value={term}
            onChange={event => {
              setTerm(event.target.value as Term)
              report({ term: event.target.value as Term })
            }}
          >
            <option value="SPRING">Spring</option>
            <option value="AUTUMN">Autumn</option>
          </select>
        </div>
        <div className="form-field">
          <label htmlFor="course-year">
            Year
            <RequiredMark />
          </label>
          <input
            id="course-year"
            type="number"
            required
            min={1}
            value={year}
            onChange={event => {
              setYear(Number(event.target.value))
              report({ year: Number(event.target.value) })
            }}
          />
        </div>
      </div>
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
      <div className="form-actions">
        <button type="submit" className="button-primary" disabled={saving}>
          {saving ? 'Saving…' : submitLabel}
        </button>
        {actions}
      </div>
    </form>
  )
}
