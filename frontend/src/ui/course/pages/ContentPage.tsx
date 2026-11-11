import { useState } from 'react'
import { useParams } from 'react-router'
import { paths } from '../../../app/paths'
import { useContent } from '../../../data/content/hooks/queries/useContent'
import { useModuleContent } from '../../../data/content/hooks/queries/useModuleContent'
import type { AssignmentContent, CodingExerciseContent, Content } from '../../../data/content/types'
import { formatDateTime, formatTimeLeft } from '../../components/dateFormat'
import MandatoryBadge from '../../components/MandatoryBadge'
import { safeHttpUrl } from '../../components/safeUrl'
import CompletedMark from '../CompletedMark'
import { CourseLink } from '../CourseLink'
import ContentTypeIcon from '../ContentTypeIcon'
import SubmissionFeedback from '../SubmissionFeedback'
import { useCourseContext } from '../courseContext'
import { contentTypeInfo } from '../contentTypes'
import './CoursePages.css'

/** One content item (course/module/content), with links to the previous and next item. */
export default function ContentPage() {
  const { moduleId, contentId } = useParams()
  const { course, modules, completed } = useCourseContext()
  const items = useModuleContent(course.id, moduleId)

  const module = modules.find(m => String(m.id) === moduleId)
  const index = items.data?.findIndex(item => String(item.id) === contentId) ?? -1
  const summary = index >= 0 ? items.data![index] : undefined
  // The type comes from the module's content list; the backend has one endpoint per type.
  const content = useContent(summary?.type, contentId ?? '')

  if (!module) return <p role="alert">This module does not exist in {course.title}.</p>
  if (items.isPending) return <p role="status">Loading…</p>
  if (items.error) return <p role="alert">Could not load the content of this module.</p>
  if (!summary) return <p role="alert">This content does not exist in {module.title}.</p>

  const previous = items.data[index - 1]
  const next = items.data[index + 1]
  const info = contentTypeInfo(summary.type)

  return (
    <>
      <header className="course-header">
        <p className="course-header-kicker content-kicker">
          <ContentTypeIcon type={summary.type} />
          {info.label}
          {content.data?.type === 'ASSIGNMENT' && content.data.mandatory && <MandatoryBadge />}
          {completed?.has(String(summary.id)) && <CompletedMark label />}
        </p>
        <h1>{summary.title}</h1>
      </header>

      {content.isPending && <p role="status">Loading {info.label.toLowerCase()}…</p>}
      {content.error && <p role="alert">Could not load this {info.label.toLowerCase()}.</p>}
      {content.data && <ContentBody content={content.data} />}

      <nav className="pager" aria-label={`Content in ${module.title}`}>
        {previous && (
          <CourseLink className="pager-link is-previous" to={paths.courseContent(course.id, module.id, previous.id)}>
            <span className="pager-direction">Previous</span>
            <span className="pager-title">{previous.title}</span>
          </CourseLink>
        )}
        {next && (
          <CourseLink className="pager-link is-next" to={paths.courseContent(course.id, module.id, next.id)}>
            <span className="pager-direction">Next</span>
            <span className="pager-title">{next.title}</span>
          </CourseLink>
        )}
      </nav>
    </>
  )
}

function ContentBody({ content }: { content: Content }) {
  switch (content.type) {
    case 'STATIC_PAGE':
      // Plain text from the backend: React escapes it; never insert it as HTML.
      return <div className="content-text">{content.body}</div>
    case 'QUIZ':
      // TODO: Start and take the quiz (data/quizzes) once the attempt API is in place.
      return (
        <dl className="content-facts">
          <div><dt>Attempts allowed</dt><dd>{content.maxAttempts}</dd></div>
        </dl>
      )
    case 'ASSIGNMENT':
    case 'CODING_EXERCISE':
      return <TaskBody content={content} />
  }
}

/** Assignments and coding exercises: deadline, late submission, starter code, instructions. */
function TaskBody({ content }: { content: AssignmentContent | CodingExerciseContent }) {
  const [now] = useState(() => new Date())
  const deadline = content.deadline ? new Date(content.deadline) : undefined
  const starterUrl = content.type === 'CODING_EXERCISE' ? safeHttpUrl(content.starterRepositoryUrl) : undefined

  // TODO: A way to submit (data/submission; backend POST /api/contents/{id}/submissions).
  return (
    <>
      <dl className="content-facts">
        <div>
          <dt>Deadline</dt>
          <dd>
            {deadline
              ? <><time dateTime={content.deadline!}>{formatDateTime(deadline)}</time> ({formatTimeLeft(deadline, now)})</>
              : 'No deadline'}
          </dd>
        </div>
        {content.type === 'ASSIGNMENT' && content.mandatory !== undefined && (
          <div>
            <dt>Mandatory</dt>
            <dd>{content.mandatory ? 'Yes' : 'No'}</dd>
          </div>
        )}
        <div>
          <dt>Late submission</dt>
          <dd>{content.allowLateSubmission ? 'Allowed' : 'Not allowed'}</dd>
        </div>
        {starterUrl && (
          <div>
            <dt>Starter code</dt>
            <dd><a href={starterUrl} target="_blank" rel="noreferrer">{starterUrl}</a></dd>
          </div>
        )}
      </dl>
      <SubmissionFeedback contentId={content.id} />
      <h2 className="course-section-title">Instructions</h2>
      <div className="content-text">{content.instructions}</div>
    </>
  )
}
