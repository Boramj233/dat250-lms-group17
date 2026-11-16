import { test } from 'node:test'
import assert from 'node:assert/strict'
import { checkContentVisibleFrom, checkModuleStartAgainstContent, effectiveVisibleFrom, isNotYetVisible } from '../src/features/content/visibility.ts'

const module = { availableFrom: '2026-12-01T08:00:00Z' }

test('content without its own date inherits the module start date', () => {
  assert.equal(effectiveVisibleFrom({}, module), module.availableFrom)
  assert.equal(effectiveVisibleFrom({ visibleFrom: null }, module), module.availableFrom)
  assert.equal(effectiveVisibleFrom({ visibleFrom: '2026-12-05T08:00:00Z' }, module), '2026-12-05T08:00:00Z')
  assert.equal(effectiveVisibleFrom({}, {}), null)
})

test('content is hidden from students before its effective date', () => {
  const now = Date.parse('2026-12-03T00:00:00Z')
  assert.equal(isNotYetVisible({}, module, now), false)
  assert.equal(isNotYetVisible({ visibleFrom: '2026-12-05T08:00:00Z' }, module, now), true)
  assert.equal(isNotYetVisible({}, {}, now), false)
})

test("a visible date cannot be before the module's start date", () => {
  assert.equal(checkContentVisibleFrom('2026-11-30T08:00:00Z', module.availableFrom) !== undefined, true)
  assert.equal(checkContentVisibleFrom('2026-12-01T08:00:00Z', module.availableFrom), undefined)
  assert.equal(checkContentVisibleFrom(null, module.availableFrom), undefined)
  assert.equal(checkContentVisibleFrom('2020-01-01T00:00:00Z', null), undefined)
})

test("a module's start date cannot move past its content's own dates", () => {
  const items = [{ title: 'A', visibleFrom: '2026-12-05T08:00:00Z' }, { title: 'B' }]
  assert.match(checkModuleStartAgainstContent('2026-12-06T00:00:00Z', items) ?? '', /“A”/)
  assert.equal(checkModuleStartAgainstContent('2026-12-04T00:00:00Z', items), undefined)
  assert.equal(checkModuleStartAgainstContent(null, items), undefined)
})

test('content is published once its visible date has passed', async () => {
  const { isPublished } = await import('../src/features/content/visibility.ts')
  const now = Date.parse('2026-12-03T00:00:00Z')
  assert.equal(isPublished({}, module, now), true)
  assert.equal(isPublished({ visibleFrom: '2026-12-05T08:00:00Z' }, module, now), false)
  assert.equal(isPublished({}, { availableFrom: '2026-12-10T00:00:00Z' }, now), false)
})
