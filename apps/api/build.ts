// Build script for API
import { createApp } from "./src/index.js";
import { getAppConfig } from "@printflow/common";

const config = getAppConfig();
const app = await createApp(config);

try {
  const address = await app.listen({ port: config.port, host: "0.0.0.0" });
  console.log(`PrintFlow API running at ${address}`);
} catch (err) {
  app.log.error(err);
  process.exit(1);
}

process.on("SIGINT", async () => {
  await app.close();
  process.exit(0);
});
