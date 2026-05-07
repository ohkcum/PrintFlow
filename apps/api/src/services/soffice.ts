// LibreOffice / SOffice Service — mirrors PrintFlowLite's SOfficeService
// Converts documents to PDF using LibreOffice

import { spawn } from "child_process";
import { promises as fs } from "fs";
import * as path from "path";
import * as os from "os";

export interface SOfficeConfig {
  sofficePath: string;
  numWorkers: number;
  taskTimeoutMs: number;
  enabled: boolean;
  unoUrls: string[];
}

export interface SOfficeTask {
  inputPath: string;
  outputPath: string;
  outputFormat: "pdf" | "docx" | "odt" | "rtf" | "txt";
  options?: {
    pageRange?: string;
    quality?: "draft" | "normal" | "high";
    pdfa?: boolean;
    encrypt?: boolean;
    password?: string;
    landscape?: boolean;
    paperSize?: string;
  };
}

export interface SOfficeResult {
  success: boolean;
  outputPath: string;
  pageCount?: number;
  fileSize?: number;
  error?: string;
}

let sofficeRunning = false;
let currentConfig: SOfficeConfig | null = null;

export async function startSOfficeService(
  config: SOfficeConfig,
): Promise<void> {
  if (!config.enabled) {
    console.log("SOffice service is disabled");
    return;
  }

  // Validate LibreOffice is available
  try {
    await fs.access(config.sofficePath);
  } catch {
    throw new Error(
      `LibreOffice not found at ${config.sofficePath}. Please install LibreOffice.`,
    );
  }

  currentConfig = config;
  sofficeRunning = true;
  console.log(
    `SOffice service started with ${config.numWorkers} worker(s)`,
  );
}

export function isSOfficeRunning(): boolean {
  return sofficeRunning;
}

export function isSOfficeEnabled(): boolean {
  return currentConfig?.enabled ?? false;
}

export async function executeSOfficeTask(
  task: SOfficeTask,
): Promise<SOfficeResult> {
  if (!currentConfig?.enabled || !sofficeRunning) {
    return { success: false, outputPath: "", error: "SOffice service is not running" };
  }

  const config = currentConfig;

  return new Promise((resolve) => {
    const timeout = setTimeout(() => {
      cleanup();
      resolve({
        success: false,
        outputPath: task.outputPath,
        error: `Task timeout after ${config.taskTimeoutMs}ms`,
      });
    }, config.taskTimeoutMs);

    const args = buildLibreOfficeArgs(task);

    const proc = spawn(config.sofficePath, args, {
      stdio: ["ignore", "pipe", "pipe"],
      windowsHide: true,
    });

    let stderr = "";

    proc.stderr?.on("data", (data) => {
      stderr += data.toString();
    });

    proc.on("error", (err) => {
      clearTimeout(timeout);
      cleanup();
      resolve({
        success: false,
        outputPath: task.outputPath,
        error: `LibreOffice error: ${err.message}`,
      });
    });

    proc.on("close", async (code) => {
      clearTimeout(timeout);

      if (code !== 0) {
        cleanup();
        resolve({
          success: false,
          outputPath: task.outputPath,
          error: `LibreOffice exited with code ${code}: ${stderr}`,
        });
        return;
      }

      try {
        const stats = await fs.stat(task.outputPath);
        resolve({
          success: true,
          outputPath: task.outputPath,
          fileSize: stats.size,
        });
      } catch {
        resolve({
          success: false,
          outputPath: task.outputPath,
          error: "Output file not found after conversion",
        });
      }
    });

    function cleanup() {
      try {
        proc.kill();
      } catch {}
    }
  });
}

function buildLibreOfficeArgs(task: SOfficeTask): string[] {
  const args = [
    "--headless",
    "--convert-to",
    task.outputFormat,
    "--outdir",
    path.dirname(task.outputPath),
    task.inputPath,
  ];

  if (task.options?.pdfa) {
    // PDF/A export
    args.push("pdf:a");
  }

  if (task.options?.quality === "draft") {
    args.push("--env:UserInstallation=file:///tmp/libreoffice-temp");
  }

  return args;
}

