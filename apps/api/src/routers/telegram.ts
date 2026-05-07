// Telegram Router — mirrors PrintFlowLite's TelegramHelper + ext/telegram/
import type { FastifyInstance, FastifyRequest, FastifyReply } from "fastify";
import { z } from "zod";
import https from "https";

interface TelegramConfig {
  enabled: boolean;
  botToken: string;
  botUsername: string;
}

const config: TelegramConfig = {
  enabled: process.env["TELEGRAM_ENABLED"] === "true",
  botToken: process.env["TELEGRAM_BOT_TOKEN"] ?? "",
  botUsername: process.env["TELEGRAM_BOT_USERNAME"] ?? "",
};

const userTelegramMap: Map<number, string> = new Map();

async function sendTelegramMessage(chatId: string, text: string): Promise<boolean> {
  if (!config.enabled || !config.botToken) {
    console.warn("[Telegram] Not configured or disabled");
    return false;
  }

  const message = encodeURIComponent(text);
  const url = `https://api.telegram.org/bot${config.botToken}/sendMessage?chat_id=${chatId}&text=${message}&parse_mode=HTML`;

  return new Promise((resolve) => {
    https
      .get(url, (res) => {
        let data = "";
        res.on("data", (chunk) => (data += chunk));
        res.on("end", () => {
          try {
            const json = JSON.parse(data);
            resolve(json.ok === true);
          } catch {
            resolve(false);
          }
        });
      })
      .on("error", () => resolve(false));
  });
}

export async function createTelegramRouter(app: FastifyInstance) {
  app.get("/status", async (_request: FastifyRequest, reply: FastifyReply) => {
    return reply.send({
      success: true,
      data: {
        configured: config.enabled && !!config.botToken,
        enabled: config.enabled,
        botUsername: config.botUsername || null,
      },
      timestamp: new Date().toISOString(),
    });
  });

  app.post(
    "/send",
    {
      schema: {
        body: z.object({
          telegramId: z.string().min(1),
          message: z.string().min(1).max(4096),
        }),
      },
    },
    async (
      request: FastifyRequest<{ Body: { telegramId: string; message: string } }>,
      reply: FastifyReply,
    ) => {
      if (!config.enabled) {
        return reply.status(503).send({
          success: false,
          error: { code: "DISABLED", message: "Telegram notifications are disabled" },
        });
      }

      const { telegramId, message } = request.body;
      const sent = await sendTelegramMessage(telegramId, message);

      return reply.send({
        success: sent,
        data: { sent },
        timestamp: new Date().toISOString(),
      });
    },
  );

  app.post(
    "/notify-user",
    {
      schema: {
        body: z.object({
          userId: z.number(),
          message: z.string().min(1).max(4096),
        }),
      },
    },
    async (
      request: FastifyRequest<{ Body: { userId: number; message: string } }>,
      reply: FastifyReply,
    ) => {
      const telegramId = userTelegramMap.get(request.body.userId);
      if (!telegramId) {
        return reply.status(404).send({
          success: false,
          error: { code: "NOT_FOUND", message: "User does not have a Telegram account linked" },
        });
      }

      const sent = await sendTelegramMessage(telegramId, request.body.message);
      return reply.send({ success: sent, timestamp: new Date().toISOString() });
    },
  );

  app.post(
    "/link",
    {
      schema: {
        body: z.object({
          userId: z.number(),
          telegramId: z.string().min(1),
        }),
      },
    },
    async (
      request: FastifyRequest<{ Body: { userId: number; telegramId: string } }>,
      reply: FastifyReply,
    ) => {
      userTelegramMap.set(request.body.userId, request.body.telegramId);
      return reply.send({
        success: true,
        data: { linked: true },
        timestamp: new Date().toISOString(),
      });
    },
  );

  app.delete(
    "/link/:userId",
    async (request: FastifyRequest<{ Params: { userId: string } }>, reply: FastifyReply) => {
      const deleted = userTelegramMap.delete(parseInt(request.params.userId));
      return reply.send({
        success: true,
        data: { unlinked: deleted },
        timestamp: new Date().toISOString(),
      });
    },
  );

  app.post("/webhook", async (request: FastifyRequest, reply: FastifyReply) => {
    const body = request.body as any;
    if (body?.message?.chat?.id && body?.message?.text) {
      const chatId = body.message.chat.id.toString();
      const text = body.message.text;

      if (text.startsWith("/start")) {
        await sendTelegramMessage(chatId, "Welcome to PrintFlow! Send /help for available commands.");
      } else if (text.startsWith("/help")) {
        await sendTelegramMessage(
          chatId,
          "Available commands:\n" +
            "/balance - Check your account balance\n" +
            "/jobs - View recent print jobs\n" +
            "/status - Check printer status\n" +
            "/help - Show this help",
        );
      } else if (text.startsWith("/balance")) {
        await sendTelegramMessage(chatId, "To check your balance, please log in to PrintFlow.");
      }
    }

    return reply.send({ success: true, timestamp: new Date().toISOString() });
  });
}
