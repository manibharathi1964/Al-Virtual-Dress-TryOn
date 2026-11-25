import express from "express";
import cors from "cors";
import Fashn from "fashn";
import crypto from "node:crypto";

const app = express();
const port = Number(process.env.PORT || 8080);
const client = new Fashn({ apiKey: process.env.FASHN_API_KEY });
const results = new Map();
const publicBaseUrl = (process.env.PUBLIC_BASE_URL || "").replace(/\\/$/, "");

app.use(cors());
app.use(express.json({ limit: "30mb" }));

app.get("/health", (_req, res) => {
  res.json({ ok: true, service: "ai-virtual-dress-tryon" });
});

app.get("/v1/results/:id", (req, res) => {
  const item = results.get(req.params.id);
  if (!item) return res.status(404).json({ message: "Result expired or not found." });
  const buffer = Buffer.from(item.base64, "base64");
  res.set("Content-Type", "image/jpeg");
  return res.send(buffer);
});

app.post("/v1/tryon", async (req, res) => {
  const { model_image, garment_image, category = "auto", mode = "balanced", instruction = "" } = req.body ?? {};

  if (!model_image || !garment_image) {
    return res.status(400).json({ message: "model_image and garment_image are required." });
  }

  if (!process.env.FASHN_API_KEY) {
    return res.status(503).json({ message: "FASHN_API_KEY is not configured on the server." });
  }

  try {
    const result = await client.predictions.subscribe({
      model_name: "tryon-max",
      inputs: {
        model_image,
        product_image: garment_image,
        prompt: "Apply the garment naturally as a realistic virtual try-on. Preserve the person's face, skin, hair, identity and pose. Keep body proportions correct, preserve the original background where appropriate, and produce natural garment fit, folds, draping, shadows and lighting. Garment category: " + category + ". Additional instruction: " + instruction,
        generation_mode: mode === "quality" ? "quality" : mode === "performance" ? "fast" : "balanced",
        resolution: "1k",
        num_images: 1,
        output_format: "jpeg",
        return_base64: true
      }
    });

    if (result.status !== "completed") {
      return res.status(502).json({
        message: (result.error && result.error.message) || ("Try-on generation ended with status: " + result.status)
      });
    }

    const image = Array.isArray(result.output) ? result.output[0] : null;
    if (!image) return res.status(502).json({ message: "AI service returned no image." });

    return res.json({
      image_data_uri: image,
      provider: "FASHN",
      model: "tryon-max"
    });
  } catch (error) {
    return res.status(502).json({
      message: error?.message || "AI try-on request failed."
    });
  }
});

setInterval(() => {\n  const cutoff = Date.now() - 30 * 60 * 1000;\n  for (const [id, item] of results) if (item.createdAt < cutoff) results.delete(id);\n}, 5 * 60 * 1000);\n\napp.listen(port, () => {
  console.log("AI Virtual Dress Try-On server listening on :" + port);
});
