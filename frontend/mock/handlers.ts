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
import type { Content, ContentType } from '../src/data/content/types'
import type { EnrolledCourse } from '../src/data/courses/types'
import type { UpcomingDeadline } from '../src/data/deadlines/types'
import { completions, contents, courses, enrollments, modules, quizQuestions, submissions, users } from './data'
import { created, forbidden, noContent, notFound, ok, type MockHandler, type MockResult, type MockUser } from './types'

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
  return enrollments.some(e =>
    e.userId === user.userId && String(e.courseId) === String(courseId) && e.role === 'TEACHER')
}

const badRequest = (error: string): MockResult => ({ status: 400, body: { error } })
const nextId = (items: { id: number | string }[]) => Math.max(0, ...items.map(item => Number(item.id))) + 1

/** Students only see published grades. */
function publishedGrade(grade: (typeof submissions)[number]['grade']) {
  return grade?.publishedAt ? grade : null
}

/** A submission in the frontend's Submission shape (src/data/submission/types.ts). */
function submissionResponse(submission: (typeof submissions)[number]) {
  const content = contents.find(c => c.id === submission.contentId)!
  return {
    id: submission.id,
    contentId: submission.contentId,
    courseId: content.courseId,
    contentType: content.type,
    submittedBy: submission.userId,
    owner: { type: 'INDIVIDUAL', userId: submission.userId },
    submittedAt: submission.submittedAt,
    late: submission.late,
    status: submission.grade ? 'GRADED' : 'SUBMITTED',
    payload: submission.type === 'GIT_URL'
      ? { type: 'REPOSITORY', repositoryUrl: submission.payload, commitRef: 'main' }
      : { type: 'FILES', files: [] },
    result: null,
  }
}

/** Course content is for enrolled users (and ADMIN). */
function canReadCourse(user: MockUser, courseId: string | number) {
  return user.access === 'ADMIN'
    || coursesOf(user.userId).some(course => String(course.id) === String(courseId))
}

// ContentSummaryResponse sends "Assignment" for assignments (not "ASSIGNMENT").
const BACKEND_TYPE: Record<ContentType, string> = {
  STATIC_PAGE: 'STATIC_PAGE',
  QUIZ: 'QUIZ',
  ASSIGNMENT: 'Assignment',
  CODING_EXERCISE: 'CODING_EXERCISE',
}

/** The content as the backend's detail endpoint for its type returns it (no `type`). */
function detailResponse(content: Content & { courseId: number }) {
  const base = { id: content.id, moduleId: content.moduleId, title: content.title, position: content.position }
  switch (content.type) {
    case 'STATIC_PAGE':
      return { ...base, courseId: content.courseId, body: content.body }
    case 'QUIZ':
      return { ...base, courseId: content.courseId, maxAttempts: content.maxAttempts }
    case 'ASSIGNMENT':
      return { ...base, instructions: content.instructions, deadline: content.deadline,
        allowLateSubmission: content.allowLateSubmission, mandatory: content.mandatory }
    case 'CODING_EXERCISE':
      return { ...base, instructions: content.instructions, starterRepositoryUrl: content.starterRepositoryUrl,
        deadline: content.deadline, allowLateSubmission: content.allowLateSubmission }
  }
}

/** Handler for GET /api/<...>/:contentId for one content type. */
function contentDetail(type: ContentType): MockHandler['handle'] {
  return ({ user, params }): MockResult => {
    const content = contents.find(c => String(c.id) === params.contentId && c.type === type)
    if (!content) return notFound()
    if (!canReadCourse(user, content.courseId)) return forbidden()
    return ok(detailResponse(content))
  }
}

