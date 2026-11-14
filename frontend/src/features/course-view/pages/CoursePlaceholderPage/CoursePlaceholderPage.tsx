import PageHeader from '../../../../shared/ui/PageHeader'

/** A course section that has no backend support yet (Groups, Files). */
export default function CoursePlaceholderPage({ title }: { title: string }) {
  return (
    <>
      <PageHeader title={title} />
      <p>This part of the course is not available yet.</p>
    </>
  )
}
