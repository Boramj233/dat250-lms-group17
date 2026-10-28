export const courseKeys = {
  allCourses: ["courses", "list"] as const,
  myCourses: (userId: string | number) =>
    ["courses", "enrolled", userId] as const,
};
