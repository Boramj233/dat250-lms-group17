import { test } from 'node:test'
import assert from 'node:assert/strict'
import { combineExtensions, formatExtensions, parseExtensions } from '../src/features/course-management/fileTypes.ts'

test('typed extensions get a dot, lower case and no duplicates', () => {
  assert.deepEqual(parseExtensions('py, .Java  ipynb;PY'), ['.py', '.java', '.ipynb'])
  assert.deepEqual(parseExtensions('..zip'), ['.zip'])
})

test('anything that is not an extension is left out', () => {
  assert.deepEqual(parseExtensions(''), [])
  assert.deepEqual(parseExtensions('tar.gz, *.exe, ., c++'), [])
})

test('ticked presets and typed extensions are combined without duplicates', () => {
  assert.deepEqual(combineExtensions(['ZIP', 'PDF'], 'zip, py'), ['.pdf', '.zip', '.py'])
  assert.deepEqual(combineExtensions([], ''), [])
})

test('an empty list means any file type', () => {
  assert.equal(formatExtensions([]), 'Any file type')
  assert.equal(formatExtensions(['.pdf', '.zip']), '.pdf, .zip')
})

test('saved extensions are split back into presets and typed text', async () => {
  const { splitExtensions } = await import('../src/features/course-management/fileTypes.ts')
  assert.deepEqual(splitExtensions(['.pdf', '.zip', '.py']), { presetLabels: ['PDF', 'ZIP'], other: '.py' })
  // Text needs both .txt and .md; only one of them stays typed.
  assert.deepEqual(splitExtensions(['.txt']), { presetLabels: [], other: '.txt' })
  assert.deepEqual(splitExtensions([]), { presetLabels: [], other: '' })
})
