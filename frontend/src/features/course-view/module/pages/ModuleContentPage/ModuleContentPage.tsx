import { useParams } from 'react-router'
import { useCourseView } from '../../../courseContext'
import ContentView from '../../../pages/ContentView'
import { useModuleView } from '../../moduleContext'

/** One content item in its module, with the module menu: …/modules/2/assignments/101. */
export default function ModuleContentPage() {
  const { contentId = '' } = useParams()
  const { isTeacher } = useCourseView()
  const { items } = useModuleView()
  const summary = items.find(item => String(item.id) === contentId)
  if (!summary) return <p role="alert">This content does not exist in this module.</p>
  return <ContentView summary={summary} isTeacher={isTeacher} />
}
