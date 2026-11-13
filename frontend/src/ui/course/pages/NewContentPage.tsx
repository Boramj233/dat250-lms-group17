import { useState, type FormEvent } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router'
import { paths } from '../../../app/paths'
import { useCreateContent } from '../../../data/content/hooks/mutations/useCreateContent'
import { useModuleContent } from '../../../data/content/hooks/queries/useModuleContent'
import type { ContentType, CreateContentInput, NewQuizQuestion } from '../../../data/content/types'
import { CourseLink } from '../CourseLink'
import { useCourseContext, useCourseLinkState } from '../courseContext'
import { CONTENT_TYPES, contentTypeBySlug } from '../contentTypes'
import '../../components/forms.css'
import './CoursePages.css'

const emptyQuestion = (): NewQuizQuestion => ({
  questionText: '',
  hint: '',
  options: [{ optionText: '', correct: true }, { optionText: '', correct: false }],
})

/**
 * Teachers: add a page, assignment, coding exercise or quiz (with questions) at the end
 * of a module. `?type=assignments` (a content type slug) preselects the type.
 */
export default function NewContentPage() {
  const { moduleId } = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { course, modules } = useCourseContext()
  const items = useModuleContent(course.id, moduleId)
  const createContent = useCreateContent()
  const linkState = useCourseLinkState()

  const [type, setType] = useState<ContentType>(contentTypeBySlug(searchParams.get('type') ?? '')?.type ?? 'STATIC_PAGE')
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [deadline, setDeadline] = useState('')
  const [allowLate, setAllowLate] = useState(false)
  const [mandatory, setMandatory] = useState(false)
  const [starterUrl, setStarterUrl] = useState('')
  const [maxAttempts, setMaxAttempts] = useState(1)
  const [questions, setQuestions] = useState<NewQuizQuestion[]>([emptyQuestion()])
  const [problem, setProblem] = useState<string | null>(null)

  const module = modules.find(m => String(m.id) === moduleId)
  if (!module) return <p role="alert">This module does not exist in {course.title}.</p>
  const moduleUrl = paths.courseModule(course.id, module.id)

  function updateQuestion(index: number, change: Partial<NewQuizQuestion>) {
    setQuestions(previous => previous.map((q, i) => (i === index ? { ...q, ...change } : q)))
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    setProblem(null)
    if (type === 'QUIZ') {
      const invalid = questions.findIndex(q =>
        !q.questionText.trim() || q.options.some(o => !o.optionText.trim()) || !q.options.some(o => o.correct))
      if (invalid >= 0) {
        setProblem(`Question ${invalid + 1} needs a text, text in every answer and at least one correct answer.`)
        return
      }
    }
    const base = {
      courseId: course.id,
      moduleId: module!.id,
      title: title.trim(),
      // At the end of the module.
      position: items.data?.length ?? 0,
    }
    const deadlineIso = deadline ? new Date(deadline).toISOString() : null
    const input: CreateContentInput =
      type === 'STATIC_PAGE' ? { ...base, type, body }
        : type === 'ASSIGNMENT' ? { ...base, type, instructions: body, deadline: deadlineIso, allowLateSubmission: allowLate, mandatory }
          : type === 'CODING_EXERCISE' ? { ...base, type, instructions: body, starterRepositoryUrl: starterUrl.trim(), deadline: deadlineIso, allowLateSubmission: allowLate }
            : { ...base, type, maxAttempts, questions }
    createContent.mutate(input, {
      onSuccess: created => navigate(paths.courseContent(course.id, module!.id, created.id), { state: linkState }),
    })
  }

  return (
    <>
      <header className="course-header">
        <p className="course-header-kicker">{module.title}</p>
        <h1>New content</h1>
      </header>

      <form className="form" onSubmit={submit}>
        <fieldset className="form-field content-type-choice">
          <legend className="form-label">Type</legend>
          <div className="form-row">
            {CONTENT_TYPES.map(info => (
              <label key={info.type} className="form-check">
                <input type="radio" name="content-type" value={info.type}
                  checked={type === info.type} onChange={() => setType(info.type)} />
                {info.label}
              </label>
            ))}
          </div>
        </fieldset>

        <div className="form-field">
          <label htmlFor="content-title">Title</label>
          <input id="content-title" required maxLength={255} value={title} onChange={event => setTitle(event.target.value)} />
        </div>

        {type !== 'QUIZ' && (
          <div className="form-field">
            <label htmlFor="content-body">{type === 'STATIC_PAGE' ? 'Text' : 'Instructions'}</label>
            <textarea id="content-body" required maxLength={10000} value={body}
              onChange={event => setBody(event.target.value)} />
          </div>
        )}

        {type === 'CODING_EXERCISE' && (
          <div className="form-field">
            <label htmlFor="content-starter">Starter repository URL</label>
            <input id="content-starter" type="url" required maxLength={2048} value={starterUrl}
              onChange={event => setStarterUrl(event.target.value)} placeholder="https://github.com/…" />
          </div>
        )}

        {(type === 'ASSIGNMENT' || type === 'CODING_EXERCISE') && (
          <>
            <div className="form-field">
              <label htmlFor="content-deadline">Deadline</label>
              <input id="content-deadline" type="datetime-local" value={deadline}
                onChange={event => setDeadline(event.target.value)} />
              <p className="form-hint">Leave empty for no deadline. Your local time.</p>
            </div>
            <label className="form-check">
              <input type="checkbox" checked={allowLate} onChange={event => setAllowLate(event.target.checked)} />
              Allow late submissions
            </label>
            {type === 'ASSIGNMENT' && (
              <label className="form-check">
                <input type="checkbox" checked={mandatory} onChange={event => setMandatory(event.target.checked)} />
                Mandatory
              </label>
            )}
          </>
        )}

        {type === 'QUIZ' && (
          <>
            <div className="form-field">
              <label htmlFor="quiz-attempts">Attempts allowed</label>
              <input id="quiz-attempts" type="number" required min={1} value={maxAttempts}
                onChange={event => setMaxAttempts(Number(event.target.value))} />
            </div>
            {questions.map((question, index) => (
              <fieldset key={index} className="quiz-question-editor">
                <legend className="form-label">Question {index + 1}</legend>
                <div className="form-field">
                  <label htmlFor={`q-${index}-text`}>Question</label>
                  <input id={`q-${index}-text`} value={question.questionText}
                    onChange={event => updateQuestion(index, { questionText: event.target.value })} />
                </div>
                <div className="form-field">
                  <label htmlFor={`q-${index}-hint`}>Hint (optional)</label>
                  <input id={`q-${index}-hint`} value={question.hint ?? ''}
                    onChange={event => updateQuestion(index, { hint: event.target.value })} />
                </div>
                <p className="form-hint">Answers: tick the correct ones.</p>
                {question.options.map((option, optionIndex) => (
                  <div key={optionIndex} className="quiz-option-editor">
                    <input type="checkbox" aria-label={`Answer ${optionIndex + 1} is correct`} checked={option.correct}
                      onChange={event => updateQuestion(index, {
                        options: question.options.map((o, i) => (i === optionIndex ? { ...o, correct: event.target.checked } : o)),
                      })} />
                    <input aria-label={`Answer ${optionIndex + 1}`} value={option.optionText}
                      placeholder={`Answer ${optionIndex + 1}`}
                      onChange={event => updateQuestion(index, {
                        options: question.options.map((o, i) => (i === optionIndex ? { ...o, optionText: event.target.value } : o)),
                      })} />
                    <button type="button" className="button-secondary" disabled={question.options.length <= 2}
                      onClick={() => updateQuestion(index, { options: question.options.filter((_, i) => i !== optionIndex) })}>
                      Remove<span className="visually-hidden"> answer {optionIndex + 1}</span>
                    </button>
                  </div>
                ))}
                <div className="form-actions">
                  <button type="button" className="button-secondary"
                    onClick={() => updateQuestion(index, { options: [...question.options, { optionText: '', correct: false }] })}>
                    Add answer
                  </button>
                  <button type="button" className="button-secondary" disabled={questions.length <= 1}
                    onClick={() => setQuestions(previous => previous.filter((_, i) => i !== index))}>
                    Remove question {index + 1}
                  </button>
                </div>
              </fieldset>
            ))}
            <div className="form-actions">
              <button type="button" className="button-secondary"
                onClick={() => setQuestions(previous => [...previous, emptyQuestion()])}>
                Add question
              </button>
            </div>
          </>
        )}

        {problem && <p className="form-error" role="alert">{problem}</p>}
        {createContent.error && <p className="form-error" role="alert">Could not save. Try again.</p>}
        <div className="form-actions">
          <button type="submit" className="button-primary" disabled={createContent.isPending}>
            {createContent.isPending ? 'Saving…' : 'Create'}
          </button>
          <CourseLink className="button-secondary" to={moduleUrl}>Cancel</CourseLink>
        </div>
      </form>
    </>
  )
}
