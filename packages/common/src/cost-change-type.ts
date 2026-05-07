import { z } from "zod";

export const costChangeTypes = [
  "PRINT_JOB",       // Deducted on print
  "MANUAL_ADD",      // Admin manually added credit
  "MANUAL_DEDUCT",   // Admin manually deducted
  "VOUCHER_REDEEM",  // Voucher redeemed
  "REFUND",          // Print job refunded
  "TRANSFER_IN",     // Transferred from another account
  "TRANSFER_OUT",    // Transferred to another account
  "INITIAL",         // Initial balance
] as const;

export type CostChangeType = (typeof costChangeTypes)[number];
export const costChangeTypeSchema = z.enum(costChangeTypes);
