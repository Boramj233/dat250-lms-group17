import { test } from 'node:test'
import assert from 'node:assert/strict'
import { safeHttpUrl } from '../src/ui/components/safeUrl.ts'

test('only http and https links are kept', () => {
  assert.equal(safeHttpUrl('https://github.com/x/y'), 'https://github.com/x/y')
  assert.equal(safeHttpUrl('http://example.com'), 'http://example.com/')
  assert.equal(safeHttpUrl('javascript:alert(1)'), undefined)
  assert.equal(safeHttpUrl('not a url'), undefined)
  assert.equal(safeHttpUrl(null), undefined)
})
