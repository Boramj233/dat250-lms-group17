import type { UserId } from "../../shared/types";

export const deadlineKeys = {
  all: ["deadlines"] as const,
  upcoming: (userId: UserId) =>
    ["deadlines", "upcoming", String(userId)] as const,
};
