/** The highest score the backend accepts (SaveGradeRequest: 0–100, two decimals). */
export const MAX_SCORE = 100

/** "85 / 100", "72.5 / 100": without trailing zeros. */
export function formatScore(score: number): string {
  return `${Number(Number(score).toFixed(2))} / ${MAX_SCORE}`
}
