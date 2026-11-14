import type { ReactNode } from 'react'
import PageHeader from '../../../../shared/ui/PageHeader'
import QueryState from '../../../../shared/ui/QueryState'
import { contentTypeLabel } from '../../../content/contentTypes'
import { useContent } from '../../../content/hooks'
import type { Content, ContentSummary } from '../../../content/types'
import QuizView from './QuizView'
import SubmittableContent from './SubmittableContent'
import './ContentView.css'

type ContentViewProps = {
  /** The item from its module's content list (title, type, active). */
  summary: ContentSummary
  isTeacher: boolean
  /** Shown under the title, e.g. which module the item is in. */
  context?: ReactNode
}

/**
 * One content item, wherever it is opened (in its module or from a course page): a page
 * (read-only), a quiz to take (QuizView), or an assignment or coding exercise that
 * students hand in (SubmittableContent).
 */
export default function ContentView({ summary, isTeacher, context }: ContentViewProps) {
  // The type comes from the content list, since each type has its own endpoint.
  const content = useContent(summary.type, summary.id)
  return (
    <article className="content-view">
      <PageHeader title={summary.title} kicker={contentTypeLabel(summary.type)}>
        {context && <p className="content-view-context">{context}</p>}
        {summary.active === false && <p className="content-view-hidden">Hidden from students</p>}
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
      return <div className="content-view-text">{content.body}</div>
    case 'ASSIGNMENT':
    case 'CODING_EXERCISE':
      return <SubmittableContent content={content} isTeacher={isTeacher} />
    case 'QUIZ':
      return <QuizView content={content} isTeacher={isTeacher} />
  }
}
