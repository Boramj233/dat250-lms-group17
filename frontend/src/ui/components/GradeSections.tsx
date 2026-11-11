import type { ComponentType, ReactNode } from 'react'
import { paths } from '../../app/paths'
import type { GradeEntry } from '../../data/grades/types'
import { formatDateTime } from './dateFormat'
import './GradeSections.css'

type LinkComponent = ComponentType<{ to: string, className?: string, children: ReactNode }>

type GradeSectionsProps = {
  entries: GradeEntry[]
  /** Show the course on each row (the all-courses page). */
  showCourse?: boolean
  /** Link component for the rows: CourseLink inside a course, TrailLink elsewhere. */
  link: LinkComponent
}

const TYPE_LABEL: Record<GradeEntry['type'], string> = {
  ASSIGNMENT: 'Assignment',
  CODING_EXERCISE: 'Coding exercise',
}

/** "Graded" and "Awaiting grade" sections with one row per submitted assignment. */
export default function GradeSections({ entries, showCourse = false, link }: GradeSectionsProps) {
  const graded = entries.filter(entry => entry.grade)
  const awaiting = entries.filter(entry => !entry.grade)
  return (
    <>
      <section className="grade-section" aria-labelledby="grades-graded">
        <h2 id="grades-graded" className="grade-section-title">
          Graded <span className="grade-section-count">{graded.length}</span>
        </h2>
        {graded.length === 0
          ? <p>Nothing has been graded yet.</p>
          : <GradeList entries={graded} showCourse={showCourse} link={link} />}
      </section>
      <section className="grade-section" aria-labelledby="grades-awaiting">
        <h2 id="grades-awaiting" className="grade-section-title">
          Awaiting grade <span className="grade-section-count">{awaiting.length}</span>
        </h2>
        {awaiting.length === 0
          ? <p>Nothing is waiting for a grade.</p>
          : <GradeList entries={awaiting} showCourse={showCourse} link={link} />}
      </section>
    </>
  )
}

function GradeList({ entries, showCourse, link: LinkTo }: Required<GradeSectionsProps>) {
  return (
    <ul className="grade-list">
      {entries.map(entry => (
        <li key={`${entry.type}-${entry.contentId}`} className="grade-row">
          <div className="grade-row-main">
            <LinkTo className="grade-row-title" to={paths.courseContent(entry.courseId, entry.moduleId, entry.contentId)}>
              {entry.title}
            </LinkTo>
            <p className="grade-row-meta">
              {TYPE_LABEL[entry.type]}
              {showCourse && <> · <LinkTo to={paths.course(entry.courseId)}>{entry.courseTitle}</LinkTo></>}
              {' · '}Submitted{' '}
              <time dateTime={entry.submittedAt}>{formatDateTime(new Date(entry.submittedAt))}</time>
              {entry.late && <strong> (late)</strong>}
            </p>
            {entry.grade?.feedback && (
              <p className="grade-row-feedback">
                <span className="visually-hidden">Comment from the teacher: </span>
                {entry.grade.feedback}
              </p>
            )}
          </div>
          {entry.grade
            ? <span className="grade-row-value">{entry.grade.value}</span>
            : <span className="grade-row-awaiting">Awaiting grade</span>}
        </li>
      ))}
    </ul>
  )
}
