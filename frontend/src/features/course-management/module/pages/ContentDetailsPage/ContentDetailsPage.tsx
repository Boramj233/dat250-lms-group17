import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { paths } from '../../../../../shared/paths'
import { useContent, useUpdateContent } from '../../../../content/hooks'
import type { Content } from '../../../../content/types'
import PageHeader from '../../../../../shared/ui/PageHeader'
import QueryState from '../../../../../shared/ui/QueryState'
import { contentTypeLabel } from '../../../../content/contentTypes'
import { useManageCourse } from '../../../manageCourseContext'
import { useManageModule } from '../../manageModuleContext'
import { useManagedContent } from '../../useManagedContent'
import ContentFacts from './ContentFacts'
import '../../../../../shared/styles/forms.css'
import './ContentDetailsPage.css'

/**
 * One content item's setup: what it contains (e.g. a coding exercise's description,
 * starter repository and starter code), and a form for its title and a page's text.
 * The API only lets these change (PATCH /api/content/{id}: title, position, body; see
 * data/content/README.md). Grading is not part of managing a course.
 */
export default function ContentDetailsPage() {
  const { module, number } = useManageModule()
  const item = useManagedContent()
  // The type comes from the module's content list; the backend has one endpoint per type.
  const content = useContent(item?.type, item?.id ?? '')

  if (!item) return <p role="alert">This content does not exist in {module.title}.</p>

  return (
    <>
      <PageHeader
        title={item.title}
        kicker={`${contentTypeLabel(item.type)} · Module ${number}${item.requiredForCompletion ? ' · Required to complete the module' : ''}`}
      />
      <QueryState query={content} loading="Loading the content…" error="Could not load the content.">
        {data => (
          <>
            <ContentFacts content={data} />
            {data.type !== 'STATIC_PAGE' && <h2 className="section-title">Edit</h2>}
            <ContentDetailsForm key={data.id} content={data} />
          </>
        )}
      </QueryState>
    </>
  )
}

function ContentDetailsForm({ content }: { content: Content }) {
  const { course } = useManageCourse()
  const { module } = useManageModule()
  const updateContent = useUpdateContent()
  const [title, setTitle] = useState(content.title)
  const [body, setBody] = useState(content.type === 'STATIC_PAGE' ? content.body : '')
  const [saved, setSaved] = useState(false)

  function submit(event: FormEvent) {
    event.preventDefault()
    setSaved(false)
    updateContent.mutate(
      {
        contentId: content.id,
        moduleId: module.id,
        changes: content.type === 'STATIC_PAGE' ? { title: title.trim(), body } : { title: title.trim() },
      },
      { onSuccess: () => setSaved(true) },
    )
  }

  return (
    <form className="form" onSubmit={submit}>
      <div className="form-field">
        <label htmlFor="content-title">Title</label>
        <input id="content-title" required maxLength={255} value={title}
          onChange={event => { setTitle(event.target.value); setSaved(false) }} />
      </div>
      {content.type === 'STATIC_PAGE' && (
        <div className="form-field">
          <label htmlFor="content-body">Text</label>
          <textarea id="content-body" required maxLength={10000} value={body}
            onChange={event => { setBody(event.target.value); setSaved(false) }} />
        </div>
      )}
      {content.type !== 'STATIC_PAGE' && (
        <p className="form-hint">Only the title can be changed for this type of content.</p>
      )}
      {updateContent.error && <p className="form-error" role="alert">Could not save. Try again.</p>}
      <div className="form-actions">
        <button type="submit" className="button-primary" disabled={updateContent.isPending}>
          {updateContent.isPending ? 'Saving…' : 'Save changes'}
        </button>
        {/* Back to the module without saving. */}
        <Link className="button-secondary" to={paths.manageModule(course.id, module.id)}>Cancel</Link>
        {saved && <span className="form-success" role="status">Saved.</span>}
      </div>
    </form>
  )
}
