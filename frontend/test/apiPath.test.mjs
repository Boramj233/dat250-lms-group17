import { test } from 'node:test'
import assert from 'node:assert/strict'
import { apiPath, jsonBody } from '../src/shared/api/apiClient.ts'

test('apiPath encodes every interpolated value', () => {
  assert.equal(apiPath`/api/courses/${42}/modules/${'7'}`, '/api/courses/42/modules/7')
  assert.equal(apiPath`/api/users/${'a/b?c'}/courses`, '/api/users/a%2Fb%3Fc/courses')
  assert.equal(apiPath`/api/courses`, '/api/courses')
})

test('jsonBody sends the body as JSON', () => {
  const init = jsonBody('PATCH', { title: 'DAT250' })
  assert.equal(init.method, 'PATCH')
  assert.equal(new Headers(init.headers).get('Content-Type'), 'application/json')
  assert.equal(init.body, '{"title":"DAT250"}')
})
