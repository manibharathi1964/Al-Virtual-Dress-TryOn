import express from "express";
import cors from "cors";
import Fashn from "fashn";

const app = express();
const port = Number(process.env.PORT || 8080);
const client = new Fashn({ apiKey: process.env.FASHN_API_KEY });

app.use(cors());
app.use(express.json({ limit: "30mb" }));

app.get("/health", (_req, res) => {
  res.json({ ok: true, service: "ai-virtual-dress-tryon" });
});

app.post("/v1/tryon", async (req, res) => {
  const { model_image, garment_image, category = "auto", mode = "balanced" } = req.body ?? {};

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
        prompt: "Apply the garment naturally as a realistic virtual try-on. Preserve the person's face, skin, hair, identity and pose. Keep body proportions correct, preserve the original background where appropriate, and produce natural garment fit, folds, draping, shadows and lighting. Garment category: " + category + ".",
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

app.listen(port, () => {
  console.log("AI Virtual Dress Try-On server listening on :" + port);
});
