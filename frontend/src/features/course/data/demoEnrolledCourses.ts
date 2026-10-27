type EnrolledCourse = {
  id: string
  name: string
}

// Temporary enrollment data until the current user's courses can be loaded from the backend.
export const demoEnrolledCourses: readonly EnrolledCourse[] = [
  { id: 'dat250', name: 'DAT250' },
]
