import { paths } from '../../app/paths'
import type { ContentSummary } from '../../data/content/types'
import type { CourseId, ModuleId } from '../../data/shared/types'
import CompletedMark from './CompletedMark'
import { CourseLink } from './CourseLink'
import ContentTypeIcon from './ContentTypeIcon'
import { useCourseContext } from './courseContext'
import { contentTypeInfo } from './contentTypes'
import './ContentList.css'

type ContentListProps = {
  courseId: CourseId
  moduleId: ModuleId
  items: ContentSummary[]
}

/**
 * Content items of one module, each a link to its page (course/module/content).
 * Completed assignments, coding exercises and quizzes get a check mark.
 */
export default function ContentList({ courseId, moduleId, items }: ContentListProps) {
  const { completed } = useCourseContext()
  return (
    <ul className="content-list">
      {items.map(item => (
        <li key={item.id}>
          <CourseLink className="content-list-item" to={paths.courseContent(courseId, moduleId, item.id)}>
            <ContentTypeIcon type={item.type} />
            <span className="content-list-title">{item.title}</span>
            {completed?.has(String(item.id)) && <CompletedMark label />}
            <span className="content-list-type">{contentTypeInfo(item.type).label}</span>
          </CourseLink>
        </li>
      ))}
    </ul>
  )
}
