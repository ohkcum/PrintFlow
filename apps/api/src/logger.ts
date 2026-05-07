import pino from "pino";

export function createAppLogger(config: { logLevel: string }) {
  return pino({
    level: config.logLevel,
  });
}
