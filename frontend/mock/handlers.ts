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
import type { Content, ContentType } from '../src/features/content/types'
import type { EnrolledCourse } from '../src/features/courses/types'
import type { UpcomingDeadline } from '../src/features/deadlines/types'
import { canRequire, checkAvailabilityDates, isAccessible } from '../src/features/modules/prerequisites'
import { completions, contents, courses, enrollments, groups, modules, quizQuestions, submissions, users } from './data'
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

/** The submission is the user's own, or their group's (proposed group assignments). */
function isOwnSubmission(user: MockUser, submission: (typeof submissions)[number]) {
  if (submission.userId === user.userId) return true
  const group = groups.find(g => g.id === submission.groupId)
  return group?.memberIds.includes(user.userId) ?? false
}

/** The members' names of a group (proposed in GroupDTO and SubmissionGroup). */
function groupMembers(group: (typeof groups)[number]) {
  return group.memberIds.flatMap(userId => {
    const member = users.find(u => u.userId === userId)
    return member ? [{ userId, firstName: member.firstName, lastName: member.lastName }] : []
  })
}

/** A group as in SubmissionGroup; null for no group. */
function groupResponse(groupId: number | undefined) {
  const group = groups.find(g => g.id === groupId)
  if (!group) return null
  return { id: group.id, name: group.name ?? `Group ${group.groupNumber}`, members: groupMembers(group) }
}

/** Students only see published grades. */
function publishedGrade(grade: (typeof submissions)[number]['grade']) {
  return grade?.publishedAt ? grade : null
}

/** A submission in the frontend's Submission shape (src/features/assignments/types.ts). */
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
/**
 * A quiz's questions as in QuizDetailResponse. For teachers of the course also the
 * proposed `hint` and `correct` fields (data/content/README.md).
 */
function quizQuestionsResponse(quizId: string | number, forTeacher: boolean) {
  return quizQuestions
    .filter(q => String(q.quizId) === String(quizId))
    .sort((a, b) => a.position - b.position)
    .map(q => ({
      id: q.id,
      questionText: q.questionText,
      position: q.position,
      ...(forTeacher ? { hint: q.hint } : {}),
      options: [...q.options]
        .sort((a, b) => a.position - b.position)
        .map(o => ({ id: o.id, optionText: o.optionText, position: o.position, ...(forTeacher ? { correct: o.correct } : {}) })),
    }))
}

function detailResponse(content: Content & { courseId: number }, forTeacher = false) {
  const base = { id: content.id, moduleId: content.moduleId, title: content.title, position: content.position }
  switch (content.type) {
    case 'STATIC_PAGE':
      return { ...base, courseId: content.courseId, body: content.body }
    case 'QUIZ':
      return { ...base, courseId: content.courseId, maxAttempts: content.maxAttempts,
        questions: quizQuestionsResponse(content.id, forTeacher) }
    case 'ASSIGNMENT':
      return { ...base, instructions: content.instructions, deadline: content.deadline,
        allowLateSubmission: content.allowLateSubmission, mandatory: content.mandatory,
        groupAssignment: content.groupAssignment ?? false, acceptedFileTypes: content.acceptedFileTypes ?? [] }
    case 'CODING_EXERCISE':
      return { ...base, instructions: content.instructions, starterRepositoryUrl: content.starterRepositoryUrl,
        starterCode: content.starterCode ?? null, deadline: content.deadline, allowLateSubmission: content.allowLateSubmission }
  }
}

