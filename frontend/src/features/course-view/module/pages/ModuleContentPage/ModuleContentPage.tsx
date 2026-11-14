import { useParams } from 'react-router'
import { formatDateTime } from '../../../../../shared/lib/dateFormat'
import { safeHttpUrl } from '../../../../../shared/lib/safeUrl'
import PageHeader from '../../../../../shared/ui/PageHeader'
import QueryState from '../../../../../shared/ui/QueryState'
import { contentTypeLabel } from '../../../../content/contentTypes'
import { useContent } from '../../../../content/hooks'
import type { Content } from '../../../../content/types'
import { useModuleView } from '../../moduleContext'
import './ModuleContentPage.css'

/** One content item in the module, read-only: a page, assignment, quiz or exercise. */
export default function ModuleContentPage() {
  const { contentId = '' } = useParams()
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
        {data => <ContentBody content={data} />}
      </QueryState>
    </article>
  )
}

function ContentBody({ content }: { content: Content }) {
  switch (content.type) {
    case 'STATIC_PAGE':
      return <div className="module-content-text">{content.body}</div>

    case 'ASSIGNMENT':
      return (
        <>
          <Deadline deadline={content.deadline} allowLate={content.allowLateSubmission} />
          <Instructions text={content.instructions} />
        </>
      )

    case 'CODING_EXERCISE': {
      const repository = safeHttpUrl(content.starterRepositoryUrl)
      return (
        <>
          <Deadline deadline={content.deadline} allowLate={content.allowLateSubmission} />
          {repository && (
            <p>Starter repository: <a href={repository} target="_blank" rel="noreferrer">{repository}</a></p>
          )}
          <Instructions text={content.instructions} />
          {content.starterCode && (
            <section aria-labelledby="starter-code-title">
              <h2 id="starter-code-title" className="section-title">Starter code</h2>
              <pre className="module-content-code"><code>{content.starterCode}</code></pre>
            </section>
          )}
        </>
      )
    }

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

function Deadline({ deadline, allowLate }: { deadline: string | null, allowLate: boolean }) {
  return (
    <p className="module-content-facts">
      {deadline ? `Deadline: ${formatDateTime(new Date(deadline))}` : 'No deadline'}
      {deadline && (allowLate ? ' · Late submissions allowed' : ' · No late submissions')}
    </p>
  )
}

function Instructions({ text }: { text: string }) {
  return (
    <section aria-labelledby="instructions-title">
      <h2 id="instructions-title" className="section-title">Instructions</h2>
      <div className="module-content-text">{text || 'No instructions.'}</div>
    </section>
  )
}
