import { formatDateTime } from '@shared/lib/dateFormat'
import { TrailLink } from '@shared/navigation/Trail'
import { paths, type CourseSection } from '@shared/paths'
import type { CourseId } from '@shared/types'
import { contentSection, isSubmittable, isSubmittableType } from '@domain/content/contentTypes'
import { useContents } from '@domain/content/hooks'
import type { Content, ContentSummary, ContentType, QuizContent } from '@domain/content/types'
import { formatScore } from '@domain/assignments/scoreFormat'
import { useMyGrades } from '@domain/grades/hooks'
import type { GradeEntry } from '@domain/grades/types'
import type { CourseModule } from '@domain/modules/types'
import { useMyQuizAttemptsFor } from '@domain/quizzes/hooks'
import type { QuizAttemptScore } from '@domain/quizzes/types'
import { quizPassed } from '@domain/quizzes/passRule'
import DataTable, { type Column } from '@shared/ui/DataTable'
import MandatoryLabel from '../../components/MandatoryLabel'
import PassedBadge from '../../components/PassedBadge'
import VisibleFromTag from '../../components/VisibleFromTag'

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
  /**
   * The course page the table is on (e.g. assignments). Its items then open there
   * (/courses/1/assignments/101); without it, in their module (…/modules/2/assignments/101).
   */
  courseSection?: CourseSection
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
  { courseId, type, title, place, rows, showModule, isTeacher, courseSection }: ContentTypeTableProps,
) {
  const submittable = isSubmittableType(type)
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
          <TrailLink to={courseSection
            ? paths.courseSectionContent(courseId, courseSection, item.id)
            : paths.courseContent(courseId, module.id, contentSection(item.type), item.id)}>
            {item.title}
          </TrailLink>
          {item.active === false && <span className="data-table-tag">Hidden from students</span>}
          <VisibleFromTag item={item} module={module} className="data-table-tag" />
        </>
      ),
    },
    // The module the item is in, as a link to it.
    ...(showModule
      ? [{ header: 'Module', cell: ({ module }: Row) => <TrailLink to={paths.courseModule(courseId, module.id)}>{module.title}</TrailLink> }]
      : []),
  ]
  if (submittable) {
    columns.push(
      { header: 'Deadline', cell: ({ details }) => <Deadline content={details} /> },
      { header: 'Late submissions', cell: ({ details }) => <LateSubmissions content={details} /> },
    )
    // First, to the left of the title.
    if (type === 'ASSIGNMENT') columns.unshift({ header: 'Mandatory', cell: ({ details }) => <MandatoryLabel content={details} /> })
    if (!isTeacher) columns.push({ header: 'Status', cell: row => <Status row={row} loaded={!grades.isPending} /> })
  } else if (type === 'QUIZ') {
    columns.push(
      { header: 'Questions', cell: ({ details }) => details?.type === 'QUIZ' ? details.questions?.length ?? '–' : '…' },
      { header: 'Attempts allowed', cell: ({ details }) => details?.type === 'QUIZ' ? details.maxAttempts : '…' },
      {
        header: 'To pass',
        cell: ({ details }) => {
          if (details?.type !== 'QUIZ') return '…'
          const required = details.requiredCorrectAnswers ?? null
          return required === null
            ? <span className="data-table-muted">–</span>
            : `${required} of ${details.questions?.length ?? '?'} correct`
        },
      },
    )
    if (!isTeacher) {
      columns.push({
        header: 'Status',
        cell: ({ item, details }) => (
          <QuizStatus attempts={quizAttempts.byQuiz.get(String(item.id))} loading={quizAttempts.isPending}
            quiz={details?.type === 'QUIZ' ? details : undefined} />
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
  if (!isSubmittable(content)) return '–'
  return content.deadline ? formatDateTime(new Date(content.deadline)) : 'No deadline'
}

function LateSubmissions({ content }: { content?: Content }) {
  if (!content) return '…'
  if (!isSubmittable(content)) return '–'
  return <span className="data-table-muted">{content.allowLateSubmission ? 'Accepted' : 'Not accepted'}</span>
}

/**
 * A quiz's status for the student. With a pass requirement it is done only once an
 * attempt passed: "Passed" with a check mark; "Not passed yet" while attempts are left;
 * "Not passed" when none are. Without one, "Completed" once it has an attempt.
 */
function QuizStatus({ attempts, loading, quiz }: {
  attempts?: QuizAttemptScore[]
  loading: boolean
  /** The quiz's details (pass requirement and attempts); undefined while loading. */
  quiz?: QuizContent
}) {
  if (!attempts) return loading ? '…' : <span className="data-table-muted">–</span>
  if (attempts.length === 0) return <span className="data-table-muted">Not started</span>
  const best = attempts.reduce((top, attempt) => (attempt.correctCount > top.correctCount ? attempt : top))
  const bestText = <span className="data-table-muted"> · best {best.correctCount}/{best.totalQuestions}</span>
  if (!quiz) return '…'

  const passed = quizPassed(attempts, quiz.requiredCorrectAnswers)
  if (passed === false) {
    const left = Math.max(0, quiz.maxAttempts - attempts.length)
    return left > 0
      ? (
        <span>
          <span className="data-table-strong">Not passed yet</span>
          {bestText}
          <span className="data-table-muted"> · {left} {left === 1 ? 'attempt' : 'attempts'} left</span>
        </span>
      )
      : (
        <span>
          <PassedBadge passed={false} />
          {bestText}
          <span className="data-table-muted"> · no attempts left</span>
        </span>
      )
  }

  return (
    <span className="data-table-done">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
        strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
        <circle cx="12" cy="12" r="10" />
        <path d="M7.5 12.5l3 3 6-6.5" />
      </svg>
      {passed ? 'Passed' : 'Completed'}
      {bestText}
    </span>
  )
}

/** Not submitted, submitted (awaiting a grade) or the grade. */
function Status({ row, loaded }: { row: Row, loaded: boolean }) {
  if (!loaded) return '…'
  if (!row.grade) return <span className="data-table-muted">Not submitted</span>
  if (!row.grade.grade) return 'Submitted'
  return (
    <span className="data-table-strong">
      Graded: {formatScore(row.grade.grade.score)} <PassedBadge passed={row.grade.grade.passed} />
    </span>
  )
}
