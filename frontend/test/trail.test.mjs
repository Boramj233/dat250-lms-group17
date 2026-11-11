import { test } from 'node:test'
import assert from 'node:assert/strict'
import { MAX_TRAIL, nextTrail, readTrail } from '../src/ui/navigation/trailState.ts'

const overview = { label: 'Overview', to: '/' }
const courses = { label: 'Courses', to: '/courses' }
const dat250 = { label: 'DAT250', to: '/courses/1' }

test('a link adds the current page to the trail', () => {
  assert.deepEqual(nextTrail([], overview, '/courses'), [overview])
  assert.deepEqual(nextTrail([overview], courses, '/courses/1'), [overview, courses])
})

test('a link to a page already in the trail cuts the trail there', () => {
  assert.deepEqual(nextTrail([overview, courses], dat250, '/courses'), [overview])
  assert.deepEqual(nextTrail([overview, courses], dat250, '/'), [])
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
  assert.deepEqual(readTrail({ trail: [overview, { label: 1 }, null] }), [overview])
})
