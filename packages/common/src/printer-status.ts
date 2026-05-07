import { z } from "zod";

export const printerStatuses = [
  "ONLINE",      // Accepting jobs
  "OFFLINE",     // Not reachable
  "IDLE",        // Online but no jobs
  "BUSY",        // Printing
  "ERROR",       // Hardware error
  "MAINTENANCE", // Under maintenance
] as const;

export type PrinterStatus = (typeof printerStatuses)[number];
export const printerStatusSchema = z.enum(printerStatuses);
