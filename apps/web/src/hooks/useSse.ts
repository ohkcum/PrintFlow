"use client";

import { useEffect, useRef, useState, useCallback } from "react";
import type { SseEventType, SseEvent } from "@printflow/api-client";

// Re-export types
export type { SseEventType, SseEvent };

const API_URL = process.env["NEXT_PUBLIC_API_URL"] ?? "http://localhost:3001";
const RECONNECT_DELAY_MS = 3000;
const MAX_RECONNECT_ATTEMPTS = 10;

export interface UseSseOptions {
  /** Reconnect automatically on disconnect (default: true) */
  autoReconnect?: boolean;
  /** Maximum reconnect attempts (default: 10) */
  maxReconnectAttempts?: number;
  /** Called when connection is established */
  onConnect?: () => void;
  /** Called when disconnected */
  onDisconnect?: () => void;
  /** Called on any event */
  onEvent?: (event: SseEvent) => void;
  /** Filter events by type */
  eventTypes?: SseEventType[];
}

export interface UseSseReturn {
  /** Current connection status */
  status: "connecting" | "connected" | "disconnected" | "error";
  /** Last received event */
  lastEvent: SseEvent | null;
  /** All received events (ring buffer, max 100) */
  events: SseEvent[];
  /** Manually disconnect */
  disconnect: () => void;
  /** Manually reconnect */
  reconnect: () => void;
  /** Clear event history */
  clearEvents: () => void;
}

/**
 * React hook for SSE real-time events.
 *
 * Usage:
 *
 * ```tsx
 * const { status, events, lastEvent } = useSse({
 *   onEvent: (event) => {
 *     if (event.type === "document:created") {
 *       queryClient.invalidateQueries({ queryKey: ["documents"] });
 *     }
 *   }
 * });
 * ```
 */
export function useSse(options: UseSseOptions = {}): UseSseReturn {
  const {
    autoReconnect = true,
    maxReconnectAttempts = MAX_RECONNECT_ATTEMPTS,
    onConnect,
    onDisconnect,
    onEvent,
    eventTypes,
  } = options;

  const [status, setStatus] = useState<UseSseReturn["status"]>("disconnected");
  const [lastEvent, setLastEvent] = useState<SseEvent | null>(null);
  const [events, setEvents] = useState<SseEvent[]>([]);

  const esRef = useRef<EventSource | null>(null);
  const reconnectCountRef = useRef(0);
  const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const mountedRef = useRef(true);

  const connect = useCallback(() => {
    if (!mountedRef.current) return;

    // Close existing connection
    if (esRef.current) {
      esRef.current.close();
      esRef.current = null;
    }

    const token = localStorage.getItem("printflow_token");
    if (!token) {
      setStatus("disconnected");
      return;
    }

    setStatus("connecting");

    const url = `${API_URL}/api/v1/events`;
    const es = new EventSource(url, { withCredentials: true });
    esRef.current = es;

    es.addEventListener("connected", () => {
      if (!mountedRef.current) return;
      reconnectCountRef.current = 0;
      setStatus("connected");
      onConnect?.();
    });

    // Listen for each known event type
    const typesToListen: SseEventType[] = eventTypes ?? [
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
    ];

    for (const type of typesToListen) {
      es.addEventListener(type, (e: MessageEvent) => {
        if (!mountedRef.current) return;
        try {
          const event: SseEvent = JSON.parse(e.data);
          setLastEvent(event);
          setEvents((prev) => {
            const next = [event, ...prev];
            return next.slice(0, 100); // ring buffer of 100
          });
          onEvent?.(event);
        } catch {
          // Ignore parse errors
        }
      });
    }

    // Generic message handler for any event
    es.onmessage = (e: MessageEvent) => {
      if (!mountedRef.current) return;
      try {
        const event: SseEvent = JSON.parse(e.data);
        setLastEvent(event);
        setEvents((prev) => {
          const next = [event, ...prev];
          return next.slice(0, 100);
        });
        onEvent?.(event);
      } catch {
        // Ignore parse errors
      }
    };

    es.onerror = () => {
      if (!mountedRef.current) return;
      es.close();
      setStatus("error");
      onDisconnect?.();

      if (autoReconnectRef.current && reconnectCountRef.current < maxReconnectAttempts) {
        reconnectCountRef.current++;
        reconnectTimerRef.current = setTimeout(() => {
          if (mountedRef.current) {
            connect();
          }
        }, RECONNECT_DELAY_MS * Math.min(reconnectCountRef.current, 3)); // backoff
      } else {
        setStatus("disconnected");
      }
    };
  }, [maxReconnectAttempts, onConnect, onDisconnect, onEvent, eventTypes]);

  const autoReconnectRef = useRef(autoReconnect);
  autoReconnectRef.current = autoReconnect;

  const disconnect = useCallback(() => {
    autoReconnectRef.current = false; // prevent auto-reconnect
    if (reconnectTimerRef.current) {
      clearTimeout(reconnectTimerRef.current);
      reconnectTimerRef.current = null;
    }
    if (esRef.current) {
      esRef.current.close();
      esRef.current = null;
    }
    setStatus("disconnected");
    onDisconnect?.();
  }, [onDisconnect]);

  const reconnect = useCallback(() => {
    reconnectCountRef.current = 0;
    connect();
  }, [connect]);

  const clearEvents = useCallback(() => {
    setEvents([]);
    setLastEvent(null);
  }, []);

  useEffect(() => {
    mountedRef.current = true;
    connect();
    return () => {
      mountedRef.current = false;
      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current);
      }
      if (esRef.current) {
        esRef.current.close();
        esRef.current = null;
      }
    };
  }, [connect]);

  return { status, lastEvent, events, disconnect, reconnect, clearEvents };
}

// ─── Specialized hooks ───────────────────────────────────────────────────────────

/**
 * Hook for document list real-time updates.
 * Automatically refreshes when documents change.
 */
export function useDocumentSse(onDocumentChange?: (event: SseEvent) => void) {
  return useSse({
    eventTypes: ["document:created", "document:updated", "document:deleted"],
    onEvent: (event) => {
      onDocumentChange?.(event);
    },
  });
}

/**
 * Hook for print job real-time updates.
 */
export function usePrintJobSse(onJobChange?: (event: SseEvent) => void) {
  return useSse({
    eventTypes: ["printjob:started", "printjob:completed", "printjob:failed"],
    onEvent: (event) => {
      onJobChange?.(event);
    },
  });
}

/**
 * Hook for system announcements.
 */
export function useAnnouncements() {
  const [announcements, setAnnouncements] = useState<SseEvent[]>([]);

  useSse({
    eventTypes: ["system:announcement"],
    onEvent: (event) => {
      if (event.type === "system:announcement") {
        setAnnouncements((prev) => [event, ...prev].slice(0, 50));
      }
    },
  });

  return announcements;
}
