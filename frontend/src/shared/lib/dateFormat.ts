const MINUTE = 60_000
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR

/** Deadlines closer than this are highlighted. */
const SOON_MS = 2 * DAY

const dateTimeFormat = new Intl.DateTimeFormat('en-GB', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  hour: '2-digit',
  minute: '2-digit',
})
const dayFormat = new Intl.DateTimeFormat('en-GB', { day: 'numeric' })
const monthFormat = new Intl.DateTimeFormat('en-GB', { month: 'short' })
const relativeFormat = new Intl.RelativeTimeFormat('en', { numeric: 'auto' })

/** "Fri 14 Nov, 23:59" in the user's time zone. */
/**
 * An ISO instant as the value of an <input type="datetime-local"> in local time
 * ("2026-11-14T09:30"), or "" for none. Turn it back with `fromDateTimeLocal`.
 */
export function toDateTimeLocal(iso: string | null | undefined): string {
  if (!iso) return ''
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** The value of an <input type="datetime-local"> (local time) as an ISO instant, or null if empty. */
export function fromDateTimeLocal(value: string): string | null {
  return value ? new Date(value).toISOString() : null
}

export function formatDateTime(date: Date): string {
  return dateTimeFormat.format(date)
}

/** Day of month and short month for a calendar-style date badge: ["14", "Nov"]. */
export function formatDayMonth(date: Date): [string, string] {
  return [dayFormat.format(date), monthFormat.format(date)]
}

/** "in 45 minutes", "in 5 hours", "tomorrow", "in 3 days"; "passed" after the time. */
export function formatTimeLeft(date: Date, now: Date = new Date()): string {
  const left = date.getTime() - now.getTime()
  if (left < 0) return 'passed'
  if (left < HOUR) return relativeFormat.format(Math.max(1, Math.round(left / MINUTE)), 'minute')
  if (left < DAY) return relativeFormat.format(Math.round(left / HOUR), 'hour')
  return relativeFormat.format(Math.round(left / DAY), 'day')
}

/** True when the deadline is in the future and less than two days away. */
export function isSoon(date: Date, now: Date = new Date()): boolean {
  const left = date.getTime() - now.getTime()
  return left >= 0 && left < SOON_MS
}
