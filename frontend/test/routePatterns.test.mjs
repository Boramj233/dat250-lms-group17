import { test } from 'node:test'
import assert from 'node:assert/strict'
import { matchPath } from 'react-router'
import { paths, routePatterns } from '../src/shared/paths.ts'

const params = (pattern, url) => matchPath(pattern, url)?.params

test('the course patterns match the URLs that paths builds', () => {
  assert.deepEqual(params(routePatterns.courseSectionItem, paths.courseSectionContent(1, 'assignments', 101)), {
    courseId: '1',
    section: 'assignments',
    contentId: '101',
  })
  assert.equal(params(routePatterns.courseSection, paths.courseSection(1, 'quizzes')).section, 'quizzes')
  assert.deepEqual(params(routePatterns.courseSubmission, paths.courseSubmission(1, 101, 7)), {
    courseId: '1',
    contentId: '101',
    submissionId: '7',
  })
})

test('the module patterns match the URLs that paths builds', () => {
  assert.equal(params(routePatterns.inModule, paths.courseModule(1, 2)).moduleId, '2')
  assert.equal(params(routePatterns.moduleSection, paths.courseModuleSection(1, 2, 'files')).section, 'files')
  assert.deepEqual(params(routePatterns.moduleItem, paths.courseContent(1, 2, 'quizzes', 13)), {
    courseId: '1',
    moduleId: '2',
    section: 'quizzes',
    contentId: '13',
  })
  // A module's start page is in the module, but not in one of its sections.
  assert.equal(matchPath(routePatterns.moduleSection, paths.courseModule(1, 2)), null)
})

test('the manage patterns match the URLs that paths builds', () => {
  assert.equal(params(routePatterns.manageModule, paths.manageModule(1, 2)).moduleId, '2')
  assert.equal(params(routePatterns.manageContent, paths.manageContent(1, 2, 13)).contentId, '13')
  assert.equal(params(routePatterns.manageNewContent, paths.manageNewContent(1, 2, 'quiz')).type, 'quiz')
})
