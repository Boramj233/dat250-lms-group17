/** "1 item", "3 items"; give `plural` when it is not just the word with an "s". */
export function formatCount(count: number, word: string, plural = `${word}s`): string {
  return `${count} ${count === 1 ? word : plural}`
}
