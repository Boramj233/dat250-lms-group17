import type { UserId } from "@shared/types";

export const deadlineKeys = {
  upcoming: (userId: UserId) =>
    ["deadlines", "upcoming", String(userId)] as const,
};
