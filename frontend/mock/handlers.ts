// The mock API for `npm run dev:mock`: one handler per backend endpoint the frontend
// uses. Add a handler here when you start using a new endpoint. Like data.ts, this file
// is reloaded on the next request after you save it.
//
// Requests without a valid token never get here (the mock answers 401, like the real
// API), so `user` is always the logged-in user. Paths that have no handler answer 404
// and are logged in the terminal.
//
// The responses copy the backend's DTOs (field names, and which fields each one has),
// so the frontend sees the same shapes as with the real backend.
import type { CreateQuizQuestionRequest } from '../src/domain/content/questionRequest'
import type { Content, ContentType } from '../src/domain/content/types'
import type { EnrolledCourse } from '../src/domain/courses/types'
import type { UpcomingDeadline } from '../src/domain/deadlines/types'
import { checkContentVisibleFrom, isNotYetVisible } from '../src/domain/content/visibility'
import { canRequire, checkAvailabilityDates, isAccessible } from '../src/domain/modules/prerequisites'
import {
  completions,
  contents,
  courses,
  enrollments,
  groups,
  modules,
  quizAttempts,
  quizQuestions,
  submissions,
  users,
} from './data'
import {
  gradeAnswer,
  questionRequestProblem,
  questionResponse,
  storedQuestion,
  type AnswerRequest,
} from './quizQuestions'
import {
  conflict,
  created,
  forbidden,
  noContent,
  notFound,
  ok,
  type MockHandler,
  type MockMultipart,
  type MockResult,
  type MockUser,
} from './types'

/** Like the backend: you may read your own data; ADMIN may read anyone's. */
function canReadUser(user: MockUser, userId: string) {
  return String(user.userId) === userId || user.access === 'ADMIN'
}

function coursesOf(userId: number): EnrolledCourse[] {
  return enrollments
    .filter(enrollment => enrollment.userId === userId)
    .flatMap(enrollment => {
      const course = courses.find(c => c.id === enrollment.courseId)
      return course ? [{ ...course, role: enrollment.role }] : []
    })
}

/** Like the backend should: only teachers of the course may change it. */
function isTeacher(user: MockUser, courseId: string | number) {
  return enrollments.some(
    e => e.userId === user.userId && String(e.courseId) === String(courseId) && e.role === 'TEACHER',
  )
}

/** An ISO instant as the backend's LocalDateTime (local time, no offset), or null. */
function localDateTime(iso: string | null | undefined): string | null {
  if (!iso) return null
  const date = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  )
}

/** A LocalDateTime from a request as an ISO instant, or null. */
function fromLocalDateTime(value: string | null | undefined): string | null {
  return value ? new Date(value).toISOString() : null
}

/** The backend's ModuleResponse. */
function moduleResponse(module: (typeof modules)[number]) {
  return {
    id: module.id,
    courseId: module.courseId,
    title: module.title,
    position: module.position,
    dependencyIds: module.prerequisiteModuleIds ?? [],
    availableFrom: localDateTime(module.availableFrom),
    availableToAllFrom: localDateTime(module.openToAllFrom),
  }
}

/** The module in the course for a teacher of it, or the answer to send instead. */
function teacherModule(user: MockUser, params: Record<string, string>) {
  const module = modules.find(m => String(m.id) === params.moduleId && String(m.courseId) === params.courseId)
  if (!module) return notFound()
  if (!isTeacher(user, params.courseId)) return forbidden()
  return module
}

/** An active student of the course (the backend's requireActiveStudent). */
function isStudent(user: MockUser, courseId: string | number) {
  return enrollments.some(
    e => e.userId === user.userId && String(e.courseId) === String(courseId) && e.role === 'STUDENT',
  )
}

/** The backend's ContentCompletionService.Status for the caller. */
function completionStatus(user: MockUser, content: { id: number | string; requiredForModuleCompletion?: boolean }) {
  return {
    contentId: content.id,
    requiredForModuleCompletion: content.requiredForModuleCompletion ?? false,
    completed: completions.some(c => c.userId === user.userId && c.contentId === Number(content.id)),
  }
}

/** Like the backend's enrollment endpoints: a teacher of this course, or an admin. */
function isTeacherOrAdmin(user: MockUser, courseId: string | number) {
  return user.access === 'ADMIN' || isTeacher(user, courseId)
}

/** The backend's EnrollmentResponse: the member with their name and email. */
function enrollmentResponse(enrollment: (typeof enrollments)[number]) {
  const member = users.find(u => u.userId === enrollment.userId)
  return member
    ? {
        userId: enrollment.userId,
        role: enrollment.role,
        firstName: member.firstName,
        lastName: member.lastName,
        email: member.email,
      }
    : undefined
}

const badRequest = (error: string): MockResult => ({ status: 400, body: { error } })
const nextId = (items: { id: number | string }[]) => Math.max(0, ...items.map(item => Number(item.id))) + 1

/** The submission is the user's own, or their group's (proposed group assignments). */
function isOwnSubmission(user: MockUser, submission: (typeof submissions)[number]) {
  if (submission.userId === user.userId) return true
  const group = groups.find(g => g.id === submission.groupId)
  return group?.memberIds.includes(user.userId) ?? false
}

/** The members' names of a group (proposed in GroupDTO). */
function groupMembers(group: (typeof groups)[number]) {
  return group.memberIds.flatMap(userId => {
    const member = users.find(u => u.userId === userId)
    return member ? [{ userId, firstName: member.firstName, lastName: member.lastName }] : []
  })
}

type MockSubmission = (typeof submissions)[number]

/** The mock has no enrollment ids; use the position in the enrollments list. */
function enrollmentIdOf(userId: number, courseId: number) {
  return enrollments.findIndex(e => e.userId === userId && e.courseId === courseId) + 1
}

/**
 * SubmissionResponse (the student's own) or, for teachers, SubmissionReviewResponse.
 * The file's downloadPath differs: /file for the student, /review/file for a teacher.
 */
function submissionResponse(s: MockSubmission, forTeacher = false) {
  const content = contents.find(c => c.id === s.contentId)!
  const group = groups.find(g => g.id === s.groupId)
  const base = {
    id: s.id,
    contentId: s.contentId,
    type: s.type,
    payload: s.payload,
    submittedAt: s.submittedAt,
    late: s.late,
    file: s.file
      ? {
          name: s.file.name,
          mediaType: s.file.mediaType,
          sizeBytes: s.file.content.length,
          downloadPath: `/api/submissions/${s.id}/${forTeacher ? 'review/file' : 'file'}`,
        }
      : null,
    groupId: s.groupId ?? null,
    submittedByUserId: s.userId,
    members: (group?.memberIds ?? []).map(userId => ({
      userId,
      enrollmentId: enrollmentIdOf(userId, content.courseId),
    })),
  }
  return forTeacher
    ? { ...base, studentUserId: s.userId, enrollmentId: enrollmentIdOf(s.userId, content.courseId) }
    : base
}

/** The submissions by the same student (or group) for the same item, newest first. */
function sequenceOf(s: MockSubmission) {
  return submissions
    .filter(
      other =>
        other.contentId === s.contentId &&
        (s.groupId !== undefined
          ? other.groupId === s.groupId
          : other.groupId === undefined && other.userId === s.userId),
    )
    .sort((a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt))
}

/** GradeReviewResponse (teachers). */
function gradeReviewResponse(s: MockSubmission) {
  const g = s.grade!
  const content = contents.find(c => c.id === s.contentId)!
  const teacher = enrollments.find(e => e.courseId === content.courseId && e.role === 'TEACHER')
  const teacherEnrollment = teacher ? enrollmentIdOf(teacher.userId, content.courseId) : 0
  return {
    submissionId: s.id,
    score: g.score,
    feedback: g.feedback,
    passed: g.passed,
    visibleFrom: g.visibleFrom ?? null,
    gradedByEnrollmentId: teacherEnrollment,
    gradedAt: g.gradedAt,
    releasedByEnrollmentId: g.releasedAt ? teacherEnrollment : null,
    releasedAt: g.releasedAt,
    version: g.version,
  }
}

/** The submission and its course, when the user teaches that course. */
function teacherSubmission(user: MockUser, submissionId: string): MockSubmission | MockResult {
  const submission = submissions.find(s => String(s.id) === submissionId)
  if (!submission) return notFound()
  const content = contents.find(c => c.id === submission.contentId)!
  if (!isTeacher(user, content.courseId)) return forbidden()
  return submission
}
const isResult = <T extends object>(value: T | MockResult): value is MockResult => 'status' in value

/**
 * Like SubmissionService: an assignment or coding exercise the user may hand in to,
 * as a member of `groupId` if given. Late after the deadline; 409 when not allowed.
 */
function checkSubmit(user: MockUser, contentId: string, groupId: number | undefined) {
  const content = contents.find(c => String(c.id) === contentId)
  if (!content) return { error: notFound() }
  if (content.type !== 'ASSIGNMENT' && content.type !== 'CODING_EXERCISE')
    return { error: badRequest('Cannot submit to this content') }
  if (!canReadCourse(user, content.courseId)) return { error: forbidden() }
  if (groupId !== undefined) {
    const group = groups.find(g => g.id === groupId && g.courseId === content.courseId)
    if (!group?.memberIds.includes(user.userId)) return { error: forbidden() }
  }
  const late = content.deadline !== null && Date.now() > Date.parse(content.deadline)
  if (late && !content.allowLateSubmission) return { error: conflict('Submission deadline has passed') }
  return { content, late }
}