/** Handler for GET /api/<...>/:contentId for one content type. */
function contentDetail(type: ContentType): MockHandler['handle'] {
  return ({ user, params }): MockResult => {
    const content = contents.find(c => String(c.id) === params.contentId && c.type === type)
    if (!content) return notFound()
    if (!canReadCourse(user, content.courseId)) return forbidden()
    const teacher = isTeacher(user, content.courseId)
    if (!teacher && content.active === false) return notFound()
    return ok(detailResponse(content, teacher))
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
            && courseIds.has(content.courseId) && content.active !== false
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
    // ModuleResponse: id, courseId, title, position, plus the proposed
    // prerequisiteModuleIds (data/modules/README.md).
    method: 'GET',
    path: '/api/courses/:courseId/modules',
    handle: ({ user, params }) => {
      if (!courses.some(c => String(c.id) === params.courseId)) return notFound()
      if (!canReadCourse(user, params.courseId)) return forbidden()
      return ok(modules
        .filter(m => String(m.courseId) === params.courseId)
        .sort((a, b) => a.position - b.position)
        .map(({ id, courseId, title, position, prerequisiteModuleIds, availableFrom, openToAllFrom }) => ({
          id, courseId, title, position, prerequisiteModuleIds: prerequisiteModuleIds ?? [],
          availableFrom: availableFrom ?? null, openToAllFrom: openToAllFrom ?? null,
        })))
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
      // Students do not see inactive content; teachers of the course do.
      const teacher = isTeacher(user, params.courseId)
      return ok(contents
        .filter(c => c.moduleId === module.id && (teacher || c.active !== false))
        .sort((a, b) => a.position - b.position)
        .map(c => ({ id: c.id, type: BACKEND_TYPE[c.type], title: c.title, position: c.position,
          requiredForCompletion: c.requiredForCompletion ?? false, active: c.active ?? true })))
    },
  },
  {
    // Proposed endpoint (src/features/content/README.md): CompletedContent[] for the caller.
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
    // SubmissionResponse[]: the caller's own (and their group's) submissions for one content item.
    method: 'GET',
    path: '/api/contents/:contentId/submissions/me',
    handle: ({ user, params }) => {
      const content = contents.find(c => String(c.id) === params.contentId)
      if (!content) return notFound()
      if (!canReadCourse(user, content.courseId)) return forbidden()
      return ok(submissions
        .filter(s => s.contentId === content.id && isOwnSubmission(user, s))
        .map(({ id, contentId, type, payload, submittedAt, late, groupId }) =>
          ({ id, contentId, type, payload, submittedAt, late, group: groupResponse(groupId) })))
    },
  },
  {
    // CreateSubmissionRequest { type, payload } → SubmissionResponse (201), like the backend.
    method: 'POST',
    path: '/api/contents/:contentId/submissions',
    handle: ({ user, params, body }) => {
      const content = contents.find(c => String(c.id) === params.contentId)
      if (!content) return notFound()
      if (content.type !== 'ASSIGNMENT' && content.type !== 'CODING_EXERCISE') return badRequest('Cannot submit to this content')
      if (!canReadCourse(user, content.courseId)) return forbidden()
      const { type, payload } = body as { type?: 'TEXT' | 'GIT_URL', payload?: string }
      if ((type !== 'TEXT' && type !== 'GIT_URL') || !payload?.trim()) return badRequest('Type and payload are required')
      const late = content.deadline !== null && Date.now() > Date.parse(content.deadline)
      if (late && !content.allowLateSubmission) return badRequest('The deadline has passed')
      // A group assignment is handed in for the user's group in the course.
      let groupId: number | undefined
      if (content.type === 'ASSIGNMENT' && content.groupAssignment) {
        groupId = groups.find(g => g.courseId === content.courseId && g.memberIds.includes(user.userId))?.id
        if (groupId === undefined) return badRequest('You are not in a group in this course')
      }
      const submission = {
        id: nextId(submissions), userId: user.userId, contentId: Number(content.id), groupId, type,
        payload: payload.trim(), submittedAt: new Date().toISOString(), late, grade: null,
      }
      submissions.push(submission)
      const { id, contentId, submittedAt } = submission
      return {
        status: 201,
        body: { id, contentId, type, payload: submission.payload, submittedAt, late, group: groupResponse(groupId) },
      }
    },
  },
  {
    // Proposed endpoint (src/features/assignments): SubmissionResult with the published grade.
    method: 'GET',
    path: '/api/submissions/:submissionId/result',
    handle: ({ user, params }) => {
      const submission = submissions.find(s => String(s.id) === params.submissionId)
      if (!submission) return notFound()
      if (!isOwnSubmission(user, submission) && user.access !== 'ADMIN') return forbidden()
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
    // Proposed endpoint (src/features/grades/README.md): the latest submission per
    // assignment / coding exercise, with its published grade or null.
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
    // Proposed endpoint (src/features/modules/README.md). The mock calls a module completed
    // when the user has completed all its content marked requiredForCompletion; with
    // none marked, all its assignments, coding exercises and quizzes (and it has at
    // least one). The backend decides the real rule.
    method: 'GET',
    path: '/api/users/:userId/courses/:courseId/modules/progress',
    handle: ({ user, params }) => {
      if (!canReadUser(user, params.userId)) return forbidden()
      if (!canReadCourse(user, params.courseId)) return forbidden()
      const done = new Set(completions.filter(c => String(c.userId) === params.userId).map(c => c.contentId))
      const courseModules = modules.filter(m => String(m.courseId) === params.courseId)
      const completedIds = new Set(courseModules
        .filter(module => {
          const inModule = contents.filter(c => c.moduleId === module.id && c.active !== false)
          const required = inModule.filter(c => c.requiredForCompletion)
          const tasks = required.length > 0 ? required : inModule.filter(c => c.type !== 'STATIC_PAGE')
          return tasks.length > 0 && tasks.every(task => done.has(Number(task.id)))
        })
        .map(module => String(module.id)))
      return ok(courseModules.map(module => ({
        moduleId: module.id,
        completed: completedIds.has(String(module.id)),
        // From its start date, when its prerequisites are completed or it is open to all.
        accessible: isAccessible(module, completedIds, new Date()),
      })))
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
    // Proposed (data/courses/README.md): UpdateCourseInput.changes -> CourseResponse.
    // Only teachers of the course may change it.
    method: 'PATCH',
    path: '/api/courses/:courseId',
    handle: ({ user, params, body }) => {
      const course = courses.find(c => String(c.id) === params.courseId)
      if (!course) return notFound()
      if (!isTeacher(user, course.id)) return forbidden()
      const changes = body as Partial<{ title: string, description: string, term: 'SPRING' | 'AUTUMN', year: number }>
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
      // Every member of the course may see who else is in it (the People page).
      if (!canReadCourse(user, params.courseId)) return forbidden()
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
    // GroupDTO[] (backend), with the proposed name and members.
    method: 'GET',
    path: '/api/courses/:courseId/groups',
    handle: ({ user, params }) => {
      if (!canReadCourse(user, params.courseId)) return forbidden()
      return ok(groups
        .filter(g => String(g.courseId) === params.courseId)
        .map(group => ({
          id: group.id, groupNumber: group.groupNumber, memberIds: group.memberIds,
          name: group.name ?? null, members: groupMembers(group),
        })))
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
      const module = { id: nextId(modules), courseId: Number(params.courseId), title, position, prerequisiteModuleIds: [] }
      modules.push(module)
      return created(module)
    },
  },
  {
    // Proposed (data/modules/README.md): title, position and/or prerequisiteModuleIds.
    // Prerequisites must be other modules in the same course, without cycles.
    method: 'PATCH',
    path: '/api/courses/:courseId/modules/:moduleId',
    handle: ({ user, params, body }) => {
      const module = modules.find(m => String(m.id) === params.moduleId && String(m.courseId) === params.courseId)
      if (!module) return notFound()
      if (!isTeacher(user, params.courseId)) return forbidden()
      const changes = body as Partial<{
        title: string, position: number, prerequisiteModuleIds: (string | number)[],
        availableFrom: string | null, openToAllFrom: string | null,
      }>
      if (changes.title !== undefined && !changes.title.trim()) return badRequest('Title is required')
      const availableFrom = changes.availableFrom !== undefined ? changes.availableFrom : module.availableFrom ?? null
      const openToAllFrom = changes.openToAllFrom !== undefined ? changes.openToAllFrom : module.openToAllFrom ?? null
      const dateProblem = checkAvailabilityDates(availableFrom, openToAllFrom)
      if (dateProblem) return badRequest(dateProblem)
      module.availableFrom = availableFrom
      module.openToAllFrom = openToAllFrom
      if (changes.prerequisiteModuleIds !== undefined) {
        const courseModules = modules.filter(m => m.courseId === module.courseId)
        const ids = changes.prerequisiteModuleIds.map(Number)
        if (!ids.every(id => courseModules.some(m => m.id === id))) return badRequest('Prerequisites must be in the same course')
        if (!ids.every(id => canRequire(courseModules, module.id, id))) return badRequest('Prerequisites cannot form a cycle')
        module.prerequisiteModuleIds = ids
      }
      if (changes.title !== undefined) module.title = changes.title
      if (changes.position !== undefined) module.position = changes.position
      return ok({ ...module, prerequisiteModuleIds: module.prerequisiteModuleIds ?? [],
        availableFrom: module.availableFrom ?? null, openToAllFrom: module.openToAllFrom ?? null })
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
    // Proposed (data/content/README.md): { moduleId, position } -> the moved content.
    // The content may only move to another module in the same course.
    method: 'PATCH',
    path: '/api/content/:contentId/move',
    handle: ({ user, params, body }) => {
      const content = contents.find(c => String(c.id) === params.contentId)
      if (!content) return notFound()
      if (!isTeacher(user, content.courseId)) return forbidden()
      const { moduleId, position } = body as { moduleId: string | number, position: number }
      const target = modules.find(m => String(m.id) === String(moduleId) && m.courseId === content.courseId)
      if (!target) return badRequest('The module is not in this course')
      content.moduleId = target.id
      content.position = position
      return ok(detailResponse(content))
    },
  },
  {
    // Proposed (data/content/README.md): title, position and/or body (pages only).
    method: 'PATCH',
    path: '/api/content/:contentId',
    handle: ({ user, params, body }) => {
      const content = contents.find(c => String(c.id) === params.contentId)
      if (!content) return notFound()
      if (!isTeacher(user, content.courseId)) return forbidden()
      const changes = body as Partial<{
        title: string, position: number, body: string, requiredForCompletion: boolean, active: boolean,
      }>
      if (changes.title !== undefined && !changes.title.trim()) return badRequest('Title is required')
      if (changes.title !== undefined) content.title = changes.title
      if (changes.position !== undefined) content.position = changes.position
      if (changes.requiredForCompletion !== undefined) content.requiredForCompletion = changes.requiredForCompletion
      if (changes.active !== undefined) content.active = changes.active
      if (changes.body !== undefined && content.type === 'STATIC_PAGE') content.body = changes.body
      return ok(detailResponse(content))
    },
  },
  {
    // Proposed (data/content/README.md).
    method: 'DELETE',
    path: '/api/content/:contentId',
    handle: ({ user, params }) => {
      const index = contents.findIndex(c => String(c.id) === params.contentId)
      if (index < 0) return notFound()
      if (!isTeacher(user, contents[index].courseId)) return forbidden()
      contents.splice(index, 1)
      return noContent()
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
      const question = body as {
        questionText: string, position: number, hint: string | null,
        options: { optionText: string, correct: boolean, position: number }[],
      }
      const optionIds = quizQuestions.flatMap(q => q.options.map(o => o.id))
      const firstOptionId = Math.max(0, ...optionIds) + 1
      const stored = {
        id: nextId(quizQuestions),
        quizId: Number(quiz.id),
        questionText: question.questionText,
        position: question.position,
        hint: question.hint,
        options: question.options.map((option, index) => ({ ...option, id: firstOptionId + index })),
      }
      quizQuestions.push(stored)
      // QuizQuestionResponse: no options, like the backend.
      return created({ id: stored.id, quizId: stored.quizId, questionText: stored.questionText,
        position: stored.position, hint: stored.hint })
    },
  },
  {
    // Proposed (src/features/assignments): every student's latest submission, for teachers.
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
