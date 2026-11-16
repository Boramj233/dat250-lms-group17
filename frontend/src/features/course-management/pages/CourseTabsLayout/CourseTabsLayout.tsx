import { Outlet } from 'react-router'
import { paths } from '@shared/paths'
import PageHeader from '@shared/ui/PageHeader'
import Tabs from '@shared/ui/Tabs'
import { formatTerm } from '@domain/courses/courseFormat'
import { useManageCourse } from '../../manageCourseContext'

/** The course title and the tabs Content, Members and Details, with the selected tab below. */
export default function CourseTabsLayout() {
  const { course } = useManageCourse()
  return (
    <>
      <PageHeader title={course.title} kicker="Manage course">
        <p className="page-header-meta">{formatTerm(course)}</p>
      </PageHeader>
      <Tabs
        label="Manage course"
        tabs={[
          { label: 'Content', to: paths.manageCourse(course.id), end: true },
          { label: 'Members', to: paths.manageCourseMembers(course.id) },
          { label: 'Details', to: paths.manageCourseDetails(course.id) },
        ]}
      />
      <Outlet />
    </>
  )
}
