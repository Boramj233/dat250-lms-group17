import { test } from 'node:test'
import assert from 'node:assert/strict'
import { switchGroup, SwitchGroupError } from '../src/domain/groups/switchGroup.ts'

/** A fake backend: records the calls, and fails the ones listed in `fails` ("leave:1", "join:2"). */
function backend(fails = []) {
  const calls = []
  const step = kind => async groupId => {
    calls.push(`${kind}:${groupId}`)
    if (fails.includes(`${kind}:${groupId}`)) throw new Error(`${kind} ${groupId} failed`)
    return `${kind}:${groupId} ok`
  }
  return { calls, leave: step('leave'), join: step('join') }
}

test('switchGroup leaves the old group, then joins the new one', async () => {
  const api = backend()
  assert.equal(await switchGroup({ from: 1, to: 2, ...api }), 'join:2 ok')
  assert.deepEqual(api.calls, ['leave:1', 'join:2'])
})

test('when leaving fails, that error is thrown and nothing else is tried', async () => {
  const api = backend(['leave:1'])
  await assert.rejects(switchGroup({ from: 1, to: 2, ...api }), error => {
    assert.ok(!(error instanceof SwitchGroupError))
    assert.equal(error.message, 'leave 1 failed')
    return true
  })
  assert.deepEqual(api.calls, ['leave:1'])
})

test('when joining fails, the student is put back in the old group', async () => {
  const api = backend(['join:2'])
  await assert.rejects(switchGroup({ from: 1, to: 2, ...api }), error => {
    assert.ok(error instanceof SwitchGroupError)
    assert.equal(error.backInOldGroup, true)
    assert.equal(error.cause.message, 'join 2 failed')
    return true
  })
  assert.deepEqual(api.calls, ['leave:1', 'join:2', 'join:1'])
})

test('when going back fails too, the error says the student is in no group', async () => {
  const api = backend(['join:2', 'join:1'])
  await assert.rejects(switchGroup({ from: 1, to: 2, ...api }), error => {
    assert.ok(error instanceof SwitchGroupError)
    assert.equal(error.backInOldGroup, false)
    return true
  })
})