export async function shutdownSOfficeService(): Promise<void> {
  sofficeRunning = false;
  currentConfig = null;
  console.log("SOffice service stopped");
}

// ─── Document Conversion Helpers ────────────────────────────────────────────────

export async function convertToPdf(
  inputPath: string,
  outputDir: string,
  options?: SOfficeTask["options"],
): Promise<SOfficeResult> {
  const filename = path.basename(inputPath, path.extname(inputPath));
  const outputPath = path.join(outputDir, `${filename}.pdf`);

  return executeSOfficeTask({
    inputPath,
    outputPath,
    outputFormat: "pdf",
    options: {
      quality: "normal",
      ...options,
    },
  });
}

export async function convertToPdfBatch(
  tasks: Array<{ inputPath: string; outputDir: string; options?: SOfficeTask["options"] }>,
): Promise<SOfficeResult[]> {
  return Promise.all(
    tasks.map((t) => convertToPdf(t.inputPath, t.outputDir, t.options)),
  );
}

// ─── PDF Processing ────────────────────────────────────────────────────────────

export interface PdfProcessingOptions {
  removeGraphics?: boolean;
  ecoPrint?: boolean;
  grayscale?: boolean;
  rasterize?: boolean;
  encrypt?: boolean;
  userPassword?: string;
  ownerPassword?: string;
  compress?: boolean;
  compressionQuality?: "low" | "medium" | "high";
  pageRange?: string;
}

export async function processPdf(
  inputPath: string,
  outputPath: string,
  options: PdfProcessingOptions,
): Promise<{ success: boolean; outputPath: string; error?: string }> {
  // For PDF processing, in production you'd use a library like pdf-lib, pdfkit, or call external tools
  // This is a placeholder that copies the file
  try {
    await fs.copyFile(inputPath, outputPath);
    return { success: true, outputPath };
  } catch (err: any) {
    return { success: false, outputPath, error: err.message };
  }
}

// ─── Job Sheet PDF Creation ────────────────────────────────────────────────────

export async function createJobTicketSheetPdf(
  ticketNumber: string,
  userName: string,
  operatorName: string,
  printerName: string,
  copies: number,
  outputPath: string,
): Promise<{ success: boolean; outputPath: string; error?: string }> {
  // For job sheets, in production you'd use react-pdf or pdfkit
  // This creates a simple text placeholder
  const content = [
    "═══════════════════════════════════════════════",
    "           PRINTFLOW - JOB TICKET",
    "═══════════════════════════════════════════════",
    "",
    `Ticket Number: ${ticketNumber}`,
    `User: ${userName}`,
    `Operator: ${operatorName}`,
    `Printer: ${printerName}`,
    `Copies: ${copies}`,
    `Date: ${new Date().toLocaleString()}`,
    "",
    "═══════════════════════════════════════════════",
    "",
    "        Please present this ticket",
    "          at the print station",
    "",
  ].join("\n");

  try {
    await fs.writeFile(outputPath.replace(".pdf", ".txt"), content);
    return { success: true, outputPath };
  } catch (err: any) {
    return { success: false, outputPath, error: err.message };
  }
}

// ─── Temp File Management ──────────────────────────────────────────────────────

const tempFiles: Set<string> = new Set();

export function registerTempFile(filePath: string): void {
  tempFiles.add(filePath);
}

export async function cleanupTempFiles(): Promise<void> {
  for (const file of tempFiles) {
    try {
      await fs.unlink(file);
      tempFiles.delete(file);
    } catch {}
  }
}

export async function createTempDir(): Promise<string> {
  const tmpDir = path.join(os.tmpdir(), `printflow-${Date.now()}`);
  await fs.mkdir(tmpDir, { recursive: true });
  return tmpDir;
}
