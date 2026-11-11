// Test data for `npm run dev:mock`. Edit freely: the mock backend reloads this file on
// the next request, so you only need to refresh the browser.
//
// The types come from src/data, so TypeScript tells you when the data no longer
// matches what the frontend expects from the backend.
import type { Content } from '../src/data/content/types'
import type { Course, CourseRole } from '../src/data/courses/types'
import type { CourseModule } from '../src/data/module/types'
import type { MockUser } from './types'

/** Delay on every API answer, so loading states are visible. 0 turns it off. */
export const latencyMs = 300

/** The users on the mock login page (instead of Keycloak). */
export const users: MockUser[] = [
  { userId: 1, email: 'student@example.com', firstName: 'Siri', lastName: 'Student', access: 'STANDARD' },
  { userId: 2, email: 'teacher@example.com', firstName: 'Tore', lastName: 'Teacher', access: 'COURSE_MANAGER' },
  { userId: 3, email: 'admin@example.com', firstName: 'Ada', lastName: 'Admin', access: 'ADMIN' },
  { userId: 4, email: 'new@example.com', firstName: 'Nina', lastName: 'New', access: 'STANDARD' },
]

export const courses: Course[] = [
  {
    id: 1,
    title: 'DAT250 Advanced Software Technologies',
    description: 'Web applications, REST APIs, messaging, containers and cloud deployment.',
    term: 'AUTUMN',
    year: 2026,
  },
  {
    id: 2,
    title: 'DAT251 Modern Software Development',
    description: 'Agile methods, testing, continuous integration and delivery.',
    term: 'AUTUMN',
    year: 2026,
  },
  {
    id: 3,
    title: 'DAT109 Programming Paradigms',
    description: 'Functional, logic and object-oriented programming.',
    term: 'SPRING',
    year: 2026,
  },
  {
    id: 4,
    title: 'DAT100 Introduction to Programming',
    description: 'Basic programming in Java.',
    term: 'AUTUMN',
    year: 2025,
  },
]

export const enrollments: { userId: number, courseId: number, role: CourseRole }[] = [
  { userId: 1, courseId: 1, role: 'STUDENT' },
  { userId: 1, courseId: 2, role: 'STUDENT' },
  { userId: 1, courseId: 3, role: 'STUDENT' },
  { userId: 1, courseId: 4, role: 'STUDENT' },
  { userId: 2, courseId: 1, role: 'TEACHER' },
  { userId: 2, courseId: 4, role: 'TEACHER' },
  { userId: 3, courseId: 2, role: 'TEACHER' },
  // User 4 is not enrolled anywhere (empty overview).
]

/** ISO time `days` from now at hh:mm local time, so the deadlines stay upcoming. */
function inDays(days: number, hours = 23, minutes = 59): string {
  const date = new Date()
  date.setDate(date.getDate() + days)
  date.setHours(hours, minutes, 0, 0)
  return date.toISOString()
}

export const modules: CourseModule[] = [
  { id: 1, courseId: 1, title: 'Introduction and REST', position: 0 },
  { id: 2, courseId: 1, title: 'Containers', position: 1 },
  { id: 3, courseId: 1, title: 'Messaging', position: 2 },
  { id: 4, courseId: 1, title: 'Cloud deployment', position: 3 },
  { id: 5, courseId: 2, title: 'Agile and Scrum', position: 0 },
  { id: 6, courseId: 2, title: 'Testing', position: 1 },
  { id: 7, courseId: 3, title: 'Haskell', position: 0 },
  { id: 8, courseId: 4, title: 'Getting started', position: 0 },
]

/**
 * What each user has completed: assignments and coding exercises they submitted,
 * quizzes they submitted an attempt for. Add rows here to see the "Completed" lists.
 */
export const completions: { userId: number, contentId: number, completedAt: string }[] = [
  { userId: 1, contentId: 13, completedAt: inDays(-5, 14, 30) },
  { userId: 1, contentId: 104, completedAt: inDays(-4, 20, 0) },
  { userId: 1, contentId: 22, completedAt: inDays(-1, 10, 15) },
  { userId: 1, contentId: 61, completedAt: inDays(-2, 9, 0) },
  { userId: 1, contentId: 102, completedAt: inDays(-1, 18, 45) },
  { userId: 1, contentId: 201, completedAt: inDays(-2, 15, 0) },
]

/**
 * Submissions to assignments and coding exercises. `grade` is what the teacher has
 * published (null: not graded yet). Keep `completions` above in step with this list.
 */
