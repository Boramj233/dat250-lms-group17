import { useContext } from 'react'
import PageHeader from '@shared/ui/PageHeader'
import DataTable from '@shared/ui/DataTable'
import { useCourseView } from '../../courseContext'
import { ModuleViewContext } from '../../module/moduleContext'

/** A file in the course (none yet: the backend has no files). */
type CourseFile = { id: string; name: string; module: string; added: string }

/**
 * The files of the course, or of one module when shown inside ModuleLayout. The backend
 * has no files yet, so the table is empty; the columns show what it will list.
 */
export default function CourseFilesPage() {
  const { course } = useCourseView()
  const module = useContext(ModuleViewContext)?.module
  const place = module ? module.title : course.title
  return (
    <>
      <PageHeader title="Files" kicker={place} />
      <DataTable<CourseFile>
        caption={`Files in ${place}`}
        columns={[
          { header: 'Name', cell: file => file.name },
          { header: 'Module', cell: file => file.module },
          { header: 'Added', cell: file => file.added },
        ]}
        rows={[]}
        rowKey={file => file.id}
        empty={`No files in this ${module ? 'module' : 'course'} yet.`}
      />
    </>
  )
}
