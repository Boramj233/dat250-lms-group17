// Test data for `npm run dev:mock`. Edit freely: the mock backend reloads this file on
// the next request, so you only need to refresh the browser.
//
// The types come from src/domain, so TypeScript tells you when the data no longer
// matches what the frontend expects from the backend.
import type { Content } from '../src/domain/content/types'
import type { Course, CourseRole } from '../src/domain/courses/types'
import type { CourseModule } from '../src/domain/modules/types'
import type { MockUser } from './types'

/** Delay on every API answer, so loading states are visible. 0 turns it off. */
export const latencyMs = 300

/** The users on the mock login page (instead of Keycloak). */
export const users: MockUser[] = [
  { userId: 1, email: 'student@example.com', firstName: 'Siri', lastName: 'Student', access: 'STANDARD' },
  { userId: 2, email: 'teacher@example.com', firstName: 'Tore', lastName: 'Teacher', access: 'COURSE_MANAGER' },
  { userId: 3, email: 'admin@example.com', firstName: 'Ada', lastName: 'Admin', access: 'ADMIN' },
  { userId: 4, email: 'new@example.com', firstName: 'Nina', lastName: 'New', access: 'STANDARD' },
  { userId: 5, email: 'sam@example.com', firstName: 'Sam', lastName: 'Sample', access: 'STANDARD' },
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

export const enrollments: { userId: number; courseId: number; role: CourseRole }[] = [
  { userId: 1, courseId: 1, role: 'STUDENT' },
  { userId: 1, courseId: 2, role: 'STUDENT' },
  { userId: 1, courseId: 3, role: 'STUDENT' },
  { userId: 1, courseId: 4, role: 'STUDENT' },
  { userId: 2, courseId: 1, role: 'TEACHER' },
  { userId: 2, courseId: 4, role: 'TEACHER' },
  { userId: 3, courseId: 2, role: 'TEACHER' },
  { userId: 5, courseId: 1, role: 'STUDENT' },
  // User 4 is not enrolled anywhere (empty dashboard).
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
  // Containers opens when module 1 is completed, Messaging when Containers is.
  { id: 2, courseId: 1, title: 'Containers', position: 1, prerequisiteModuleIds: [1] },
  { id: 3, courseId: 1, title: 'Messaging', position: 2, prerequisiteModuleIds: [2] },
  // Cloud deployment opens in a week (start date) and is open to all two weeks later.
  {
    id: 4,
    courseId: 1,
    title: 'Cloud deployment',
    position: 3,
    availableFrom: inDays(7, 8, 0),
    openToAllFrom: inDays(21, 8, 0),
  },
  { id: 5, courseId: 2, title: 'Agile and Scrum', position: 0 },
  { id: 6, courseId: 2, title: 'Testing', position: 1 },
  { id: 7, courseId: 3, title: 'Haskell', position: 0 },
  { id: 8, courseId: 4, title: 'Getting started', position: 0 },
]

/**
 * What each user has completed: assignments and coding exercises they submitted,
 * quizzes they submitted an attempt for. Add rows here to see the "Completed" lists.
 */
export const completions: { userId: number; contentId: number; completedAt: string }[] = [
  { userId: 1, contentId: 13, completedAt: inDays(-5, 14, 30) },
  { userId: 1, contentId: 104, completedAt: inDays(-4, 20, 0) },
  { userId: 1, contentId: 22, completedAt: inDays(-1, 10, 15) },
  { userId: 1, contentId: 61, completedAt: inDays(-2, 9, 0) },
  { userId: 1, contentId: 102, completedAt: inDays(-1, 18, 45) },
  { userId: 1, contentId: 201, completedAt: inDays(-2, 15, 0) },
]

/** A tiny but valid PDF, for the sample file submission. */
const SAMPLE_PDF = new TextEncoder().encode(
  '%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n' +
    '2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n' +
    '3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 200 100]>>endobj\n' +
    'trailer<</Root 1 0 R>>\n%%EOF\n',
)

/**
 * Submissions to assignments and coding exercises, like the backend's Submission. A
 * group submission has `groupId` and counts for the group's members. `grade` is the
 * teacher's grade (GradeReviewResponse): a score from 0 to 100; `releasedAt` null means
 * the student cannot see it yet. Keep `completions` above in step with this list.
 */
export const submissions: {
  id: number
  userId: number
  contentId: number
  type: 'TEXT' | 'GIT_URL' | 'FILE'
  /** Null for FILE. */
  payload: string | null
  file?: { name: string; mediaType: string; content: Uint8Array }
  submittedAt: string
  late: boolean
  groupId?: number
  /** The teacher's passed / not passed (GradeReviewResponse.passed); null when not decided. */
  // `visibleFrom` (proposed): the grade becomes visible to the student at that time by itself.
  grade: {
    score: number
    feedback: string | null
    passed: boolean | null
    visibleFrom?: string | null
    gradedAt: string
    releasedAt: string | null
    version: number
  } | null
}[] = [
  {
    id: 1,
    userId: 1,
    contentId: 104,
    type: 'TEXT',
    late: false,
    payload: 'My answer to the old assignment.',
    submittedAt: inDays(-4, 20, 0),
    grade: {
      score: 90,
      passed: true,
      feedback: 'Good work! The explanation of the status codes is clear.\n' + 'Remember to test the error cases too.',
      gradedAt: inDays(-2, 10, 0),
      releasedAt: inDays(-1, 9, 30),
      version: 1,
    },
  },
  {
    id: 2,
    userId: 1,
    contentId: 102,
    type: 'GIT_URL',
    late: false,
    payload: 'https://github.com/example/siri-docker-compose',
    submittedAt: inDays(-1, 18, 45),
    grade: null,
  },
  {
    id: 3,
    userId: 1,
    contentId: 201,
    type: 'TEXT',
    late: false,
    payload: 'Our first sprint went well.',
    submittedAt: inDays(-2, 15, 0),
    grade: {
      score: 80,
      feedback: null,
      passed: true,
      gradedAt: inDays(-1, 14, 0),
      releasedAt: inDays(0, 8, 0),
      version: 1,
    },
  },
  {
    id: 4,
    userId: 5,
    contentId: 102,
    type: 'GIT_URL',
    late: true,
    payload: 'https://github.com/example/sam-docker-compose',
    submittedAt: inDays(1, 13, 0),
    grade: null,
  },
  {
    id: 5,
    userId: 5,
    contentId: 104,
    type: 'TEXT',
    late: false,
    payload: 'Sam answered the old assignment.',
    submittedAt: inDays(-3, 22, 10),
    grade: {
      score: 40,
      feedback: 'Please explain the HTTP methods.',
      passed: false,
      gradedAt: inDays(-1, 11, 0),
      releasedAt: null,
      version: 0,
    },
  },
  {
    // Group assignment: Sam handed in for the group, so Siri sees it too.
    id: 6,
    userId: 5,
    contentId: 103,
    groupId: 1,
    type: 'TEXT',
    late: false,
    payload: 'We send a PollCreated event to a topic exchange and log it in a consumer.',
    submittedAt: inDays(-1, 16, 20),
    grade: {
      score: 95,
      feedback: 'Nice and simple. Add a dead letter queue next time.',
      passed: true,
      gradedAt: inDays(0, 8, 30),
      releasedAt: inDays(0, 9, 0),
      version: 1,
    },
  },
  {
    id: 7,
    userId: 5,
    contentId: 101,
    type: 'TEXT',
    late: false,
    payload: 'My REST API has GET, POST and DELETE for polls. The code is in the repository I shared.',
    submittedAt: inDays(-2, 12, 0),
    grade: null,
  },
  {
    // Sam handed in again, as a PDF: only this latest one can be graded.
    id: 8,
    userId: 5,
    contentId: 101,
    type: 'FILE',
    late: false,
    payload: null,
    file: { name: 'rest-api-report.pdf', mediaType: 'application/pdf', content: SAMPLE_PDF },
    submittedAt: inDays(-1, 12, 0),
    grade: null,
  },
]

/**
 * Groups in a course, like the backend's GroupDTO; `name` is proposed (see src/domain/groups).
 * A student is in at most one group per course.
 */
export const groups: { id: number; courseId: number; groupNumber: number; name?: string; memberIds: number[] }[] = [
  { id: 1, courseId: 1, groupNumber: 7, name: 'Rabbit Runners', memberIds: [1, 5] },
  { id: 2, courseId: 1, groupNumber: 2, memberIds: [] },
  { id: 3, courseId: 1, groupNumber: 3, memberIds: [] },
  { id: 4, courseId: 2, groupNumber: 1, memberIds: [1, 5] },
]

/** A quiz question; new ones are added in mock mode by POST /api/quizzes/:quizId/questions. */
export type MockQuizQuestion = {
  id: number
  quizId: number
  questionText: string
  position: number
  /** The backend's QuestionType; missing means MULTIPLE_CHOICE (its default). */
  type?: 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'FILL_IN_BLANK' | 'ORDERING' | 'MATCHING'
  hint: string | null
  /** MULTIPLE_CHOICE and TRUE_FALSE; empty for the other types. */
  options: { id: number; optionText: string; position: number; correct: boolean }[]
  /** FILL_IN_BLANK: the answers that count as correct (compared without case). */
  acceptedAnswers?: string[]
  /** ORDERING and MATCHING: the items (UUIDs like the backend's QuizItem). */
  items?: { id: string; text: string; kind: 'ORDERING' | 'MATCHING_LEFT' | 'MATCHING_RIGHT' }[]
  /** ORDERING: the item ids in the correct order. */
  correctOrder?: string[]
  /** MATCHING: the correct pairs. */
  correctMatches?: { leftItemId: string; rightItemId: string }[]
}

/** Questions of the quizzes; new ones are added by POST /api/quizzes/:quizId/questions. */
export const quizQuestions: MockQuizQuestion[] = [
  {
    id: 1,
    quizId: 13,
    questionText: 'Which HTTP method should create a new resource?',
    position: 0,
    hint: 'It is not idempotent.',
    options: [
      { id: 1, optionText: 'GET', position: 0, correct: false },
      { id: 2, optionText: 'POST', position: 1, correct: true },
      { id: 3, optionText: 'DELETE', position: 2, correct: false },
    ],
  },
  {
    id: 2,
    quizId: 13,
    questionText: 'Which status codes mean success?',
    position: 1,
    hint: null,
    type: 'MULTIPLE_CHOICE',
    options: [
      { id: 4, optionText: '200 OK', position: 0, correct: true },
      { id: 5, optionText: '201 Created', position: 1, correct: true },
      { id: 6, optionText: '404 Not Found', position: 2, correct: false },
    ],
  },
  {
    id: 3,
    quizId: 13,
    questionText: 'A PUT request should be idempotent.',
    position: 2,
    hint: null,
    type: 'TRUE_FALSE',
    options: [
      { id: 7, optionText: 'True', position: 0, correct: true },
      { id: 8, optionText: 'False', position: 1, correct: false },
    ],
  },
  {
    id: 4,
    quizId: 22,
    questionText: 'Which file describes the services for Docker Compose?',
    position: 0,
    hint: null,
    options: [
      { id: 9, optionText: 'Dockerfile', position: 0, correct: false },
      { id: 10, optionText: 'compose.yaml', position: 1, correct: true },
      { id: 11, optionText: 'package.json', position: 2, correct: false },
    ],
  },
  // DAT251 "Testing quiz": one question of each of the newer types.
  {
    id: 5,
    quizId: 61,
    questionText: 'A test that checks one class on its own is a ___ test.',
    position: 0,
    hint: 'The smallest kind of test.',
    type: 'FILL_IN_BLANK',
    options: [],
    acceptedAnswers: ['unit'],
  },
  {
    id: 6,
    quizId: 61,
    questionText: 'Put the steps of test-driven development in order.',
    position: 1,
    hint: 'Red, green, …',
    type: 'ORDERING',
    options: [],
    items: [
      { id: '6a1e0c52-0000-4000-8000-000000000001', text: 'Write a failing test', kind: 'ORDERING' },
      { id: '1f9b7d33-0000-4000-8000-000000000002', text: 'Make the test pass', kind: 'ORDERING' },
      { id: '9c4d2a18-0000-4000-8000-000000000003', text: 'Refactor', kind: 'ORDERING' },
    ],
    correctOrder: [
      '6a1e0c52-0000-4000-8000-000000000001',
      '1f9b7d33-0000-4000-8000-000000000002',
      '9c4d2a18-0000-4000-8000-000000000003',
    ],
  },
  {
    id: 7,
    quizId: 61,
    questionText: 'Match each kind of test with what it checks.',
    position: 2,
    hint: null,
    type: 'MATCHING',
    options: [],
    items: [
      { id: '30000000-0000-4000-8000-00000000000a', text: 'Unit test', kind: 'MATCHING_LEFT' },
      { id: '30000000-0000-4000-8000-00000000000b', text: 'Integration test', kind: 'MATCHING_LEFT' },
      { id: '30000000-0000-4000-8000-00000000000c', text: 'End-to-end test', kind: 'MATCHING_LEFT' },
      { id: 'e0000000-0000-4000-8000-00000000000d', text: 'One class on its own', kind: 'MATCHING_RIGHT' },
      { id: '70000000-0000-4000-8000-00000000000e', text: 'Several parts together', kind: 'MATCHING_RIGHT' },
      { id: 'a0000000-0000-4000-8000-00000000000f', text: 'The whole app, as a user', kind: 'MATCHING_RIGHT' },
    ],
    correctMatches: [
      { leftItemId: '30000000-0000-4000-8000-00000000000a', rightItemId: 'e0000000-0000-4000-8000-00000000000d' },
      { leftItemId: '30000000-0000-4000-8000-00000000000b', rightItemId: '70000000-0000-4000-8000-00000000000e' },
      { leftItemId: '30000000-0000-4000-8000-00000000000c', rightItemId: 'a0000000-0000-4000-8000-00000000000f' },
    ],
  },
]

/** Quiz attempts, like the backend's QuizAttemptResponse plus who took it. */
export const quizAttempts: {
  id: number
  quizId: number
  userId: number
  submittedAt: string
  correctCount: number
  totalQuestions: number
  /** How each question was answered (the backend's QuizAnswer); read by the proposed attempt endpoint. */
  answers: {
    questionId: number
    selectedOptionIds: number[]
    textAnswer?: string | null
    orderedItemIds?: string[] | null
    matches?: { leftItemId: string; rightItemId: string }[] | null
    correct: boolean
  }[]
}[] = [
  // Siri: POST right, only "200 OK" of the two success codes (wrong), PUT idempotent right.
  {
    id: 1,
    quizId: 13,
    userId: 1,
    submittedAt: inDays(-2, 10, 15),
    correctCount: 2,
    totalQuestions: 3,
    answers: [
      { questionId: 1, selectedOptionIds: [2], correct: true },
      { questionId: 2, selectedOptionIds: [4], correct: false },
      { questionId: 3, selectedOptionIds: [7], correct: true },
    ],
  },
]

/**
 * All content, with its courseId so the handlers can check the course. The handlers
 * return each type in the backend's response shape. Assignments and coding exercises
 * with a deadline also show up as upcoming deadlines on the dashboard.
 */
export const contents: (Content & { courseId: number })[] = [
  // DAT250, module 1
  {
    id: 11,
    courseId: 1,
    moduleId: 1,
    position: 0,
    type: 'STATIC_PAGE',
    title: 'Welcome to DAT250',
    body:
      'Welcome!\n\nThis course is about building and running modern web applications.\n' +
      'Start with the page about REST, then take the quiz.',
  },
  {
    // Proposed visibleFrom: students see it in two days, later than the rest of the module.
    id: 12,
    courseId: 1,
    moduleId: 1,
    position: 1,
    type: 'STATIC_PAGE',
    title: 'What is REST?',
    visibleFrom: inDays(2, 8, 0),
    body:
      'REST is an architectural style for APIs.\n\nResources are identified by URLs, and the ' +
      'HTTP methods (GET, POST, PUT, PATCH, DELETE) say what to do with them.',
  },
  {
    id: 13,
    courseId: 1,
    moduleId: 1,
    position: 2,
    type: 'QUIZ',
    title: 'REST basics quiz',
    maxAttempts: 3,
    passingPercentage: 100,
    requiredForModuleCompletion: true,
    description:
      'Three short questions about HTTP methods and status codes. You get your score right after you submit.',
    deadline: inDays(5, 23, 59),
  },
  {
    id: 101,
    courseId: 1,
    moduleId: 1,
    position: 3,
    type: 'ASSIGNMENT',
    title: 'Assignment 1: REST API',
    instructions: 'Build a REST API for polls with Spring Boot.\n\nHand in a link to your repository.',
    deadline: inDays(0, 23, 59),
    allowLateSubmission: false,
    mandatoryForExam: true,
    requiredForModuleCompletion: true,
  },
  {
    id: 104,
    courseId: 1,
    moduleId: 1,
    position: 4,
    type: 'ASSIGNMENT',
    title: 'Old assignment (already passed)',
    instructions: 'This deadline has passed, so it is not on the dashboard.',
    deadline: inDays(-3),
    allowLateSubmission: false,
    mandatoryForExam: false,
  },
  // DAT250, module 2
  {
    id: 21,
    courseId: 1,
    moduleId: 2,
    position: 0,
    type: 'STATIC_PAGE',
    title: 'Docker basics',
    body: 'Images, containers, volumes and networks.',
  },
  {
    id: 102,
    courseId: 1,
    moduleId: 2,
    position: 1,
    type: 'CODING_EXERCISE',
    title: 'Exercise: Docker Compose',
    instructions: 'Run the poll app, a database and RabbitMQ with Docker Compose.',
    starterRepositoryUrl: 'https://github.com/example/docker-compose-starter',
    starterCode:
      'services:\n  app:\n    build: .\n    ports:\n      - "8080:8080"\n  # Add the database and RabbitMQ here.\n',
    deadline: inDays(1, 12, 0),
    allowLateSubmission: true,
  },
  {
    id: 22,
    courseId: 1,
    moduleId: 2,
    position: 2,
    type: 'QUIZ',
    title: 'Containers quiz',
    maxAttempts: 1,
    passingPercentage: 60,
    description: 'One question about Docker Compose. You have only one attempt.',
    deadline: null,
  },
  // DAT250, module 3
  {
    id: 31,
    courseId: 1,
    moduleId: 3,
    position: 0,
    type: 'STATIC_PAGE',
    title: 'Messaging with RabbitMQ',
    body: 'Queues, exchanges and bindings.',
  },
  // DAT250, module 4: no visible date of its own, so it is visible when the module is.
  {
    id: 41,
    courseId: 1,
    moduleId: 4,
    position: 0,
    type: 'STATIC_PAGE',
    title: 'Deploying to the cloud',
    body: 'Container registries, managed databases and secrets.',
  },
  {
    id: 103,
    courseId: 1,
    moduleId: 3,
    position: 1,
    type: 'ASSIGNMENT',
    title: 'Assignment 2: Messaging',
    instructions: 'Send poll events through RabbitMQ.',
    deadline: inDays(9),
    allowLateSubmission: false,
    mandatoryForExam: true,
    groupAssignment: true,
    acceptedFileTypes: ['.pdf', '.zip'],
  },
  // DAT251
  {
    id: 51,
    courseId: 2,
    moduleId: 5,
    position: 0,
    type: 'STATIC_PAGE',
    title: 'Scrum in practice',
    body: 'Sprints, stand-ups, reviews and retrospectives.',
  },
  {
    id: 201,
    courseId: 2,
    moduleId: 5,
    position: 1,
    type: 'ASSIGNMENT',
    title: 'Sprint report',
    instructions: 'Write a short report about your first sprint.',
    deadline: inDays(4),
    allowLateSubmission: false,
    mandatoryForExam: false,
  },
  {
    id: 61,
    courseId: 2,
    moduleId: 6,
    position: 0,
    type: 'QUIZ',
    title: 'Testing quiz',
    maxAttempts: 2,
    passingPercentage: 60,
  },
  // DAT109
  {
    id: 71,
    courseId: 3,
    moduleId: 7,
    position: 0,
    type: 'STATIC_PAGE',
    title: 'Introduction to Haskell',
    body: 'Pure functions, types and pattern matching.',
  },
  {
    id: 301,
    courseId: 3,
    moduleId: 7,
    position: 1,
    type: 'CODING_EXERCISE',
    title: 'Haskell exercises',
    instructions: 'Solve the exercises in the starter code.',
    starterRepositoryUrl: null,
    deadline: inDays(16, 16, 0),
    allowLateSubmission: true,
  },
  // DAT100
  {
    id: 81,
    courseId: 4,
    moduleId: 8,
    position: 0,
    type: 'STATIC_PAGE',
    title: 'Hello Java',
    body: 'Your first Java program.',
  },
]