export const submissions: {
  id: number
  userId: number
  contentId: number
  type: 'TEXT' | 'GIT_URL'
  payload: string
  submittedAt: string
  late: boolean
  grade: { value: string, feedback: string | null, publishedAt: string } | null
}[] = [
  {
    id: 1, userId: 1, contentId: 104, type: 'TEXT', late: false,
    payload: 'My answer to the old assignment.', submittedAt: inDays(-4, 20, 0),
    grade: {
      value: 'Passed',
      feedback: 'Good work! The explanation of the status codes is clear.\n'
        + 'Remember to test the error cases too.',
      publishedAt: inDays(-1, 9, 30),
    },
  },
  {
    id: 2, userId: 1, contentId: 102, type: 'GIT_URL', late: false,
    payload: 'https://github.com/example/siri-docker-compose', submittedAt: inDays(-1, 18, 45),
    grade: null,
  },
  {
    id: 3, userId: 1, contentId: 201, type: 'TEXT', late: false,
    payload: 'Our first sprint went well.', submittedAt: inDays(-2, 15, 0),
    grade: { value: 'B', feedback: null, publishedAt: inDays(0, 8, 0) },
  },
]

/**
 * All content, with its courseId so the handlers can check the course. The handlers
 * return each type in the backend's response shape. Assignments and coding exercises
 * with a deadline also show up as upcoming deadlines on the overview.
 */
export const contents: (Content & { courseId: number })[] = [
  // DAT250, module 1
  {
    id: 11, courseId: 1, moduleId: 1, position: 0, type: 'STATIC_PAGE', title: 'Welcome to DAT250',
    body: 'Welcome!\n\nThis course is about building and running modern web applications.\n'
      + 'Start with the page about REST, then take the quiz.',
  },
  {
    id: 12, courseId: 1, moduleId: 1, position: 1, type: 'STATIC_PAGE', title: 'What is REST?',
    body: 'REST is an architectural style for APIs.\n\nResources are identified by URLs, and the '
      + 'HTTP methods (GET, POST, PUT, PATCH, DELETE) say what to do with them.',
  },
  { id: 13, courseId: 1, moduleId: 1, position: 2, type: 'QUIZ', title: 'REST basics quiz', maxAttempts: 3 },
  {
    id: 101, courseId: 1, moduleId: 1, position: 3, type: 'ASSIGNMENT', title: 'Assignment 1: REST API',
    instructions: 'Build a REST API for polls with Spring Boot.\n\nHand in a link to your repository.',
    deadline: inDays(0, 23, 59), allowLateSubmission: false, mandatory: true,
  },
  {
    id: 104, courseId: 1, moduleId: 1, position: 4, type: 'ASSIGNMENT', title: 'Old assignment (already passed)',
    instructions: 'This deadline has passed, so it is not on the overview.',
    deadline: inDays(-3), allowLateSubmission: false, mandatory: false,
  },
  // DAT250, module 2
  {
    id: 21, courseId: 1, moduleId: 2, position: 0, type: 'STATIC_PAGE', title: 'Docker basics',
    body: 'Images, containers, volumes and networks.',
  },
  {
    id: 102, courseId: 1, moduleId: 2, position: 1, type: 'CODING_EXERCISE', title: 'Exercise: Docker Compose',
    instructions: 'Run the poll app, a database and RabbitMQ with Docker Compose.',
    starterRepositoryUrl: 'https://github.com/example/docker-compose-starter',
    deadline: inDays(1, 12, 0), allowLateSubmission: true,
  },
  { id: 22, courseId: 1, moduleId: 2, position: 2, type: 'QUIZ', title: 'Containers quiz', maxAttempts: 1 },
  // DAT250, module 3 (module 4 has no content)
  {
    id: 31, courseId: 1, moduleId: 3, position: 0, type: 'STATIC_PAGE', title: 'Messaging with RabbitMQ',
    body: 'Queues, exchanges and bindings.',
  },
  {
    id: 103, courseId: 1, moduleId: 3, position: 1, type: 'ASSIGNMENT', title: 'Assignment 2: Messaging',
    instructions: 'Send poll events through RabbitMQ.', deadline: inDays(9), allowLateSubmission: false,
    mandatory: true,
  },
  // DAT251
  {
    id: 51, courseId: 2, moduleId: 5, position: 0, type: 'STATIC_PAGE', title: 'Scrum in practice',
    body: 'Sprints, stand-ups, reviews and retrospectives.',
  },
  {
    id: 201, courseId: 2, moduleId: 5, position: 1, type: 'ASSIGNMENT', title: 'Sprint report',
    instructions: 'Write a short report about your first sprint.', deadline: inDays(4), allowLateSubmission: false,
    mandatory: false,
  },
  { id: 61, courseId: 2, moduleId: 6, position: 0, type: 'QUIZ', title: 'Testing quiz', maxAttempts: 2 },
  // DAT109
  {
    id: 71, courseId: 3, moduleId: 7, position: 0, type: 'STATIC_PAGE', title: 'Introduction to Haskell',
    body: 'Pure functions, types and pattern matching.',
  },
  {
    id: 301, courseId: 3, moduleId: 7, position: 1, type: 'CODING_EXERCISE', title: 'Haskell exercises',
    instructions: 'Solve the exercises in the starter code.', starterRepositoryUrl: null,
    deadline: inDays(16, 16, 0), allowLateSubmission: true,
  },
  // DAT100
  {
    id: 81, courseId: 4, moduleId: 8, position: 0, type: 'STATIC_PAGE', title: 'Hello Java',
    body: 'Your first Java program.',
  },
]
