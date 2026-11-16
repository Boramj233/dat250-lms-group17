/** The URL if it is http(s), else undefined, so a stored `javascript:` URL can never run. */
export function safeHttpUrl(value: string | null | undefined): string | undefined {
  if (!value) return undefined
  try {
    const url = new URL(value)
    return url.protocol === 'http:' || url.protocol === 'https:' ? url.href : undefined
  } catch {
    return undefined
  }
}
