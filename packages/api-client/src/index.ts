// @printflow/api-client — Shared tRPC client and API utilities for Next.js frontend

export type { AppConfig } from "@printflow/common";

export const SSE_EVENT_TYPES = [
  "document:created",
  "document:updated",
  "document:deleted",
  "printjob:started",
  "printjob:completed",
  "printjob:failed",
  "printer:status_changed",
  "user:login",
  "user:logout",
  "system:announcement",
] as const;

export type SseEventType = typeof SSE_EVENT_TYPES[number];

export type SseEvent = {
  type: SseEventType;
  data: Record<string, unknown>;
  timestamp: string;
};
