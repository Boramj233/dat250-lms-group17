import { formatDateTime } from '../../../../shared/lib/dateFormat'
import { TrailLink } from '../../../../shared/navigation/Trail'
import { paths } from '../../../../shared/paths'
import type { CourseId } from '../../../../shared/types'
import { useContents } from '../../../content/hooks'
import type { Content, ContentSummary, ContentType } from '../../../content/types'
import { useMyGrades } from '../../../grades/hooks'
import type { GradeEntry } from '../../../grades/types'
import type { CourseModule } from '../../../modules/types'
import { useMyQuizAttemptsFor } from '../../../quizzes/hooks'
import type { QuizAttemptScore } from '../../../quizzes/types'
import DataTable, { type Column } from '../../components/DataTable'

export type ContentTypeRow = { item: ContentSummary, module: CourseModule }

type ContentTypeTableProps = {
  courseId: CourseId
  type: ContentType
  /** "Assignments", "Pages", …: for the caption and the empty text. */
  title: string
  /** Where the items are, e.g. the course or module title, for the caption. */
  place: string
  /** The items, in order; only those the user may see. */
  rows: ContentTypeRow[]
  /** Course pages show which module each item is in; module pages leave it out. */
  showModule: boolean
  /** Students get a Status column for assignments and code exercises. */
  isTeacher: boolean
}

type Row = ContentTypeRow & {
  /** Undefined while loading. */
  details?: Content
  /** Students: their latest submission, if any (from their grades). */
  grade?: GradeEntry
}

/**
 * Content of one type as a table, used by the course's and the module's pages.
 * Assignments and code exercises show the deadline (and for students their status);
 * quizzes show the number of questions and attempts; pages only their title.
 */
export default function ContentTypeTable(
  { courseId, type, title, place, rows, showModule, isTeacher }: ContentTypeTableProps,
) {
  const submittable = type === 'ASSIGNMENT' || type === 'CODING_EXERCISE'
  // Deadlines, questions and attempts are in each item's details (same cache as the
  // content page). Pages need no details.
  const details = useContents(type === 'STATIC_PAGE' ? [] : rows.map(({ item }) => ({ type: item.type, id: item.id })))
  // Students' status per item; the same request as the Grades page.
  const grades = useMyGrades()
  // Students' quiz attempts, for the quizzes' Status (same cache as the quiz page).
  const quizAttempts = useMyQuizAttemptsFor(type === 'QUIZ' && !isTeacher ? rows.map(({ item }) => item.id) : [])

  const fullRows: Row[] = rows.map(row => ({
    ...row,
    details: details.byId.get(String(row.item.id)),
    grade: grades.data?.find(entry => String(entry.contentId) === String(row.item.id)),
  }))

  const columns: Column<Row>[] = [
    {
      header: 'Title',
      cell: ({ item, module }) => (
        <>
          <TrailLink to={paths.courseContent(courseId, module.id, item.id)}>{item.title}</TrailLink>
          {item.active === false && <span className="data-table-tag">Hidden from students</span>}
        </>
      ),
    },
    ...(showModule ? [{ header: 'Module', cell: ({ module }: Row) => module.title }] : []),
  ]
  if (submittable) {
    columns.push(
      { header: 'Deadline', cell: ({ details }) => <Deadline content={details} /> },
      { header: 'Late submissions', cell: ({ details }) => <LateSubmissions content={details} /> },
    )
    if (!isTeacher) columns.push({ header: 'Status', cell: row => <Status row={row} loaded={!grades.isPending} /> })
  } else if (type === 'QUIZ') {
    columns.push(
      { header: 'Questions', cell: ({ details }) => details?.type === 'QUIZ' ? details.questions?.length ?? '–' : '…' },
      { header: 'Attempts allowed', cell: ({ details }) => details?.type === 'QUIZ' ? details.maxAttempts : '…' },
    )
    if (!isTeacher) {
      columns.push({
        header: 'Status',
        cell: ({ item }) => (
          <QuizStatus attempts={quizAttempts.byQuiz.get(String(item.id))} loading={quizAttempts.isPending} />
        ),
      })
    }
  }

  return (
    <DataTable
      caption={`${title} in ${place}`}
      columns={columns}
      rows={fullRows}
      rowKey={({ item }) => String(item.id)}
      empty={`No ${title.toLowerCase()} in this ${showModule ? 'course' : 'module'} yet.`}
    />
  )
}

function Deadline({ content }: { content?: Content }) {
  if (!content) return '…'
  if (content.type !== 'ASSIGNMENT' && content.type !== 'CODING_EXERCISE') return '–'
  return content.deadline ? formatDateTime(new Date(content.deadline)) : 'No deadline'
}

function LateSubmissions({ content }: { content?: Content }) {
  if (!content) return '…'
  if (content.type !== 'ASSIGNMENT' && content.type !== 'CODING_EXERCISE') return '–'
  return <span className="data-table-muted">{content.allowLateSubmission ? 'Accepted' : 'Not accepted'}</span>
}

/** Completed (with a check mark and the best score) once a quiz has an attempt. */
function QuizStatus({ attempts, loading }: { attempts?: QuizAttemptScore[], loading: boolean }) {
  if (!attempts) return loading ? '…' : <span className="data-table-muted">–</span>
  if (attempts.length === 0) return <span className="data-table-muted">Not started</span>
  const best = attempts.reduce((top, attempt) => (attempt.correctCount > top.correctCount ? attempt : top))
  return (
    <span className="data-table-done">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
        strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
        <circle cx="12" cy="12" r="10" />
        <path d="M7.5 12.5l3 3 6-6.5" />
      </svg>
      Completed
      <span className="data-table-muted"> · best {best.correctCount}/{best.totalQuestions}</span>
    </span>
  )
}

/** Not submitted, submitted (awaiting a grade) or the grade. */
function Status({ row, loaded }: { row: Row, loaded: boolean }) {
  if (!loaded) return '…'
  if (!row.grade) return <span className="data-table-muted">Not submitted</span>
  if (!row.grade.grade) return 'Submitted'
  return <span className="data-table-strong">Graded: {row.grade.grade.value}</span>
}
