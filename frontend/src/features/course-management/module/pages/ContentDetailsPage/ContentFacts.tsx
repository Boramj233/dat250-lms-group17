import type { ReactNode } from 'react'
import type { Content } from '../../../../content/types'
import { formatDateTime } from '../../../../../shared/lib/dateFormat'
import { safeHttpUrl } from '../../../../../shared/lib/safeUrl'
import { formatExtensions } from '../../../fileTypes'
import QuizQuestionsView from './QuizQuestionsView'

/**
 * What a teacher set when creating the content, read-only: the description and
 * starter repository and code of a coding exercise, an assignment's instructions,
 * a quiz's questions and answers, deadlines and so on. A page's text is edited in the
 * form instead, so nothing is shown.
 */
export default function ContentFacts({ content }: { content: Content }) {
  switch (content.type) {
    case 'STATIC_PAGE':
      return null
    case 'QUIZ':
      return (
        <>
          <dl className="content-facts">
            <Fact label="Attempts allowed">{content.maxAttempts}</Fact>
            <Fact label="Questions">{content.questions?.length ?? 'Unknown'}</Fact>
          </dl>
          <section aria-labelledby="quiz-questions-title">
            <h2 id="quiz-questions-title" className="section-title">Questions and answers</h2>
            {content.questions
              ? <QuizQuestionsView questions={content.questions} />
              : <p>The server did not send the questions.</p>}
          </section>
        </>
      )
    case 'ASSIGNMENT':
      return (
        <dl className="content-facts">
          <Fact label="Instructions" wide><p className="content-facts-text">{content.instructions}</p></Fact>
          <Fact label="Deadline">{formatDeadline(content.deadline)}</Fact>
          <Fact label="Late submissions">{content.allowLateSubmission ? 'Allowed' : 'Not allowed'}</Fact>
          {content.mandatory !== undefined && <Fact label="Mandatory">{content.mandatory ? 'Yes' : 'No'}</Fact>}
          {content.groupAssignment !== undefined && (
            <Fact label="Group assignment">{content.groupAssignment ? 'Yes, one submission per group' : 'No'}</Fact>
          )}
          {content.acceptedFileTypes !== undefined && (
            <Fact label="Accepted file types">{formatExtensions(content.acceptedFileTypes)}</Fact>
          )}
        </dl>
      )
    case 'CODING_EXERCISE': {
      const repository = safeHttpUrl(content.starterRepositoryUrl)
      return (
        <dl className="content-facts">
          <Fact label="Description" wide><p className="content-facts-text">{content.instructions}</p></Fact>
          <Fact label="Starter repository" wide>
            {repository
              ? <a href={repository} target="_blank" rel="noreferrer">{repository}</a>
              : 'None'}
          </Fact>
          <Fact label="Starter code" wide>
            {content.starterCode
              ? <pre className="content-facts-code"><code>{content.starterCode}</code></pre>
              : 'None'}
          </Fact>
          <Fact label="Deadline">{formatDeadline(content.deadline)}</Fact>
          <Fact label="Late submissions">{content.allowLateSubmission ? 'Allowed' : 'Not allowed'}</Fact>
        </dl>
      )
    }
  }
}

function Fact({ label, wide = false, children }: { label: string, wide?: boolean, children: ReactNode }) {
  return (
    <div className={`content-fact${wide ? ' is-wide' : ''}`}>
      <dt>{label}</dt>
      <dd>{children}</dd>
    </div>
  )
}

function formatDeadline(deadline: string | null): ReactNode {
  return deadline
    ? <time dateTime={deadline}>{formatDateTime(new Date(deadline))}</time>
    : 'No deadline'
}
