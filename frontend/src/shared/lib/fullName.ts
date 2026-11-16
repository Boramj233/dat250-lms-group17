/** "Kari Nordmann": a user's, member's or student's name as shown everywhere. */
export function fullName(person: { firstName: string; lastName: string }): string {
  return `${person.firstName} ${person.lastName}`
}
