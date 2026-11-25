# Phase Status

## P2 — Input Quality
Status: Completed implementation on this branch.

- Photo resolution, brightness and sharpness validation.
- ML Kit accurate pose detection.
- ML Kit selfie segmentation foreground analysis.
- Person validation before AI generation.
- Garment resolution/aspect validation.
- Garment image labeling for category hints.
- Dominant garment color detection.
- Lightweight garment background isolation before AI submission.
- Clear validation feedback and retry path in the Try-On UI.

## P3 — Result Quality
Status: Completed implementation on this branch.

- AI request uses quality generation mode.
- Server prompt explicitly requests identity, face/skin/hair, pose and proportion preservation.
- Natural fit, folds, draping, shadows and lighting are requested from the AI provider.
- Original background preservation is requested where appropriate.
- Client-side result decoding and resolution/detail validation rejects unusable outputs.
- Failed/low-quality generation returns a clear retryable error.

Important boundary: final visual realism is still dependent on the configured AI inference provider and its model output. The Android app now enforces the input/output quality gates and sends explicit quality-preservation instructions.

## P4 — Store Catalogue
Status: Completed implementation on phase-4-store-catalogue.

- Product image, code, name, category and price.
- Variant colors and sizes.
- Per-size stock status and overall availability.
- Search by product name/code.
- Category filtering.
- Catalogue product → Try On integration.
- Out-of-stock products cannot be sent to Try On.

## Next
P5 — Customer Workflow:
- Product image
- Product code
- Product name
- Category
- Price
- Available colors
- Available sizes
- Stock status
- Product variants
- Catalogue → Try On
