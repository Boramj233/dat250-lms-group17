import { useParams } from 'react-router'
import PageHeader from '../../../../../shared/ui/PageHeader'
import QueryState from '../../../../../shared/ui/QueryState'
import { contentTypeLabel } from '../../../../content/contentTypes'
import { useContent } from '../../../../content/hooks'
import type { Content } from '../../../../content/types'
import { useCourseView } from '../../../courseContext'
import { useModuleView } from '../../moduleContext'
import SubmittableContent from './SubmittableContent'
import './ModuleContentPage.css'

/**
 * One content item in the module: a page or quiz (read-only), or an assignment or coding
 * exercise that students hand in (SubmittableContent).
 */
export default function ModuleContentPage() {
  const { contentId = '' } = useParams()
  const { isTeacher } = useCourseView()
  const { items } = useModuleView()
  // The type comes from the module's list, since each type has its own endpoint.
  const summary = items.find(item => String(item.id) === contentId)
  const content = useContent(summary?.type, contentId)

  if (!summary) return <p role="alert">This content does not exist in this module.</p>

  return (
    <article className="module-content">
      <PageHeader title={summary.title} kicker={contentTypeLabel(summary.type)}>
        {summary.active === false && <p className="module-content-hidden">Hidden from students</p>}
      </PageHeader>
      <QueryState query={content} loading="Loading…" error="Could not load this content. Try again later.">
        {data => <ContentBody content={data} isTeacher={isTeacher} />}
      </QueryState>
    </article>
  )
}

function ContentBody({ content, isTeacher }: { content: Content, isTeacher: boolean }) {
  switch (content.type) {
    case 'STATIC_PAGE':
      return <div className="module-content-text">{content.body}</div>

    case 'ASSIGNMENT':
    case 'CODING_EXERCISE':
      return <SubmittableContent content={content} isTeacher={isTeacher} />

    case 'QUIZ': {
      const questions = [...(content.questions ?? [])].sort((a, b) => a.position - b.position)
      return (
        <>
          <p className="module-content-facts">
            {questions.length} {questions.length === 1 ? 'question' : 'questions'}
            {' · '}
            {content.maxAttempts} {content.maxAttempts === 1 ? 'attempt' : 'attempts'} allowed
          </p>
          <ol className="module-content-questions">
            {questions.map(question => (
              <li key={question.id}>
                <p>{question.questionText}</p>
                <ul>
                  {[...question.options].sort((a, b) => a.position - b.position).map(option => (
                    <li key={option.id}>{option.optionText}</li>
                  ))}
                </ul>
              </li>
            ))}
          </ol>
        </>
      )
    }
  }
}
