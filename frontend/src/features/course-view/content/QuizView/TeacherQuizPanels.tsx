import { useState } from 'react'
import { useCourseEnrollments } from '@domain/courses/hooks'
import type { QuizContent, QuizContentQuestion } from '@domain/content/types'
import { useQuizAnswerKey, useStudentQuizAttempts } from '@domain/quizzes/hooks'
import type { QuizAttemptScore } from '@domain/quizzes/types'
import { fullName } from '@shared/lib/fullName'
import type { UserId } from '@shared/types'
import { Field } from '@shared/ui/Form'
import { Card } from '../../components/DetailLayout'
import { AttemptList, AttemptReview } from './QuizAttempts'
import QuizQuestion from './QuizQuestion'
import { fromAnswerKey } from './quizAnswers'

type AnswerKeyProps = {
  quizId: QuizContent['id']
  /** The quiz's questions, in order. */
  questions: QuizContentQuestion[]
  onBack: () => void
}

/**
 * Teachers: every question with its correct answer and hint
 * (GET /api/quizzes/{quizId}/answer-key, only for teachers of the course).
 */
export function AnswerKey({ quizId, questions, onBack }: AnswerKeyProps) {
  const key = useQuizAnswerKey(quizId)
  const back = (
    <div className="submittable-actions">
      <button type="button" className="button-secondary" onClick={onBack}>
        Back to the quiz
      </button>
    </div>
  )

  if (key.isPending) return <p role="status">Loading the answer key…</p>
  if (key.error) {
    return (
      <Card>
        <p role="alert">Could not load the answer key.</p>
        {back}
      </Card>
    )
  }

  const keyOf = (question: QuizContentQuestion) =>
    key.data.questions.find(entry => String(entry.question?.id) === String(question.id))

  return (
    <>
      <Card id="quiz-key-title" title="Answer key">
        <p className="text-meta">
          The correct answer of every question, as the quiz is graded. Only teachers of the course can see this.
        </p>
        {back}
      </Card>
      <div className="quiz-form" aria-label="Answer key">
        {questions.map((question, i) => {
          const entry = keyOf(question)
          return (
            <QuizQuestion
              key={question.id}
              question={question}
              number={i + 1}
              total={questions.length}
              answer={fromAnswerKey(question, entry)}
              answerKey={{ hint: entry?.hint ?? null }}
            />
          )
        })}
      </div>
      <Card>{back}</Card>
    </>
  )
}

type StudentAttemptsProps = {
  quiz: QuizContent
  /** The quiz's questions, in order. */
  questions: QuizContentQuestion[]
}

/**
 * Teachers: pick a student of the course and see their attempts at the quiz
 * (GET …/attempts/students/{userId}), and the answers of one attempt
 * (GET …/attempts/{attemptId}/review).
 */
export function StudentAttempts({ quiz, questions }: StudentAttemptsProps) {
  const members = useCourseEnrollments(quiz.courseId)
  const [studentId, setStudentId] = useState<UserId | null>(null)
  const [viewing, setViewing] = useState<QuizAttemptScore['id'] | null>(null)
  const attempts = useStudentQuizAttempts(quiz.id, studentId)

  const students = (members.data ?? [])
    .filter(member => member.role === 'STUDENT')
    .sort((a, b) => fullName(a).localeCompare(fullName(b)))
  const student = students.find(member => String(member.userId) === String(studentId))
  const name = student ? fullName(student) : `User #${studentId}`

  if (viewing !== null && attempts.data) {
    return (
      <AttemptReview
        quizId={quiz.id}
        questions={questions}
        attempts={attempts.data}
        attemptId={viewing}
        required={quiz.passingPercentage}
        studentName={name}
        backLabel="Back to the attempts"
        onBack={() => setViewing(null)}
      />
    )
  }

  return (
    <>
      <Card id="quiz-students-title" title="Student attempts">
        {members.isPending ? (
          <p role="status">Loading the students…</p>
        ) : members.error ? (
          <p role="alert">Could not load the students of the course.</p>
        ) : students.length === 0 ? (
          <p>The course has no students yet.</p>
        ) : (
          <Field label="Student">
            {props => (
              <select
                {...props}
                value={studentId === null ? '' : String(studentId)}
                onChange={event => {
                  setStudentId(event.target.value === '' ? null : event.target.value)
                  setViewing(null)
                }}
              >
                <option value="">Choose a student…</option>
                {students.map(member => (
                  <option key={member.userId} value={String(member.userId)}>
                    {fullName(member)}
                  </option>
                ))}
              </select>
            )}
          </Field>
        )}
        {studentId !== null && attempts.isPending && <p role="status">Loading the attempts…</p>}
        {studentId !== null && attempts.error && <p role="alert">Could not load the attempts of {name}.</p>}
        {attempts.data?.length === 0 && <p>{name} has not taken the quiz yet.</p>}
      </Card>
      {studentId !== null && attempts.data && attempts.data.length > 0 && (
        <AttemptList
          title={`Attempts of ${name}`}
          attempts={attempts.data}
          required={quiz.passingPercentage}
          onView={setViewing}
        />
      )}
    </>
  )
}
