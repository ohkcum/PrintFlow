import { z } from "zod";

export const userStatuses = ["ACTIVE", "BLOCKED", "DELETED", "EXPIRED"] as const;
export type UserStatus = (typeof userStatuses)[number];
export const userStatusSchema = z.enum(userStatuses);
