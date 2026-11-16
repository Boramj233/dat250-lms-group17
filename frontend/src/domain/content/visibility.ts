// When content becomes visible to students (proposed `visibleFrom`, see README). Content
// without its own date inherits its module's start date (`availableFrom`), and its own
// date cannot be before the module's.

type HasVisibleFrom = { visibleFrom?: string | null };
type HasAvailableFrom = { availableFrom?: string | null };

/** The item's own visible date, otherwise its module's start date; null for "always". */
export function effectiveVisibleFrom(item: HasVisibleFrom, module: HasAvailableFrom): string | null {
  return item.visibleFrom ?? module.availableFrom ?? null;
}

/** Students cannot see the item yet. */
export function isNotYetVisible(item: HasVisibleFrom, module: HasAvailableFrom, now: number): boolean {
  const from = effectiveVisibleFrom(item, module);
  return from !== null && Date.parse(from) > now;
}

/** A message when an item's visible date is before its module's start date. */
export function checkContentVisibleFrom(
  visibleFrom: string | null,
  moduleAvailableFrom: string | null | undefined,
): string | undefined {
  if (visibleFrom && moduleAvailableFrom && Date.parse(visibleFrom) < Date.parse(moduleAvailableFrom)) {
    return "“Visible from” cannot be before the module is available.";
  }
  return undefined;
}

/**
 * A message when a module's new start date would come after content in it that has its
 * own, earlier visible date.
 */
export function checkModuleStartAgainstContent(
  availableFrom: string | null,
  items: (HasVisibleFrom & { title: string })[],
): string | undefined {
  if (!availableFrom) return undefined;
  const before = items.filter(item => item.visibleFrom && Date.parse(item.visibleFrom) < Date.parse(availableFrom));
  if (before.length === 0) return undefined;
  const names = before.map(item => `“${item.title}”`).join(", ");
  return `“Available from” cannot be after the visible date of content in the module: ${names}. Change their dates first.`;
}

/**
 * Published: the item's visible date (its own, or its module's start date) has passed.
 * Decided by the date alone; until then a teacher may change all of it.
 */
export function isPublished(item: HasVisibleFrom, module: HasAvailableFrom, now: number): boolean {
  return !isNotYetVisible(item, module, now);
}
