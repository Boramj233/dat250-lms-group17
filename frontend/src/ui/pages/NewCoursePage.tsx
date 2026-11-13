import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { paths } from '../../app/paths'
import { useCreateCourse } from '../../data/courses/hooks/mutations/useCreateCourse'
import type { Term } from '../../data/courses/types'
import { termOf } from '../components/courseFormat'
import { TrailPage } from '../navigation/Trail'
import '../components/forms.css'

/**
 * Course managers create a course here. The backend must make the creator a teacher
 * of the new course, so they can add members, modules and content afterwards.
 */
export default function NewCoursePage() {
  const navigate = useNavigate()
  const createCourse = useCreateCourse()
  const [initialTerm] = useState(() => termOf(new Date()))
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [term, setTerm] = useState<Term>(initialTerm.term)
  const [year, setYear] = useState(initialTerm.year)

  function submit(event: FormEvent) {
    event.preventDefault()
    createCourse.mutate(
      { title: title.trim(), description: description.trim(), term, year },
      { onSuccess: course => navigate(paths.course(course.id)) },
    )
  }

  return (
    <TrailPage label="New course">
      <h1>New course</h1>
      <form className="form" onSubmit={submit}>
        <div className="form-field">
          <label htmlFor="course-title">Title</label>
          <input id="course-title" required maxLength={255} value={title}
            onChange={event => setTitle(event.target.value)} placeholder="DAT250 Advanced Software Technologies" />
        </div>
        <div className="form-field">
          <label htmlFor="course-description">Description</label>
          <textarea id="course-description" required value={description}
            onChange={event => setDescription(event.target.value)} />
        </div>
        <div className="form-row">
          <div className="form-field">
            <label htmlFor="course-term">Term</label>
            <select id="course-term" value={term} onChange={event => setTerm(event.target.value as Term)}>
              <option value="SPRING">Spring</option>
              <option value="AUTUMN">Autumn</option>
            </select>
          </div>
          <div className="form-field">
            <label htmlFor="course-year">Year</label>
            <input id="course-year" type="number" required min={1} value={year}
              onChange={event => setYear(Number(event.target.value))} />
          </div>
        </div>
        {createCourse.error && <p className="form-error" role="alert">Could not create the course. Try again.</p>}
        <div className="form-actions">
          <button type="submit" className="button-primary" disabled={createCourse.isPending}>
            {createCourse.isPending ? 'Creating…' : 'Create course'}
          </button>
          <button type="button" className="button-secondary" onClick={() => navigate(paths.courses)}>Cancel</button>
        </div>
      </form>
    </TrailPage>
  )
}