/** Publishes a saved grade at `at`; a graded submission also counts as completed. */
function publishGrade(s: MockSubmission, at: string) {
  if (!s.grade || s.grade.releasedAt) return
  s.grade = { ...s.grade, releasedAt: at, version: s.grade.version + 1 }
  if (!completions.some(c => c.userId === s.userId && c.contentId === s.contentId)) {
    completions.push({ userId: s.userId, contentId: s.contentId, completedAt: s.submittedAt })
  }
}

/**
 * Visible dates, like the backend's GradeReleaseScheduler: a saved grade whose own visibleFrom has passed is published
 * (at that time). Run before every request, like a scheduled job.
 */
function applyVisibleDates(now = Date.now()) {
  for (const s of submissions) {
    if (s.grade && !s.grade.releasedAt && s.grade.visibleFrom && Date.parse(s.grade.visibleFrom) <= now) {
      publishGrade(s, s.grade.visibleFrom)
    }
  }
}

/** The latest submission of each student or group for a content item. */
function latestSubmissions(contentId: number) {
  return submissions.filter(s => s.contentId === contentId && sequenceOf(s)[0].id === s.id)
}

/** Students only see released grades: ReleasedGradeResponse without submissionId. */
function releasedGrade(s: MockSubmission) {
  return s.grade?.releasedAt
    ? { score: s.grade.score, feedback: s.grade.feedback, passed: s.grade.passed, releasedAt: s.grade.releasedAt }
    : null
}

/** Course content is for enrolled users (and ADMIN). */
function canReadCourse(user: MockUser, courseId: string | number) {
  return user.access === 'ADMIN' || coursesOf(user.userId).some(course => String(course.id) === String(courseId))
}

// ContentSummaryResponse sends "Assignment" for assignments (not "ASSIGNMENT").
const BACKEND_TYPE: Record<ContentType, string> = {
  STATIC_PAGE: 'STATIC_PAGE',
  QUIZ: 'QUIZ',
  ASSIGNMENT: 'Assignment',
  CODING_EXERCISE: 'CODING_EXERCISE',
}

/** The content as the backend's detail endpoint for its type returns it (no `type`). */
/**
 * A quiz's questions as in QuizDetailResponse. For teachers of the course also the
 * proposed `hint` and `correct` fields (data/content/README.md).
 */
function quizQuestionsResponse(quizId: string | number, forTeacher: boolean) {
  return quizQuestions
    .filter(q => String(q.quizId) === String(quizId))
    .sort((a, b) => a.position - b.position)
    .map(q => questionResponse(q, forTeacher))
}

function detailResponse(content: Content & { courseId: number }, forTeacher = false) {
  const base = {
    id: content.id,
    moduleId: content.moduleId,
    title: content.title,
    position: content.position,
    visibleFrom: content.visibleFrom ?? null,
    visibleUntil: content.visibleUntil ?? null,
  }
  switch (content.type) {
    case 'STATIC_PAGE':
      return { ...base, courseId: content.courseId, body: content.body }
    case 'QUIZ':
      return {
        ...base,
        courseId: content.courseId,
        maxAttempts: content.maxAttempts,
        passingPercentage: content.passingPercentage,
        description: content.description ?? null,
        deadline: content.deadline ?? null,
        questions: quizQuestionsResponse(content.id, forTeacher),
      }
    case 'ASSIGNMENT':
      return {
        ...base,
        instructions: content.instructions,
        deadline: content.deadline,
        allowLateSubmission: content.allowLateSubmission,
        mandatoryForExam: content.mandatoryForExam,
        groupAssignment: content.groupAssignment ?? false,
        acceptedFileTypes: content.acceptedFileTypes ?? [],
      }
    case 'CODING_EXERCISE':
      return {
        ...base,
        instructions: content.instructions,
        starterRepositoryUrl: content.starterRepositoryUrl,
        starterCode: content.starterCode ?? null,
        deadline: content.deadline,
        allowLateSubmission: content.allowLateSubmission,
      }
  }
}

/**
 * Students see content whose visible date (its own, or its module's start date) has
 * passed, and whose `visibleUntil` (if any) has not.
 */
function visibleToStudents(content: {
  visibleFrom?: string | null
  visibleUntil?: string | null
  moduleId: number | string
}) {
  if (content.visibleUntil && Date.parse(content.visibleUntil) <= Date.now()) return false
  const module = modules.find(m => String(m.id) === String(content.moduleId))
  return !(module && isNotYetVisible(content, module, Date.now()))
}

/** Stores a valid CreateQuizQuestionRequest for a quiz, with new ids for it, its options and items. */
function storeQuestion(quizId: number, question: CreateQuizQuestionRequest) {
  const optionIds = quizQuestions.flatMap(q => q.options.map(o => o.id))
  const stored = storedQuestion(question, {
    questionId: nextId(quizQuestions),
    quizId,
    firstOptionId: Math.max(0, ...optionIds) + 1,
  })
  quizQuestions.push(stored)
  return stored
}

/** Handler for GET /api/<...>/:contentId for one content type. */
function contentDetail(type: ContentType): MockHandler['handle'] {
  return ({ user, params }): MockResult => {
    const content = contents.find(c => String(c.id) === params.contentId && c.type === type)
    if (!content) return notFound()
    if (!canReadCourse(user, content.courseId)) return forbidden()
    const teacher = isTeacher(user, content.courseId)
    if (!teacher && !visibleToStudents(content)) return notFound()
    return ok(detailResponse(content, teacher))
  }
}

/** The quiz with this id, or the answer to send instead. */
function quizFor(params: Record<string, string>) {
  const quiz = contents.find(c => String(c.id) === params.quizId && c.type === 'QUIZ')
  return quiz && quiz.type === 'QUIZ' ? quiz : notFound()
}

/** The attempt without the stored user id (QuizAttemptResponse, plus answers when kept). */
function attemptResponse(attempt: (typeof quizAttempts)[number], withAnswers: boolean) {
  const { userId: _userId, answers, ...response } = attempt
  return withAnswers ? { ...response, answers } : response
}

/** QuizAnswerKeyResponse.QuestionKeyResponse for one stored question. */
function questionKey(question: (typeof quizQuestions)[number]) {
  return {
    question: questionResponse(question, false),
    hint: question.hint,
    correctOptionIds: question.options
      .filter(o => o.correct)
      .map(o => o.id)
      .sort((a, b) => a - b),
    acceptedAnswers: [...(question.acceptedAnswers ?? [])].sort(),
    correctOrder: question.correctOrder ?? [],
    correctMatches: [...(question.correctMatches ?? [])].sort((a, b) => a.leftItemId.localeCompare(b.leftItemId)),
  }
}

const ACCESS_LEVELS: readonly MockUser['access'][] = ['STANDARD', 'COURSE_MANAGER', 'ADMIN']
const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/

/** Why the backend would refuse these profile fields (400), or null. */
function profileProblem(fields: { firstName?: unknown; lastName?: unknown; email?: unknown }, withEmail: boolean) {
  const text = (value: unknown) => typeof value === 'string' && value.trim() !== '' && value.length <= 255
  if (!text(fields.firstName) || !text(fields.lastName)) return 'Invalid profile details'
  if (withEmail && !(text(fields.email) && EMAIL.test(String(fields.email).trim()))) return 'Invalid profile details'
  return null
}

/** Splits one CSV line with "…" quoting, like the backend's UserCsvReader. */
function csvFields(line: string, delimiter: string): string[] {
  const fields: string[] = []
  let field = ''
  let quoted = false
  for (let i = 0; i < line.length; i++) {
    const char = line[i]
    if (quoted) {
      if (char === '"' && line[i + 1] === '"') {
        field += '"'
        i++
      } else if (char === '"') quoted = false
      else field += char
    } else if (char === '"') quoted = true
    else if (char === delimiter) {
      fields.push(field)
      field = ''
    } else field += char
  }
  fields.push(field)
  return fields
}

/** Admin-only routes answer 403 to everyone else, like @IsAdmin. */
const adminOnly = (user: MockUser) => (user.access === 'ADMIN' ? null : forbidden())

