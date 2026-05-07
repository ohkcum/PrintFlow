import { z } from "zod";

export const documentStatuses = [
  "PENDING",     // Uploaded, waiting for processing
  "PROCESSING",   // Thumbnails / validation in progress
  "READY",        // Ready for release
  "RELEASED",     // Sent to printer
  "CANCELLED",    // User cancelled
  "EXPIRED",      // Auto-deleted after retention period
  "ERROR",        // Processing failed
] as const;

export type DocumentStatus = (typeof documentStatuses)[number];
export const documentStatusSchema = z.enum(documentStatuses);
