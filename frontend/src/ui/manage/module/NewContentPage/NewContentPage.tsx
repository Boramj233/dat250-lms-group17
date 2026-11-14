import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { paths } from '../../../../app/paths'
import { useCreateContent } from '../../../../data/content/hooks'
import type { CreateContentInput, NewQuizQuestion } from '../../../../data/content/types'
import PageHeader from '../../../../shared/ui/PageHeader'
import NotFoundPage from '../../../pages/NotFoundPage'
import { contentTypeBySlug } from '../../contentTypes'
import { combineExtensions, FILE_TYPE_PRESETS, formatExtensions } from '../../fileTypes'
import { useManageCourse } from '../../manageCourseContext'
import { useManageModule } from '../manageModuleContext'
import { emptyQuestion, firstInvalidQuestion } from './quizQuestions'
import QuizQuestionsEditor from './QuizQuestionsEditor'
import '../../../../shared/styles/forms.css'
import './NewContentPage.css'

/**
 * Create one type of content at the end of the module: /new/assignment, /new/quiz,
 * /new/coding-exercise or /new/page. Only the fields of that type are shown.
 */
export default function NewContentPage() {
  const info = contentTypeBySlug(useParams().type)
  const navigate = useNavigate()
  const { course } = useManageCourse()
  const { module, number, content } = useManageModule()
  const createContent = useCreateContent()

  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [deadline, setDeadline] = useState('')
  const [allowLate, setAllowLate] = useState(false)
  const [mandatory, setMandatory] = useState(false)
  const [groupAssignment, setGroupAssignment] = useState(false)
  const [fileTypePresets, setFileTypePresets] = useState<string[]>([])
  const [otherFileTypes, setOtherFileTypes] = useState('')
  const [starterUrl, setStarterUrl] = useState('')
  const [starterCode, setStarterCode] = useState('')
  const [maxAttempts, setMaxAttempts] = useState(1)
  const [questions, setQuestions] = useState<NewQuizQuestion[]>([emptyQuestion()])
  const [problem, setProblem] = useState<string | null>(null)

  if (!info) return <NotFoundPage />
  const type = info.type
  const backTo = paths.manageModule(course.id, module.id)

  function submit(event: FormEvent) {
    event.preventDefault()
    setProblem(null)
    const invalid = type === 'QUIZ' ? firstInvalidQuestion(questions) : undefined
    if (invalid) {
      setProblem(`Question ${invalid} needs a text, text in every answer and at least one correct answer.`)
      return
    }
    if (type === 'CODING_EXERCISE' && !starterUrl.trim() && !starterCode.trim()) {
      setProblem('Add a starter repository, starter code, or both.')
      return
    }
    // At the end of the module.
    const base = { courseId: course.id, moduleId: module.id, title: title.trim(), position: content.length }
    const deadlineIso = deadline ? new Date(deadline).toISOString() : null
    const input: CreateContentInput =
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
            : { ...base, type, maxAttempts, questions }
    createContent.mutate(input, { onSuccess: () => navigate(backTo) })
  }

  return (
    <>
      <PageHeader title={`New ${info.label.toLowerCase()}`} kicker={`Module ${number}: ${module.title}`} />

      <form className="form" onSubmit={submit}>
        <div className="form-field">
          <label htmlFor="content-title">Title</label>
          <input id="content-title" required maxLength={255} value={title} onChange={event => setTitle(event.target.value)} />
        </div>

        {type !== 'QUIZ' && (
          <div className="form-field">
            <label htmlFor="content-body">
              {type === 'STATIC_PAGE' ? 'Text' : type === 'CODING_EXERCISE' ? 'Description' : 'Instructions'}
            </label>
            <textarea id="content-body" required maxLength={10000} value={body}
              onChange={event => setBody(event.target.value)} />
          </div>
        )}

        {type === 'CODING_EXERCISE' && (
          <>
            <div className="form-field">
              <label htmlFor="content-starter">Starter repository URL</label>
              <input id="content-starter" type="url" maxLength={2048} value={starterUrl}
                onChange={event => setStarterUrl(event.target.value)} placeholder="https://github.com/…" />
            </div>
            <div className="form-field">
              <label htmlFor="content-starter-code">Starter code</label>
              <textarea id="content-starter-code" className="code-input" spellCheck={false} value={starterCode}
                onChange={event => setStarterCode(event.target.value)} />
              <p className="form-hint">A repository, starter code, or both.</p>
            </div>
          </>
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
              <>
                <label className="form-check">
                  <input type="checkbox" checked={mandatory} onChange={event => setMandatory(event.target.checked)} />
                  Mandatory
                </label>
                <label className="form-check">
                  <input type="checkbox" checked={groupAssignment}
                    onChange={event => setGroupAssignment(event.target.checked)} />
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
                          onChange={event => setFileTypePresets(previous => event.target.checked
                            ? [...previous, preset.label]
                            : previous.filter(label => label !== preset.label))}
                        />
                        {preset.label}
                      </label>
                    ))}
                  </div>
                  <label htmlFor="file-types-other">Other extensions</label>
                  <input id="file-types-other" value={otherFileTypes} placeholder="e.g. .py, .java"
                    onChange={event => setOtherFileTypes(event.target.value)} />
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
                onChange={event => setMaxAttempts(Number(event.target.value))} />
            </div>
            <QuizQuestionsEditor questions={questions} onChange={setQuestions} />
          </>
        )}

        {problem && <p className="form-error" role="alert">{problem}</p>}
        {createContent.error && <p className="form-error" role="alert">Could not save. Try again.</p>}
        <div className="form-actions">
          <button type="submit" className="button-primary" disabled={createContent.isPending}>
            {createContent.isPending ? 'Saving…' : `Create ${info.label.toLowerCase()}`}
          </button>
          <Link className="button-secondary" to={backTo}>Cancel</Link>
        </div>
      </form>
    </>
  )
}
