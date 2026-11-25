import { spawn } from "node:child_process";

const port = "18080";
const child = spawn(process.execPath, ["src/index.mjs"], {
  cwd: new URL(".", import.meta.url).pathname,
  env: { ...process.env, PORT: port, FASHN_API_KEY: "" },
  stdio: ["ignore", "pipe", "pipe"]
});

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
const fail = (message) => {
  child.kill("SIGTERM");
  console.error(message);
  process.exit(1);
};

try {
  await sleep(1200);

  const health = await fetch("http://127.0.0.1:" + port + "/health");
  if (!health.ok) fail("Health endpoint failed: HTTP " + health.status);
  const healthBody = await health.json();
  if (healthBody.ok !== true) fail("Health endpoint returned an invalid payload.");

  const missingKey = await fetch("http://127.0.0.1:" + port + "/v1/tryon", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      model_image: "data:image/jpeg;base64,AA==",
      garment_image: "data:image/jpeg;base64,AA=="
    })
  });

  if (missingKey.status !== 503) {
    fail("Missing FASHN_API_KEY guard failed: expected HTTP 503, got HTTP " + missingKey.status);
  }

  const missingKeyBody = await missingKey.json();
  if (!String(missingKeyBody.message || "").includes("FASHN_API_KEY")) {
    fail("Missing FASHN_API_KEY response did not identify the configuration problem.");
  }

  console.log("Server smoke test passed.");
} catch (error) {
  fail("Server smoke test failed: " + (error?.stack || error));
} finally {
  child.kill("SIGTERM");
}
