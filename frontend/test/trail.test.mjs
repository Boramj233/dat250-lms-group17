import { test } from 'node:test'
import assert from 'node:assert/strict'
import { MAX_TRAIL, nextTrail, readTrail } from '../src/shared/navigation/trailState.ts'

const dashboard = { label: 'Dashboard', to: '/' }
const courses = { label: 'Courses', to: '/courses' }
const dat250 = { label: 'DAT250', to: '/courses/1' }

test('a link adds the current page to the trail', () => {
  assert.deepEqual(nextTrail([], dashboard, '/courses'), [dashboard])
  assert.deepEqual(nextTrail([dashboard], courses, '/courses/1'), [dashboard, courses])
})

test('a link to a page already in the trail cuts the trail there', () => {
  assert.deepEqual(nextTrail([dashboard, courses], dat250, '/courses'), [dashboard])
  assert.deepEqual(nextTrail([dashboard, courses], dat250, '/'), [])
})

test('the trail keeps only the last steps', () => {
  const long = Array.from({ length: 10 }, (_, i) => ({ label: `P${i}`, to: `/p/${i}` }))
  const trail = nextTrail(long, dat250, '/elsewhere')
  assert.equal(trail.length, MAX_TRAIL)
  assert.deepEqual(trail.at(-1), dat250)
})

test('a missing or malformed trail is empty', () => {
  assert.deepEqual(readTrail(null), [])
  assert.deepEqual(readTrail({}), [])
  assert.deepEqual(readTrail({ trail: 'x' }), [])
  assert.deepEqual(readTrail({ trail: [dashboard, { label: 1 }, null] }), [dashboard])
})
