import { z } from "zod";

// Mirrors PrintOutStatus from PrintFlowLite
export const printJobStatuses = [
  "QUEUED",       // Submitted, waiting for printer
  "PROCESSING",   // Being sent to printer
  "PRINTING",     // In progress at printer
  "COMPLETED",    // Successfully printed
  "CANCELLED",    // Cancelled by user
  "FAILED",       // Printer error / timeout
  "HELD",         // Held (requires attention)
] as const;

export type PrintJobStatus = (typeof printJobStatuses)[number];
export const printJobStatusSchema = z.enum(printJobStatuses);
