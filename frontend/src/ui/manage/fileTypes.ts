/** Common file types a teacher can tick for an assignment. */
export const FILE_TYPE_PRESETS: { label: string, extensions: string[] }[] = [
  { label: 'PDF', extensions: ['.pdf'] },
  { label: 'Word', extensions: ['.docx'] },
  { label: 'ZIP', extensions: ['.zip'] },
  { label: 'Text', extensions: ['.txt', '.md'] },
  { label: 'Images', extensions: ['.png', '.jpg', '.jpeg'] },
]

/**
 * Extensions typed by the teacher, e.g. "py, .Java  ipynb", as [".py", ".java", ".ipynb"]:
 * split on commas and spaces, lower case, with a leading dot, without duplicates.
 * Anything that is not letters and digits after the dot is left out.
 */
export function parseExtensions(text: string): string[] {
  const extensions = text
    .split(/[\s,;]+/)
    .map(part => part.trim().toLowerCase().replace(/^\.+/, ''))
    .filter(part => /^[a-z0-9]+$/.test(part))
    .map(part => `.${part}`)
  return [...new Set(extensions)]
}

/** The ticked presets plus the typed extensions, without duplicates, in a stable order. */
export function combineExtensions(presetLabels: string[], typed: string): string[] {
  const fromPresets = FILE_TYPE_PRESETS
    .filter(preset => presetLabels.includes(preset.label))
    .flatMap(preset => preset.extensions)
  return [...new Set([...fromPresets, ...parseExtensions(typed)])]
}

/** ".pdf, .zip", or "Any file type" for an empty list. */
export function formatExtensions(extensions: string[]): string {
  return extensions.length === 0 ? 'Any file type' : extensions.join(', ')
}
