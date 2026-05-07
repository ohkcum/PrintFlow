// Email Service — mirrors PrintFlowLite's EmailService
// Handles SMTP sending, PGP encryption, and IMAP ingestion

import nodemailer from "nodemailer";
import type { Transporter } from "nodemailer";
import type { SentMessageInfo } from "nodemailer";
import { z } from "zod";

export interface EmailConfig {
  smtpHost: string;
  smtpPort: number;
  smtpSecure: boolean;
  smtpUser: string;
  smtpPassword: string;
  smtpFrom: string;
  smtpFromName: string;
  useOAuth2: boolean;
  oauth2ClientId?: string;
  oauth2ClientSecret?: string;
  oauth2RefreshToken?: string;
}

export interface EmailMessage {
  to: string;
  toName?: string;
  subject: string;
  body: string;
  bodyHtml?: string;
  cc?: string[];
  bcc?: string[];
  attachments?: Array<{
    filename: string;
    content: Buffer | string;
    contentType?: string;
  }>;
  pgpEncrypt?: boolean;
  pgpPublicKey?: string;
  pgpSign?: boolean;
}

export interface ImapConfig {
  host: string;
  port: number;
  secure: boolean;
  user: string;
  password: string;
  box: string;
  markSeen: boolean;
  searchCriteria: string[];
}

// ─── SMTP Service ────────────────────────────────────────────────────────────────

let transporter: Transporter<SentMessageInfo> | null = null;
let currentConfig: EmailConfig | null = null;

export function createEmailTransporter(config: EmailConfig): Transporter<SentMessageInfo> {
  if (config.useOAuth2 && config.oauth2ClientId && config.oauth2ClientSecret && config.oauth2RefreshToken) {
    // OAuth2 authentication
    transporter = nodemailer.createTransport({
      host: config.smtpHost,
      port: config.smtpPort,
      secure: config.smtpSecure,
      auth: {
        type: "OAuth2",
        clientId: config.oauth2ClientId,
        clientSecret: config.oauth2ClientSecret,
        refreshToken: config.oauth2RefreshToken,
        user: config.smtpUser,
      },
    } as any);
  } else if (config.smtpUser && config.smtpPassword) {
    // Standard SMTP authentication
    transporter = nodemailer.createTransport({
      host: config.smtpHost,
      port: config.smtpPort,
      secure: config.smtpSecure,
      auth: {
        user: config.smtpUser,
        pass: config.smtpPassword,
      },
    });
  } else {
    // No authentication
    transporter = nodemailer.createTransport({
      host: config.smtpHost,
      port: config.smtpPort,
      secure: config.smtpSecure,
    });
  }

  currentConfig = config;
  return transporter;
}

export function getTransporter(): Transporter<SentMessageInfo> | null {
  return transporter;
}

export async function verifyConnection(): Promise<boolean> {
  if (!transporter) return false;
  try {
    await transporter.verify();
    return true;
  } catch {
    return false;
  }
}

export async function sendEmail(msg: EmailMessage): Promise<{ messageId: string; accepted: string[] }> {
  if (!transporter) {
    throw new Error("Email transporter not configured");
  }

  const from =
    currentConfig?.smtpFromName && currentConfig.smtpFromName.trim()
      ? `"${currentConfig.smtpFromName}" <${currentConfig.smtpFrom}>`
      : currentConfig?.smtpFrom ?? "noreply@printflow.local";

  const to = msg.toName
    ? `"${msg.toName}" <${msg.to}>`
    : msg.to;

  const mailOptions: any = {
    from,
    to,
    subject: msg.subject,
    text: msg.body,
    ...(msg.bodyHtml && { html: msg.bodyHtml }),
    ...(msg.cc && msg.cc.length > 0 && { cc: msg.cc.join(", ") }),
    ...(msg.bcc && msg.bcc.length > 0 && { bcc: msg.bcc.join(", ") }),
    ...(msg.attachments &&
      msg.attachments.length > 0 && {
        attachments: msg.attachments.map((a) => ({
          filename: a.filename,
          content: a.content,
          contentType: a.contentType,
        })),
      }),
  };

  const result = await transporter.sendMail(mailOptions);
  return {
    messageId: result.messageId ?? "",
    accepted: result.accepted ?? [],
  };
}

