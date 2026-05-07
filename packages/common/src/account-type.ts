import { z } from "zod";

export const accountTypes = [
  "USER",        // Personal account
  "GROUP",       // Shared group account
  "SHARED",      // Shared anonymous account (e.g. walk-up)
  "SYSTEM",      // Internal system account
] as const;

export type AccountType = (typeof accountTypes)[number];
export const accountTypeSchema = z.enum(accountTypes);