const routes: MockHandler[] = [
  {
    method: 'GET',
    path: '/api/users/me',
    handle: ({ user }) => ok(user),
  },
  {
    // Backend (exists): UpdateUserRequest → UserResponse; only the names change.
    method: 'PUT',
    path: '/api/users/me',
    handle: ({ user, body }) => {
      const request = body as { firstName?: string; lastName?: string }
      const problem = profileProblem(request, false)
      if (problem) return badRequest(problem)
      user.firstName = request.firstName!.trim()
      user.lastName = request.lastName!.trim()
      return ok(user)
    },
  },
  {
    // Backend (exists): UserPageResponse, sorted by id, filtered by name or email.
    method: 'GET',
    path: '/api/admin/users',
    handle: ({ user, query }) => {
      const denied = adminOnly(user)
      if (denied) return denied
      const text = (query.get('search') ?? '').trim().toLowerCase()
      const page = Number(query.get('page') ?? 0)
      const size = Number(query.get('size') ?? 20)
      if (!(page >= 0) || !(size >= 1 && size <= 100)) return badRequest('Invalid request parameters')
      const found = users
        .filter(u => `${u.firstName} ${u.lastName} ${u.email}`.toLowerCase().includes(text))
        .sort((a, b) => a.userId - b.userId)
      return ok({
        users: found.slice(page * size, (page + 1) * size),
        page,
        size,
        totalElements: found.length,
        totalPages: Math.ceil(found.length / size),
      })
    },
  },
  {
    // Backend (exists): CreateUserRequest → 201 UserResponse; 409 for a known email.
    method: 'POST',
    path: '/api/admin/users',
    handle: ({ user, body }) => {
      const denied = adminOnly(user)
      if (denied) return denied
      const request = body as { firstName?: string; lastName?: string; email?: string }
      const problem = profileProblem(request, true)
      if (problem) return badRequest(problem)
      const email = request.email!.trim().toLowerCase()
      if (users.some(u => u.email.toLowerCase() === email)) {
        return conflict('Profile data conflicts with an existing user')
      }
      const added: MockUser = {
        userId: Math.max(0, ...users.map(u => u.userId)) + 1,
        email,
        firstName: request.firstName!.trim(),
        lastName: request.lastName!.trim(),
        access: 'STANDARD',
      }
      users.push(added)
      return created(added)
    },
  },
  {
    // Backend (exists): multipart "file" (CSV) → UserImportResponse with a row per line.
    method: 'POST',
    path: '/api/admin/users/import',
    handle: ({ user, body }) => {
      const denied = adminOnly(user)
      if (denied) return denied
      const file = (body as MockMultipart | undefined)?.files.find(f => f.field === 'file')
      if (!file) return badRequest('A file part named file is required')
      const lines = new TextDecoder()
        .decode(file.content)
        .replace(/^﻿/, '')
        .split(/\r\n|\n|\r/)
      const header = lines[0] ?? ''
      if (!header.trim()) return badRequest('The file is empty; expected a header row: email,firstName,lastName,access')
      const delimiter = header.includes(';') && !header.includes(',') ? ';' : ','
      const columns = csvFields(header, delimiter).map(name => name.trim().toLowerCase())
      for (const required of ['email', 'firstname', 'lastname']) {
        if (!columns.includes(required)) {
          return badRequest(`Missing column '${required}' in the header; expected: email,firstName,lastName,access`)
        }
      }
      const seen = new Set<string>()
      const rows: { line: number; email: string; status: string; userId: number | null; errors: string[] }[] = []
      for (const [index, line] of lines.entries()) {
        if (index === 0 || !line.trim()) continue
        const fields = csvFields(line, delimiter)
        const value = (column: string) => (fields[columns.indexOf(column)] ?? '').trim()
        const email = value('email').toLowerCase()
        const access = value('access').toUpperCase() || 'STANDARD'
        const row = { line: index + 1, email, status: 'CREATED', userId: null as number | null, errors: [] as string[] }
        const existing = users.find(u => u.email.toLowerCase() === email)
        if (email && seen.has(email)) row.status = 'DUPLICATE_IN_FILE'
        else if (existing) {
          row.status = 'ALREADY_EXISTS'
          row.userId = existing.userId
        } else {
          const problem = profileProblem({ firstName: value('firstname'), lastName: value('lastname'), email }, true)
          if (problem) row.errors.push(problem)
          if (!(ACCESS_LEVELS as readonly string[]).includes(access)) row.errors.push(`Unknown access: ${access}`)
          if (row.errors.length > 0) row.status = 'INVALID'
          else {
            const added: MockUser = {
              userId: Math.max(0, ...users.map(u => u.userId)) + 1,
              email,
              firstName: value('firstname'),
              lastName: value('lastname'),
              access: access as MockUser['access'],
            }
            users.push(added)
            row.userId = added.userId
          }
        }
        if (email) seen.add(email)
        rows.push(row)
      }
      const count = (status: string) => rows.filter(r => r.status === status).length
      return ok({
        created: count('CREATED'),
        skipped: count('ALREADY_EXISTS'),
        failed: count('INVALID') + count('DUPLICATE_IN_FILE'),
        rows,
      })
    },
  },
  {
    // Backend (exists): UserResponse.
    method: 'GET',
    path: '/api/admin/users/:userId',
    handle: ({ user, params }) => {
      const denied = adminOnly(user)
      if (denied) return denied
      const found = users.find(u => String(u.userId) === params.userId)
      return found ? ok(found) : notFound()
    },
  },
  {
    // Backend (exists): UpdateAccessRequest → UserResponse; 403 when an admin demotes themselves.
    method: 'PATCH',
    path: '/api/admin/users/:userId/access',
    handle: ({ user, params, body }) => {
      const denied = adminOnly(user)
      if (denied) return denied
      const { access } = body as { access?: MockUser['access'] }
      if (!access || !ACCESS_LEVELS.includes(access)) return badRequest('Invalid request body')
      if (String(user.userId) === params.userId && access !== 'ADMIN') {
        return { status: 403, body: { detail: 'You cannot remove your own ADMIN access' } }
      }
      const found = users.find(u => String(u.userId) === params.userId)
      if (!found) return notFound()
      found.access = access
      return ok(found)
    },
  },
  {
    method: 'GET',
    path: '/api/users/:userId/courses',
    handle: ({ user, params }) => {
      if (!canReadUser(user, params.userId)) return forbidden()
      if (!users.some(u => String(u.userId) === params.userId)) return notFound()
      return ok(coursesOf(Number(params.userId)))
    },
  },
  {
    method: 'GET',
    path: '/api/users/:userId/upcoming-deadlines',
    handle: ({ user, params }) => {
      if (!canReadUser(user, params.userId)) return forbidden()
      // Like the backend: students only get content inside its visible window, teachers get all of it.
      const roles = new Map(coursesOf(Number(params.userId)).map(course => [course.id, course.role]))
      const now = Date.now()
      const result: UpcomingDeadline[] = contents
        .flatMap(content =>
          (content.type === 'ASSIGNMENT' || content.type === 'CODING_EXERCISE') &&
          content.deadline &&
          Date.parse(content.deadline) > now &&
          roles.has(content.courseId) &&
          (roles.get(content.courseId) === 'TEACHER' || visibleToStudents(content))
            ? [
                {
                  contentId: content.id,
                  type: content.type,
                  title: content.title,
                  deadline: content.deadline,
                  allowLateSubmission: content.allowLateSubmission,
                  mandatoryForExam: content.type === 'ASSIGNMENT' ? content.mandatoryForExam : null,
                  courseId: content.courseId,
                  courseTitle: courses.find(c => c.id === content.courseId)?.title ?? '',
                  moduleId: content.moduleId,
                },
              ]
            : [],
        )
        .sort((a, b) => Date.parse(a.deadline) - Date.parse(b.deadline))
      return ok(result)
    },
  },
  {
    method: 'GET',
    path: '/api/courses/:courseId',
    handle: ({ user, params }) => {
      const course = courses.find(c => String(c.id) === params.courseId)
      if (!course) return notFound()
      if (!canReadCourse(user, course.id)) return forbidden()
      return ok(course)
    },
  },
  {
    // ModuleResponse: id, courseId, title, position, dependencyIds, availableFrom,
    // availableToAllFrom (LocalDateTime).
    method: 'GET',
    path: '/api/courses/:courseId/modules',
    handle: ({ user, params }) => {
      if (!courses.some(c => String(c.id) === params.courseId)) return notFound()
      if (!canReadCourse(user, params.courseId)) return forbidden()
      return ok(
        modules
          .filter(m => String(m.courseId) === params.courseId)
          .sort((a, b) => a.position - b.position)
          .map(moduleResponse),
      )
    },
  },
  {
    method: 'GET',
    path: '/api/courses/:courseId/modules/:moduleId',
    handle: ({ user, params }) => {
      const module = modules.find(m => String(m.id) === params.moduleId && String(m.courseId) === params.courseId)
      if (!module) return notFound()
      if (!canReadCourse(user, params.courseId)) return forbidden()
      return ok(moduleResponse(module))
    },
  },
  {
    // ContentSummaryResponse: id, type, title, position.
    method: 'GET',
    path: '/api/courses/:courseId/modules/:moduleId/contents',
    handle: ({ user, params }) => {
      const module = modules.find(m => String(m.id) === params.moduleId && String(m.courseId) === params.courseId)
      if (!module) return notFound()
      if (!canReadCourse(user, params.courseId)) return forbidden()
      // Students do not see content before its visible date; teachers do.
      const teacher = isTeacher(user, params.courseId)
      return ok(
        contents
          .filter(c => c.moduleId === module.id && (teacher || visibleToStudents(c)))
          .sort((a, b) => a.position - b.position)
          .map(c => ({
            id: c.id,
            type: BACKEND_TYPE[c.type],
            title: c.title,
            position: c.position,
            requiredForModuleCompletion: c.requiredForModuleCompletion ?? false,
            visibleFrom: c.visibleFrom ?? null,
          })),
      )
    },
  },
  {
    // Proposed endpoint (src/domain/content/README.md): CompletedContent[] for the caller.
    method: 'GET',
    path: '/api/courses/:courseId/completions/me',
    handle: ({ user, params }) => {
      if (!courses.some(c => String(c.id) === params.courseId)) return notFound()
      if (!canReadCourse(user, params.courseId)) return forbidden()
      const inCourse = new Set(contents.filter(c => String(c.courseId) === params.courseId).map(c => c.id))
      return ok(
        completions
          .filter(row => row.userId === user.userId && inCourse.has(row.contentId))
          .map(({ contentId, completedAt }) => ({ contentId, completedAt })),
      )
    },
  },
  {
    // SubmissionResponse[]: the caller's own (and their group's) submissions for one content item.
    method: 'GET',
    path: '/api/contents/:contentId/submissions/me',
    handle: ({ user, params }) => {
      const content = contents.find(c => String(c.id) === params.contentId)
      if (!content) return notFound()
      if (!canReadCourse(user, content.courseId)) return forbidden()
      return ok(
        submissions.filter(s => s.contentId === content.id && isOwnSubmission(user, s)).map(s => submissionResponse(s)),
      )
    },
  },
  {
    // CreateSubmissionRequest { type, payload, groupId? } → SubmissionResponse (201).
    method: 'POST',
    path: '/api/contents/:contentId/submissions',
    handle: ({ user, params, body }) => {
      const { type, payload, groupId } = body as { type?: 'TEXT' | 'GIT_URL'; payload?: string; groupId?: number }
      if ((type !== 'TEXT' && type !== 'GIT_URL') || !payload?.trim())
        return badRequest('Type and payload are required')
      const check = checkSubmit(user, params.contentId, groupId ?? undefined)
      if (check.error) return check.error
      const submission: MockSubmission = {
        id: nextId(submissions),
        userId: user.userId,
        contentId: Number(check.content.id),
        groupId: groupId ?? undefined,
        type,
        payload: payload.trim(),
        submittedAt: new Date().toISOString(),
        late: check.late,
        grade: null,
      }
      submissions.push(submission)
      return created(submissionResponse(submission))
    },
  },
  {
    // Multipart: one PDF in "file" (at most 10 MB), optional "groupId" → SubmissionResponse (201).
    method: 'POST',
    path: '/api/contents/:contentId/submissions/file',
    handle: ({ user, params, body }) => {
      const form = body as MockMultipart | undefined
      const files = form?.files.filter(f => f.field === 'file') ?? []
      if (files.length !== 1) return badRequest('Exactly one file part named file is required')
      const file = files[0]
      const header = new TextDecoder().decode(file.content.slice(0, 5))
      if (!file.name.toLowerCase().endsWith('.pdf') || file.mediaType !== 'application/pdf' || header !== '%PDF-') {
        return {
          status: 415,
          body: { detail: 'Only PDF files with application/pdf content type and a PDF header are accepted' },
        }
      }
      if (file.content.length > 10 * 1024 * 1024) return { status: 413, body: { detail: 'File too large' } }
      const groupId = form?.fields.groupId ? Number(form.fields.groupId) : undefined
      const check = checkSubmit(user, params.contentId, groupId)
      if (check.error) return check.error
      const submission: MockSubmission = {
        id: nextId(submissions),
        userId: user.userId,
        contentId: Number(check.content.id),
        groupId,
        type: 'FILE',
        payload: null,
        file: { name: file.name, mediaType: file.mediaType, content: file.content },
        submittedAt: new Date().toISOString(),
        late: check.late,
        grade: null,
      }
      submissions.push(submission)
      return created(submissionResponse(submission))
    },
  },
  {
    // The student's own file.
    method: 'GET',
    path: '/api/submissions/:submissionId/file',
    handle: ({ user, params }) => {
      const submission = submissions.find(s => String(s.id) === params.submissionId)
      if (!submission?.file || !isOwnSubmission(user, submission)) return notFound()
      return { status: 200, download: submission.file }
    },
  },
  {
    // ReleasedGradeResponse for the student's own submission; 404 until released.
    method: 'GET',
    path: '/api/submissions/:submissionId/grade',
    handle: ({ user, params }) => {
      const submission = submissions.find(s => String(s.id) === params.submissionId)
      if (!submission || !isOwnSubmission(user, submission)) return notFound()
      const grade = releasedGrade(submission)
      return grade ? ok({ submissionId: submission.id, ...grade }) : notFound()
    },
  },
  {
    // Backend: UserGradeResponse[]. The latest submission per assignment / coding
    // exercise, with its released grade or null, newest first.
    method: 'GET',
    path: '/api/users/me/grades',
    handle: ({ user }) => {
      const latest = new Map<number, (typeof submissions)[number]>()
      for (const submission of submissions.filter(s => isOwnSubmission(user, s))) {
        const previous = latest.get(submission.contentId)
        if (!previous || Date.parse(submission.submittedAt) > Date.parse(previous.submittedAt)) {
          latest.set(submission.contentId, submission)
        }
      }
      return ok(
        [...latest.values()].flatMap(submission => {
          const content = contents.find(c => c.id === submission.contentId)
          if (!content || (content.type !== 'ASSIGNMENT' && content.type !== 'CODING_EXERCISE')) return []
          return [
            {
              contentId: content.id,
              type: content.type,
              title: content.title,
              courseId: content.courseId,
              courseTitle: courses.find(c => c.id === content.courseId)?.title ?? '',
              moduleId: content.moduleId,
              submissionId: submission.id,
              submittedAt: submission.submittedAt,
              late: submission.late,
              grade: releasedGrade(submission),
            },
          ]
        }),
      )
    },
  },
  {
    // Proposed endpoint (src/domain/modules/README.md). The mock calls a module completed
    // when the user has completed all its content marked requiredForModuleCompletion; with
    // none marked, all its assignments, coding exercises and quizzes (and it has at
    // least one). The backend decides the real rule.
    method: 'GET',
    path: '/api/users/:userId/courses/:courseId/modules/progress',
    handle: ({ user, params }) => {
      if (!canReadUser(user, params.userId)) return forbidden()
      if (!canReadCourse(user, params.courseId)) return forbidden()
      const done = new Set(completions.filter(c => String(c.userId) === params.userId).map(c => c.contentId))
      const courseModules = modules.filter(m => String(m.courseId) === params.courseId)
      const completedIds = new Set(
        courseModules
          .filter(module => {
            const inModule = contents.filter(c => c.moduleId === module.id)
            const required = inModule.filter(c => c.requiredForModuleCompletion)
            const tasks = required.length > 0 ? required : inModule.filter(c => c.type !== 'STATIC_PAGE')
            return tasks.length > 0 && tasks.every(task => done.has(Number(task.id)))
          })
          .map(module => String(module.id)),
      )
      return ok(
        courseModules.map(module => ({
          moduleId: module.id,
          completed: completedIds.has(String(module.id)),
          // From its start date, when its prerequisites are completed or it is open to all.
          accessible: isAccessible(module, completedIds, new Date()),
        })),
      )
    },
  },
  // ---------- Teacher and course manager endpoints ----------
  {
    // Backend: CreateCourseRequest -> CourseResponse. The creator becomes TEACHER
    // (the real backend does not do that yet).
    method: 'POST',
    path: '/api/courses',
    handle: ({ user, body }) => {
      if (user.access === 'STANDARD') return forbidden()
      const input = body as { title: string; description: string; term: 'SPRING' | 'AUTUMN'; year: number }
      if (!input?.title?.trim() || !input.description?.trim()) return badRequest('Title and description are required')
      const course = {
        id: nextId(courses),
        title: input.title,
        description: input.description,
        term: input.term,
        year: input.year,
      }
      courses.push(course)
      enrollments.push({ userId: user.userId, courseId: course.id, role: 'TEACHER' })
      return created(course)
    },
  },
  {
    // Proposed (data/courses/README.md): UpdateCourseInput.changes -> CourseResponse.
    // Only teachers of the course may change it.
    method: 'PATCH',
    path: '/api/courses/:courseId',
    handle: ({ user, params, body }) => {
      const course = courses.find(c => String(c.id) === params.courseId)
      if (!course) return notFound()
      if (!isTeacher(user, course.id)) return forbidden()
      const changes = body as Partial<{ title: string; description: string; term: 'SPRING' | 'AUTUMN'; year: number }>
      if (changes.title !== undefined && !changes.title.trim()) return badRequest('Title is required')
      if (changes.description !== undefined && !changes.description.trim()) return badRequest('Description is required')
      Object.assign(course, changes)
      return ok(course)
    },
  },
  {
    // Backend (exists): only COURSE_MANAGER and ADMIN may search.
    method: 'GET',
    path: '/api/users/search',
    handle: ({ user, query }) => {
      if (user.access === 'STANDARD') return forbidden()
      const text = (query.get('search') ?? '').toLowerCase()
      const found = users
        .filter(u => `${u.firstName} ${u.lastName} ${u.email}`.toLowerCase().includes(text))
        .map(({ userId, firstName, lastName, email }) => ({ userId, firstName, lastName, email }))
      return ok({ users: found.slice(0, 10), page: 0, size: 10, totalElements: found.length, totalPages: 1 })
    },
  },
  {
    method: 'GET',
    path: '/api/courses/:courseId/enrollments',
    handle: ({ user, params }) => {
      // Every member of the course (and admins) may see who is in it, sorted by name.
      if (!canReadCourse(user, params.courseId)) return forbidden()
      return ok(
        enrollments
          .filter(e => String(e.courseId) === params.courseId)
          .flatMap(e => enrollmentResponse(e) ?? [])
          .sort((a, b) => a.lastName.localeCompare(b.lastName) || a.firstName.localeCompare(b.firstName)),
      )
    },
  },
  {
    // GroupDTO[] (backend), with the proposed name and members.
    method: 'GET',
    path: '/api/courses/:courseId/groups',
    handle: ({ user, params }) => {
      if (!canReadCourse(user, params.courseId)) return forbidden()
      return ok(
        groups
          .filter(g => String(g.courseId) === params.courseId)
          .map(group => ({
            id: group.id,
            groupNumber: group.groupNumber,
            memberIds: group.memberIds,
            name: group.name ?? null,
            members: groupMembers(group),
          })),
      )
    },
  },
  {
    // Teachers: { groupNumber } → GroupDTO (201). 400 below 1, 409 when the number is taken.
    method: 'POST',
    path: '/api/courses/:courseId/groups',
    handle: ({ user, params, body }) => {
      if (!isTeacher(user, params.courseId)) return forbidden()
      const groupNumber = Number((body as { groupNumber?: number } | undefined)?.groupNumber)
      if (!Number.isInteger(groupNumber) || groupNumber < 1)
        return badRequest('Group number must be a positive integer')
      if (groups.some(g => String(g.courseId) === params.courseId && g.groupNumber === groupNumber)) {
        return conflict('Group number already exists in this course')
      }
      const group = { id: nextId(groups), courseId: Number(params.courseId), groupNumber, memberIds: [] as number[] }
      groups.push(group)
      return created({ id: group.id, groupNumber, memberIds: [], name: null, members: [] })
    },
  },
  {
    // Teachers. 409 when the group has submissions (retained history).
    method: 'DELETE',
    path: '/api/courses/:courseId/groups/:groupId',
    handle: ({ user, params }) => {
      if (!isTeacher(user, params.courseId)) return forbidden()
      const index = groups.findIndex(g => String(g.id) === params.groupId && String(g.courseId) === params.courseId)
      if (index === -1) return notFound()
      if (submissions.some(s => String(s.groupId) === params.groupId)) return conflict('Group has retained history')
      groups.splice(index, 1)
      return noContent()
    },
  },
  {
    // Body: the bare user id → GroupDTO. Proposed: a student may add themselves (join);
    // the backend lets only teachers do this today. Only active students; one group per course.
    method: 'POST',
    path: '/api/courses/:courseId/groups/:groupId/members',
    handle: ({ user, params, body }) => {
      const userId = Number(body)
      if (!isTeacher(user, params.courseId) && userId !== user.userId) return forbidden()
      const group = groups.find(g => String(g.id) === params.groupId && String(g.courseId) === params.courseId)
      if (!group) return notFound()
      if (
        !enrollments.some(e => e.userId === userId && String(e.courseId) === params.courseId && e.role === 'STUDENT')
      ) {
        return forbidden()
      }
      if (group.memberIds.includes(userId)) return conflict('User is already a member of this group')
      if (groups.some(g => String(g.courseId) === params.courseId && g.memberIds.includes(userId))) {
        return conflict('User already belongs to another group in this course')
      }
      group.memberIds.push(userId)
      return ok({
        id: group.id,
        groupNumber: group.groupNumber,
        memberIds: group.memberIds,
        name: group.name ?? null,
        members: groupMembers(group),
      })
    },
  },
  {
    // Proposed: a student may remove themselves (leave); today only teachers may.
    method: 'DELETE',
    path: '/api/courses/:courseId/groups/:groupId/members/:userId',
    handle: ({ user, params }) => {
      const userId = Number(params.userId)
      if (!isTeacher(user, params.courseId) && userId !== user.userId) return forbidden()
      const group = groups.find(g => String(g.id) === params.groupId && String(g.courseId) === params.courseId)
      if (!group) return notFound()
      if (!group.memberIds.includes(userId)) return conflict('User is not a member of this group')
      group.memberIds = group.memberIds.filter(id => id !== userId)
      return noContent()
    },
  },
  {
    method: 'POST',
    path: '/api/courses/:courseId/enrollments',
    handle: ({ user, params, body }) => {
      // Backend: EnrollRequest -> 201 EnrollmentResponse; 404 unknown user or course, 409 already a member.
      if (!isTeacherOrAdmin(user, params.courseId)) return forbidden()
      if (!courses.some(c => String(c.id) === params.courseId)) return notFound()
      const { userId, role = 'STUDENT' } = body as { userId: number; role?: 'STUDENT' | 'TEACHER' }
      if (!users.some(u => u.userId === Number(userId))) return notFound()
      if (enrollments.some(e => e.userId === Number(userId) && String(e.courseId) === params.courseId)) {
        return { status: 409, body: { error: 'Already enrolled' } }
      }
      const enrollment = { userId: Number(userId), courseId: Number(params.courseId), role }
      enrollments.push(enrollment)
      return created(enrollmentResponse(enrollment))
    },
  },
  {
    // Backend: UpdateCourseRoleRequest -> EnrollmentResponse.
    method: 'PATCH',
    path: '/api/courses/:courseId/enrollments/:userId',
    handle: ({ user, params, body }) => {
      if (!isTeacherOrAdmin(user, params.courseId)) return forbidden()
      const enrollment = enrollments.find(
        e => String(e.userId) === params.userId && String(e.courseId) === params.courseId,
      )
      if (!enrollment) return notFound()
      const { role } = (body ?? {}) as { role?: 'STUDENT' | 'TEACHER' }
      if (role !== 'STUDENT' && role !== 'TEACHER') return badRequest('role is required')
      enrollment.role = role
      return ok(enrollmentResponse(enrollment))
    },
  },
  {
    method: 'DELETE',
    path: '/api/courses/:courseId/enrollments/:userId',
    handle: ({ user, params }) => {
      if (!isTeacherOrAdmin(user, params.courseId)) return forbidden()
      const index = enrollments.findIndex(
        e => String(e.userId) === params.userId && String(e.courseId) === params.courseId,
      )
      if (index < 0) return notFound()
      enrollments.splice(index, 1)
      return noContent()
    },
  },
  {
    // Backend (exists): CreateModuleRequest -> ModuleResponse.
    method: 'POST',
    path: '/api/courses/:courseId/modules',
    handle: ({ user, params, body }) => {
      if (!isTeacher(user, params.courseId)) return forbidden()
      const { title, position } = body as { title: string; position: number }
      if (!title?.trim()) return badRequest('Title is required')
      const module = {
        id: nextId(modules),
        courseId: Number(params.courseId),
        title,
        position,
        prerequisiteModuleIds: [],
      }
      modules.push(module)
      return created(moduleResponse(module))
    },
  },
  {
    // UpdateModuleRequest { title, position } (both required) -> ModuleResponse.
    method: 'PUT',
    path: '/api/courses/:courseId/modules/:moduleId',
    handle: ({ user, params, body }) => {
      const module = teacherModule(user, params)
      if (isResult(module)) return module
      const { title, position } = (body ?? {}) as { title?: string; position?: number }
      if (!title?.trim() || typeof position !== 'number' || position < 0) {
        return badRequest('Title and a position of 0 or more are required')
      }
      module.title = title
      module.position = position
      return ok(moduleResponse(module))
    },
  },
  {
    // UpdateModuleAvailabilityRequest { availableFrom, availableToAllFrom } (LocalDateTime;
    // both are set, a missing one becomes null) -> ModuleResponse.
    method: 'PUT',
    path: '/api/courses/:courseId/modules/:moduleId/availability',
    handle: ({ user, params, body }) => {
      const module = teacherModule(user, params)
      if (isResult(module)) return module
      const request = (body ?? {}) as { availableFrom?: string | null; availableToAllFrom?: string | null }
      const availableFrom = fromLocalDateTime(request.availableFrom)
      const openToAllFrom = fromLocalDateTime(request.availableToAllFrom)
      const dateProblem = checkAvailabilityDates(availableFrom, openToAllFrom)
      if (dateProblem) return badRequest(dateProblem)
      module.availableFrom = availableFrom
      module.openToAllFrom = openToAllFrom
      return ok(moduleResponse(module))
    },
  },
  {
    // Removes the module and the other modules' dependencies on it (204). Like the
    // backend, its content is not deleted; the mock refuses a module with content.
    method: 'DELETE',
    path: '/api/courses/:courseId/modules/:moduleId',
    handle: ({ user, params }) => {
      const module = teacherModule(user, params)
      if (isResult(module)) return module
      if (contents.some(c => c.moduleId === module.id)) return conflict('The module still has content')
      for (const other of modules) {
        other.prerequisiteModuleIds = (other.prerequisiteModuleIds ?? []).filter(id => id !== module.id)
      }
      modules.splice(modules.indexOf(module), 1)
      return noContent()
    },
  },
  {
    // The ids of the modules this one requires.
    method: 'GET',
    path: '/api/courses/:courseId/modules/:moduleId/dependencies',
    handle: ({ user, params }) => {
      const module = modules.find(m => String(m.id) === params.moduleId && String(m.courseId) === params.courseId)
      if (!module) return notFound()
      if (!canReadCourse(user, params.courseId)) return forbidden()
      return ok(module.prerequisiteModuleIds ?? [])
    },
  },
  {
    // Adds a prerequisite in the same course, without cycles -> ModuleResponse.
    method: 'POST',
    path: '/api/courses/:courseId/modules/:moduleId/dependencies/:requiredModuleId',
    handle: ({ user, params }) => {
      const module = teacherModule(user, params)
      if (isResult(module)) return module
      const courseModules = modules.filter(m => m.courseId === module.courseId)
      const requiredId = Number(params.requiredModuleId)
      if (!courseModules.some(m => m.id === requiredId)) return notFound()
      if (!canRequire(courseModules, module.id, requiredId)) return badRequest('Prerequisites cannot form a cycle')
      const ids = module.prerequisiteModuleIds ?? []
      if (!ids.includes(requiredId)) module.prerequisiteModuleIds = [...ids, requiredId]
      return ok(moduleResponse(module))
    },
  },
  {
    method: 'DELETE',
    path: '/api/courses/:courseId/modules/:moduleId/dependencies/:requiredModuleId',
    handle: ({ user, params }) => {
      const module = teacherModule(user, params)
      if (isResult(module)) return module
      const requiredId = Number(params.requiredModuleId)
      if (!modules.some(m => m.id === requiredId && m.courseId === module.courseId)) return notFound()
      module.prerequisiteModuleIds = (module.prerequisiteModuleIds ?? []).filter(id => id !== requiredId)
      return noContent()
    },
  },
  ...(
    [
      ['pages', 'STATIC_PAGE'],
      ['assignments', 'ASSIGNMENT'],
      ['coding-exercises', 'CODING_EXERCISE'],
      ['quizzes', 'QUIZ'],
    ] as const
  ).map(([slug, type]): MockHandler => ({
    // Backend (exists): one create endpoint per type; answers like its GET endpoint.
    method: 'POST',
    path: `/api/courses/:courseId/modules/:moduleId/contents/${slug}`,
    handle: ({ user, params, body }) => {
      const module = modules.find(m => String(m.id) === params.moduleId && String(m.courseId) === params.courseId)
      if (!module) return notFound()
      if (!isTeacher(user, params.courseId)) return forbidden()
      const fields = body as Record<string, unknown>
      if (!String(fields.title ?? '').trim()) return badRequest('Title is required')
      // Proposed: visibleFrom (or null to inherit the module's start date), never before it.
      const visibleProblem = checkContentVisibleFrom(
        (fields.visibleFrom as string | null | undefined) ?? null,
        module.availableFrom,
      )
      if (visibleProblem) return badRequest(visibleProblem)
      const content = {
        ...fields,
        id: nextId(contents),
        courseId: module.courseId,
        moduleId: module.id,
        type,
        requiredForModuleCompletion: false,
        // Optional fields the backend fills in.
        ...(type === 'ASSIGNMENT' || type === 'CODING_EXERCISE' ? { deadline: fields.deadline ?? null } : {}),
        ...(type === 'ASSIGNMENT' ? { mandatoryForExam: fields.mandatoryForExam === true } : {}),
        // CreateQuizRequest has no passing percentage: the backend's default, changed
        // afterwards with PATCH …/completion-settings.
        ...(type === 'QUIZ' ? { passingPercentage: 60 } : {}),
      } as (typeof contents)[number]
      contents.push(content)
      return created(detailResponse(content))
    },
  })),
  {
    // Proposed (data/content/README.md): { moduleId, position } -> the moved content.
    // The content may only move to another module in the same course.
    method: 'PATCH',
    path: '/api/contents/:contentId/move',
    handle: ({ user, params, body }) => {
      const content = contents.find(c => String(c.id) === params.contentId)
      if (!content) return notFound()
      if (!isTeacher(user, content.courseId)) return forbidden()
      const { moduleId, position } = body as { moduleId: string | number; position: number }
      const target = modules.find(m => String(m.id) === String(moduleId) && m.courseId === content.courseId)
      if (!target) return badRequest('The module is not in this course')
      content.moduleId = target.id
      content.position = position
      return ok(detailResponse(content))
    },
  },
  {
    // Backend (exists, ContentCompletionController): { requiredForModuleCompletion,
    // passingPercentage } -> 204. passingPercentage (1–100) only for quizzes; null keeps it.
    method: 'PATCH',
    path: '/api/contents/:contentId/completion-settings',
    handle: ({ user, params, body }) => {
      const content = contents.find(c => String(c.id) === params.contentId)
      if (!content) return notFound()
      if (!isTeacher(user, content.courseId)) return forbidden()
      const { requiredForModuleCompletion, passingPercentage } = body as {
        requiredForModuleCompletion?: boolean
        passingPercentage?: number | null
      }
      if (typeof requiredForModuleCompletion !== 'boolean') return badRequest('requiredForModuleCompletion is required')
      if (passingPercentage !== undefined && passingPercentage !== null) {
        if (content.type !== 'QUIZ') return badRequest('Only quizzes have a passing percentage')
        if (!Number.isInteger(passingPercentage) || passingPercentage < 1 || passingPercentage > 100) {
          return badRequest('Passing percentage must be between 1 and 100')
        }
        content.passingPercentage = passingPercentage
      }
      content.requiredForModuleCompletion = requiredForModuleCompletion
      return noContent()
    },
  },
  {
    // Backend (ContentCompletionController): the caller's status; active students only.
    // The mock counts an item as completed when it has a row in `completions`.
    method: 'GET',
    path: '/api/contents/:contentId/completion',
    handle: ({ user, params }) => {
      const content = contents.find(c => String(c.id) === params.contentId)
      if (!content) return notFound()
      if (!isStudent(user, content.courseId)) return forbidden()
      return ok(completionStatus(user, content))
    },
  },
  {
    // Backend: { completed } -> the new status. Pages only (400), inside their visible window (403).
    method: 'PUT',
    path: '/api/contents/:contentId/completion',
    handle: ({ user, params, body }) => {
      const content = contents.find(c => String(c.id) === params.contentId)
      if (!content) return notFound()
      if (!isStudent(user, content.courseId)) return forbidden()
      const { completed } = (body ?? {}) as { completed?: boolean }
      if (typeof completed !== 'boolean') return badRequest('completed is required')
      if (content.type !== 'STATIC_PAGE') return badRequest('Only pages support manual completion')
      if (!visibleToStudents(content)) return forbidden()
      const contentId = Number(content.id)
      const index = completions.findIndex(c => c.userId === user.userId && c.contentId === contentId)
      if (completed && index < 0) {
        completions.push({ userId: user.userId, contentId, completedAt: new Date().toISOString() })
      } else if (!completed && index >= 0) {
        completions.splice(index, 1)
      }
      return ok(completionStatus(user, content))
    },
  },
  {
    // Backend (AssignmentController): { mandatoryForExam } -> 204, also after publishing.
    method: 'PATCH',
    path: '/api/assignments/:assignmentId/exam-requirement',
    handle: ({ user, params, body }) => {
      const content = contents.find(c => String(c.id) === params.assignmentId)
      if (!content || content.type !== 'ASSIGNMENT') return notFound()
      if (!isTeacher(user, content.courseId)) return forbidden()
      const { mandatoryForExam } = (body ?? {}) as { mandatoryForExam?: boolean }
      if (typeof mandatoryForExam !== 'boolean') return badRequest('mandatoryForExam is required')
      content.mandatoryForExam = mandatoryForExam
      return noContent()
    },
  },
  // Backend (exists): one update endpoint per type, each with its own fields (all
  // required). PUT /api/contents/{id} is for pages only.
  ...(
    [
      ['/api/contents/:contentId', 'STATIC_PAGE', ['title', 'body']],
      ['/api/assignments/:contentId', 'ASSIGNMENT', ['title', 'instructions']],
      [
        '/api/coding-exercises/:contentId',
        'CODING_EXERCISE',
        ['title', 'instructions', 'starterRepositoryUrl', 'starterCode'],
      ],
      ['/api/assignments/:contentId/submission-settings', 'ASSIGNMENT', ['deadline', 'allowLateSubmission']],
      ['/api/coding-exercises/:contentId/submission-settings', 'CODING_EXERCISE', ['deadline', 'allowLateSubmission']],
    ] as const
  ).map(([path, type, fields]): MockHandler => ({
    method: 'PUT',
    path,
    handle: ({ user, params, body }) => {
      const content = contents.find(c => String(c.id) === params.contentId && c.type === type)
      if (!content) return notFound()
      if (!isTeacher(user, content.courseId)) return forbidden()
      const changes = (body ?? {}) as Record<string, unknown>
      const required = fields.filter(f => !['starterCode', 'deadline'].includes(f))
      if (required.some(f => changes[f] === undefined || changes[f] === null || changes[f] === '')) {
        return badRequest(`Required: ${required.join(', ')}`)
      }
      for (const field of fields) (content as unknown as Record<string, unknown>)[field] = changes[field] ?? null
      return ok(detailResponse(content, true))
    },
  })),
  {
    // UpdateContentVisibilityRequest { visibleFrom, visibleUntil? }: a missing
    // visibleUntil keeps the saved one -> ContentVisibilityResponse.
    method: 'PUT',
    path: '/api/contents/:contentId/visibility',
    handle: ({ user, params, body }) => {
      const content = contents.find(c => String(c.id) === params.contentId)
      if (!content) return notFound()
      if (!isTeacher(user, content.courseId)) return forbidden()
      const request = (body ?? {}) as { visibleFrom?: string | null; visibleUntil?: string | null }
      const visibleUntil = 'visibleUntil' in request ? (request.visibleUntil ?? null) : (content.visibleUntil ?? null)
      if (request.visibleFrom && visibleUntil && Date.parse(visibleUntil) <= Date.parse(request.visibleFrom)) {
        return badRequest('visibleUntil must be after visibleFrom')
      }
      content.visibleFrom = request.visibleFrom ?? null
      content.visibleUntil = visibleUntil
      return ok({ contentId: content.id, visibleFrom: content.visibleFrom, visibleUntil: content.visibleUntil })
    },
  },
  {
    // ReorderContentRequest { contentIds }: every item of the module once (204).
    method: 'PUT',
    path: '/api/courses/:courseId/modules/:moduleId/contents/order',
    handle: ({ user, params, body }) => {
      const module = teacherModule(user, params)
      if (isResult(module)) return module
      const ids = ((body as { contentIds?: (number | string)[] } | undefined)?.contentIds ?? []).map(Number)
      const moduleContents = contents.filter(c => c.moduleId === module.id)
      const sameSet =
        ids.length === moduleContents.length &&
        new Set(ids).size === ids.length &&
        moduleContents.every(c => ids.includes(Number(c.id)))
      if (!sameSet) return badRequest('Order must contain every Content in this module exactly once')
      ids.forEach((id, position) => {
        moduleContents.find(c => Number(c.id) === id)!.position = position
      })
      return noContent()
    },
  },
  {
    // Backend (exists): 409 when students have work on it (submissions, quiz attempts,
    // pages marked as done); such content is never deleted.
    method: 'DELETE',
    path: '/api/contents/:contentId',
    handle: ({ user, params }) => {
      const index = contents.findIndex(c => String(c.id) === params.contentId)
      if (index < 0) return notFound()
      const content = contents[index]
      if (!isTeacher(user, content.courseId)) return forbidden()
      const used =
        submissions.some(s => String(s.contentId) === String(content.id)) ||
        quizAttempts.some(a => String(a.quizId) === String(content.id)) ||
        completions.some(c => String(c.contentId) === String(content.id))
      if (used) return conflict('Content with learning records cannot be deleted')
      contents.splice(index, 1)
      return noContent()
    },
  },
  {
    // Backend (exists): SubmitQuizRequest → QuizAttemptResponse, graded at once. A question
    // is correct when the chosen options are exactly its correct options.
    method: 'POST',
    path: '/api/quizzes/:quizId/attempts',
    handle: ({ user, params, body }) => {
      const quiz = contents.find(c => String(c.id) === params.quizId && c.type === 'QUIZ')
      if (!quiz || quiz.type !== 'QUIZ') return notFound()
      if (!canReadCourse(user, quiz.courseId)) return forbidden()
      if (quiz.deadline && Date.now() > Date.parse(quiz.deadline)) return badRequest('The deadline has passed')
      const used = quizAttempts.filter(a => a.quizId === Number(quiz.id) && a.userId === user.userId).length
      if (used >= quiz.maxAttempts) return { status: 409, body: { error: 'No attempts left' } }
      const { answers } = body as { answers?: AnswerRequest[] }
      if (!Array.isArray(answers) || answers.length === 0) return badRequest('Answers are required')
      const questions = quizQuestions.filter(q => q.quizId === Number(quiz.id))
      // Graded per question and stored, like the backend's QuizAnswer. An incomplete
      // fill-in, ordering or matching answer refuses the whole attempt, like QuizGrader.
      const graded = []
      for (const question of questions) {
        const answer = answers.find(a => Number(a.questionId) === question.id)
        const correct = gradeAnswer(question, answer)
        if (typeof correct === 'string') return badRequest(correct)
        graded.push({
          questionId: question.id,
          selectedOptionIds: (answer?.selectedOptionIds ?? []).map(Number),
          textAnswer: answer?.textAnswer ?? null,
          orderedItemIds: answer?.orderedItemIds ?? null,
          matches: answer?.matches ?? null,
          correct,
        })
      }
      const correctCount = graded.filter(answer => answer.correct).length
      const attempt = {
        id: nextId(quizAttempts),
        quizId: Number(quiz.id),
        userId: user.userId,
        submittedAt: new Date().toISOString(),
        correctCount,
        totalQuestions: questions.length,
        answers: graded,
      }
      quizAttempts.push(attempt)
      const { userId: _userId, answers: _answers, ...response } = attempt
      // Like the backend: hints for the questions answered wrong that have one.
      const hints = graded.flatMap(answer => {
        const hint = questions.find(q => q.id === answer.questionId)?.hint
        return !answer.correct && hint ? [{ questionId: answer.questionId, hint }] : []
      })
      return created({ ...response, hints })
    },
  },
  {
    // Backend (exists): QuizAttemptCountResponse, the caller's used attempts.
    method: 'GET',
    path: '/api/quizzes/:quizId/attempts/me/count',
    handle: ({ user, params }) => {
      const quiz = quizFor(params)
      if ('status' in quiz) return quiz
      if (!canReadCourse(user, quiz.courseId)) return forbidden()
      const used = quizAttempts.filter(a => a.quizId === Number(quiz.id) && a.userId === user.userId).length
      return ok({ usedAttempts: used })
    },
  },
  {
    // Backend (exists): teachers of the course, one student's attempts (QuizAttemptResponse[]).
    method: 'GET',
    path: '/api/quizzes/:quizId/attempts/students/:studentUserId',
    handle: ({ user, params }) => {
      const quiz = quizFor(params)
      if ('status' in quiz) return quiz
      if (!isTeacher(user, quiz.courseId)) return forbidden()
      return ok(
        quizAttempts
          .filter(a => a.quizId === Number(quiz.id) && String(a.userId) === params.studentUserId)
          .map(a => attemptResponse(a, false)),
      )
    },
  },
  {
    // Backend (exists): teachers of the course, any attempt with its answers (QuizAttemptDetailResponse).
    method: 'GET',
    path: '/api/quizzes/:quizId/attempts/:attemptId/review',
    handle: ({ user, params }) => {
      const quiz = quizFor(params)
      if ('status' in quiz) return quiz
      if (!isTeacher(user, quiz.courseId)) return forbidden()
      const attempt = quizAttempts.find(a => String(a.id) === params.attemptId && a.quizId === Number(quiz.id))
      return attempt ? ok(attemptResponse(attempt, true)) : notFound()
    },
  },
  {
    // Backend (exists): teachers of the course, the correct answers (QuizAnswerKeyResponse).
    method: 'GET',
    path: '/api/quizzes/:quizId/answer-key',
    handle: ({ user, params }) => {
      const quiz = quizFor(params)
      if ('status' in quiz) return quiz
      if (!isTeacher(user, quiz.courseId)) return forbidden()
      const questions = quizQuestions
        .filter(q => q.quizId === Number(quiz.id))
        .sort((a, b) => a.position - b.position)
        .map(questionKey)
      return ok({ quizId: Number(quiz.id), questions })
    },
  },
  {
    // Backend (exists): the caller's attempts at a quiz.
    method: 'GET',
    path: '/api/quizzes/:quizId/attempts/me',
    handle: ({ user, params }) => {
      const quiz = contents.find(c => String(c.id) === params.quizId && c.type === 'QUIZ')
      if (!quiz || quiz.type !== 'QUIZ') return notFound()
      if (!canReadCourse(user, quiz.courseId)) return forbidden()
      return ok(
        quizAttempts
          .filter(a => a.quizId === Number(quiz.id) && a.userId === user.userId)
          .map(({ userId: _userId, answers: _answers, ...attempt }) => attempt),
      )
    },
  },
  {
    // Backend (exists): one of the caller's own attempts with its answers (QuizAttemptDetailResponse).
    method: 'GET',
    path: '/api/quizzes/:quizId/attempts/:attemptId',
    handle: ({ user, params }) => {
      const quiz = contents.find(c => String(c.id) === params.quizId && c.type === 'QUIZ')
      if (!quiz || quiz.type !== 'QUIZ') return notFound()
      const attempt = quizAttempts.find(a => String(a.id) === params.attemptId && a.quizId === Number(quiz.id))
      // Only your own attempts; someone else's looks like it does not exist.
      if (!attempt || attempt.userId !== user.userId) return notFound()
      const { userId: _userId, ...response } = attempt
      return ok(response)
    },
  },
  {
    // Backend (exists): UpdateQuizRequest { title, maxAttempts? }; a missing maxAttempts keeps it.
    method: 'PUT',
    path: '/api/quizzes/:quizId',
    handle: ({ user, params, body }) => {
      const quiz = quizFor(params)
      if ('status' in quiz) return quiz
      if (!isTeacher(user, quiz.courseId)) return forbidden()
      const { title, maxAttempts } = (body ?? {}) as { title?: string; maxAttempts?: number | null }
      if (!title?.trim() || title.length > 255) return badRequest('Title is required')
      if (maxAttempts !== undefined && maxAttempts !== null && !(Number.isInteger(maxAttempts) && maxAttempts >= 1)) {
        return badRequest('Max attempts must be at least 1')
      }
      quiz.title = title
      if (typeof maxAttempts === 'number') quiz.maxAttempts = maxAttempts
      return ok(detailResponse(quiz, true))
    },
  },
  {
    // Backend (exists): ReplaceQuizQuestionsRequest { questions }: replaces every question;
    // 409 once a student has made an attempt. Answers like GET /api/quizzes/{id}.
    method: 'PUT',
    path: '/api/quizzes/:quizId/questions',
    handle: ({ user, params, body }) => {
      const quiz = quizFor(params)
      if ('status' in quiz) return quiz
      if (!isTeacher(user, quiz.courseId)) return forbidden()
      const { questions } = (body ?? {}) as { questions?: CreateQuizQuestionRequest[] }
      if (!Array.isArray(questions) || questions.length > 100) return badRequest('Questions are required')
      const problem = questions.map(questionRequestProblem).find(Boolean)
      if (problem) return badRequest(problem)
      if (quizAttempts.some(a => a.quizId === Number(quiz.id))) {
        return conflict('Questions cannot be changed after a student has made an attempt')
      }
      for (let i = quizQuestions.length - 1; i >= 0; i--) {
        if (quizQuestions[i].quizId === Number(quiz.id)) quizQuestions.splice(i, 1)
      }
      for (const question of questions) storeQuestion(Number(quiz.id), question)
      return ok(detailResponse(quiz, true))
    },
  },
  {
    // Backend (exists): CreateQuizQuestionRequest.
    method: 'POST',
    path: '/api/quizzes/:quizId/questions',
    handle: ({ user, params, body }) => {
      const quiz = contents.find(c => String(c.id) === params.quizId && c.type === 'QUIZ')
      if (!quiz) return notFound()
      if (!isTeacher(user, quiz.courseId)) return forbidden()
      // Questions are added right after the quiz is created, so this is not locked.
      const request = body as CreateQuizQuestionRequest
      const problem = questionRequestProblem(request)
      if (problem) return badRequest(problem)
      const stored = storeQuestion(Number(quiz.id), request)
      // QuizQuestionResponse: no options, like the backend.
      return created({
        id: stored.id,
        quizId: stored.quizId,
        questionText: stored.questionText,
        position: stored.position,
        hint: stored.hint,
        type: stored.type,
      })
    },
  },
  {
    // Page<SubmissionReviewResponse>: the latest submission per student or group (teachers).
    method: 'GET',
    path: '/api/contents/:contentId/submissions/review',
    handle: ({ user, params, query }) => {
      const content = contents.find(c => String(c.id) === params.contentId)
      if (!content) return notFound()
      if (!isTeacher(user, content.courseId)) return forbidden()
      const page = Number(query.get('page') ?? 0)
      const size = Number(query.get('size') ?? 20)
      if (page < 0 || size < 1 || size > 100) return badRequest('Page must be nonnegative and size between 1 and 100')
      const latest = new Map<string, MockSubmission>()
      for (const s of submissions.filter(s => s.contentId === content.id)) {
        const key = s.groupId !== undefined ? `group-${s.groupId}` : `user-${s.userId}`
        const previous = latest.get(key)
        if (!previous || Date.parse(s.submittedAt) > Date.parse(previous.submittedAt)) latest.set(key, s)
      }
      const all = [...latest.values()].sort((a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt))
      const pageItems = all.slice(page * size, page * size + size)
      const totalPages = Math.ceil(all.length / size)
      return ok({
        content: pageItems.map(s => submissionResponse(s, true)),
        totalElements: all.length,
        totalPages,
        number: page,
        size,
        first: page === 0,
        last: page >= totalPages - 1,
        numberOfElements: pageItems.length,
        empty: pageItems.length === 0,
      })
    },
  },
  {
    method: 'GET',
    path: '/api/submissions/:submissionId/review',
    handle: ({ user, params }) => {
      const submission = teacherSubmission(user, params.submissionId)
      return isResult(submission) ? submission : ok(submissionResponse(submission, true))
    },
  },
  {
    method: 'GET',
    path: '/api/submissions/:submissionId/review/history',
    handle: ({ user, params }) => {
      const submission = teacherSubmission(user, params.submissionId)
      return isResult(submission) ? submission : ok(sequenceOf(submission).map(s => submissionResponse(s, true)))
    },
  },
  {
    method: 'GET',
    path: '/api/submissions/:submissionId/review/file',
    handle: ({ user, params }) => {
      const submission = teacherSubmission(user, params.submissionId)
      if (isResult(submission)) return submission
      return submission.file ? { status: 200, download: submission.file } : notFound()
    },
  },
  {
    // GradeReviewResponse (teachers); 404 when not graded.
    method: 'GET',
    path: '/api/submissions/:submissionId/grade/review',
    handle: ({ user, params }) => {
      const submission = teacherSubmission(user, params.submissionId)
      if (isResult(submission)) return submission
      return submission.grade ? ok(gradeReviewResponse(submission)) : notFound()
    },
  },
  {
    // Backend (exists): SaveGradeRequest { score, feedback, passed, visibleFrom, version };
    // version null creates the grade. `visibleFrom` needs `passed`; a past time releases at once.
    method: 'PUT',
    path: '/api/submissions/:submissionId/grade',
    handle: ({ user, params, body }) => {
      const submission = teacherSubmission(user, params.submissionId)
      if (isResult(submission)) return submission
      const {
        score,
        feedback,
        passed = null,
        visibleFrom = null,
        version,
      } = body as {
        score?: number
        feedback?: string | null
        passed?: boolean | null
        visibleFrom?: string | null
        version?: number | null
      }
      if (
        typeof score !== 'number' ||
        score < 0 ||
        score > 100 ||
        Math.round(score * 100) !== score * 100 ||
        (feedback?.length ?? 0) > 10000 ||
        (passed !== null && typeof passed !== 'boolean') ||
        (visibleFrom !== null && (Number.isNaN(Date.parse(visibleFrom)) || passed === null))
      ) {
        return badRequest('Invalid grade request')
      }
      if (sequenceOf(submission)[0].id !== submission.id)
        return conflict('A newer submission exists; review the latest version')
      const now = new Date().toISOString()
      if (!submission.grade) {
        if (version !== null && version !== undefined) return conflict('Grade does not exist; refresh before creating')
        submission.grade = {
          score,
          feedback: feedback || null,
          passed,
          visibleFrom,
          gradedAt: now,
          releasedAt: null,
          version: 0,
        }
      } else {
        if (submission.grade.releasedAt) return conflict('Released grades cannot be edited')
        if (version !== submission.grade.version) return conflict('Grade changed; refresh before updating')
        submission.grade = {
          ...submission.grade,
          score,
          feedback: feedback || null,
          passed,
          visibleFrom,
          gradedAt: now,
          version: version + 1,
        }
      }
      if (visibleFrom !== null && Date.parse(visibleFrom) <= Date.now()) publishGrade(submission, now)
      return ok(gradeReviewResponse(submission))
    },
  },
  {
    // ReleaseGradeRequest { version }: shows the grade to the student(s).
    method: 'POST',
    path: '/api/submissions/:submissionId/grade/release',
    handle: ({ user, params, body }) => {
      const submission = teacherSubmission(user, params.submissionId)
      if (isResult(submission)) return submission
      if (!submission.grade) return notFound()
      const { version } = body as { version?: number }
      if (version !== submission.grade.version) return conflict('Grade changed; refresh before updating')
      if (submission.grade.releasedAt) return conflict('Grade is already released')
      publishGrade(submission, new Date().toISOString())
      return ok(gradeReviewResponse(submission))
    },
  },
  {
    // Backend (exists): publishes every saved, unpublished grade of the latest submissions that
    // has a passed / not passed decision → { released, undecided }.
    method: 'POST',
    path: '/api/contents/:contentId/grades/release',
    handle: ({ user, params }) => {
      const content = contents.find(c => String(c.id) === params.contentId)
      if (!content) return notFound()
      if (!isTeacher(user, content.courseId)) return forbidden()
      const waiting = latestSubmissions(Number(content.id)).filter(s => s.grade && !s.grade.releasedAt)
      const decided = waiting.filter(s => s.grade!.passed !== null)
      const now = new Date().toISOString()
      decided.forEach(s => publishGrade(s, now))
      return ok({ released: decided.length, undecided: waiting.length - decided.length })
    },
  },
  { method: 'GET', path: '/api/contents/:contentId', handle: contentDetail('STATIC_PAGE') },
  { method: 'GET', path: '/api/quizzes/:contentId', handle: contentDetail('QUIZ') },
  { method: 'GET', path: '/api/assignments/:contentId', handle: contentDetail('ASSIGNMENT') },
  { method: 'GET', path: '/api/coding-exercises/:contentId', handle: contentDetail('CODING_EXERCISE') },
]

/** Every route; grades whose visible date has passed are published first, like a scheduled job would. */
export const handlers: MockHandler[] = routes.map(route => ({
  ...route,
  handle: request => {
    applyVisibleDates()
    return route.handle(request)
  },
}))