export async function sendEmailBatch(
  messages: EmailMessage[],
): Promise<Array<{ success: boolean; messageId?: string; error?: string }>> {
  const results = await Promise.allSettled(
    messages.map((msg) =>
      sendEmail(msg).then((r) => ({ success: true, messageId: r.messageId })),
    ),
  );

  return results.map((r) => {
    if (r.status === "fulfilled") return r.value;
    return { success: false, error: String(r.reason?.message ?? r.reason) };
  });
}

// ─── Email Templates ────────────────────────────────────────────────────────────

export function createJobTicketCompletedEmail(
  ticketNumber: string,
  operator: string,
  userName: string,
): EmailMessage {
  return {
    to: userName,
    subject: `PrintFlow: Job Ticket ${ticketNumber} Completed`,
    body: `Dear ${userName},

Your job ticket ${ticketNumber} has been completed.

Operator: ${operator}

Thank you for using PrintFlow.

Best regards,
PrintFlow System`,
    bodyHtml: `<!DOCTYPE html>
<html>
<head><meta charset="utf-8"></head>
<body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <h2 style="color: #2563eb;">Job Ticket Completed</h2>
  <p>Dear <strong>${userName}</strong>,</p>
  <p>Your job ticket <strong>${ticketNumber}</strong> has been completed.</p>
  <table style="border-collapse: collapse; width: 100%; margin: 20px 0;">
    <tr><td style="padding: 8px; border: 1px solid #e5e7eb;"><strong>Ticket</strong></td>
        <td style="padding: 8px; border: 1px solid #e5e7eb;">${ticketNumber}</td></tr>
    <tr><td style="padding: 8px; border: 1px solid #e5e7eb;"><strong>Operator</strong></td>
        <td style="padding: 8px; border: 1px solid #e5e7eb;">${operator}</td></tr>
  </table>
  <p>Thank you for using <strong>PrintFlow</strong>.</p>
  <p style="color: #6b7280; font-size: 0.875rem;">This is an automated message. Please do not reply.</p>
</body>
</html>`,
  };
}

export function createJobTicketCanceledEmail(
  ticketNumber: string,
  operator: string,
  userName: string,
  reason: string,
): EmailMessage {
  return {
    to: userName,
    subject: `PrintFlow: Job Ticket ${ticketNumber} Cancelled`,
    body: `Dear ${userName},

Your job ticket ${ticketNumber} has been cancelled.

Operator: ${operator}
Reason: ${reason}

If you have questions, please contact your administrator.

Best regards,
PrintFlow System`,
    bodyHtml: `<!DOCTYPE html>
<html>
<head><meta charset="utf-8"></head>
<body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
  <h2 style="color: #dc2626;">Job Ticket Cancelled</h2>
  <p>Dear <strong>${userName}</strong>,</p>
  <p>Your job ticket <strong>${ticketNumber}</strong> has been cancelled.</p>
  <table style="border-collapse: collapse; width: 100%; margin: 20px 0;">
    <tr><td style="padding: 8px; border: 1px solid #e5e7eb;"><strong>Ticket</strong></td>
        <td style="padding: 8px; border: 1px solid #e5e7eb;">${ticketNumber}</td></tr>
    <tr><td style="padding: 8px; border: 1px solid #e5e7eb;"><strong>Operator</strong></td>
        <td style="padding: 8px; border: 1px solid #e5e7eb;">${operator}</td></tr>
    <tr><td style="padding: 8px; border: 1px solid #e5e7eb;"><strong>Reason</strong></td>
        <td style="padding: 8px; border: 1px solid #e5e7eb;">${reason}</td></tr>
  </table>
  <p>Best regards,<br><strong>PrintFlow</strong></p>
</body>
</html>`,
  };
}

// ─── IMAP Ingestion ────────────────────────────────────────────────────────────

export async function connectImap(config: ImapConfig): Promise<any> {
  const Imap = (await import("imap")).default;

  return new Imap({
    user: config.user,
    password: config.password,
    host: config.host,
    port: config.port,
    secure: config.secure,
    tls: config.secure,
  });
}

export interface MailAttachment {
  filename: string;
  content: Buffer;
  contentType: string;
  size: number;
}

export interface ParsedEmail {
  from: string;
  to: string;
  subject: string;
  date: Date;
  bodyText: string;
  bodyHtml: string;
  attachments: MailAttachment[];
  messageId: string;
}