export const handlers: MockHandler[] = [
  {
    method: 'GET',
    path: '/api/users/me',
    handle: ({ user }) => ok(user),
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
      const courseIds = new Set(coursesOf(Number(params.userId)).map(course => course.id))
      const now = Date.now()
      const result: UpcomingDeadline[] = contents
        .flatMap(content =>
          (content.type === 'ASSIGNMENT' || content.type === 'CODING_EXERCISE')
            && content.deadline && Date.parse(content.deadline) > now
            && courseIds.has(content.courseId)
            ? [{
              contentId: content.id,
              type: content.type,
              title: content.title,
              deadline: content.deadline,
              allowLateSubmission: content.allowLateSubmission,
              ...(content.type === 'ASSIGNMENT' ? { mandatory: content.mandatory } : {}),
              courseId: content.courseId,
              courseTitle: courses.find(c => c.id === content.courseId)?.title ?? '',
              moduleId: content.moduleId,
            }]
            : [])
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
    // ModuleResponse: id, courseId, title, position.
    method: 'GET',
    path: '/api/courses/:courseId/modules',
    handle: ({ user, params }) => {
      if (!courses.some(c => String(c.id) === params.courseId)) return notFound()
      if (!canReadCourse(user, params.courseId)) return forbidden()
      return ok(modules
        .filter(m => String(m.courseId) === params.courseId)
        .sort((a, b) => a.position - b.position)
        .map(({ id, courseId, title, position }) => ({ id, courseId, title, position })))
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
      return ok(contents
        .filter(c => c.moduleId === module.id)
        .sort((a, b) => a.position - b.position)
        .map(c => ({ id: c.id, type: BACKEND_TYPE[c.type], title: c.title, position: c.position })))
    },
  },
  {
    // Proposed endpoint (src/data/content/README.md): CompletedContent[] for the caller.
    method: 'GET',
    path: '/api/courses/:courseId/completions/me',
    handle: ({ user, params }) => {
      if (!courses.some(c => String(c.id) === params.courseId)) return notFound()
      if (!canReadCourse(user, params.courseId)) return forbidden()
      const inCourse = new Set(contents.filter(c => String(c.courseId) === params.courseId).map(c => c.id))
      return ok(completions
        .filter(row => row.userId === user.userId && inCourse.has(row.contentId))
        .map(({ contentId, completedAt }) => ({ contentId, completedAt })))
    },
  },
  {
    // SubmissionResponse[]: the caller's own submissions for one content item.
    method: 'GET',
    path: '/api/contents/:contentId/submissions/me',
    handle: ({ user, params }) => {
      const content = contents.find(c => String(c.id) === params.contentId)
      if (!content) return notFound()
      if (!canReadCourse(user, content.courseId)) return forbidden()
      return ok(submissions
        .filter(s => s.userId === user.userId && s.contentId === content.id)
        .map(({ id, contentId, type, payload, submittedAt, late }) =>
          ({ id, contentId, type, payload, submittedAt, late })))
    },
  },
  {
    // Proposed endpoint (src/data/submission): SubmissionResult with the published grade.
    method: 'GET',
    path: '/api/submissions/:submissionId/result',
    handle: ({ user, params }) => {
      const submission = submissions.find(s => String(s.id) === params.submissionId)
      if (!submission) return notFound()
      if (submission.userId !== user.userId && user.access !== 'ADMIN') return forbidden()
      return ok({
        submissionId: submission.id,
        status: submission.grade ? 'GRADED' : 'SUBMITTED',
        grade: publishedGrade(submission.grade),
        testResults: null,
        errorMessage: null,
      })
    },
  },
  {
    // Proposed endpoint (src/data/grades/README.md): the latest submission per
    // assignment / coding exercise, with its published grade or null.
    method: 'GET',
    path: '/api/users/me/grades',
    handle: ({ user }) => {
      const latest = new Map<number, (typeof submissions)[number]>()
      for (const submission of submissions.filter(s => s.userId === user.userId)) {
        const previous = latest.get(submission.contentId)
        if (!previous || Date.parse(submission.submittedAt) > Date.parse(previous.submittedAt)) {
          latest.set(submission.contentId, submission)
        }
      }
      return ok([...latest.values()].flatMap(submission => {
        const content = contents.find(c => c.id === submission.contentId)
        if (!content || (content.type !== 'ASSIGNMENT' && content.type !== 'CODING_EXERCISE')) return []
        return [{
          contentId: content.id,
          type: content.type,
          title: content.title,
          courseId: content.courseId,
          courseTitle: courses.find(c => c.id === content.courseId)?.title ?? '',
          moduleId: content.moduleId,
          submissionId: submission.id,
          submittedAt: submission.submittedAt,
          late: submission.late,
          grade: publishedGrade(submission.grade),
        }]
      }))
    },
  },
  {
    // Proposed endpoint (src/data/module/README.md). The mock calls a module completed
    // when the user has completed all its assignments, coding exercises and quizzes
    // (and it has at least one). The backend decides the real rule.
    method: 'GET',
    path: '/api/users/:userId/courses/:courseId/modules/progress',
    handle: ({ user, params }) => {
      if (!canReadUser(user, params.userId)) return forbidden()
      if (!canReadCourse(user, params.courseId)) return forbidden()
      const done = new Set(completions.filter(c => String(c.userId) === params.userId).map(c => c.contentId))
      return ok(modules
        .filter(m => String(m.courseId) === params.courseId)
        .map(module => {
          const tasks = contents.filter(c => c.moduleId === module.id && c.type !== 'STATIC_PAGE')
          return {
            moduleId: module.id,
            completed: tasks.length > 0 && tasks.every(task => done.has(Number(task.id))),
            accessible: true,
          }
        }))
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
      const input = body as { title: string, description: string, term: 'SPRING' | 'AUTUMN', year: number }
      if (!input?.title?.trim() || !input.description?.trim()) return badRequest('Title and description are required')
      const course = { id: nextId(courses), title: input.title, description: input.description, term: input.term, year: input.year }
      courses.push(course)
      enrollments.push({ userId: user.userId, courseId: course.id, role: 'TEACHER' })
      return created(course)
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
      if (!isTeacher(user, params.courseId) && user.access !== 'ADMIN') return forbidden()
      return ok(enrollments
        .filter(e => String(e.courseId) === params.courseId)
        .flatMap(e => {
          const member = users.find(u => u.userId === e.userId)
          return member
            ? [{ userId: e.userId, role: e.role, firstName: member.firstName, lastName: member.lastName, email: member.email }]
            : []
        }))
    },
  },
  {
    method: 'POST',
    path: '/api/courses/:courseId/enrollments',
    handle: ({ user, params, body }) => {
      if (!isTeacher(user, params.courseId)) return forbidden()
      const { userId, role = 'STUDENT' } = body as { userId: number, role?: 'STUDENT' | 'TEACHER' }
      if (!users.some(u => u.userId === Number(userId))) return notFound()
      if (enrollments.some(e => e.userId === Number(userId) && String(e.courseId) === params.courseId)) {
        return { status: 409, body: { error: 'Already enrolled' } }
      }
      enrollments.push({ userId: Number(userId), courseId: Number(params.courseId), role })
      return noContent()
    },
  },
  {
    method: 'PATCH',
    path: '/api/courses/:courseId/enrollments/:userId',
    handle: ({ user, params, body }) => {
      if (!isTeacher(user, params.courseId)) return forbidden()
      const enrollment = enrollments.find(e => String(e.userId) === params.userId && String(e.courseId) === params.courseId)
      if (!enrollment) return notFound()
      enrollment.role = (body as { role: 'STUDENT' | 'TEACHER' }).role
      return noContent()
    },
  },
  {
    method: 'DELETE',
    path: '/api/courses/:courseId/enrollments/:userId',
    handle: ({ user, params }) => {
      if (!isTeacher(user, params.courseId)) return forbidden()
      const index = enrollments.findIndex(e => String(e.userId) === params.userId && String(e.courseId) === params.courseId)
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
      const { title, position } = body as { title: string, position: number }
      if (!title?.trim()) return badRequest('Title is required')
      const module = { id: nextId(modules), courseId: Number(params.courseId), title, position }
      modules.push(module)
      return created(module)
    },
  },
  ...([
    ['pages', 'STATIC_PAGE'],
    ['assignments', 'ASSIGNMENT'],
    ['coding-exercises', 'CODING_EXERCISE'],
    ['quizzes', 'QUIZ'],
  ] as const).map(([slug, type]): MockHandler => ({
    // Backend (exists): one create endpoint per type; answers like its GET endpoint.
    method: 'POST',
    path: `/api/courses/:courseId/modules/:moduleId/contents/${slug}`,
    handle: ({ user, params, body }) => {
      const module = modules.find(m => String(m.id) === params.moduleId && String(m.courseId) === params.courseId)
      if (!module) return notFound()
      if (!isTeacher(user, params.courseId)) return forbidden()
      const fields = body as Record<string, unknown>
      if (!String(fields.title ?? '').trim()) return badRequest('Title is required')
      const content = {
        ...fields,
        id: nextId(contents),
        courseId: module.courseId,
        moduleId: module.id,
        type,
        // Optional fields the backend fills in.
        ...(type === 'ASSIGNMENT' || type === 'CODING_EXERCISE' ? { deadline: fields.deadline ?? null } : {}),
      } as (typeof contents)[number]
      contents.push(content)
      return created(detailResponse(content))
    },
  })),
  {
    // Backend (exists): CreateQuizQuestionRequest.
    method: 'POST',
    path: '/api/quizzes/:quizId/questions',
    handle: ({ user, params, body }) => {
      const quiz = contents.find(c => String(c.id) === params.quizId && c.type === 'QUIZ')
      if (!quiz) return notFound()
      if (!isTeacher(user, quiz.courseId)) return forbidden()
      const question = body as { questionText: string, options: unknown[] }
      quizQuestions.push({ quizId: Number(quiz.id), questionText: question.questionText, options: question.options })
      return created({ id: quizQuestions.length, ...question })
    },
  },
  {
    // Proposed (src/data/submission): every student's latest submission, for teachers.
    method: 'GET',
    path: '/api/contents/:contentId/submissions',
    handle: ({ user, params }) => {
      const content = contents.find(c => String(c.id) === params.contentId)
      if (!content) return notFound()
      if (!isTeacher(user, content.courseId)) return forbidden()
      const latest = new Map<number, (typeof submissions)[number]>()
      for (const s of submissions.filter(s => s.contentId === content.id)) {
        const previous = latest.get(s.userId)
        if (!previous || Date.parse(s.submittedAt) > Date.parse(previous.submittedAt)) latest.set(s.userId, s)
      }
      return ok([...latest.values()].map(s => {
        const student = users.find(u => u.userId === s.userId)!
        return {
          id: s.id, contentId: s.contentId, type: s.type, payload: s.payload, submittedAt: s.submittedAt, late: s.late,
          student: { userId: student.userId, firstName: student.firstName, lastName: student.lastName, email: student.email },
          grade: s.grade,
        }
      }))
    },
  },
  {
    // Proposed: save a grade (not visible to the student until published).
    method: 'PUT',
    path: '/api/submissions/:submissionId/grade',
    handle: ({ user, params, body }) => {
      const submission = submissions.find(s => String(s.id) === params.submissionId)
      if (!submission) return notFound()
      const content = contents.find(c => c.id === submission.contentId)!
      if (!isTeacher(user, content.courseId)) return forbidden()
      const { grade, feedback } = body as { grade: string, feedback?: string }
      if (!grade?.trim()) return badRequest('Grade is required')
      submission.grade = { value: grade, feedback: feedback ?? null, publishedAt: null }
      return ok(submissionResponse(submission))
    },
  },
  {
    // Proposed: publish the saved grade to the student.
    method: 'POST',
    path: '/api/submissions/:submissionId/grade/publish',
    handle: ({ user, params }) => {
      const submission = submissions.find(s => String(s.id) === params.submissionId)
      if (!submission?.grade) return notFound()
      const content = contents.find(c => c.id === submission.contentId)!
      if (!isTeacher(user, content.courseId)) return forbidden()
      submission.grade.publishedAt = new Date().toISOString()
      // A graded submission also counts as completed.
      if (!completions.some(c => c.userId === submission.userId && c.contentId === submission.contentId)) {
        completions.push({ userId: submission.userId, contentId: submission.contentId, completedAt: submission.submittedAt })
      }
      return ok(submissionResponse(submission))
    },
  },
  { method: 'GET', path: '/api/contents/:contentId', handle: contentDetail('STATIC_PAGE') },
  { method: 'GET', path: '/api/quizzes/:contentId', handle: contentDetail('QUIZ') },
  { method: 'GET', path: '/api/assignments/:contentId', handle: contentDetail('ASSIGNMENT') },
  { method: 'GET', path: '/api/coding-exercises/:contentId', handle: contentDetail('CODING_EXERCISE') },
]
