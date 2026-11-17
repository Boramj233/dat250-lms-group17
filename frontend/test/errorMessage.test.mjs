import { test } from 'node:test'
import assert from 'node:assert/strict'
import { ApiError } from '../src/shared/api/apiClient.ts'
import { errorMessage } from '../src/shared/api/errorMessage.ts'

const byStatus = { 409: 'Already exists.', 403: 'Not allowed.' }

test('errorMessage gives the message for the status code', () => {
  assert.equal(errorMessage(new ApiError(409), byStatus, 'Try again.'), 'Already exists.')
  assert.equal(errorMessage(new ApiError(403), byStatus, 'Try again.'), 'Not allowed.')
})

test('errorMessage falls back for other status codes and other errors', () => {
  assert.equal(errorMessage(new ApiError(500), byStatus, 'Try again.'), 'Try again.')
  assert.equal(errorMessage(new TypeError('Failed to fetch'), byStatus, 'Try again.'), 'Try again.')
  assert.equal(errorMessage(undefined, byStatus, 'Try again.'), 'Try again.')
})
