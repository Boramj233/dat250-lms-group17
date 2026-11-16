import { useState } from 'react'

/** Changes one value: `set('title')('New title')`. */
export type SetFormValue<T> = <K extends keyof T>(key: K) => (value: T[K]) => void

/**
 * A form's values as one object. `set('title')` gives the onChange for one value; every
 * change marks the form `dirty` (changed since it opened or was last sent) and calls
 * `onChange` with the new values.
 */
export function useFormValues<T extends object>(initial: T | (() => T), onChange?: (values: T) => void) {
  const [values, setValues] = useState<T>(initial)
  const [dirty, setDirty] = useState(false)

  const set: SetFormValue<T> = key => value => {
    const next = { ...values, [key]: value }
    setValues(next)
    setDirty(true)
    onChange?.(next)
  }

  return { values, set, dirty, setDirty }
}