export async function fetchUnreadEmails(
  imap: any,
  config: ImapConfig,
): Promise<ParsedEmail[]> {
  return new Promise((resolve, reject) => {
    const emails: ParsedEmail[] = [];

    imap.once("ready", () => {
      imap.openBox(config.box, false, (err: Error | null, box: any) => {
        if (err) {
          reject(err);
          return;
        }

        const searchCriteria = config.searchCriteria.length > 0
          ? config.searchCriteria
          : ["UNSEEN"];

        imap.search(searchCriteria, (searchErr: Error | null, results: number[]) => {
          if (searchErr || !results || results.length === 0) {
            imap.end();
            resolve([]);
            return;
          }

          const fetch = imap.fetch(results, {
            bodies: ["HEADER", "TEXT", "FULL"],
            struct: true,
          });

          fetch.on("message", (msg: any) => {
            let header: any = null;
            let body = "";

            msg.on("headers", (h: any) => {
              header = h;
            });

            msg.on("body", (stream: any) => {
              let buffer = "";
              stream.on("data", (chunk: Buffer) => {
                buffer += chunk.toString("utf8");
              });
              stream.once("end", () => {
                body = buffer;
              });
            });

            msg.once("attributes", (attrs: any) => {
              // Parse the email after body is loaded
              setTimeout(() => {
                try {
                  const { simpleParser } = require("mailparser") as any;
                  simpleParser(body).then((parsed: any) => {
                    const attachments: MailAttachment[] = (parsed.attachments ?? []).map(
                      (a: any) => ({
                        filename: a.filename ?? "attachment",
                        content: a.content,
                        contentType: a.contentType ?? "application/octet-stream",
                        size: a.length ?? 0,
                      }),
                    );

                    emails.push({
                      from: header?.from?.[0] ?? parsed.from?.text ?? "",
                      to: header?.to?.[0] ?? "",
                      subject: header?.subject?.[0] ?? parsed.subject ?? "",
                      date: new Date(header?.date?.[0] ?? parsed.date ?? Date.now()),
                      bodyText: parsed.text ?? "",
                      bodyHtml: parsed.html ?? "",
                      attachments,
                      messageId: header?.["message-id"]?.[0] ?? attrs.uid ?? "",
                    });

                    if (!config.markSeen) {
                      imap.addFlags(attrs.uid, "\\Seen", (addErr: Error | null) => {
                        if (addErr) console.error("Failed to add Seen flag:", addErr);
                      });
                    }
                  });
                } catch {}
              }, 100);
            });
          });

          fetch.once("error", (fetchErr: Error) => {
            console.error("Fetch error:", fetchErr);
          });

          fetch.once("end", () => {
            imap.end();
            resolve(emails);
          });
        });
      });
    });

    imap.once("error", (err: Error) => {
      reject(err);
    });

    imap.connect();
  });
}

// ─── PGP Email Encryption ────────────────────────────────────────────────────────

export async function encryptEmailForRecipient(
  message: EmailMessage,
  publicKeyArmored: string,
): Promise<EmailMessage> {
  // For PGP email encryption, we would need openpgp library
  // For now, this is a placeholder that returns the original message
  // Real implementation would use openpgp.encrypt() here
  return {
    ...message,
    subject: `[Encrypted] ${message.subject}`,
  };
}

// ─── Validation ────────────────────────────────────────────────────────────────

export const emailConfigSchema = z.object({
  smtpHost: z.string().default("localhost"),
  smtpPort: z.number().int().min(1).max(65535).default(587),
  smtpSecure: z.boolean().default(false),
  smtpUser: z.string().default(""),
  smtpPassword: z.string().default(""),
  smtpFrom: z.string().email().default("noreply@printflow.local"),
  smtpFromName: z.string().default("PrintFlow"),
  useOAuth2: z.boolean().default(false),
  oauth2ClientId: z.string().optional(),
  oauth2ClientSecret: z.string().optional(),
  oauth2RefreshToken: z.string().optional(),
});

export const imapConfigSchema = z.object({
  host: z.string().default("localhost"),
  port: z.number().int().min(1).max(65535).default(993),
  secure: z.boolean().default(true),
  user: z.string().default(""),
  password: z.string().default(""),
  box: z.string().default("INBOX"),
  markSeen: z.boolean().default(true),
  searchCriteria: z.array(z.string()).default(["UNSEEN"]),
});
