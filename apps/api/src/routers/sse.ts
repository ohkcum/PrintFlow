// SSE Router — real-time events (port from CometD/BayeauInitializer.java)
import type { FastifyInstance } from "fastify";

// Event types for SSE stream
export type SseEventType =
  | "document:created"
  | "document:updated"
  | "document:deleted"
  | "printjob:started"
  | "printjob:completed"
  | "printjob:failed"
  | "printer:status_changed"
  | "user:login"
  | "user:logout"
  | "system:announcement"
  | "heartbeat";

export interface SseEvent {
  type: SseEventType;
  data: Record<string, unknown>;
  timestamp: string;
}

// Global event emitter for broadcasting events across SSE connections
type Listener = (event: SseEvent) => void;

class SseEventBus {
  private listeners: Set<Listener> = new Set();

  subscribe(listener: Listener): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  broadcast(event: SseEvent): void {
    for (const listener of this.listeners) {
      try {
        listener(event);
      } catch {
        // Remove broken listeners
        this.listeners.delete(listener);
      }
    }
  }

  get listenerCount(): number {
    return this.listeners.size;
  }
}

// Singleton event bus (per server instance)
const eventBus = new SseEventBus();

// ─── Public API: broadcast events from other routers ──────────────────────────────

export function broadcastSseEvent(type: SseEventType, data: Record<string, unknown> = {}): void {
  eventBus.broadcast({
    type,
    data,
    timestamp: new Date().toISOString(),
  });
}

// ─── SSE Router ─────────────────────────────────────────────────────────────────

export async function createSseRouter(app: FastifyInstance) {
  // GET /api/v1/events — SSE stream
  app.get("/", async (request, reply) => {
    const userId = request.userId;

    // Optionally support unauthenticated streams for public kiosks
    if (!userId && request.roles?.includes("ADMIN")) {
      // Admin can monitor without userId
    } else if (!userId) {
      // For unauthenticated, use PRINT_SITE_USER check via header
      const cardId = request.headers["x-printflow-cardid"] as string | undefined;
      if (!cardId) {
        return reply.status(401).send({
          success: false,
          error: { code: "UNAUTHORIZED", message: "Authentication required" },
        });
      }
    }

    // Set SSE headers
    reply.raw.writeHead(200, {
      "Content-Type": "text/event-stream",
      "Cache-Control": "no-cache, no-transform",
      "Connection": "keep-alive",
      "X-Accel-Buffering": "no", // Disable nginx buffering
      "Access-Control-Allow-Origin": "*",
    });

    // Send initial connection event
    const connectEvent: SseEvent = {
      type: "heartbeat",
      data: {
        message: "connected",
        userId: userId ?? null,
        serverTime: new Date().toISOString(),
      },
      timestamp: new Date().toISOString(),
    };
    reply.raw.write(`event: connected\ndata: ${JSON.stringify(connectEvent)}\n\n`);

    // Subscribe to event bus
    const unsubscribe = eventBus.subscribe((event) => {
      // Filter: only send events relevant to this user (or admin events)
      const isAdmin = request.roles?.includes("ADMIN") ?? false;
      const isManager = request.roles?.includes("MANAGER") ?? false;

      // Admins/managers get all events
      if (isAdmin || isManager) {
        reply.raw.write(`event: ${event.type}\ndata: ${JSON.stringify(event)}\n\n`);
        return;
      }

      // Regular users only get their own document events
      if (event.type.startsWith("document:") || event.type.startsWith("printjob:")) {
        const eventUserId = event.data["userId"] as number | undefined;
        if (eventUserId === userId) {
          reply.raw.write(`event: ${event.type}\ndata: ${JSON.stringify(event)}\n\n`);
        }
        return;
      }

      // Printer events visible to all authenticated users
      if (event.type.startsWith("printer:")) {
        reply.raw.write(`event: ${event.type}\ndata: ${JSON.stringify(event)}\n\n`);
      }
    });

    // Heartbeat every 30 seconds to keep connection alive
    const heartbeat = setInterval(() => {
      if (reply.raw.writableEnded) {
        clearInterval(heartbeat);
        return;
      }
      try {
        reply.raw.write(`: heartbeat\n\n`);
      } catch {
        clearInterval(heartbeat);
      }
    }, 30000);

    // Cleanup on disconnect
    request.raw.on("close", () => {
      clearInterval(heartbeat);
      unsubscribe();
    });

    // Keep connection alive until client disconnects
    await new Promise<void>((resolve) => {
      request.raw.on("close", resolve);
      request.raw.on("error", resolve);
    });
  });

  // POST /api/v1/events/broadcast — admin-only: broadcast a system announcement
  app.post("/broadcast", async (request, reply) => {
    if (!request.roles?.includes("ADMIN")) {
      return reply.status(403).send({
        success: false,
        error: { code: "FORBIDDEN", message: "Admin role required" },
      });
    }

    const schema = {
      type: "object",
      required: ["message"],
      properties: {
        message: { type: "string", minLength: 1, maxLength: 500 },
        level: { type: "string", enum: ["info", "warning", "error"], default: "info" },
      },
    };

    const body = schema as any;
    if (!body || !body.message) {
      return reply.status(400).send({
        success: false,
        error: { code: "VALIDATION_ERROR", message: "message is required" },
      });
    }

    broadcastSseEvent("system:announcement", {
      message: body.message,
      level: body.level ?? "info",
      sentBy: (request as any).userName ?? "admin",
    });

    return reply.send({
      success: true,
      data: { listeners: eventBus.listenerCount },
      timestamp: new Date().toISOString(),
    });
  });

  // GET /api/v1/events/status — get SSE connection status (admin)
  app.get("/status", async (request, reply) => {
    if (!request.roles?.includes("ADMIN")) {
      return reply.status(403).send({
        success: false,
        error: { code: "FORBIDDEN", message: "Admin role required" },
      });
    }

    return reply.send({
      success: true,
      data: {
        activeConnections: eventBus.listenerCount,
        serverTime: new Date().toISOString(),
      },
      timestamp: new Date().toISOString(),
    });
  });
}
