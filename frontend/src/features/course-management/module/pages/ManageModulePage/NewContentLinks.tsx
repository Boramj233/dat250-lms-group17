import { Link } from 'react-router'
import { paths } from '@shared/paths'
import type { CourseId, ModuleId } from '@shared/types'
import { CONTENT_TYPES } from '@domain/content/contentTypes'

/** "Create": one card per content type, each opening the form for a new item in the module. */
export default function NewContentLinks({ courseId, moduleId }: { courseId: CourseId; moduleId: ModuleId }) {
  return (
    <section aria-labelledby="new-content-title">
      <h2 id="new-content-title" className="section-title">
        Create
      </h2>
      <ul className="new-content-types">
        {CONTENT_TYPES.map(info => (
          <li key={info.type}>
            <Link className="new-content-type" to={paths.manageNewContent(courseId, moduleId, info.slug)}>
              <span className="new-content-type-label">+ {info.label}</span>
              <span className="new-content-type-description">{info.description}</span>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}
